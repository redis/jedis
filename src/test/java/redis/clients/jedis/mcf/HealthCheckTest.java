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
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.UnifiedJedis;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

public class HealthCheckTest {

    @Mock
    private UnifiedJedis mockJedis;

    @Mock
    private HealthCheckStrategy mockStrategy;

    private final HealthCheckStrategy alwaysHealthyStrategy = new HealthCheckStrategy() {
        @Override
        public int getInterval() {
            return 100;
        }

        @Override
        public int getTimeout() {
            return 50;
        }

        @Override
        public HealthStatus doHealthCheck(Endpoint endpoint) {
            return HealthStatus.HEALTHY;
        }
    };

    @Mock
    private Consumer<HealthStatusChangeEvent> mockCallback;

    private HostAndPort testEndpoint;
    private JedisClientConfig testConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testEndpoint = new HostAndPort("localhost", 6379);
        testConfig = DefaultJedisClientConfig.builder().build();
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
        HealthCheck[] healthChecks = { new HealthCheckImpl(new HostAndPort("host1", 6379), mockStrategy, mockCallback),
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
        HealthCheck mockHealthCheck1 = spy(new HealthCheckImpl(testEndpoint, mockStrategy, mockCallback));

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
        healthCheck.safeUpdate(2000, HealthStatus.HEALTHY);   // Newer timestamp
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
        healthCheck.safeUpdate(2000, HealthStatus.HEALTHY);   // Should update and notify
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
        verify(listener, atLeastOnce()).onStatusChange(argThat(event -> event.getEndpoint().equals(testEndpoint)));
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

        // Add health check - this will start async health checking
        manager.add(testEndpoint, alwaysHealthyStrategy);

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

    // ========== EchoStrategy Tests ==========

    @Test
    void testEchoStrategyCustomIntervalTimeout() {
        EchoStrategy strategy = new EchoStrategy(testEndpoint, testConfig,
            new HealthCheckStrategy.Config(2000, 1500, 1000));

        assertEquals(2000, strategy.getInterval());
        assertEquals(1500, strategy.getTimeout());
    }

    @Test
    void testEchoStrategyDefaultSupplier() {
        MultiClusterClientConfig.StrategySupplier supplier = EchoStrategy.DEFAULT;
        HealthCheckStrategy strategy = supplier.get(testEndpoint, testConfig);

        assertInstanceOf(EchoStrategy.class, strategy);
    }

    // ========== Failover configuration Tests ==========

    @Test
    void testNewFieldLocations() {
        // Test new field locations in ClusterConfig and MultiClusterClientConfig
        MultiClusterClientConfig.ClusterConfig clusterConfig = MultiClusterClientConfig.ClusterConfig
            .builder(testEndpoint, testConfig).weight(2.5f).build();

        MultiClusterClientConfig multiConfig = new MultiClusterClientConfig.Builder(
            new MultiClusterClientConfig.ClusterConfig[] { clusterConfig }).retryOnFailover(true)
                .failbackSupported(false).build();

        assertEquals(2.5f, clusterConfig.getWeight());
        assertTrue(multiConfig.isRetryOnFailover());
        assertFalse(multiConfig.isFailbackSupported());
    }

    @Test
    void testDefaultValues() {
        // Test default values in ClusterConfig
        MultiClusterClientConfig.ClusterConfig clusterConfig = MultiClusterClientConfig.ClusterConfig
            .builder(testEndpoint, testConfig).build();

        assertEquals(1.0f, clusterConfig.getWeight()); // Default weight
        assertEquals(EchoStrategy.DEFAULT, clusterConfig.getHealthCheckStrategySupplier()); // Default is null (no
                                                                                            // health check)

        // Test default values in MultiClusterClientConfig
        MultiClusterClientConfig multiConfig = new MultiClusterClientConfig.Builder(
            new MultiClusterClientConfig.ClusterConfig[] { clusterConfig }).build();

        assertFalse(multiConfig.isRetryOnFailover()); // Default is false
        assertTrue(multiConfig.isFailbackSupported()); // Default is true
    }

    @Test
    void testClusterConfigWithHealthCheckStrategy() {
        HealthCheckStrategy customStrategy = mock(HealthCheckStrategy.class);

        MultiClusterClientConfig.StrategySupplier supplier = (hostAndPort, jedisClientConfig) -> customStrategy;

        MultiClusterClientConfig.ClusterConfig clusterConfig = MultiClusterClientConfig.ClusterConfig
            .builder(testEndpoint, testConfig).healthCheckStrategySupplier(supplier).build();

        assertNotNull(clusterConfig.getHealthCheckStrategySupplier());
        HealthCheckStrategy result = clusterConfig.getHealthCheckStrategySupplier().get(testEndpoint, testConfig);
        assertEquals(customStrategy, result);
    }

    @Test
    void testClusterConfigWithStrategySupplier() {
        MultiClusterClientConfig.StrategySupplier customSupplier = (hostAndPort, jedisClientConfig) -> {
            return mock(HealthCheckStrategy.class);
        };

        MultiClusterClientConfig.ClusterConfig clusterConfig = MultiClusterClientConfig.ClusterConfig
            .builder(testEndpoint, testConfig).healthCheckStrategySupplier(customSupplier).build();

        assertEquals(customSupplier, clusterConfig.getHealthCheckStrategySupplier());
    }

    @Test
    void testClusterConfigWithEchoStrategy() {
        MultiClusterClientConfig.StrategySupplier echoSupplier = (hostAndPort, jedisClientConfig) -> {
            return new EchoStrategy(hostAndPort, jedisClientConfig);
        };

        MultiClusterClientConfig.ClusterConfig clusterConfig = MultiClusterClientConfig.ClusterConfig
            .builder(testEndpoint, testConfig).healthCheckStrategySupplier(echoSupplier).build();

        MultiClusterClientConfig.StrategySupplier supplier = clusterConfig.getHealthCheckStrategySupplier();
        assertNotNull(supplier);
        assertInstanceOf(EchoStrategy.class, supplier.get(testEndpoint, testConfig));
    }

    @Test
    void testClusterConfigWithDefaultHealthCheck() {
        MultiClusterClientConfig.ClusterConfig clusterConfig = MultiClusterClientConfig.ClusterConfig
            .builder(testEndpoint, testConfig).build(); // Should use default EchoStrategy

        assertNotNull(clusterConfig.getHealthCheckStrategySupplier());
        assertEquals(EchoStrategy.DEFAULT, clusterConfig.getHealthCheckStrategySupplier());
    }

    @Test
    void testClusterConfigWithDisabledHealthCheck() {
        MultiClusterClientConfig.ClusterConfig clusterConfig = MultiClusterClientConfig.ClusterConfig
            .builder(testEndpoint, testConfig).healthCheckEnabled(false).build();

        assertNull(clusterConfig.getHealthCheckStrategySupplier());
    }

    @Test
    void testClusterConfigHealthCheckEnabledExplicitly() {
        MultiClusterClientConfig.ClusterConfig clusterConfig = MultiClusterClientConfig.ClusterConfig
            .builder(testEndpoint, testConfig).healthCheckEnabled(true).build();

        assertNotNull(clusterConfig.getHealthCheckStrategySupplier());
        assertEquals(EchoStrategy.DEFAULT, clusterConfig.getHealthCheckStrategySupplier());
    }

    // ========== Integration Tests ==========
    @Test
    @Timeout(5)
    void testHealthCheckRecoversAfterException() throws InterruptedException {
        // Create a mock strategy that alternates between healthy and throwing an exception
        HealthCheckStrategy alternatingStrategy = new HealthCheckStrategy() {
            volatile boolean isHealthy = true;

            @Override
            public int getInterval() {
                return 1;
            }

            @Override
            public int getTimeout() {
                return 5;
            }

            @Override
            public HealthStatus doHealthCheck(Endpoint endpoint) {
                if (isHealthy) {
                    isHealthy = false;
                    throw new RuntimeException("Simulated exception");
                } else {
                    isHealthy = true;
                    return HealthStatus.HEALTHY;
                }
            }
        };

        CountDownLatch statusChangeLatch = new CountDownLatch(2); // Wait for 2 status changes
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
        HealthCheckStrategy alternatingStrategy = new HealthCheckStrategy() {
            @Override
            public int getInterval() {
                return 100;
            }

            @Override
            public int getTimeout() {
                return 50;
            }

            @Override
            public HealthStatus doHealthCheck(Endpoint endpoint) {
                HealthStatus current = statusToReturn.get();
                statusToReturn.set(current == HealthStatus.HEALTHY ? HealthStatus.UNHEALTHY : HealthStatus.HEALTHY);
                return current;
            }
        };

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
        MultiClusterClientConfig.StrategySupplier supplier = (hostAndPort, jedisClientConfig) -> {
            if (jedisClientConfig != null) {
                return new EchoStrategy(hostAndPort, jedisClientConfig, new HealthCheckStrategy.Config(500, 250, 1));
            } else {
                return new EchoStrategy(hostAndPort, DefaultJedisClientConfig.builder().build());
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
        assertEquals(1000, strategyWithoutConfig.getInterval()); // Default values
        assertEquals(1000, strategyWithoutConfig.getTimeout());
    }
}
