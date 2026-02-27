package redis.clients.jedis.mcf;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.junit.jupiter.api.*;
import redis.clients.jedis.*;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisValidationException;
import redis.clients.jedis.mcf.MultiDbConnectionProvider.Database;
import redis.clients.jedis.mcf.ProbingPolicy.BuiltIn;
import redis.clients.jedis.mcf.JedisFailoverException.JedisPermanentlyNotAvailableException;
import redis.clients.jedis.mcf.JedisFailoverException.JedisTemporarilyNotAvailableException;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @see MultiDbConnectionProvider
 */
@Tag("integration")
public class MultiDbConnectionProviderTest {

  private static EndpointConfig endpointStandalone0;
  private static EndpointConfig endpointStandalone1;

  private MultiDbConnectionProvider provider;

  @BeforeAll
  public static void prepareEndpoints() {
    endpointStandalone0 = Endpoints.getRedisEndpoint("standalone0");
    endpointStandalone1 = Endpoints.getRedisEndpoint("standalone1");
  }

  @BeforeEach
  public void setUp() {

    DatabaseConfig[] databaseConfigs = new DatabaseConfig[2];
    databaseConfigs[0] = DatabaseConfig.builder(endpointStandalone0.getHostAndPort(),
      endpointStandalone0.getClientConfigBuilder().build()).weight(0.5f).build();
    databaseConfigs[1] = DatabaseConfig.builder(endpointStandalone1.getHostAndPort(),
      endpointStandalone1.getClientConfigBuilder().build()).weight(0.3f).build();

    provider = new MultiDbConnectionProvider(new MultiDbConfig.Builder(databaseConfigs).build());
  }

  @AfterEach
  public void destroy() {
    provider.close();
    provider = null;
  }

  @Test
  public void testCircuitBreakerForcedTransitions() {

    CircuitBreaker circuitBreaker = provider.getDatabaseCircuitBreaker();
    circuitBreaker.getState();

    if (CircuitBreaker.State.FORCED_OPEN.equals(circuitBreaker.getState()))
      circuitBreaker.transitionToClosedState();

    circuitBreaker.transitionToForcedOpenState();
    assertEquals(CircuitBreaker.State.FORCED_OPEN, circuitBreaker.getState());

    circuitBreaker.transitionToClosedState();
    assertEquals(CircuitBreaker.State.CLOSED, circuitBreaker.getState());
  }

  @Test
  public void testSwitchToHealthyDatabase() throws InterruptedException {
    waitForDatabaseToGetHealthy(provider.getDatabase(endpointStandalone0.getHostAndPort()),
      provider.getDatabase(endpointStandalone1.getHostAndPort()));

    Endpoint e2 = provider.switchToHealthyDatabase(SwitchReason.HEALTH_CHECK,
      provider.getDatabase());
    assertEquals(endpointStandalone1.getHostAndPort(), e2);
  }

  @Test
  public void testCanIterateOnceMore() {
    Endpoint endpoint0 = endpointStandalone0.getHostAndPort();
    waitForDatabaseToGetHealthy(provider.getDatabase(endpoint0),
      provider.getDatabase(endpointStandalone1.getHostAndPort()));

    provider.setActiveDatabase(endpoint0);
    provider.getDatabase().setDisabled(true);
    provider.switchToHealthyDatabase(SwitchReason.HEALTH_CHECK, provider.getDatabase(endpoint0));

    assertFalse(provider.canIterateFrom(provider.getDatabase()));
  }

  private void waitForDatabaseToGetHealthy(Database... databases) {
    Awaitility.await().pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
        .atMost(Durations.TWO_SECONDS)
        .until(() -> Arrays.stream(databases).allMatch(Database::isHealthy));
  }

