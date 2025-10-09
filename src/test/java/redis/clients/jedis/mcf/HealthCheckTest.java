package redis.clients.jedis.mcf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Endpoint;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiDbConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.mcf.ProbingPolicy.BuiltIn;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class HealthCheckTest {

  @Mock
  private UnifiedJedis mockJedis;

  @Mock
  private HealthCheckStrategy mockStrategy;

  private final HealthCheckStrategy alwaysHealthyStrategy = new TestHealthCheckStrategy(100, 50, 1,
      BuiltIn.ANY_SUCCESS, 10, e -> HealthStatus.HEALTHY);

  @Mock
  private Consumer<HealthStatusChangeEvent> mockCallback;

  private HostAndPort testEndpoint;
  private JedisClientConfig testConfig;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    testEndpoint = new HostAndPort("localhost", 6379);
    testConfig = DefaultJedisClientConfig.builder().build();

    // Default stubs for mockStrategy used across tests
    when(mockStrategy.getNumProbes()).thenReturn(1);
    when(mockStrategy.getDelayInBetweenProbes()).thenReturn(100);
    when(mockStrategy.getPolicy()).thenReturn(BuiltIn.ANY_SUCCESS);
  }

  // ========== HealthCheckCollection Tests ==========

  @Test
  void testHealthCheckCollectionAdd() {
    HealthCheckCollection collection = new HealthCheckCollection();
    HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy, mockCallback);

    HealthCheck previous = collection.add(healthCheck);
    assertNull(previous);

    assertEquals(healthCheck, collection.get(testEndpoint));
  }

  @Test
  void testHealthCheckCollectionRemoveByEndpoint() {
    HealthCheckCollection collection = new HealthCheckCollection();
    HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy, mockCallback);

    collection.add(healthCheck);
    HealthCheck removed = collection.remove(testEndpoint);

    assertEquals(healthCheck, removed);
    assertNull(collection.get(testEndpoint));
  }

  @Test
  void testHealthCheckCollectionAddAll() {
    HealthCheckCollection collection = new HealthCheckCollection();
    HealthCheck[] healthChecks = {
        new HealthCheckImpl(new HostAndPort("host1", 6379), mockStrategy, mockCallback),
        new HealthCheckImpl(new HostAndPort("host2", 6379), mockStrategy, mockCallback) };

    HealthCheck[] previous = collection.addAll(healthChecks);

    assertNotNull(previous);
    assertEquals(2, previous.length);
    assertNull(previous[0]); // No previous health check for host1
    assertNull(previous[1]); // No previous health check for host2

    assertEquals(healthChecks[0], collection.get(new HostAndPort("host1", 6379)));
    assertEquals(healthChecks[1], collection.get(new HostAndPort("host2", 6379)));
  }

  @Test
  void testHealthCheckCollectionReplacement() {
    HealthCheckCollection collection = new HealthCheckCollection();
    HealthCheck healthCheck1 = new HealthCheckImpl(testEndpoint, mockStrategy, mockCallback);
    HealthCheck healthCheck2 = new HealthCheckImpl(testEndpoint, mockStrategy, mockCallback);

    collection.add(healthCheck1);
    HealthCheck previous = collection.add(healthCheck2);

    assertEquals(healthCheck1, previous);
    assertEquals(healthCheck2, collection.get(testEndpoint));
  }

  @Test
  void testHealthCheckCollectionRemoveByHealthCheck() {
    HealthCheckCollection collection = new HealthCheckCollection();
    HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy, mockCallback);

    collection.add(healthCheck);
    HealthCheck removed = collection.remove(healthCheck);

    assertEquals(healthCheck, removed);
    assertNull(collection.get(testEndpoint));
  }

  @Test
  void testHealthCheckCollectionClose() {
    HealthCheckCollection collection = new HealthCheckCollection();

    // Create mock health checks
    HealthCheck mockHealthCheck1 = spy(
      new HealthCheckImpl(testEndpoint, mockStrategy, mockCallback));

    collection.add(mockHealthCheck1);

    // Call close
    collection.close();

    // Verify stop was called on all health checks
    verify(mockHealthCheck1).stop();
  }
  // ========== HealthCheck Tests ==========

  @Test
  void testHealthCheckStatusUpdate() throws InterruptedException {
    when(mockStrategy.getInterval()).thenReturn(1);
    when(mockStrategy.getTimeout()).thenReturn(50);
    when(mockStrategy.doHealthCheck(any(Endpoint.class))).thenReturn(HealthStatus.UNHEALTHY);

    CountDownLatch latch = new CountDownLatch(1);
    Consumer<HealthStatusChangeEvent> callback = event -> {
      assertEquals(HealthStatus.UNKNOWN, event.getOldStatus());
      assertEquals(HealthStatus.UNHEALTHY, event.getNewStatus());
      latch.countDown();
    };

    HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy, callback);
    healthCheck.start();

    assertTrue(latch.await(2, TimeUnit.SECONDS));
    healthCheck.stop();
  }

  @Test
  void testSafeUpdateChecksDoNotTriggerFalseNotifications() {
    AtomicInteger notificationCount = new AtomicInteger(0);
    Consumer<HealthStatusChangeEvent> callback = event -> notificationCount.incrementAndGet();

    HealthCheckImpl healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy, callback);

    // Simulate concurrent health checks with different results
    healthCheck.safeUpdate(2000, HealthStatus.HEALTHY); // Newer timestamp
    healthCheck.safeUpdate(1000, HealthStatus.UNHEALTHY); // Older timestamp (should be ignored)

    // Should only have 1 notification (for the first update), not 2
    assertEquals(1, notificationCount.get());
    assertEquals(HealthStatus.HEALTHY, healthCheck.getStatus());
  }

  @Test
  void testSafeUpdateWithConcurrentResults() {
    AtomicInteger notificationCount = new AtomicInteger(0);
    Consumer<HealthStatusChangeEvent> callback = event -> notificationCount.incrementAndGet();

    HealthCheckImpl healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy, callback);

    // Test the exact scenario: newer result first, then older result
    healthCheck.safeUpdate(2000, HealthStatus.HEALTHY); // Should update and notify
    assertEquals(1, notificationCount.get());
    assertEquals(HealthStatus.HEALTHY, healthCheck.getStatus());

    healthCheck.safeUpdate(1000, HealthStatus.UNHEALTHY); // Should NOT update or notify
    assertEquals(1, notificationCount.get()); // Still 1, no additional notification
    assertEquals(HealthStatus.HEALTHY, healthCheck.getStatus()); // Status unchanged
  }

  @Test
  void testHealthCheckStop() {
    when(mockStrategy.getInterval()).thenReturn(1000);
    when(mockStrategy.getTimeout()).thenReturn(500);

    HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, mockStrategy, mockCallback);
    healthCheck.start();

    assertDoesNotThrow(healthCheck::stop);
  }

  // ========== HealthStatusManager Tests ==========

  @Test
  void testHealthStatusManagerRegisterListener() {
    HealthStatusManager manager = new HealthStatusManager();
    HealthStatusListener listener = mock(HealthStatusListener.class);

    manager.registerListener(listener);

    // Verify listener is registered by triggering an event
    manager.add(testEndpoint, alwaysHealthyStrategy);
    // Give some time for health check to run
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
    }

    verify(listener, atLeastOnce()).onStatusChange(any(HealthStatusChangeEvent.class));
  }

  @Test
  void testHealthStatusManagerUnregisterListener() {
    HealthStatusManager manager = new HealthStatusManager();
    HealthStatusListener listener = mock(HealthStatusListener.class);

    manager.registerListener(listener);
    manager.unregisterListener(listener);

    manager.add(testEndpoint, alwaysHealthyStrategy);

    // Give some time for potential health check
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
    }

    verify(listener, never()).onStatusChange(any(HealthStatusChangeEvent.class));
  }

  @Test
  void testHealthStatusManagerEndpointSpecificListener() {
    HealthStatusManager manager = new HealthStatusManager();
    HealthStatusListener listener = mock(HealthStatusListener.class);
    HostAndPort otherEndpoint = new HostAndPort("other", 6379);

    manager.registerListener(testEndpoint, listener);
    manager.add(testEndpoint, alwaysHealthyStrategy);
    manager.add(otherEndpoint, alwaysHealthyStrategy);

    // Give some time for health checks
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
    }

    // Listener should only receive events for testEndpoint
    verify(listener, atLeastOnce())
        .onStatusChange(argThat(event -> event.getEndpoint().equals(testEndpoint)));
  }

  @Test
  void testHealthStatusManagerLifecycle() throws InterruptedException {
    HealthStatusManager manager = new HealthStatusManager();

    // Before adding health check
    assertEquals(HealthStatus.UNKNOWN, manager.getHealthStatus(testEndpoint));

    // Set up event listener to wait for initial health check completion
    CountDownLatch healthCheckCompleteLatch = new CountDownLatch(1);
    HealthStatusListener listener = event -> healthCheckCompleteLatch.countDown();

    // Register listener before adding health check to capture the initial event
    manager.registerListener(testEndpoint, listener);

    HealthCheckStrategy delayedStrategy = new TestHealthCheckStrategy(2000, 1000, 3,
        BuiltIn.ALL_SUCCESS, 100, e -> HealthStatus.HEALTHY);

    // Add health check - this will start async health checking
    manager.add(testEndpoint, delayedStrategy);

    // Initially should still be UNKNOWN until first check completes
    assertEquals(HealthStatus.UNKNOWN, manager.getHealthStatus(testEndpoint));

    // Wait for initial health check to complete
    assertTrue(healthCheckCompleteLatch.await(2, TimeUnit.SECONDS),
      "Initial health check should complete within timeout");

    // Now should be HEALTHY after initial check
    assertEquals(HealthStatus.HEALTHY, manager.getHealthStatus(testEndpoint));

    // Clean up and verify removal
    manager.remove(testEndpoint);
    assertEquals(HealthStatus.UNKNOWN, manager.getHealthStatus(testEndpoint));
  }

  @Test
  void testHealthStatusManagerClose() {
    HealthCheckStrategy closeableStrategy = mock(HealthCheckStrategy.class);
    when(closeableStrategy.getNumProbes()).thenReturn(1);
    when(closeableStrategy.getInterval()).thenReturn(1000);
    when(closeableStrategy.getTimeout()).thenReturn(500);
    when(closeableStrategy.doHealthCheck(any(Endpoint.class))).thenReturn(HealthStatus.HEALTHY);

    HealthStatusManager manager = new HealthStatusManager();

    // Add health check
    manager.add(testEndpoint, closeableStrategy);

    // Close manager
    manager.close();

    // Verify health check is stopped
    verify(closeableStrategy).close();
  }

  // ========== PingStrategy Tests ==========

  @Test
  void testPingStrategyCustomIntervalTimeout() {
    try (PingStrategy strategy = new PingStrategy(testEndpoint, testConfig,
        HealthCheckStrategy.Config.builder().interval(2000).timeout(1500).delayInBetweenProbes(50)
            .numProbes(11).policy(BuiltIn.ANY_SUCCESS).build())) {
      assertEquals(2000, strategy.getInterval());
      assertEquals(1500, strategy.getTimeout());
      assertEquals(11, strategy.getNumProbes());
      assertEquals(BuiltIn.ANY_SUCCESS, strategy.getPolicy());
      assertEquals(50, strategy.getDelayInBetweenProbes());
    }
  }

  @Test
  void testPingStrategyDefaultSupplier() {
    MultiDbConfig.StrategySupplier supplier = PingStrategy.DEFAULT;
    HealthCheckStrategy strategy = supplier.get(testEndpoint, testConfig);

    assertInstanceOf(PingStrategy.class, strategy);
  }

  // ========== Failover configuration Tests ==========

  @Test
  void testNewFieldLocations() {
    // Test new field locations in DatabaseConfig and MultiDbConfig
    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(testEndpoint, testConfig).weight(2.5f).build();

    MultiDbConfig multiConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).retryOnFailover(true)
            .failbackSupported(false).build();

    assertEquals(2.5f, databaseConfig.getWeight());
    assertTrue(multiConfig.isRetryOnFailover());
    assertFalse(multiConfig.isFailbackSupported());
  }

  @Test
  void testDefaultValues() {
    // Test default values in DatabaseConfig
    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(testEndpoint, testConfig).build();

    assertEquals(1.0f, databaseConfig.getWeight()); // Default weight
    assertEquals(PingStrategy.DEFAULT, databaseConfig.getHealthCheckStrategySupplier()); // Default
                                                                                         // is null
                                                                                         // (no
                                                                                         // health
                                                                                         // check)

    // Test default values in MultiDbConfig
    MultiDbConfig multiConfig = new MultiDbConfig.Builder(
        new MultiDbConfig.DatabaseConfig[] { databaseConfig }).build();

    assertFalse(multiConfig.isRetryOnFailover()); // Default is false
    assertTrue(multiConfig.isFailbackSupported()); // Default is true
  }

  @Test
  void testDatabaseConfigWithHealthCheckStrategy() {
    HealthCheckStrategy customStrategy = mock(HealthCheckStrategy.class);

    MultiDbConfig.StrategySupplier supplier = (hostAndPort, jedisClientConfig) -> customStrategy;

    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(testEndpoint, testConfig).healthCheckStrategySupplier(supplier).build();

    assertNotNull(databaseConfig.getHealthCheckStrategySupplier());
    HealthCheckStrategy result = databaseConfig.getHealthCheckStrategySupplier().get(testEndpoint,
      testConfig);
    assertEquals(customStrategy, result);
  }

  @Test
  void testDatabaseConfigWithStrategySupplier() {
    MultiDbConfig.StrategySupplier customSupplier = (hostAndPort, jedisClientConfig) -> {
      return mock(HealthCheckStrategy.class);
    };

    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(testEndpoint, testConfig).healthCheckStrategySupplier(customSupplier).build();

    assertEquals(customSupplier, databaseConfig.getHealthCheckStrategySupplier());
  }

  @Test
  void testDatabaseConfigWithPingStrategy() {
    MultiDbConfig.StrategySupplier pingSupplier = (hostAndPort, jedisClientConfig) -> {
      return new PingStrategy(hostAndPort, jedisClientConfig);
    };

    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(testEndpoint, testConfig).healthCheckStrategySupplier(pingSupplier).build();

    MultiDbConfig.StrategySupplier supplier = databaseConfig.getHealthCheckStrategySupplier();
    assertNotNull(supplier);
    assertInstanceOf(PingStrategy.class, supplier.get(testEndpoint, testConfig));
  }

  @Test
  void testDatabaseConfigWithDefaultHealthCheck() {
    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(testEndpoint, testConfig).build(); // Should use default PingStrategy

    assertNotNull(databaseConfig.getHealthCheckStrategySupplier());
    assertEquals(PingStrategy.DEFAULT, databaseConfig.getHealthCheckStrategySupplier());
  }

  @Test
  void testDatabaseConfigWithDisabledHealthCheck() {
    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(testEndpoint, testConfig).healthCheckEnabled(false).build();

    assertNull(databaseConfig.getHealthCheckStrategySupplier());
  }

  @Test
  void testDatabaseConfigHealthCheckEnabledExplicitly() {
    MultiDbConfig.DatabaseConfig databaseConfig = MultiDbConfig.DatabaseConfig
        .builder(testEndpoint, testConfig).healthCheckEnabled(true).build();

    assertNotNull(databaseConfig.getHealthCheckStrategySupplier());
    assertEquals(PingStrategy.DEFAULT, databaseConfig.getHealthCheckStrategySupplier());
  }

  // ========== Integration Tests ==========
  @Test
  @Timeout(5)
  void testHealthCheckRecoversAfterException() throws InterruptedException {
    // Create a strategy that alternates between exception/UNHEALTHY and HEALTHY
    AtomicBoolean isHealthy = new AtomicBoolean(true);
    AtomicInteger exceptionOccurred = new AtomicInteger(0);
    int exceptionLimit = 2;
    HealthCheckStrategy alternatingStrategy = new TestHealthCheckStrategy(
        HealthCheckStrategy.Config.builder().interval(1).timeout(5).numProbes(1).build(), e -> {
          if (isHealthy.get()) {
            isHealthy.set(false);
            if (exceptionOccurred.getAndIncrement() < exceptionLimit) {
              throw new RuntimeException("Simulated exception");
            } else {
              return HealthStatus.UNHEALTHY;
            }
          } else {
            isHealthy.set(true);
            return HealthStatus.HEALTHY;
          }
        });

    // Wait for 2 status changes,
    // it will start with unhealthy(due to simulated exception) and then switch to healthy
    CountDownLatch statusChangeLatch = new CountDownLatch(2);
    HealthStatusListener listener = event -> statusChangeLatch.countDown();

    HealthStatusManager manager = new HealthStatusManager();
    manager.registerListener(listener);
    manager.add(testEndpoint, alternatingStrategy);

    assertTrue(statusChangeLatch.await(1, TimeUnit.SECONDS));

    manager.remove(testEndpoint);
  }

  @Test
  @Timeout(5)
  void testHealthCheckIntegration() throws InterruptedException {
    // Create a mock strategy that alternates between healthy and unhealthy
    AtomicReference<HealthStatus> statusToReturn = new AtomicReference<>(HealthStatus.HEALTHY);
    HealthCheckStrategy alternatingStrategy = new TestHealthCheckStrategy(100, 50, 1,
        BuiltIn.ALL_SUCCESS, 10, e -> {
          HealthStatus current = statusToReturn.get();
          statusToReturn
              .set(current == HealthStatus.HEALTHY ? HealthStatus.UNHEALTHY : HealthStatus.HEALTHY);
          return current;
        });

    CountDownLatch statusChangeLatch = new CountDownLatch(2); // Wait for 2 status changes
    HealthStatusListener listener = event -> statusChangeLatch.countDown();

    HealthStatusManager manager = new HealthStatusManager();
    manager.registerListener(listener);
    manager.add(testEndpoint, alternatingStrategy);

    assertTrue(statusChangeLatch.await(3, TimeUnit.SECONDS));

    manager.remove(testEndpoint);
  }

  @Test
  void testStrategySupplierPolymorphism() {
    // Test that the polymorphic design works correctly
    MultiDbConfig.StrategySupplier supplier = (hostAndPort, jedisClientConfig) -> {
      if (jedisClientConfig != null) {
        return new PingStrategy(hostAndPort, jedisClientConfig,
            HealthCheckStrategy.Config.builder().interval(500).timeout(250).numProbes(1).build());
      } else {
        return new PingStrategy(hostAndPort, DefaultJedisClientConfig.builder().build());
      }
    };

    // Test with config
    HealthCheckStrategy strategyWithConfig = supplier.get(testEndpoint, testConfig);
    assertNotNull(strategyWithConfig);
    assertEquals(500, strategyWithConfig.getInterval());
    assertEquals(250, strategyWithConfig.getTimeout());

    // Test without config
    HealthCheckStrategy strategyWithoutConfig = supplier.get(testEndpoint, null);
    assertNotNull(strategyWithoutConfig);
    assertEquals(5000, strategyWithoutConfig.getInterval()); // Default values
    assertEquals(1000, strategyWithoutConfig.getTimeout());
  }

  // ========== Retry Logic Unit Tests ==========

  @Test
  void testRetryLogic_SuccessOnFirstAttempt() throws InterruptedException {
    AtomicInteger callCount = new AtomicInteger(0);
    TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(100, 50, 2, BuiltIn.ANY_SUCCESS,
        10, e -> {
          callCount.incrementAndGet();
          return HealthStatus.HEALTHY; // Always succeeds
        });

    CountDownLatch latch = new CountDownLatch(1);
    Consumer<HealthStatusChangeEvent> callback = event -> latch.countDown();

    HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, strategy, callback);
    healthCheck.start();

    assertTrue(latch.await(2, TimeUnit.SECONDS));
    assertEquals(HealthStatus.HEALTHY, healthCheck.getStatus());

    // Should only call doHealthCheck once (no retries needed)
    assertEquals(1, callCount.get());

    healthCheck.stop();
  }

  @Test
  void testRetryLogic_FailThenSucceedOnRetry() throws InterruptedException {
    AtomicInteger callCount = new AtomicInteger(0);
    TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(100, 50, 2, BuiltIn.ANY_SUCCESS,
        10, e -> {
          int attempt = callCount.incrementAndGet();
          if (attempt == 1) {
            throw new RuntimeException("First attempt fails");
          }
          return HealthStatus.HEALTHY; // Second attempt succeeds
        });

    CountDownLatch latch = new CountDownLatch(1);
    Consumer<HealthStatusChangeEvent> callback = event -> {
      if (event.getNewStatus() == HealthStatus.HEALTHY) {
        latch.countDown();
      }
    };

    HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, strategy, callback);
    healthCheck.start();

    assertTrue(latch.await(3, TimeUnit.SECONDS));
    assertEquals(HealthStatus.HEALTHY, healthCheck.getStatus());

    // Should call doHealthCheck twice (first fails, second succeeds)
    assertEquals(2, callCount.get());

    healthCheck.stop();
  }

  @Test
  void testRetryLogic_ExhaustAllProbesAndFail() throws InterruptedException {
    AtomicInteger callCount = new AtomicInteger(0);
    TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(100, 50, 3, BuiltIn.ANY_SUCCESS,
        10, e -> {
          callCount.incrementAndGet();
          throw new RuntimeException("Always fails");
        });

    CountDownLatch latch = new CountDownLatch(1);
    Consumer<HealthStatusChangeEvent> callback = event -> {
      if (event.getNewStatus() == HealthStatus.UNHEALTHY) {
        latch.countDown();
      }
    };

    HealthCheck healthCheck = new HealthCheckImpl(testEndpoint, strategy, callback);
    healthCheck.start();

    assertTrue(latch.await(3, TimeUnit.SECONDS));
    assertEquals(HealthStatus.UNHEALTHY, healthCheck.getStatus());

    // Should call doHealthCheck 3 times (all probes fail)
    assertEquals(3, callCount.get());

    healthCheck.stop();
  }

  @Test
  void testRetryLogic_ZeroProbes() throws InterruptedException {
    AtomicInteger callCount = new AtomicInteger(0);
    TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(100, 50, 0, BuiltIn.ANY_SUCCESS,
        10, e -> {
          callCount.incrementAndGet();
          throw new RuntimeException("Fails");
        });

    CountDownLatch latch = new CountDownLatch(1);
    Consumer<HealthStatusChangeEvent> callback = event -> {
      if (event.getNewStatus() == HealthStatus.UNHEALTHY) {
        latch.countDown();
      }
    };

    assertThrows(IllegalArgumentException.class,
      () -> new HealthCheckImpl(testEndpoint, strategy, callback));
  }

  @Test
  void testRetryLogic_NegativeProbes() throws InterruptedException {
    AtomicInteger callCount = new AtomicInteger(0);
    TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(100, 50, -1, BuiltIn.ANY_SUCCESS,
        10, e -> {
          callCount.incrementAndGet();
          throw new RuntimeException("Fails");
        });

    CountDownLatch latch = new CountDownLatch(1);
    Consumer<HealthStatusChangeEvent> callback = event -> {
      if (event.getNewStatus() == HealthStatus.UNHEALTHY) {
        latch.countDown();
      }
    };

    assertThrows(IllegalArgumentException.class,
      () -> new HealthCheckImpl(testEndpoint, strategy, callback));
  }

  /**
   * <p>
   * - Verifies that the health check probes stop after the first probe when the scheduler thread is
   * interrupted.
   * <p>
   * - The scheduler thread is the one that calls healthCheck(), which in turn calls
   * doHealthCheck().
   * <p>
   * - This test interrupts the scheduler thread while it is waiting on the future from the first
   * probe.
   * <p>
   * - The health check operation itself is not interrupted. This test does not validate
   * interruption of the health check operation itself, as that is not the responsibility of the
   * HealthCheckImpl.
   */
  @Test
  void testRetryLogic_InterruptionStopsProbes() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);
    CountDownLatch schedulerTaskStarted = new CountDownLatch(1);
    CountDownLatch statusChanged = new CountDownLatch(1);

    Thread[] schedulerThread = new Thread[1];

    final int OPERATION_TIMEOUT = 1000;
    final int LESS_THAN_OPERATION_TIMEOUT = 800;
    final int NUM_PROBES = 3;
    // Long interval so no second run, generous timeout so we can interrupt while waiting
    Function<Endpoint, HealthStatus> healthCheckOperation = e -> {
      callCount.incrementAndGet();
      try {
        Thread.sleep(LESS_THAN_OPERATION_TIMEOUT); // keep worker busy so scheduler waits on
                                                   // future.get
      } catch (InterruptedException ie) {
      }
      return HealthStatus.UNHEALTHY;
    };

    // Override getPolicy() to capture the scheduler thread
    HealthCheckStrategy strategy = new TestHealthCheckStrategy(5000, OPERATION_TIMEOUT, NUM_PROBES,
        BuiltIn.ANY_SUCCESS, 10, healthCheckOperation) {
      public ProbingPolicy getPolicy() {
        schedulerThread[0] = Thread.currentThread();
        schedulerTaskStarted.countDown();
        return super.getPolicy();
      }
    };

    Consumer<HealthStatusChangeEvent> callback = evt -> statusChanged.countDown();

    HealthCheckImpl hc = new HealthCheckImpl(testEndpoint, strategy, callback);
    hc.start();

    // Ensure first probe is in flight (scheduler is blocked on future.get)
    assertTrue(schedulerTaskStarted.await(1, TimeUnit.SECONDS), "Task should have started");

    // Interrupt the scheduler thread that runs HealthCheckImpl.healthCheck()
    schedulerThread[0].interrupt();

    // No status change should be published because healthCheck() returns early without safeUpdate
    assertFalse(statusChanged.await(hc.getMaxWaitFor(), TimeUnit.MILLISECONDS),
      "No status change expected");
    assertEquals(HealthStatus.UNKNOWN, hc.getStatus());

    // Only the first probe should have been attempted
    int calls = callCount.get();
    assertTrue(calls <= 1, "Only one probe should have been attempted: " + calls);

    hc.stop();
  }

  // ========== ProbingPolicy Unit Tests ==========
  @Test
  void testPolicy_AllSuccess_StopsOnFirstFailure() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);
    CountDownLatch unhealthyLatch = new CountDownLatch(1);

    TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(
        HealthCheckStrategy.Config.builder().interval(5).timeout(200).numProbes(3)
            .policy(BuiltIn.ALL_SUCCESS).delayInBetweenProbes(5).build(),
        e -> {
          int c = callCount.incrementAndGet();
          return c == 1 ? HealthStatus.UNHEALTHY : HealthStatus.HEALTHY;
        });

    HealthCheckImpl hc = new HealthCheckImpl(testEndpoint, strategy, evt -> {
      if (evt.getNewStatus() == HealthStatus.UNHEALTHY) unhealthyLatch.countDown();
    });

    hc.start();
    assertTrue(unhealthyLatch.await(1, TimeUnit.SECONDS));
    assertEquals(HealthStatus.UNHEALTHY, hc.getStatus());
    assertEquals(1, callCount.get(), "ALL_SUCCESS should stop after first failure");
    hc.stop();
  }

  @Test
  void testPolicy_Majority_EarlySuccessStopsAtThree() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);
    CountDownLatch healthyLatch = new CountDownLatch(1);

    TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(
        HealthCheckStrategy.Config.builder().interval(5000).timeout(200).numProbes(5)
            .policy(BuiltIn.MAJORITY_SUCCESS).delayInBetweenProbes(5).build(),
        e -> {
          int c = callCount.incrementAndGet();
          return c <= 3 ? HealthStatus.HEALTHY : HealthStatus.UNHEALTHY;
        });

    HealthCheckImpl hc = new HealthCheckImpl(testEndpoint, strategy, evt -> {
      if (evt.getNewStatus() == HealthStatus.HEALTHY) healthyLatch.countDown();
    });

    hc.start();
    assertTrue(healthyLatch.await(1, TimeUnit.SECONDS));
    assertEquals(HealthStatus.HEALTHY, hc.getStatus());
    assertEquals(3, callCount.get(), "MAJORITY early success should stop after 3 successes");
    hc.stop();
  }

  @Test
  void testPolicy_Majority_EarlyFailStopsAtTwo() throws Exception {
    AtomicInteger callCount = new AtomicInteger(0);
    CountDownLatch unhealthyLatch = new CountDownLatch(1);

    TestHealthCheckStrategy strategy = new TestHealthCheckStrategy(
        HealthCheckStrategy.Config.builder().interval(5000).timeout(200).numProbes(4)
            .policy(BuiltIn.MAJORITY_SUCCESS).delayInBetweenProbes(5).build(),
        e -> {
          int c = callCount.incrementAndGet();
          return c <= 2 ? HealthStatus.UNHEALTHY : HealthStatus.HEALTHY;
        });

    HealthCheckImpl hc = new HealthCheckImpl(testEndpoint, strategy, evt -> {
      if (evt.getNewStatus() == HealthStatus.UNHEALTHY) unhealthyLatch.countDown();
    });

    hc.start();
    assertTrue(unhealthyLatch.await(1, TimeUnit.SECONDS));
    assertEquals(HealthStatus.UNHEALTHY, hc.getStatus());
    assertEquals(2, callCount.get(), "MAJORITY early fail should stop when majority impossible");
    hc.stop();
  }

}
