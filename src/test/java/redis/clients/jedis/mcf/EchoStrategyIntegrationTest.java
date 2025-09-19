package redis.clients.jedis.mcf;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@Tag("failover")
public class EchoStrategyIntegrationTest {

  private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("redis-failover-1");
  private static final HostAndPort proxyHostAndPort = endpoint.getHostAndPort();
  private static final ToxiproxyClient tp = new ToxiproxyClient("localhost", 8474);
  private static Proxy redisProxy;

  @BeforeAll
  public static void setupProxy() throws IOException {
    if (tp.getProxyOrNull("redis-health-test") != null) {
      tp.getProxy("redis-health-test").delete();
    }
    redisProxy = tp.createProxy("redis-health-test", "0.0.0.0:29379", "redis-failover-1:9379");
  }

  @AfterAll
  public static void cleanupProxy() throws IOException {
    if (redisProxy != null) {
      redisProxy.delete();
    }
  }

  @BeforeEach
  public void resetProxy() throws IOException {
    redisProxy.enable();
    redisProxy.toxics().getAll().forEach(toxic -> {
      try {
        toxic.remove();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
  }

  @Test
  public void testEchoStrategyRecoversAfterDisconnect() throws Exception {
    JedisClientConfig config = DefaultJedisClientConfig.builder().socketTimeoutMillis(1000)
        .connectionTimeoutMillis(1000).build();
    try (EchoStrategy strategy = new EchoStrategy(proxyHostAndPort, config,
        HealthCheckStrategy.Config.create())) {

      // Initial health check should work
      HealthStatus initialStatus = strategy.doHealthCheck(proxyHostAndPort);
      assertEquals(HealthStatus.HEALTHY, initialStatus);

      // Disable the proxy to simulate network failure
      redisProxy.disable();

      // Health check should now fail - this will expose the bug
      assertThrows(JedisException.class, () -> strategy.doHealthCheck(proxyHostAndPort));

      // Re-enable proxy
      redisProxy.enable();
      // Health check should recover
      HealthStatus statusAfterEnable = strategy.doHealthCheck(proxyHostAndPort);
      assertEquals(HealthStatus.HEALTHY, statusAfterEnable);
    }

  }

  @Test
  public void testEchoStrategyWithConnectionTimeout() throws Exception {
    JedisClientConfig config = DefaultJedisClientConfig.builder().socketTimeoutMillis(100)
        .connectionTimeoutMillis(100).build();

    try (EchoStrategy strategy = new EchoStrategy(proxyHostAndPort, config,
        HealthCheckStrategy.Config.builder().interval(1000).timeout(500).numProbes(1).build())) {

      // Initial health check should work
      assertEquals(HealthStatus.HEALTHY, strategy.doHealthCheck(proxyHostAndPort));

      // Add latency toxic to simulate slow network
      redisProxy.toxics().latency("slow-connection", ToxicDirection.DOWNSTREAM, 1000);

      // Health check should timeout and return unhealthy
      assertThrows(JedisException.class, () -> strategy.doHealthCheck(proxyHostAndPort));

      // Remove toxic
      redisProxy.toxics().get("slow-connection").remove();

      // Health check should recover
      HealthStatus recoveredStatus = strategy.doHealthCheck(proxyHostAndPort);
      assertEquals(HealthStatus.HEALTHY, recoveredStatus,
        "Health check should recover from high latency");
    }
  }

  @Test
  public void testConnectionDropDuringHealthCheck() throws Exception {
    JedisClientConfig config = DefaultJedisClientConfig.builder().socketTimeoutMillis(2000).build();
    try (EchoStrategy strategy = new EchoStrategy(proxyHostAndPort, config,
        HealthCheckStrategy.Config.create())) {

      // Initial health check
      assertEquals(HealthStatus.HEALTHY, strategy.doHealthCheck(proxyHostAndPort));

      // Simulate connection drop by limiting data transfer
      redisProxy.toxics().limitData("connection-drop", ToxicDirection.UPSTREAM, 10);

      // This should fail due to connection issues
      assertThrows(JedisException.class, () -> strategy.doHealthCheck(proxyHostAndPort));

      // Remove toxic
      redisProxy.toxics().get("connection-drop").remove();

      // Health check should recover
      HealthStatus afterRecovery = strategy.doHealthCheck(proxyHostAndPort);
      assertEquals(HealthStatus.HEALTHY, afterRecovery);
    }
  }
}