  @Test
  public void testDatabaseSwitchListener() {
    DatabaseConfig[] databaseConfigs = new DatabaseConfig[2];
    databaseConfigs[0] = DatabaseConfig
        .builder(new HostAndPort("purposefully-incorrect", 0000),
          DefaultJedisClientConfig.builder().build())
        .weight(0.5f).healthCheckEnabled(false).build();
    databaseConfigs[1] = DatabaseConfig
        .builder(new HostAndPort("purposefully-incorrect", 0001),
          DefaultJedisClientConfig.builder().build())
        .weight(0.4f).healthCheckEnabled(false).build();

    MultiDbConfig.Builder builder = new MultiDbConfig.Builder(databaseConfigs);

    // Configures a single failed command to trigger an open circuit on the next subsequent failure
    builder.failureDetector(MultiDbConfig.CircuitBreakerConfig.builder().slidingWindowSize(3)
        .minNumOfFailures(1).failureRateThreshold(0).build());

    AtomicBoolean isValidTest = new AtomicBoolean(false);

    MultiDbConnectionProvider localProvider = new MultiDbConnectionProvider(builder.build());
    localProvider.setDatabaseSwitchListener(a -> {
      isValidTest.set(true);
    });

    try (MultiDbClient jedis = MultiDbClient.builder().connectionProvider(localProvider).build()) {

      // This will fail due to unable to connect and open the circuit which will trigger the post
      // processor
      try {
        jedis.get("foo");
      } catch (Exception e) {
      }

    }

    assertTrue(isValidTest.get());
  }

  @Test
  public void testSetActiveDatabaseNull() {
    assertThrows(JedisValidationException.class, () -> provider.setActiveDatabase(null));
  }

  @Test
  public void testSetActiveDatabaseByMissingEndpoint() {
    assertThrows(JedisValidationException.class, () -> provider.setActiveDatabase(new Endpoint() {
      @Override
      public String getHost() {
        return "purposefully-incorrect";
      }

      @Override
      public int getPort() {
        return 0000;
      }
    })); // Should throw an exception
  }

  @Test
  public void testConnectionPoolConfigApplied() {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(8);
    poolConfig.setMaxIdle(4);
    poolConfig.setMinIdle(1);
    DatabaseConfig[] databaseConfigs = new DatabaseConfig[2];
    databaseConfigs[0] = new DatabaseConfig(endpointStandalone0.getHostAndPort(),
        endpointStandalone0.getClientConfigBuilder().build(), poolConfig);
    databaseConfigs[1] = new DatabaseConfig(endpointStandalone1.getHostAndPort(),
        endpointStandalone0.getClientConfigBuilder().build(), poolConfig);
    try (MultiDbConnectionProvider customProvider = new MultiDbConnectionProvider(
        new MultiDbConfig.Builder(databaseConfigs).build())) {
      MultiDbConnectionProvider.Database activeDatabase = customProvider.getDatabase();
      ConnectionPool connectionPool = activeDatabase.getConnectionPool();
      assertEquals(8, connectionPool.getMaxTotal());
      assertEquals(4, connectionPool.getMaxIdle());
      assertEquals(1, connectionPool.getMinIdle());
    }
  }

  @Test
  @Timeout(5)
  void testHealthChecksStopAfterProviderClose() throws InterruptedException {
    AtomicInteger healthCheckCount = new AtomicInteger(0);

    // Custom strategy that counts health checks
    HealthCheckStrategy countingStrategy = new redis.clients.jedis.mcf.TestHealthCheckStrategy(
        redis.clients.jedis.mcf.HealthCheckStrategy.Config.builder().interval(5).timeout(50)
            .policy(BuiltIn.ANY_SUCCESS).build(),
        e -> {
          healthCheckCount.incrementAndGet();
          return HealthStatus.HEALTHY;
        });

    // Create new provider with health check strategy (don't use the setUp() provider)
    DatabaseConfig config = DatabaseConfig
        .builder(endpointStandalone0.getHostAndPort(),
          endpointStandalone0.getClientConfigBuilder().build())
        .healthCheckStrategy(countingStrategy).build();

    MultiDbConnectionProvider testProvider = new MultiDbConnectionProvider(
        new MultiDbConfig.Builder(Collections.singletonList(config)).build());

    try {
      // Wait for some health checks to occur
      Awaitility.await().atMost(Durations.ONE_SECOND).until(() -> healthCheckCount.get() > 2);

      int checksBeforeClose = healthCheckCount.get();

      // Close provider
      testProvider.close();

      // Wait longer than health check interval
      Thread.sleep(100);

      int checksAfterClose = healthCheckCount.get();

      // Health check count should not increase after close
      assertEquals(checksBeforeClose, checksAfterClose,
        "Health checks should stop after provider is closed");

    } finally {
      // Ensure cleanup even if test fails
      testProvider.close();
    }
  }

