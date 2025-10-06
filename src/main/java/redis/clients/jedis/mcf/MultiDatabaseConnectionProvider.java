package redis.clients.jedis.mcf;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics;
import io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import java.util.function.Consumer;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.*;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.VisibleForTesting;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.mcf.JedisFailoverException.*;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.MultiDbConfig.StrategySupplier;
import redis.clients.jedis.util.Pool;

/**
 * @author Allen Terleto (aterleto)
 *         <p>
 *         ConnectionProvider which supports multiple cluster/database endpoints each with their own
 *         isolated connection pool. With this ConnectionProvider users can seamlessly failover to
 *         Disaster Recovery (DR), Backup, and Active-Active cluster(s) by using simple
 *         configuration which is passed through from Resilience4j -
 *         <a href="https://resilience4j.readme.io/docs">docs</a>
 *         <p>
 *         Support for manual failback is provided by way of {@link #setActiveDatabase(Endpoint)}
 *         <p>
 */
@Experimental
public class MultiDatabaseConnectionProvider implements ConnectionProvider {

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Ordered map of database. Users can move down (failover) or (up) failback the map depending on
   * their availability and order.
   */
  private final Map<Endpoint, Database> databaseMap = new ConcurrentHashMap<>();

  /**
   * Indicates the actively used database endpoint (connection pool) amongst the pre-configured list
   * which were provided at startup via the MultiDbConfig. All traffic will be routed with
   * this database
   */
  private volatile Database activeDatabase;

  private final Lock activeDatabaseChangeLock = new ReentrantLock(true);

  /**
   * Functional interface for listening to cluster switch events. The event args contain the reason
   * for the switch, the endpoint, and the cluster.
   */
  private Consumer<ClusterSwitchEventArgs> databaseSwitchListener;

  private List<Class<? extends Throwable>> fallbackExceptionList;

  private HealthStatusManager healthStatusManager = new HealthStatusManager();

  // Flag to control when handleHealthStatusChange should process events (only after initialization)
  private volatile boolean initializationComplete = false;

