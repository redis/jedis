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
import java.util.List;
import java.util.Map;
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
import redis.clients.jedis.MultiClusterClientConfig.ClusterConfig;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.VisibleForTesting;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.mcf.JedisFailoverException.*;
import redis.clients.jedis.providers.ConnectionProvider;
import redis.clients.jedis.MultiClusterClientConfig.StrategySupplier;
import redis.clients.jedis.util.Pool;

/**
 * @author Allen Terleto (aterleto)
 *         <p>
 *         ConnectionProvider which supports multiple cluster/database endpoints each with their own
 *         isolated connection pool. With this ConnectionProvider users can seamlessly failover to
 *         Disaster Recovery (DR), Backup, and Active-Active cluster(s) by using simple
 *         configuration which is passed through from Resilience4j -
 *         https://resilience4j.readme.io/docs
 *         <p>
 *         Support for manual failback is provided by way of {@link #setActiveCluster(Endpoint)}
 *         <p>
 */
@Experimental
public class MultiClusterPooledConnectionProvider implements ConnectionProvider {

  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Ordered map of cluster/database endpoints which were provided at startup via the
   * MultiClusterClientConfig. Users can move down (failover) or (up) failback the map depending on
   * their availability and order.
   */
  private final Map<Endpoint, Cluster> multiClusterMap = new ConcurrentHashMap<>();

  /**
   * Indicates the actively used cluster/database endpoint (connection pool) amongst the
   * pre-configured list which were provided at startup via the MultiClusterClientConfig. All
   * traffic will be routed with this cluster/database
   */
  private volatile Cluster activeCluster;

  private final Lock activeClusterChangeLock = new ReentrantLock(true);

  /**
   * Functional interface for listening to cluster switch events. The event args contain the reason
   * for the switch, the endpoint, and the cluster.
   */
  private Consumer<ClusterSwitchEventArgs> clusterSwitchListener;

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

  // Store retry and circuit breaker configs for dynamic cluster addition/removal
  private RetryConfig retryConfig;
  private CircuitBreakerConfig circuitBreakerConfig;
  private MultiClusterClientConfig multiClusterClientConfig;

  private AtomicLong failoverFreezeUntil = new AtomicLong(0);
  private AtomicInteger failoverAttemptCount = new AtomicInteger(0);