  @Test
  public void userCommand_firstTemporary_thenPermanent_inOrder() {
    DatabaseConfig[] databaseConfigs = new DatabaseConfig[2];
    databaseConfigs[0] = DatabaseConfig.builder(endpointStandalone0.getHostAndPort(),
      endpointStandalone0.getClientConfigBuilder().build()).weight(0.5f).build();
    databaseConfigs[1] = DatabaseConfig.builder(endpointStandalone1.getHostAndPort(),
      endpointStandalone1.getClientConfigBuilder().build()).weight(0.3f).build();

    MultiDbConnectionProvider testProvider = new MultiDbConnectionProvider(
        new MultiDbConfig.Builder(databaseConfigs).delayInBetweenFailoverAttempts(100)
            .maxNumFailoverAttempts(2)
            .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(1).build()).build());

    try (MultiDbClient jedis = MultiDbClient.builder().connectionProvider(testProvider).build()) {
      jedis.get("foo");

      // Disable both databases so any attempt to switch results in 'no healthy database' path
      testProvider.getDatabase(endpointStandalone0.getHostAndPort()).setDisabled(true);
      testProvider.getDatabase(endpointStandalone1.getHostAndPort()).setDisabled(true);

      // Simulate user running a command that fails and triggers failover iteration
      assertThrows(JedisTemporarilyNotAvailableException.class, () -> jedis.get("foo"));

      // Next immediate attempt should exceed max attempts and become permanent (expected to fail
      // until feature exists)
      await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
          .until(() -> (assertThrows(JedisFailoverException.class,
            () -> jedis.get("foo")) instanceof JedisPermanentlyNotAvailableException));
    }
  }

  @Test
  public void userCommand_connectionExceptions_thenMultipleTemporary_thenPermanent_inOrder() {
    DatabaseConfig[] databaseConfigs = new DatabaseConfig[2];
    databaseConfigs[0] = DatabaseConfig
        .builder(endpointStandalone0.getHostAndPort(),
          endpointStandalone0.getClientConfigBuilder().build())
        .weight(0.5f).healthCheckEnabled(false).build();
    databaseConfigs[1] = DatabaseConfig
        .builder(endpointStandalone1.getHostAndPort(),
          endpointStandalone1.getClientConfigBuilder().build())
        .weight(0.3f).healthCheckEnabled(false).build();

    // ATTENTION: these configuration settings are not random and
    // adjusted to get exact numbers of failures with exact exception types
    // and open to impact from other defaulted values withing the components in use.
    MultiDbConnectionProvider testProvider = new MultiDbConnectionProvider(
        new MultiDbConfig.Builder(databaseConfigs).delayInBetweenFailoverAttempts(100)
            .maxNumFailoverAttempts(2)
            .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(1).build())
            .failureDetector(MultiDbConfig.CircuitBreakerConfig.builder().slidingWindowSize(5)
                .failureRateThreshold(60).build())
            .build()) {
    };

    try (MultiDbClient jedis = MultiDbClient.builder().connectionProvider(testProvider).build()) {
      jedis.get("foo");

      // disable most weighted database so that it will fail on initial requests
      testProvider.getDatabase(endpointStandalone0.getHostAndPort()).setDisabled(true);

      Exception e = assertThrows(JedisConnectionException.class, () -> jedis.get("foo"));
      assertEquals(JedisConnectionException.class, e.getClass());

      e = assertThrows(JedisConnectionException.class, () -> jedis.get("foo"));
      assertEquals(JedisConnectionException.class, e.getClass());

      // then disable the second ones
      testProvider.getDatabase(endpointStandalone1.getHostAndPort()).setDisabled(true);
      assertThrows(JedisTemporarilyNotAvailableException.class, () -> jedis.get("foo"));
      assertThrows(JedisTemporarilyNotAvailableException.class, () -> jedis.get("foo"));

      // Third get request should exceed max attempts and throw
      // JedisPermanentlyNotAvailableException
      await().atMost(Durations.FIVE_HUNDRED_MILLISECONDS).pollInterval(Duration.ofMillis(50))
          .until(() -> (assertThrows(JedisFailoverException.class,
            () -> jedis.get("foo")) instanceof JedisPermanentlyNotAvailableException));

      // Fourth get request should continue to throw JedisPermanentlyNotAvailableException
      assertThrows(JedisPermanentlyNotAvailableException.class, () -> jedis.get("foo"));
    }
  }

  @Test
  public void recoveryAfterPermanentException_withFailbackEnabled() {
    DatabaseConfig[] databaseConfigs = new DatabaseConfig[2];
    // Note: db0 has higher weight (0.5) than db1 (0.3)
    // Periodic failback only switches to higher-weight databases
    databaseConfigs[0] = DatabaseConfig
        .builder(endpointStandalone0.getHostAndPort(),
          endpointStandalone0.getClientConfigBuilder().build())
        .weight(0.5f).healthCheckEnabled(false).build();
    databaseConfigs[1] = DatabaseConfig
        .builder(endpointStandalone1.getHostAndPort(),
          endpointStandalone1.getClientConfigBuilder().build())
        .weight(0.3f).healthCheckEnabled(false).build();

    // Configure with failback enabled and short intervals for faster test execution
    MultiDbConnectionProvider testProvider = new MultiDbConnectionProvider(
        new MultiDbConfig.Builder(databaseConfigs).delayInBetweenFailoverAttempts(100)
            .maxNumFailoverAttempts(2)
            .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(1).build())
            .failbackSupported(true) // Enable automatic failback
            .failbackCheckInterval(500) // Check every 500ms for faster recovery
            .gracePeriod(100) // Short grace period for faster test
            .build());

    try (MultiDbClient jedis = MultiDbClient.builder().connectionProvider(testProvider).build()) {
      // Verify initial connection works (should be on db0 with weight 0.5)
      jedis.set("test-key", "initial-value");
      assertEquals("initial-value", jedis.get("test-key"));

      // Manually switch to db1 (lower weight) to set up the scenario
      testProvider.getDatabase(endpointStandalone0.getHostAndPort()).setDisabled(true);
      testProvider.setActiveDatabase(endpointStandalone1.getHostAndPort());

      // Verify we're now on db1
      jedis.set("test-key", "on-db1");
      assertEquals("on-db1", jedis.get("test-key"));

      // Now disable the other database to trigger permanent exception
      testProvider.getDatabase(endpointStandalone1.getHostAndPort()).setDisabled(true);

      // Trigger temporary exceptions first
      assertThrows(JedisTemporarilyNotAvailableException.class, () -> jedis.get("test-key"));

      // Wait for permanent exception
      await().atMost(Durations.ONE_SECOND).pollInterval(Duration.ofMillis(50))
          .until(() -> (assertThrows(JedisFailoverException.class,
            () -> jedis.get("test-key")) instanceof JedisPermanentlyNotAvailableException));

      // Verify we're in permanent failure state
      assertThrows(JedisPermanentlyNotAvailableException.class, () -> jedis.get("test-key"));

      // Re-enable the HIGHER-weight database (db0) to simulate recovery
      MultiDbConnectionProvider.Database db0 = testProvider
          .getDatabase(endpointStandalone0.getHostAndPort());
      db0.setDisabled(false);

      // Wait for automatic failback to recover
      await().atMost(Durations.ONE_SECOND).pollInterval(Duration.ofMillis(50)).ignoreExceptions()
          .untilAsserted(() -> {
            assertEquals(endpointStandalone0.getHostAndPort(), jedis.getActiveDatabaseEndpoint());
            assertTrue(testProvider.getDatabase().isHealthy());
          });
    }
  }

  @Test
  public void recoveryAfterPermanentException_withFailbackDisabled() {
    DatabaseConfig[] databaseConfigs = new DatabaseConfig[2];
    databaseConfigs[0] = DatabaseConfig
        .builder(endpointStandalone0.getHostAndPort(),
          endpointStandalone0.getClientConfigBuilder().build())
        .weight(0.5f).healthCheckEnabled(false).build();
    databaseConfigs[1] = DatabaseConfig
        .builder(endpointStandalone1.getHostAndPort(),
          endpointStandalone1.getClientConfigBuilder().build())
        .weight(0.3f).healthCheckEnabled(false).build();

    // Configure with failback disabled - requires manual intervention
    MultiDbConnectionProvider testProvider = new MultiDbConnectionProvider(
        new MultiDbConfig.Builder(databaseConfigs).delayInBetweenFailoverAttempts(100)
            .maxNumFailoverAttempts(2)
            .commandRetry(MultiDbConfig.RetryConfig.builder().maxAttempts(1).build())
            .failbackSupported(false) // Disable automatic failback
            .build());

    try (MultiDbClient jedis = MultiDbClient.builder().connectionProvider(testProvider).build()) {
      // Verify initial connection works
      jedis.set("test-key", "initial-value");
      assertEquals("initial-value", jedis.get("test-key"));

      // Disable both databases to trigger permanent exception
      testProvider.getDatabase(endpointStandalone0.getHostAndPort()).setDisabled(true);
      testProvider.getDatabase(endpointStandalone1.getHostAndPort()).setDisabled(true);

      // Trigger temporary exceptions first
      assertThrows(JedisTemporarilyNotAvailableException.class, () -> jedis.get("test-key"));

      // Wait for permanent exception
      await().atMost(Durations.ONE_SECOND).pollInterval(Duration.ofMillis(50))
          .until(() -> (assertThrows(JedisFailoverException.class,
            () -> jedis.get("test-key")) instanceof JedisPermanentlyNotAvailableException));

      // Verify we're in permanent failure state
      assertThrows(JedisPermanentlyNotAvailableException.class, () -> jedis.get("test-key"));

      // Re-enable one database
      // Need to clear grace period and reset circuit breaker to make it healthy immediately
      MultiDbConnectionProvider.Database db1 = testProvider
          .getDatabase(endpointStandalone1.getHostAndPort());
      db1.setDisabled(false);
      db1.clearGracePeriod();
      // Reset circuit breaker to allow connections
      db1.getCircuitBreaker().transitionToClosedState();

      // Verify that automatic recovery does NOT happen
      // The system knows there's a healthy database available (db1), but the active database
      // is still the disabled one (db0), so commands will fail with JedisConnectionException
      // (not JedisPermanentlyNotAvailableException, because canIterateFrom returns true now)
      assertThrows(JedisConnectionException.class, () -> jedis.get("test-key"));

      // Manually trigger recovery by setting active database
      // This simulates application code checking isHealthy and calling setActiveDatabase
      Endpoint healthyEndpoint = endpointStandalone1.getHostAndPort();
      assertTrue(testProvider.isHealthy(healthyEndpoint),
        "Database should be healthy after re-enabling");
      testProvider.setActiveDatabase(healthyEndpoint);

      // After manual intervention, commands should succeed
      jedis.set("test-key", "recovered-value");
      assertEquals("recovered-value", jedis.get("test-key"));

      // Verify continued operation
      jedis.set("test-key", "final-value");
      assertEquals("final-value", jedis.get("test-key"));
    }
  }
}
