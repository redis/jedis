package redis.clients.jedis.mcf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.UnifiedJedis;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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

    private HealthCheckStrategy alwaysHealthyStrategy = new HealthCheckStrategy() {
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

    // ========== HealthStatus Tests ==========

    @Test
    void testHealthStatusIsHealthy() {
        assertTrue(HealthStatus.HEALTHY.isHealthy());
        assertFalse(HealthStatus.UNHEALTHY.isHealthy());
        assertFalse(HealthStatus.FORCED_UNHEALTHY.isHealthy());
    }

    @Test
    void testHealthStatusIsForcedUnhealthy() {
        assertFalse(HealthStatus.HEALTHY.isForcedUnhealthy());
        assertFalse(HealthStatus.UNHEALTHY.isForcedUnhealthy());
        assertTrue(HealthStatus.FORCED_UNHEALTHY.isForcedUnhealthy());
    }

    // ========== HostAndPort Tests ==========
    @Test
    void testHostAndPortEquality() {
        HostAndPort endpoint1 = new HostAndPort("localhost", 6379);
        HostAndPort endpoint2 = new HostAndPort("localhost", 6379);
        HostAndPort endpoint3 = new HostAndPort("localhost", 6380);

        assertEquals(endpoint1, endpoint2);
        assertNotEquals(endpoint1, endpoint3);
        assertEquals(endpoint1.hashCode(), endpoint2.hashCode());
    }

    // ========== HealthCheckCollection Tests ==========

    void testHealthCheckCollectionAdd() {
        HealthCheckCollection collection = new HealthCheckCollection();
        HealthCheck healthCheck = new HealthCheck(testEndpoint, mockStrategy, mockCallback);

        HealthCheck previous = collection.add(healthCheck);
        assertNull(previous);

        assertEquals(healthCheck, collection.get(testEndpoint));
    }

    @Test
    void testHealthCheckCollectionRemoveByEndpoint() {
        HealthCheckCollection collection = new HealthCheckCollection();
        HealthCheck healthCheck = new HealthCheck(testEndpoint, mockStrategy, mockCallback);

        collection.add(healthCheck);
        HealthCheck removed = collection.remove(testEndpoint);

        assertEquals(healthCheck, removed);
        assertNull(collection.get(testEndpoint));
    }

    @Test
    void testHealthCheckCollectionAddAll() {
        HealthCheckCollection collection = new HealthCheckCollection();
        HealthCheck[] healthChecks = { new HealthCheck(new HostAndPort("host1", 6379), mockStrategy, mockCallback),
                new HealthCheck(new HostAndPort("host2", 6379), mockStrategy, mockCallback) };

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
        HealthCheck healthCheck1 = new HealthCheck(testEndpoint, mockStrategy, mockCallback);
        HealthCheck healthCheck2 = new HealthCheck(testEndpoint, mockStrategy, mockCallback);

        collection.add(healthCheck1);
        HealthCheck previous = collection.add(healthCheck2);

        assertEquals(healthCheck1, previous);
        assertEquals(healthCheck2, collection.get(testEndpoint));
    }

    @Test
    void testHealthCheckCollectionRemoveByHealthCheck() {
        HealthCheckCollection collection = new HealthCheckCollection();
        HealthCheck healthCheck = new HealthCheck(testEndpoint, mockStrategy, mockCallback);

        collection.add(healthCheck);
        HealthCheck removed = collection.remove(healthCheck);

        assertEquals(healthCheck, removed);
        assertNull(collection.get(testEndpoint));
    }

    // ========== HealthCheck Tests ==========

    @Test
    void testHealthCheckStatusUpdate() throws InterruptedException {
        when(mockStrategy.getInterval()).thenReturn(100);
        when(mockStrategy.getTimeout()).thenReturn(50);
        when(mockStrategy.doHealthCheck(any(Endpoint.class))).thenReturn(HealthStatus.UNHEALTHY);

        CountDownLatch latch = new CountDownLatch(1);
        Consumer<HealthStatusChangeEvent> callback = event -> {
            assertEquals(HealthStatus.HEALTHY, event.getOldStatus());
            assertEquals(HealthStatus.UNHEALTHY, event.getNewStatus());
            latch.countDown();
        };

        HealthCheck healthCheck = new HealthCheck(testEndpoint, mockStrategy, callback);
        healthCheck.start();

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        healthCheck.stop();
    }

    @Test
    void testHealthCheckStop() {
        when(mockStrategy.getInterval()).thenReturn(1000);
        when(mockStrategy.getTimeout()).thenReturn(500);

        HealthCheck healthCheck = new HealthCheck(testEndpoint, mockStrategy, mockCallback);
        healthCheck.start();

        assertDoesNotThrow(() -> healthCheck.stop());
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
    void testHealthStatusManagerLifecycle() {
        HealthStatusManager manager = new HealthStatusManager();

        // Before adding health check
        assertEquals(HealthStatus.UNHEALTHY, manager.getHealthStatus(testEndpoint));

        manager.add(testEndpoint, alwaysHealthyStrategy);
        assertEquals(HealthStatus.HEALTHY, manager.getHealthStatus(testEndpoint));

        manager.remove(testEndpoint);
        assertEquals(HealthStatus.UNHEALTHY, manager.getHealthStatus(testEndpoint));
    }

    // ========== EchoStrategy Tests ==========

    @Test
    void testEchoStrategyCustomIntervalTimeout() {
        EchoStrategy strategy = new EchoStrategy(testEndpoint, testConfig, 2000, 1500);

        assertEquals(2000, strategy.getInterval());
        assertEquals(1500, strategy.getTimeout());
    }

    @Test
    void testEchoStrategyDefaultSupplier() {
        FailoverOptions.StrategySupplier supplier = EchoStrategy.DEFAULT;
        HealthCheckStrategy strategy = supplier.get(testEndpoint, testConfig);

        assertInstanceOf(EchoStrategy.class, strategy);
    }

    // ========== FailoverOptions Tests ==========

    @Test
    void testFailoverOptionsBuilder() {
        FailoverOptions options = FailoverOptions.builder().retryOnFailover(true).weight(2.5f).failback(true).build();

        assertTrue(options.isRetryOnFailover());
        assertEquals(2.5f, options.getWeight());
        assertTrue(options.isFailbackEnabled());
    }

    @Test
    void testFailoverOptionsDefaultValues() {
        FailoverOptions options = FailoverOptions.builder().build();

        assertFalse(options.isRetryOnFailover()); // Default is false
        assertEquals(1.0f, options.getWeight()); // Default weight
        assertFalse(options.isFailbackEnabled()); // Default is false
    }

    @Test
    void testFailoverOptionsWithHealthCheckStrategy() {
        HealthCheckStrategy customStrategy = mock(HealthCheckStrategy.class);

        FailoverOptions options = FailoverOptions.builder().healthCheckStrategy(customStrategy).build();

        FailoverOptions.StrategySupplier supplier = options.getStrategySupplier();
        assertNotNull(supplier);

        HealthCheckStrategy result = supplier.get(testEndpoint, testConfig);
        assertEquals(customStrategy, result);
    }

    @Test
    void testFailoverOptionsWithStrategySupplier() {
        FailoverOptions.StrategySupplier customSupplier = (hostAndPort, jedisClientConfig) -> {
            return mock(HealthCheckStrategy.class);
        };

        FailoverOptions options = FailoverOptions.builder().healthCheckStrategySupplier(customSupplier).build();

        assertEquals(customSupplier, options.getStrategySupplier());
    }

    @Test
    void testFailoverOptionsWithEnabledHealthCheck() {
        FailoverOptions options = FailoverOptions.builder().enableHealthCheck(true).build();

        FailoverOptions.StrategySupplier supplier = options.getStrategySupplier();
        assertNotNull(supplier);
        assertEquals(EchoStrategy.DEFAULT, supplier);
    }

    @Test
    void testFailoverOptionsWithDisabledHealthCheck() {
        FailoverOptions options = FailoverOptions.builder().enableHealthCheck(false).build();

        assertNull(options.getStrategySupplier());
    }

    // ========== Integration Tests ==========

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
    void testFailoverOptionsStrategySupplierPolymorphism() {
        // Test that the polymorphic design works correctly
        FailoverOptions.StrategySupplier supplier = (hostAndPort, jedisClientConfig) -> {
            if (jedisClientConfig != null) {
                return new EchoStrategy(hostAndPort, jedisClientConfig, 500, 250);
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