  public MultiClusterPooledConnectionProvider(MultiClusterClientConfig multiClusterClientConfig) {

    if (multiClusterClientConfig == null) throw new JedisValidationException(
        "MultiClusterClientConfig must not be NULL for MultiClusterPooledConnectionProvider");

    this.multiClusterClientConfig = multiClusterClientConfig;

    ////////////// Configure Retry ////////////////////

    RetryConfig.Builder retryConfigBuilder = RetryConfig.custom();
    retryConfigBuilder.maxAttempts(multiClusterClientConfig.getRetryMaxAttempts());
    retryConfigBuilder.intervalFunction(
      IntervalFunction.ofExponentialBackoff(multiClusterClientConfig.getRetryWaitDuration(),
        multiClusterClientConfig.getRetryWaitDurationExponentialBackoffMultiplier()));
    retryConfigBuilder.failAfterMaxAttempts(false); // JedisConnectionException will be thrown
    retryConfigBuilder.retryExceptions(
      multiClusterClientConfig.getRetryIncludedExceptionList().stream().toArray(Class[]::new));

    List<Class> retryIgnoreExceptionList = multiClusterClientConfig.getRetryIgnoreExceptionList();
    if (retryIgnoreExceptionList != null)
      retryConfigBuilder.ignoreExceptions(retryIgnoreExceptionList.stream().toArray(Class[]::new));

    this.retryConfig = retryConfigBuilder.build();

    ////////////// Configure Circuit Breaker ////////////////////

    CircuitBreakerConfig.Builder circuitBreakerConfigBuilder = CircuitBreakerConfig.custom();

    CircuitBreakerThresholdsAdapter adapter = new CircuitBreakerThresholdsAdapter(
        multiClusterClientConfig);
    circuitBreakerConfigBuilder.minimumNumberOfCalls(adapter.getMinimumNumberOfCalls());
    circuitBreakerConfigBuilder.failureRateThreshold(adapter.getFailureRateThreshold());
    circuitBreakerConfigBuilder.slidingWindowSize(adapter.getSlidingWindowSize());
    circuitBreakerConfigBuilder.slidingWindowType(adapter.getSlidingWindowType());

    circuitBreakerConfigBuilder.recordExceptions(multiClusterClientConfig
        .getCircuitBreakerIncludedExceptionList().stream().toArray(Class[]::new));
    circuitBreakerConfigBuilder.automaticTransitionFromOpenToHalfOpenEnabled(false); // State
                                                                                     // transitions
                                                                                     // are
                                                                                     // forced. No
                                                                                     // half open
                                                                                     // states
                                                                                     // are used

    List<Class> circuitBreakerIgnoreExceptionList = multiClusterClientConfig
        .getCircuitBreakerIgnoreExceptionList();
    if (circuitBreakerIgnoreExceptionList != null) circuitBreakerConfigBuilder
        .ignoreExceptions(circuitBreakerIgnoreExceptionList.stream().toArray(Class[]::new));

    this.circuitBreakerConfig = circuitBreakerConfigBuilder.build();

    ////////////// Configure Cluster Map ////////////////////

    ClusterConfig[] clusterConfigs = multiClusterClientConfig.getClusterConfigs();

    // Now add clusters - health checks will start but events will be queued
    for (ClusterConfig config : clusterConfigs) {
      addClusterInternal(multiClusterClientConfig, config);
    }

    // Initialize StatusTracker for waiting on health check results
    StatusTracker statusTracker = new StatusTracker(healthStatusManager);

    // Wait for initial health check results and select active cluster based on weights
    activeCluster = waitForInitialHealthyCluster(statusTracker);

    // Mark initialization as complete - handleHealthStatusChange can now process events
    initializationComplete = true;
    Cluster temp = activeCluster;
    if (!temp.isHealthy()) {
      // Race condition: Direct assignment to 'activeCluster' is not thread safe because
      // 'onHealthStatusChange' may execute concurrently once 'initializationComplete'
      // is set to true.
      // Simple rule is to never assign value of 'activeCluster' outside of
      // 'activeClusterChangeLock' once the 'initializationComplete' is done.
      waitForInitialHealthyCluster(statusTracker);
      switchToHealthyCluster(SwitchReason.HEALTH_CHECK, temp);
    }
    this.fallbackExceptionList = multiClusterClientConfig.getFallbackExceptionList();

    // Start periodic failback checker
    if (multiClusterClientConfig.isFailbackSupported()) {
      long failbackInterval = multiClusterClientConfig.getFailbackCheckInterval();
      failbackScheduler.scheduleAtFixedRate(this::periodicFailbackCheck, failbackInterval,
        failbackInterval, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * Adds a new cluster endpoint to the provider.
   * @param clusterConfig the configuration for the new cluster
   * @throws JedisValidationException if the endpoint already exists
   */
  public void add(ClusterConfig clusterConfig) {
    if (clusterConfig == null) {
      throw new JedisValidationException("ClusterConfig must not be null");
    }

    Endpoint endpoint = clusterConfig.getHostAndPort();
    if (multiClusterMap.containsKey(endpoint)) {
      throw new JedisValidationException(
          "Endpoint " + endpoint + " already exists in the provider");
    }

    activeClusterChangeLock.lock();
    try {
      addClusterInternal(multiClusterClientConfig, clusterConfig);
    } finally {
      activeClusterChangeLock.unlock();
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

    if (!multiClusterMap.containsKey(endpoint)) {
      throw new JedisValidationException(
          "Endpoint " + endpoint + " does not exist in the provider");
    }

    if (multiClusterMap.size() < 2) {
      throw new JedisValidationException("Cannot remove the last remaining endpoint");
    }
    log.debug("Removing endpoint {}", endpoint);

    Map.Entry<Endpoint, Cluster> notificationData = null;
    activeClusterChangeLock.lock();
    try {
      Cluster clusterToRemove = multiClusterMap.get(endpoint);
      boolean isActiveCluster = (activeCluster == clusterToRemove);

      if (isActiveCluster) {
        log.info("Active cluster is being removed. Finding a new active cluster...");
        Map.Entry<Endpoint, Cluster> candidate = findWeightedHealthyClusterToIterate(
          clusterToRemove);
        if (candidate != null) {
          Cluster selectedCluster = candidate.getValue();
          if (setActiveCluster(selectedCluster, true)) {
            log.info("New active cluster set to {}", candidate.getKey());
            notificationData = candidate;
          }
        } else {
          throw new JedisException(
              "Cluster can not be removed due to no healthy cluster available to switch!");
        }
      }

      // Remove from health status manager first
      healthStatusManager.unregisterListener(endpoint, this::onHealthStatusChange);
      healthStatusManager.remove(endpoint);

      // Remove from cluster map
      multiClusterMap.remove(endpoint);

      // Close the cluster resources
      if (clusterToRemove != null) {
        clusterToRemove.setDisabled(true);
        clusterToRemove.close();
      }
    } finally {
      activeClusterChangeLock.unlock();
    }
    if (notificationData != null) {
      onClusterSwitch(SwitchReason.FORCED, notificationData.getKey(), notificationData.getValue());
    }
  }

  /**
   * Internal method to add a cluster configuration. This method is not thread-safe and should be
   * called within appropriate locks.
   */
  private void addClusterInternal(MultiClusterClientConfig multiClusterClientConfig,
      ClusterConfig config) {
    if (multiClusterMap.containsKey(config.getHostAndPort())) {
      throw new JedisValidationException(
          "Endpoint " + config.getHostAndPort() + " already exists in the provider");
    }

    String clusterId = "cluster:" + config.getHostAndPort();

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
        .hostAndPort(config.getHostAndPort()).clientConfig(config.getJedisClientConfig())
        .poolConfig(config.getConnectionPoolConfig()).build();

    Cluster cluster;
    StrategySupplier strategySupplier = config.getHealthCheckStrategySupplier();
    if (strategySupplier != null) {
      HealthCheckStrategy hcs = strategySupplier.get(config.getHostAndPort(),
        config.getJedisClientConfig());
      // Register listeners BEFORE adding clusters to avoid missing events
      healthStatusManager.registerListener(config.getHostAndPort(), this::onHealthStatusChange);
      HealthCheck hc = healthStatusManager.add(config.getHostAndPort(), hcs);
      cluster = new Cluster(pool, retry, hc, circuitBreaker, config.getWeight(),
          multiClusterClientConfig);
    } else {
      cluster = new Cluster(pool, retry, circuitBreaker, config.getWeight(),
          multiClusterClientConfig);
    }

    multiClusterMap.put(config.getHostAndPort(), cluster);

    // this is the place where we listen tracked errors and check if
    // thresholds are exceeded for the cluster
    circuitBreakerEventPublisher.onError(event -> {
      cluster.evaluateThresholds(false);
    });
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
    Cluster clusterWithHealthChange = multiClusterMap.get(endpoint);

    if (clusterWithHealthChange == null) return;

    if (initializationComplete) {
      if (!newStatus.isHealthy() && clusterWithHealthChange == activeCluster) {
        clusterWithHealthChange.setGracePeriod();
        switchToHealthyCluster(SwitchReason.HEALTH_CHECK, clusterWithHealthChange);
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
  private Cluster waitForInitialHealthyCluster(StatusTracker statusTracker) {
    // Sort clusters by weight in descending order
    List<Map.Entry<Endpoint, Cluster>> sortedClusters = multiClusterMap.entrySet().stream()
        .sorted(Map.Entry.<Endpoint, Cluster> comparingByValue(
          Comparator.comparing(Cluster::getWeight).reversed()))
        .collect(Collectors.toList());

    log.info("Selecting initial cluster from {} configured clusters", sortedClusters.size());

    // Select cluster in weight order
    for (Map.Entry<Endpoint, Cluster> entry : sortedClusters) {
      Endpoint endpoint = entry.getKey();
      Cluster cluster = entry.getValue();

      log.info("Evaluating cluster {} (weight: {})", endpoint, cluster.getWeight());

      HealthStatus status;

      // Check if health checks are enabled for this endpoint
      if (healthStatusManager.hasHealthCheck(endpoint)) {
        log.info("Health checks enabled for {}, waiting for result", endpoint);
        // Wait for this cluster's health status to be determined
        status = statusTracker.waitForHealthStatus(endpoint);
      } else {
        // No health check configured - assume healthy
        log.info("No health check configured for cluster {}, defaulting to HEALTHY", endpoint);
        status = HealthStatus.HEALTHY;
      }

      if (status.isHealthy()) {
        log.info("Found healthy cluster: {} (weight: {})", endpoint, cluster.getWeight());
        return cluster;
      } else {
        log.info("Cluster {} is unhealthy, trying next cluster", endpoint);
      }
    }

    // All clusters are unhealthy
    throw new JedisConnectionException(
        "All configured clusters are unhealthy. Cannot initialize MultiClusterPooledConnectionProvider.");
  }

  /**
   * Periodic failback checker - runs at configured intervals to check for failback opportunities
   */
  @VisibleForTesting
  void periodicFailbackCheck() {
    try {
      // Find the best candidate cluster for failback
      Map.Entry<Endpoint, Cluster> bestCandidate = null;
      float bestWeight = activeCluster.getWeight();

      for (Map.Entry<Endpoint, Cluster> entry : multiClusterMap.entrySet()) {
        Cluster cluster = entry.getValue();

        // Skip if this is already the active cluster
        if (cluster == activeCluster) {
          continue;
        }

        // Skip if cluster is not healthy
        if (!cluster.isHealthy()) {
          continue;
        }

        // This cluster is a valid candidate
        if (cluster.getWeight() > bestWeight) {
          bestCandidate = entry;
          bestWeight = cluster.getWeight();
        }
      }

      // Perform failback if we found a better candidate
      if (bestCandidate != null) {
        Cluster selectedCluster = bestCandidate.getValue();
        log.info("Performing failback from {} to {} (higher weight cluster available)",
          activeCluster.getCircuitBreaker().getName(),
          selectedCluster.getCircuitBreaker().getName());
        if (setActiveCluster(selectedCluster, true)) {
          onClusterSwitch(SwitchReason.FAILBACK, bestCandidate.getKey(), selectedCluster);
        }
      }
    } catch (Exception e) {
      log.error("Error during periodic failback check", e);
    }
  }

  Endpoint switchToHealthyCluster(SwitchReason reason, Cluster iterateFrom) {
    Map.Entry<Endpoint, Cluster> clusterToIterate = findWeightedHealthyClusterToIterate(
      iterateFrom);
    if (clusterToIterate == null) {
      // throws exception anyway since not able to iterate
      handleNoHealthyCluster();
    }

    Cluster cluster = clusterToIterate.getValue();
    boolean changed = setActiveCluster(cluster, false);
    if (!changed) return null;
    failoverAttemptCount.set(0);
    onClusterSwitch(reason, clusterToIterate.getKey(), cluster);
    return clusterToIterate.getKey();
  }

  private void handleNoHealthyCluster() {
    int max = multiClusterClientConfig.getMaxNumFailoverAttempts();
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
      long nextUntil = now + multiClusterClientConfig.getDelayInBetweenFailoverAttempts();
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
    Cluster current = activeCluster;
    if (!current.isHealthy() && !this.canIterateFrom(current)) {
      handleNoHealthyCluster();
    }
  }

  private static Comparator<Map.Entry<Endpoint, Cluster>> maxByWeight = Map.Entry
      .<Endpoint, Cluster> comparingByValue(Comparator.comparing(Cluster::getWeight));

  private static Predicate<Map.Entry<Endpoint, Cluster>> filterByHealth = c -> c.getValue()
      .isHealthy();

  private Map.Entry<Endpoint, Cluster> findWeightedHealthyClusterToIterate(Cluster iterateFrom) {
    return multiClusterMap.entrySet().stream().filter(filterByHealth)
        .filter(entry -> entry.getValue() != iterateFrom).max(maxByWeight).orElse(null);
  }

  /**
   * Design decision was made to defer responsibility for cross-replication validation to the user.
   * Alternatively there was discussion to handle cross-cluster replication validation by setting a
   * key/value pair per hashslot in the active connection (with a TTL) and subsequently reading it
   * from the target connection.
   */
  public void validateTargetConnection(Endpoint endpoint) {
    Cluster cluster = multiClusterMap.get(endpoint);
    validateTargetConnection(cluster);
  }

  private void validateTargetConnection(Cluster cluster) {
    CircuitBreaker circuitBreaker = cluster.getCircuitBreaker();

    State originalState = circuitBreaker.getState();
    try {
      // Transitions the state machine to a CLOSED state, allowing state transition, metrics and
      // event publishing
      // Safe since the activeMultiClusterIndex has not yet been changed and therefore no traffic
      // will be routed
      // yet
      circuitBreaker.transitionToClosedState();

      try (Connection targetConnection = cluster.getConnection()) {
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

  public void setActiveCluster(Endpoint endpoint) {
    if (endpoint == null) {
      throw new JedisValidationException(
          "Provided endpoint is null. Please use one from the configuration");
    }
    Cluster cluster = multiClusterMap.get(endpoint);
    if (cluster == null) {
      throw new JedisValidationException("Provided endpoint: " + endpoint + " is not within "
          + "the configured endpoints. Please use one from the configuration");
    }
    if (setActiveCluster(cluster, true)) {
      onClusterSwitch(SwitchReason.FORCED, endpoint, cluster);
    }
  }

  public void forceActiveCluster(Endpoint endpoint, long forcedActiveDuration) {
    Cluster cluster = multiClusterMap.get(endpoint);
    cluster.clearGracePeriod();
    if (!cluster.isHealthy()) {
      throw new JedisValidationException("Provided endpoint: " + endpoint
          + " is not healthy. Please consider a healthy endpoint from the configuration");
    }
    multiClusterMap.entrySet().stream().forEach(entry -> {
      if (entry.getKey() != endpoint) {
        entry.getValue().setGracePeriod(forcedActiveDuration);
      }
    });
    setActiveCluster(endpoint);
  }

  private boolean setActiveCluster(Cluster cluster, boolean validateConnection) {
    // Cluster cluster = clusterEntry.getValue();
    // Field-level synchronization is used to avoid the edge case in which
    // incrementActiveMultiClusterIndex() is called at the same time
    activeClusterChangeLock.lock();
    Cluster oldCluster;
    try {

      // Allows an attempt to reset the current cluster from a FORCED_OPEN to CLOSED state in the
      // event that no failover is possible
      if (activeCluster == cluster && !cluster.isCBForcedOpen()) return false;

      if (validateConnection) validateTargetConnection(cluster);

      String originalClusterName = getClusterCircuitBreaker().getName();

      if (activeCluster == cluster)
        log.warn("Cluster/database endpoint '{}' successfully closed its circuit breaker",
          originalClusterName);
      else log.warn("Cluster/database endpoint successfully updated from '{}' to '{}'",
        originalClusterName, cluster.circuitBreaker.getName());
      oldCluster = activeCluster;
      activeCluster = cluster;
    } finally {
      activeClusterChangeLock.unlock();
    }
    boolean switched = oldCluster != cluster;
    if (switched && this.multiClusterClientConfig.isFastFailover()) {
      log.info("Forcing disconnect of all active connections in old cluster: {}",
        oldCluster.circuitBreaker.getName());
      oldCluster.forceDisconnect();
      log.info("Disconnected all active connections in old cluster: {}",
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
    for (Cluster cluster : multiClusterMap.values()) {
      cluster.close();
    }
  }

  @Override
  public Connection getConnection() {
    return activeCluster.getConnection();
  }

  public Connection getConnection(Endpoint endpoint) {
    return multiClusterMap.get(endpoint).getConnection();
  }

  @Override
  public Connection getConnection(CommandArguments args) {
    return activeCluster.getConnection();
  }

  @Override
  public Map<?, Pool<Connection>> getConnectionMap() {
    ConnectionPool connectionPool = activeCluster.connectionPool;
    return Collections.singletonMap(connectionPool.getFactory(), connectionPool);
  }

  public Cluster getCluster() {
    return activeCluster;
  }

  @VisibleForTesting
  public Cluster getCluster(Endpoint endpoint) {
    return multiClusterMap.get(endpoint);
  }

  public CircuitBreaker getClusterCircuitBreaker() {
    return activeCluster.getCircuitBreaker();
  }

  /**
   * Indicates the final cluster/database endpoint (connection pool), according to the
   * pre-configured list provided at startup via the MultiClusterClientConfig, is unavailable and
   * therefore no further failover is possible. Users can manually failback to an available cluster
   */
  public boolean canIterateFrom(Cluster iterateFrom) {
    Map.Entry<Endpoint, Cluster> e = findWeightedHealthyClusterToIterate(iterateFrom);
    return e != null;
  }

  public void onClusterSwitch(SwitchReason reason, Endpoint endpoint, Cluster cluster) {
    if (clusterSwitchListener != null) {
      ClusterSwitchEventArgs eventArgs = new ClusterSwitchEventArgs(reason, endpoint, cluster);
      clusterSwitchListener.accept(eventArgs);
    }
  }

  public void setClusterSwitchListener(Consumer<ClusterSwitchEventArgs> clusterSwitchListener) {
    this.clusterSwitchListener = clusterSwitchListener;
  }

  public List<Class<? extends Throwable>> getFallbackExceptionList() {
    return fallbackExceptionList;
  }

  public static class Cluster {

    private TrackingConnectionPool connectionPool;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final float weight;
    private final HealthCheck healthCheck;
    private final MultiClusterClientConfig multiClusterClientConfig;
    private boolean disabled = false;

    // Grace period tracking
    private volatile long gracePeriodEndsAt = 0;
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Cluster(TrackingConnectionPool connectionPool, Retry retry,
        CircuitBreaker circuitBreaker, float weight,
        MultiClusterClientConfig multiClusterClientConfig) {

      this.connectionPool = connectionPool;
      this.retry = retry;
      this.circuitBreaker = circuitBreaker;
      this.weight = weight;
      this.multiClusterClientConfig = multiClusterClientConfig;
      this.healthCheck = null;
    }

    private Cluster(TrackingConnectionPool connectionPool, Retry retry, HealthCheck hc,
        CircuitBreaker circuitBreaker, float weight,
        MultiClusterClientConfig multiClusterClientConfig) {

      this.connectionPool = connectionPool;
      this.retry = retry;
      this.circuitBreaker = circuitBreaker;
      this.weight = weight;
      this.multiClusterClientConfig = multiClusterClientConfig;
      this.healthCheck = hc;
    }

    public Connection getConnection() {
      if (!isHealthy()) throw new JedisConnectionException("Cluster is not healthy");
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
      return multiClusterClientConfig.isRetryOnFailover();
    }

    public int getCircuitBreakerMinNumOfFailures() {
      return multiClusterClientConfig.getCircuitBreakerMinNumOfFailures();
    }

    public float getCircuitBreakerFailureRateThreshold() {
      return multiClusterClientConfig.getCircuitBreakerFailureRateThreshold();
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
      setGracePeriod(multiClusterClientConfig.getGracePeriod());
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
      return multiClusterClientConfig.isFailbackSupported();
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

    private static boolean isThresholdsExceeded(Cluster cluster, boolean lastFailRecorded) {
      Metrics metrics = cluster.getCircuitBreaker().getMetrics();
      // ATTENTION: this is to increment fails in regard to the current call that is failing,
      // DO NOT remove the increment, it will change the behaviour in case of initial requests to
      // cluster fail
      int fails = metrics.getNumberOfFailedCalls() + (lastFailRecorded ? 0 : 1);
      int succ = metrics.getNumberOfSuccessfulCalls();
      if (fails >= cluster.getCircuitBreakerMinNumOfFailures()) {
        float ratePercentThreshold = cluster.getCircuitBreakerFailureRateThreshold();// 0..100
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
          + getHealthStatus() + ", multiClusterClientConfig=" + multiClusterClientConfig + '}';
    }
  }
}