  // Failback mechanism fields
  private static final AtomicInteger failbackThreadCounter = new AtomicInteger(1);
  private final ScheduledExecutorService failbackScheduler = Executors
      .newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "jedis-failback-" + failbackThreadCounter.getAndIncrement());
        t.setDaemon(true);
        return t;
      });

  // Store retry and circuit breaker configs for dynamic database addition/removal
  private RetryConfig retryConfig;
  private CircuitBreakerConfig circuitBreakerConfig;
  private MultiDbConfig multiDbConfig;

  private AtomicLong failoverFreezeUntil = new AtomicLong(0);
  private AtomicInteger failoverAttemptCount = new AtomicInteger(0);

  public MultiDatabaseConnectionProvider(MultiDbConfig multiDbConfig) {

    if (multiDbConfig == null) throw new JedisValidationException(
        "MultiDbConfig must not be NULL for MultiDatabaseConnectionProvider");

    this.multiDbConfig = multiDbConfig;

    ////////////// Configure Retry ////////////////////

    RetryConfig.Builder retryConfigBuilder = RetryConfig.custom();
    retryConfigBuilder.maxAttempts(multiDbConfig.getRetryMaxAttempts());
    retryConfigBuilder.intervalFunction(
      IntervalFunction.ofExponentialBackoff(multiDbConfig.getRetryWaitDuration(),
        multiDbConfig.getRetryWaitDurationExponentialBackoffMultiplier()));
    retryConfigBuilder.failAfterMaxAttempts(false); // JedisConnectionException will be thrown
    retryConfigBuilder.retryExceptions(
      multiDbConfig.getRetryIncludedExceptionList().stream().toArray(Class[]::new));

    List<Class> retryIgnoreExceptionList = multiDbConfig.getRetryIgnoreExceptionList();
    if (retryIgnoreExceptionList != null)
      retryConfigBuilder.ignoreExceptions(retryIgnoreExceptionList.stream().toArray(Class[]::new));

    this.retryConfig = retryConfigBuilder.build();

    ////////////// Configure Circuit Breaker ////////////////////

    CircuitBreakerConfig.Builder circuitBreakerConfigBuilder = CircuitBreakerConfig.custom();

    CircuitBreakerThresholdsAdapter adapter = new CircuitBreakerThresholdsAdapter(
        multiDbConfig);
    circuitBreakerConfigBuilder.minimumNumberOfCalls(adapter.getMinimumNumberOfCalls());
    circuitBreakerConfigBuilder.failureRateThreshold(adapter.getFailureRateThreshold());
    circuitBreakerConfigBuilder.slidingWindowSize(adapter.getSlidingWindowSize());
    circuitBreakerConfigBuilder.slidingWindowType(adapter.getSlidingWindowType());

    circuitBreakerConfigBuilder.recordExceptions(
      multiDbConfig.getCircuitBreakerIncludedExceptionList().stream().toArray(Class[]::new));
    circuitBreakerConfigBuilder.automaticTransitionFromOpenToHalfOpenEnabled(false); // State
                                                                                     // transitions
                                                                                     // are
                                                                                     // forced. No
                                                                                     // half open
                                                                                     // states
                                                                                     // are used

    List<Class> circuitBreakerIgnoreExceptionList = multiDbConfig
        .getCircuitBreakerIgnoreExceptionList();
    if (circuitBreakerIgnoreExceptionList != null) circuitBreakerConfigBuilder
        .ignoreExceptions(circuitBreakerIgnoreExceptionList.stream().toArray(Class[]::new));

    this.circuitBreakerConfig = circuitBreakerConfigBuilder.build();

    ////////////// Configure Database Map ////////////////////

    DatabaseConfig[] databaseConfigs = multiDbConfig.getDatabaseConfigs();

    // Now add databases - health checks will start but events will be queued
    for (DatabaseConfig config : databaseConfigs) {
      addClusterInternal(multiDbConfig, config);
    }

    // Initialize StatusTracker for waiting on health check results
    StatusTracker statusTracker = new StatusTracker(healthStatusManager);

    // Wait for initial health check results and select active cluster based on weights
    activeDatabase = waitForInitialHealthyCluster(statusTracker);

    // Mark initialization as complete - handleHealthStatusChange can now process events
    initializationComplete = true;
    Database temp = activeDatabase;
    if (!temp.isHealthy()) {
      // Race condition: Direct assignment to 'activeDatabase' is not thread safe because
      // 'onHealthStatusChange' may execute concurrently once 'initializationComplete'
      // is set to true.
      // Simple rule is to never assign value of 'activeDatabase' outside of
      // 'activeDatabaseChangeLock' once the 'initializationComplete' is done.
      waitForInitialHealthyCluster(statusTracker);
      switchToHealthyDatabase(SwitchReason.HEALTH_CHECK, temp);
    }
    this.fallbackExceptionList = multiDbConfig.getFallbackExceptionList();

    // Start periodic failback checker
    if (multiDbConfig.isFailbackSupported()) {
      long failbackInterval = multiDbConfig.getFailbackCheckInterval();
      failbackScheduler.scheduleAtFixedRate(this::periodicFailbackCheck, failbackInterval,
        failbackInterval, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Adds a new cluster endpoint to the provider.
   * @param databaseConfig the configuration for the new database
   * @throws JedisValidationException if the endpoint already exists
   */
  public void add(DatabaseConfig databaseConfig) {
    if (databaseConfig == null) {
      throw new JedisValidationException("DatabaseConfig must not be null");
    }

    Endpoint endpoint = databaseConfig.getEndpoint();
    if (databaseMap.containsKey(endpoint)) {
      throw new JedisValidationException(
          "Endpoint " + endpoint + " already exists in the provider");
    }

    activeDatabaseChangeLock.lock();
    try {
      addClusterInternal(multiDbConfig, databaseConfig);
    } finally {
      activeDatabaseChangeLock.unlock();
    }
  }

  /**
   * Removes a cluster endpoint from the provider.
   * @param endpoint the endpoint to remove
   * @throws JedisValidationException if the endpoint doesn't exist or is the last remaining
   *           endpoint
   */
  public void remove(Endpoint endpoint) {
    if (endpoint == null) {
      throw new JedisValidationException("Endpoint must not be null");
    }

    if (!databaseMap.containsKey(endpoint)) {
      throw new JedisValidationException(
          "Endpoint " + endpoint + " does not exist in the provider");
    }

    if (databaseMap.size() < 2) {
      throw new JedisValidationException("Cannot remove the last remaining endpoint");
    }
    log.debug("Removing endpoint {}", endpoint);

    Map.Entry<Endpoint, Database> notificationData = null;
    activeDatabaseChangeLock.lock();
    try {
      Database databaseToRemove = databaseMap.get(endpoint);
      boolean isActiveDatabase = (activeDatabase == databaseToRemove);

      if (isActiveDatabase) {
        log.info("Active cluster is being removed. Finding a new active cluster...");
        Map.Entry<Endpoint, Database> candidate = findWeightedHealthyClusterToIterate(
          databaseToRemove);
        if (candidate != null) {
          Database selectedCluster = candidate.getValue();
          if (setActiveDatabase(selectedCluster, true)) {
            log.info("New active cluster set to {}", candidate.getKey());
            notificationData = candidate;
          }
        } else {
          throw new JedisException(
              "Database can not be removed due to no healthy cluster available to switch!");
        }
      }

      // Remove from health status manager first
      healthStatusManager.unregisterListener(endpoint, this::onHealthStatusChange);
      healthStatusManager.remove(endpoint);

      // Remove from cluster map
      databaseMap.remove(endpoint);

      // Close the cluster resources
      if (databaseToRemove != null) {
        databaseToRemove.setDisabled(true);
        databaseToRemove.close();
      }
    } finally {
      activeDatabaseChangeLock.unlock();
    }
    if (notificationData != null) {
      onClusterSwitch(SwitchReason.FORCED, notificationData.getKey(), notificationData.getValue());
    }
  }

  /**
   * Internal method to add a database configuration. This method is not thread-safe and should be
   * called within appropriate locks.
   */
  private void addClusterInternal(MultiDbConfig multiDbConfig, DatabaseConfig config) {
    if (databaseMap.containsKey(config.getEndpoint())) {
      throw new JedisValidationException(
          "Endpoint " + config.getEndpoint() + " already exists in the provider");
    }

    String clusterId = "database:" + config.getEndpoint();

    Retry retry = RetryRegistry.of(retryConfig).retry(clusterId);

    Retry.EventPublisher retryPublisher = retry.getEventPublisher();
    retryPublisher.onRetry(event -> log.warn(String.valueOf(event)));
    retryPublisher.onError(event -> log.error(String.valueOf(event)));

    CircuitBreaker circuitBreaker = CircuitBreakerRegistry.of(circuitBreakerConfig)
        .circuitBreaker(clusterId);

    CircuitBreaker.EventPublisher circuitBreakerEventPublisher = circuitBreaker.getEventPublisher();
    circuitBreakerEventPublisher.onCallNotPermitted(event -> log.error(String.valueOf(event)));
    circuitBreakerEventPublisher.onError(event -> log.error(String.valueOf(event)));
    circuitBreakerEventPublisher.onFailureRateExceeded(event -> log.error(String.valueOf(event)));
    circuitBreakerEventPublisher.onSlowCallRateExceeded(event -> log.error(String.valueOf(event)));

    TrackingConnectionPool pool = TrackingConnectionPool.builder()
        .hostAndPort(hostPort(config.getEndpoint())).clientConfig(config.getJedisClientConfig())
        .poolConfig(config.getConnectionPoolConfig()).build();

    Database database;
    StrategySupplier strategySupplier = config.getHealthCheckStrategySupplier();
    if (strategySupplier != null) {
      HealthCheckStrategy hcs = strategySupplier.get(hostPort(config.getEndpoint()),
        config.getJedisClientConfig());
      // Register listeners BEFORE adding clusters to avoid missing events
      healthStatusManager.registerListener(config.getEndpoint(), this::onHealthStatusChange);
      HealthCheck hc = healthStatusManager.add(config.getEndpoint(), hcs);
      database = new Database(config.getEndpoint(), pool, retry, hc, circuitBreaker,
          config.getWeight(), multiDbConfig);
    } else {
      database = new Database(config.getEndpoint(), pool, retry, circuitBreaker, config.getWeight(),
          multiDbConfig);
    }

    databaseMap.put(config.getEndpoint(), database);

    // this is the place where we listen tracked errors and check if
    // thresholds are exceeded for the database
    circuitBreakerEventPublisher.onError(event -> {
      database.evaluateThresholds(false);
    });
  }

  private HostAndPort hostPort(Endpoint endpoint) {
    return new HostAndPort(endpoint.getHost(), endpoint.getPort());
  }

  /**
   * Handles health status changes for clusters. This method is called by the health status manager
   * when the health status of a cluster changes.
   */
  @VisibleForTesting
  void onHealthStatusChange(HealthStatusChangeEvent eventArgs) {
    Endpoint endpoint = eventArgs.getEndpoint();
    HealthStatus newStatus = eventArgs.getNewStatus();
    log.debug("Health status changed for {} from {} to {}", endpoint, eventArgs.getOldStatus(),
      newStatus);
    Database clusterWithHealthChange = databaseMap.get(endpoint);

    if (clusterWithHealthChange == null) return;

    if (initializationComplete) {
      if (!newStatus.isHealthy() && clusterWithHealthChange == activeDatabase) {
        clusterWithHealthChange.setGracePeriod();
        switchToHealthyDatabase(SwitchReason.HEALTH_CHECK, clusterWithHealthChange);
      }
    }
  }

  /**
   * Waits for initial health check results and selects the first healthy cluster based on weight
   * priority. Blocks until at least one cluster becomes healthy or all clusters are determined to
   * be unhealthy.
   * @param statusTracker the status tracker to use for waiting on health check results
   * @return the first healthy cluster found, ordered by weight (highest first)
   * @throws JedisConnectionException if all clusters are unhealthy
   */
  private Database waitForInitialHealthyCluster(StatusTracker statusTracker) {
    // Sort clusters by weight in descending order
    List<Map.Entry<Endpoint, Database>> sortedClusters = databaseMap.entrySet().stream()
        .sorted(Map.Entry.<Endpoint, Database> comparingByValue(
          Comparator.comparing(Database::getWeight).reversed()))
        .collect(Collectors.toList());

    log.info("Selecting initial cluster from {} configured clusters", sortedClusters.size());

    // Select cluster in weight order
    for (Map.Entry<Endpoint, Database> entry : sortedClusters) {
      Endpoint endpoint = entry.getKey();
      Database database = entry.getValue();

      log.info("Evaluating database {} (weight: {})", endpoint, database.getWeight());

      HealthStatus status;

      // Check if health checks are enabled for this endpoint
      if (healthStatusManager.hasHealthCheck(endpoint)) {
        log.info("Health checks enabled for {}, waiting for result", endpoint);
        // Wait for this database's health status to be determined
        status = statusTracker.waitForHealthStatus(endpoint);
      } else {
        // No health check configured - assume healthy
        log.info("No health check configured for database {}, defaulting to HEALTHY", endpoint);
        status = HealthStatus.HEALTHY;
      }

      if (status.isHealthy()) {
        log.info("Found healthy database: {} (weight: {})", endpoint, database.getWeight());
        return database;
      } else {
        log.info("Database {} is unhealthy, trying next database", endpoint);
      }
    }

    // All clusters are unhealthy
    throw new JedisConnectionException(
        "All configured clusters are unhealthy. Cannot initialize MultiDatabaseConnectionProvider.");
  }

  /**
   * Periodic failback checker - runs at configured intervals to check for failback opportunities
   */
  @VisibleForTesting
  void periodicFailbackCheck() {
    try {
      // Find the best candidate cluster for failback
      Map.Entry<Endpoint, Database> bestCandidate = null;
      float bestWeight = activeDatabase.getWeight();

      for (Map.Entry<Endpoint, Database> entry : databaseMap.entrySet()) {
        Database database = entry.getValue();

        // Skip if this is already the active database
        if (database == activeDatabase) {
          continue;
        }

        // Skip if database is not healthy
        if (!database.isHealthy()) {
          continue;
        }

        // This database is a valid candidate
        if (database.getWeight() > bestWeight) {
          bestCandidate = entry;
          bestWeight = database.getWeight();
        }
      }

      // Perform failback if we found a better candidate
      if (bestCandidate != null) {
        Database selectedCluster = bestCandidate.getValue();
        log.info("Performing failback from {} to {} (higher weight cluster available)",
          activeDatabase.getCircuitBreaker().getName(),
          selectedCluster.getCircuitBreaker().getName());
        if (setActiveDatabase(selectedCluster, true)) {
          onClusterSwitch(SwitchReason.FAILBACK, bestCandidate.getKey(), selectedCluster);
        }
      }
    } catch (Exception e) {
      log.error("Error during periodic failback check", e);
    }
  }

  Endpoint switchToHealthyDatabase(SwitchReason reason, Database iterateFrom) {
    Map.Entry<Endpoint, Database> databaseToIterate = findWeightedHealthyClusterToIterate(
      iterateFrom);
    if (databaseToIterate == null) {
      // throws exception anyway since not able to iterate
      handleNoHealthyCluster();
    }

    Database database = databaseToIterate.getValue();
    boolean changed = setActiveDatabase(database, false);
    if (!changed) return null;
    failoverAttemptCount.set(0);
    onClusterSwitch(reason, databaseToIterate.getKey(), database);
    return databaseToIterate.getKey();
  }

  private void handleNoHealthyCluster() {
    int max = multiDbConfig.getMaxNumFailoverAttempts();
    log.error("No healthy cluster available to switch to");
    if (failoverAttemptCount.get() > max) {
      throw new JedisPermanentlyNotAvailableException();
    }

    int currentAttemptCount = markAsFreeze() ? failoverAttemptCount.incrementAndGet()
        : failoverAttemptCount.get();

    if (currentAttemptCount > max) {
      throw new JedisPermanentlyNotAvailableException();
    }
    throw new JedisTemporarilyNotAvailableException();
  }

  private boolean markAsFreeze() {
    long until = failoverFreezeUntil.get();
    long now = System.currentTimeMillis();
    if (until <= now) {
      long nextUntil = now + multiDbConfig.getDelayInBetweenFailoverAttempts();
      if (failoverFreezeUntil.compareAndSet(until, nextUntil)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Asserts that the active cluster is operable. If not, throws an exception.
   * <p>
   * This method is called by the circuit breaker command executor before executing a command.
   * @throws JedisPermanentlyNotAvailableException if the there is no operable cluster and the max
   *           number of failover attempts has been exceeded.
   * @throws JedisTemporarilyNotAvailableException if the there is no operable cluster and the max
   *           number of failover attempts has not been exceeded.
   */
  @VisibleForTesting
  public void assertOperability() {
    Database current = activeDatabase;
    if (!current.isHealthy() && !this.canIterateFrom(current)) {
      handleNoHealthyCluster();
    }
  }

  private static Comparator<Map.Entry<Endpoint, Database>> maxByWeight = Map.Entry
      .<Endpoint, Database> comparingByValue(Comparator.comparing(Database::getWeight));

  private static Predicate<Map.Entry<Endpoint, Database>> filterByHealth = c -> c.getValue()
      .isHealthy();

  private Map.Entry<Endpoint, Database> findWeightedHealthyClusterToIterate(Database iterateFrom) {
    return databaseMap.entrySet().stream().filter(filterByHealth)
        .filter(entry -> entry.getValue() != iterateFrom).max(maxByWeight).orElse(null);
  }

  /**
   * Design decision was made to defer responsibility for cross-replication validation to the user.
   * Alternatively there was discussion to handle cross-cluster replication validation by setting a
   * key/value pair per hashslot in the active connection (with a TTL) and subsequently reading it
   * from the target connection.
   */
  public void validateTargetConnection(Endpoint endpoint) {
    Database database = databaseMap.get(endpoint);
    validateTargetConnection(database);
  }

  private void validateTargetConnection(Database database) {
    CircuitBreaker circuitBreaker = database.getCircuitBreaker();

    State originalState = circuitBreaker.getState();
    try {
      // Transitions the state machine to a CLOSED state, allowing state transition, metrics and
      // event publishing
      // Safe since the activeMultiClusterIndex has not yet been changed and therefore no traffic
      // will be routed
      // yet
      circuitBreaker.transitionToClosedState();

      try (Connection targetConnection = database.getConnection()) {
        targetConnection.ping();
      }
    } catch (Exception e) {

      // If the original state was FORCED_OPEN, then transition it back which stops state
      // transition, metrics and
      // event publishing
      if (State.FORCED_OPEN.equals(originalState)) circuitBreaker.transitionToForcedOpenState();

      throw new JedisValidationException(circuitBreaker.getName()
          + " failed to connect. Please check configuration and try again.", e);
    }
  }

  /**
   * Returns the set of all configured endpoints.
   * @return
   */
  public Set<Endpoint> getEndpoints() {
    return new HashSet<>(databaseMap.keySet());
  }

  public void setActiveDatabase(Endpoint endpoint) {
    if (endpoint == null) {
      throw new JedisValidationException(
          "Provided endpoint is null. Please use one from the configuration");
    }
    Database database = databaseMap.get(endpoint);
    if (database == null) {
      throw new JedisValidationException("Provided endpoint: " + endpoint + " is not within "
          + "the configured endpoints. Please use one from the configuration");
    }
    if (setActiveDatabase(database, true)) {
      onClusterSwitch(SwitchReason.FORCED, endpoint, database);
    }
  }

  public void forceActiveDatabase(Endpoint endpoint, long forcedActiveDuration) {
    Database database = databaseMap.get(endpoint);

    if (database == null) {
      throw new JedisValidationException("Provided endpoint: " + endpoint + " is not within "
          + "the configured endpoints. Please use one from the configuration");
    }

    database.clearGracePeriod();
    if (!database.isHealthy()) {
      throw new JedisValidationException("Provided endpoint: " + endpoint
          + " is not healthy. Please consider a healthy endpoint from the configuration");
    }
    databaseMap.entrySet().stream().forEach(entry -> {
      if (entry.getKey() != endpoint) {
        entry.getValue().setGracePeriod(forcedActiveDuration);
      }
    });
    setActiveDatabase(endpoint);
  }

  private boolean setActiveDatabase(Database database, boolean validateConnection) {
    // Database database = clusterEntry.getValue();
    // Field-level synchronization is used to avoid the edge case in which
    // incrementActiveMultiClusterIndex() is called at the same time
    activeDatabaseChangeLock.lock();
    Database oldCluster;
    try {

      // Allows an attempt to reset the current database from a FORCED_OPEN to CLOSED state in the
      // event that no failover is possible
      if (activeDatabase == database && !database.isCBForcedOpen()) return false;

      if (validateConnection) validateTargetConnection(database);

      String originalClusterName = getDatabaseCircuitBreaker().getName();

      if (activeDatabase == database)
        log.warn("Database/database endpoint '{}' successfully closed its circuit breaker",
          originalClusterName);
      else log.warn("Database/database endpoint successfully updated from '{}' to '{}'",
        originalClusterName, database.circuitBreaker.getName());
      oldCluster = activeDatabase;
      activeDatabase = database;
    } finally {
      activeDatabaseChangeLock.unlock();
    }
    boolean switched = oldCluster != database;
    if (switched && this.multiDbConfig.isFastFailover()) {
      log.info("Forcing disconnect of all active connections in old database: {}",
        oldCluster.circuitBreaker.getName());
      oldCluster.forceDisconnect();
      log.info("Disconnected all active connections in old database: {}",
        oldCluster.circuitBreaker.getName());

    }
    return switched;

  }

  @Override
  public void close() {
    if (healthStatusManager != null) {
      healthStatusManager.close();
    }

    // Shutdown the failback scheduler
    failbackScheduler.shutdown();
    try {
      if (!failbackScheduler.awaitTermination(1, TimeUnit.SECONDS)) {
        failbackScheduler.shutdownNow();
      }
    } catch (InterruptedException e) {
      failbackScheduler.shutdownNow();
      Thread.currentThread().interrupt();
    }

    // Close all cluster connection pools
    for (Database database : databaseMap.values()) {
      database.close();
    }
  }

  @Override
  public Connection getConnection() {
    return activeDatabase.getConnection();
  }

  public Connection getConnection(Endpoint endpoint) {
    return databaseMap.get(endpoint).getConnection();
  }

  @Override
  public Connection getConnection(CommandArguments args) {
    return activeDatabase.getConnection();
  }

  @Override
  public Map<?, Pool<Connection>> getConnectionMap() {
    ConnectionPool connectionPool = activeDatabase.connectionPool;
    return Collections.singletonMap(connectionPool.getFactory(), connectionPool);
  }

  public Database getDatabase() {
    return activeDatabase;
  }

  @VisibleForTesting
  public Database getDatabase(Endpoint endpoint) {
    return databaseMap.get(endpoint);
  }

  /**
   * Returns the active endpoint
   * <p>
   * Active endpoint is the one which is currently being used for all operations. It can change at
   * any time due to health checks, failover, failback, etc.
   * @return the active cluster endpoint
   */
  public Endpoint getActiveEndpoint() {
    return activeDatabase.getEndpoint();
  }

  /**
   * Returns the health state of the given endpoint
   * @param endpoint the endpoint to check
   * @return the health status of the endpoint
   */
  public boolean isHealthy(Endpoint endpoint) {
    Database database = getDatabase(endpoint);
    if (database == null) {
      throw new JedisValidationException(
          "Endpoint " + endpoint + " does not exist in the provider");
    }
    return database.isHealthy();
  }

  public CircuitBreaker getDatabaseCircuitBreaker() {
    return activeDatabase.getCircuitBreaker();
  }

  /**
   * Indicates the final cluster/database endpoint (connection pool), according to the
   * pre-configured list provided at startup via the MultiDbConfig, is unavailable and
   * therefore no further failover is possible. Users can manually failback to an available cluster
   */
  public boolean canIterateFrom(Database iterateFrom) {
    Map.Entry<Endpoint, Database> e = findWeightedHealthyClusterToIterate(iterateFrom);
    return e != null;
  }

  public void onClusterSwitch(SwitchReason reason, Endpoint endpoint, Database database) {
    if (databaseSwitchListener != null) {
      ClusterSwitchEventArgs eventArgs = new ClusterSwitchEventArgs(reason, endpoint, database);
      databaseSwitchListener.accept(eventArgs);
    }
  }

  public void setDatabaseSwitchListener(Consumer<ClusterSwitchEventArgs> databaseSwitchListener) {
    this.databaseSwitchListener = databaseSwitchListener;
  }

  public List<Class<? extends Throwable>> getFallbackExceptionList() {
    return fallbackExceptionList;
  }

  public static class Database {

    private TrackingConnectionPool connectionPool;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final float weight;
    private final HealthCheck healthCheck;
    private final MultiDbConfig multiDbConfig;
    private boolean disabled = false;
    private final Endpoint endpoint;

    // Grace period tracking
    private volatile long gracePeriodEndsAt = 0;
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Database(Endpoint endpoint, TrackingConnectionPool connectionPool, Retry retry,
        CircuitBreaker circuitBreaker, float weight, MultiDbConfig multiDbConfig) {

      this.endpoint = endpoint;
      this.connectionPool = connectionPool;
      this.retry = retry;
      this.circuitBreaker = circuitBreaker;
      this.weight = weight;
      this.multiDbConfig = multiDbConfig;
      this.healthCheck = null;
    }

    private Database(Endpoint endpoint, TrackingConnectionPool connectionPool, Retry retry,
        HealthCheck hc, CircuitBreaker circuitBreaker, float weight,
        MultiDbConfig multiDbConfig) {

      this.endpoint = endpoint;
      this.connectionPool = connectionPool;
      this.retry = retry;
      this.circuitBreaker = circuitBreaker;
      this.weight = weight;
      this.multiDbConfig = multiDbConfig;
      this.healthCheck = hc;
    }

    public Endpoint getEndpoint() {
      return endpoint;
    }

    public Connection getConnection() {
      if (!isHealthy()) throw new JedisConnectionException("Database is not healthy");
      if (connectionPool.isClosed()) {
        connectionPool = TrackingConnectionPool.from(connectionPool);
      }
      return connectionPool.getResource();
    }

    @VisibleForTesting
    public ConnectionPool getConnectionPool() {
      return connectionPool;
    }

    public Retry getRetry() {
      return retry;
    }

    public CircuitBreaker getCircuitBreaker() {
      return circuitBreaker;
    }

    public HealthStatus getHealthStatus() {
      return healthCheck == null ? HealthStatus.HEALTHY : healthCheck.getStatus();
    }

    /**
     * Assigned weight for this cluster
     */
    public float getWeight() {
      return weight;
    }

    public boolean isCBForcedOpen() {
      if (circuitBreaker.getState() == State.FORCED_OPEN && !isInGracePeriod()) {
        log.info(
          "Transitioning circuit breaker from FORCED_OPEN to CLOSED state due to end of grace period!");
        circuitBreaker.transitionToClosedState();
      }
      return circuitBreaker.getState() == CircuitBreaker.State.FORCED_OPEN;
    }

    public boolean isHealthy() {
      return getHealthStatus().isHealthy() && !isCBForcedOpen() && !disabled && !isInGracePeriod();
    }

    public boolean retryOnFailover() {
      return multiDbConfig.isRetryOnFailover();
    }

    public int getCircuitBreakerMinNumOfFailures() {
      return multiDbConfig.getCircuitBreakerMinNumOfFailures();
    }

    public float getCircuitBreakerFailureRateThreshold() {
      return multiDbConfig.getCircuitBreakerFailureRateThreshold();
    }

    public boolean isDisabled() {
      return disabled;
    }

    public void setDisabled(boolean disabled) {
      this.disabled = disabled;
    }

    /**
     * Checks if the cluster is currently in grace period
     */
    public boolean isInGracePeriod() {
      return System.currentTimeMillis() < gracePeriodEndsAt;
    }

    /**
     * Sets the grace period for this cluster
     */
    public void setGracePeriod() {
      setGracePeriod(multiDbConfig.getGracePeriod());
    }

    public void setGracePeriod(long gracePeriod) {
      long endTime = System.currentTimeMillis() + gracePeriod;
      if (endTime < gracePeriodEndsAt) return;
      gracePeriodEndsAt = endTime;
    }

    public void clearGracePeriod() {
      gracePeriodEndsAt = 0;
    }

    /**
     * Whether failback is supported by client
     */
    public boolean isFailbackSupported() {
      return multiDbConfig.isFailbackSupported();
    }

    public void forceDisconnect() {
      connectionPool.forceDisconnect();
    }

    public void close() {
      connectionPool.close();
    }

    void evaluateThresholds(boolean lastFailRecorded) {
      if (getCircuitBreaker().getState() == State.CLOSED
          && isThresholdsExceeded(this, lastFailRecorded)) {
        getCircuitBreaker().transitionToOpenState();
      }
    }

    private static boolean isThresholdsExceeded(Database database, boolean lastFailRecorded) {
      Metrics metrics = database.getCircuitBreaker().getMetrics();
      // ATTENTION: this is to increment fails in regard to the current call that is failing,
      // DO NOT remove the increment, it will change the behaviour in case of initial requests to
      // database fail
      int fails = metrics.getNumberOfFailedCalls() + (lastFailRecorded ? 0 : 1);
      int succ = metrics.getNumberOfSuccessfulCalls();
      if (fails >= database.getCircuitBreakerMinNumOfFailures()) {
        float ratePercentThreshold = database.getCircuitBreakerFailureRateThreshold();// 0..100
        int total = fails + succ;
        if (total == 0) return false;
        float failureRatePercent = (fails * 100.0f) / total;
        return failureRatePercent >= ratePercentThreshold;
      }
      return false;
    }

    @Override
    public String toString() {
      return circuitBreaker.getName() + "{" + "connectionPool=" + connectionPool + ", retry="
          + retry + ", circuitBreaker=" + circuitBreaker + ", weight=" + weight + ", healthStatus="
          + getHealthStatus() + ", multiClusterClientConfig=" + multiDbConfig + '}';
    }

  }
}