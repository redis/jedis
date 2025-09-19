package redis.clients.jedis.providers;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.mcf.HealthCheckStrategy;
import redis.clients.jedis.mcf.HealthStatus;
import redis.clients.jedis.mcf.HealthStatusListener;
import redis.clients.jedis.mcf.HealthStatusManager;
import redis.clients.jedis.mcf.TestHealthCheckStrategy;

public class HealthStatusManagerTest {

  private final HostAndPort endpoint = new HostAndPort("localhost", 6379);

  @Test
  void manager_event_emission_order_basic() throws InterruptedException {
    HealthStatusManager manager = new HealthStatusManager();

    CountDownLatch eventLatch = new CountDownLatch(1);
    HealthStatusListener listener = event -> eventLatch.countDown();

    manager.registerListener(endpoint, listener);

    HealthCheckStrategy immediateHealthy = new TestHealthCheckStrategy(
        HealthCheckStrategy.Config.builder().interval(50).timeout(25).build(),
        e -> HealthStatus.HEALTHY);

    manager.add(endpoint, immediateHealthy);

    assertTrue(eventLatch.await(2, TimeUnit.SECONDS), "Should receive health status event");

    manager.remove(endpoint);
  }

  @Test
  void manager_hasHealthCheck_add_remove() {
    HealthStatusManager manager = new HealthStatusManager();
    assertFalse(manager.hasHealthCheck(endpoint));

    HealthCheckStrategy strategy = new TestHealthCheckStrategy(
        HealthCheckStrategy.Config.builder().interval(50).timeout(25).build(),
        e -> HealthStatus.HEALTHY);

    manager.add(endpoint, strategy);
    assertTrue(manager.hasHealthCheck(endpoint));
    manager.remove(endpoint);
    assertFalse(manager.hasHealthCheck(endpoint));
  }
}
