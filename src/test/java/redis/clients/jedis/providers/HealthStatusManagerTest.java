package redis.clients.jedis.providers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Endpoint;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.mcf.HealthCheckStrategy;
import redis.clients.jedis.mcf.HealthStatus;
import redis.clients.jedis.mcf.HealthStatusListener;
import redis.clients.jedis.mcf.HealthStatusManager;

public class HealthStatusManagerTest {

    private final HostAndPort endpoint = new HostAndPort("localhost", 6379);
    private final JedisClientConfig config = DefaultJedisClientConfig.builder().build();

    @Test
    void manager_event_emission_order_basic() throws InterruptedException {
        HealthStatusManager manager = new HealthStatusManager();

        CountDownLatch eventLatch = new CountDownLatch(1);
        HealthStatusListener listener = event -> eventLatch.countDown();

        manager.registerListener(endpoint, listener);

        HealthCheckStrategy immediateHealthy = new HealthCheckStrategy() {
            @Override
            public int getInterval() {
                return 50;
            }

            @Override
            public int getTimeout() {
                return 25;
            }

            @Override
            public HealthStatus doHealthCheck(Endpoint endpoint) {
                return HealthStatus.HEALTHY;
            }
        };

        manager.add(endpoint, immediateHealthy);

        assertTrue(eventLatch.await(2, TimeUnit.SECONDS), "Should receive health status event");

        manager.remove(endpoint);
    }

    @Test
    void manager_hasHealthCheck_add_remove() {
        HealthStatusManager manager = new HealthStatusManager();
        assertFalse(manager.hasHealthCheck(endpoint));

        HealthCheckStrategy strategy = new HealthCheckStrategy() {
            @Override
            public int getInterval() {
                return 50;
            }

            @Override
            public int getTimeout() {
                return 25;
            }

            @Override
            public HealthStatus doHealthCheck(Endpoint endpoint) {
                return HealthStatus.HEALTHY;
            }
        };

        manager.add(endpoint, strategy);
        assertTrue(manager.hasHealthCheck(endpoint));
        manager.remove(endpoint);
        assertFalse(manager.hasHealthCheck(endpoint));
    }
}
