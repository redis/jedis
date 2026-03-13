package redis.clients.jedis;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.sentinel.api.SentinelInstanceClient;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration tests for {@link RedisSentinelClient}.
 */
public class RedisSentinelClientIT {

  private static final String MASTER_NAME = "mymaster";

  private static EndpointConfig primary;

  private static Set<HostAndPort> sentinels = new HashSet<>();

  @BeforeAll
  public static void prepareEndpoints() {
    EndpointConfig sentinel1 = Endpoints.getRedisEndpoint("sentinel-standalone2-1");
    EndpointConfig sentinel2 = Endpoints.getRedisEndpoint("sentinel-standalone2-3");
    sentinels = new HashSet<>(
        Arrays.asList(sentinel1.getHostAndPort(), sentinel2.getHostAndPort()));
    primary = Endpoints.getRedisEndpoint("standalone2-primary");
  }

  @Test
  public void testGetAvailableSentinel() throws Exception {
    EndpointConfig master = Endpoints.getRedisEndpoint("standalone2-primary");

    try (RedisSentinelClient client = RedisSentinelClient.builder()
        .clientConfig(primary.getClientConfigBuilder().build()).masterName(MASTER_NAME)
        .sentinels(sentinels).build()) {

      // Get an available Sentinel instance
      try (SentinelInstanceClient sentinelInstance = client.getAvailableSentinel()) {
        assertNotNull(sentinelInstance, "Should return a Sentinel instance");

        // Verify we can communicate with the Sentinel
        String pingResponse = sentinelInstance.ping();
        assertEquals("PONG", pingResponse, "Sentinel should respond to PING");

        // Verify we can get specific master info
        Map<String, String> masterInfo = sentinelInstance.sentinelMaster(MASTER_NAME);
        assertNotNull(masterInfo, "Should return master info");
        assertEquals(MASTER_NAME, masterInfo.get("name"), "Master name should match");
        assertEquals(String.valueOf(master.getPort()), masterInfo.get("port"),
          "Master port should match");
      }
    }
  }

  @Test
  public void testBasicOperations() {

    try (RedisSentinelClient client = RedisSentinelClient.builder()
        .clientConfig(primary.getClientConfigBuilder().build()).masterName(MASTER_NAME)
        .sentinels(sentinels).build()) {

      // Test basic Redis operations
      String key = "test-key";
      String value = "test-value";

      client.set(key, value);
      String retrieved = client.get(key);
      assertEquals(value, retrieved, "Should retrieve the same value");

      client.del(key);
    }
  }

  @Test
  public void testGetCurrentMaster() {
    EndpointConfig master = Endpoints.getRedisEndpoint("standalone2-primary");

    try (RedisSentinelClient client = RedisSentinelClient.builder()
        .clientConfig(primary.getClientConfigBuilder().build()).masterName(MASTER_NAME)
        .sentinels(sentinels).build()) {

      HostAndPort currentMaster = client.getCurrentMaster();
      assertNotNull(currentMaster, "Should return current master");
      assertEquals(master.getPort(), currentMaster.getPort(), "Master port should match");
    }
  }
}
