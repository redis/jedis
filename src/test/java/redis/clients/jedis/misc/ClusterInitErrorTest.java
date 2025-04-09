package redis.clients.jedis.misc;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ClusterInitErrorTest {

  private static final String INIT_NO_ERROR_PROPERTY = "jedis.cluster.initNoError";

  @AfterEach
  public void cleanUp() {
    System.getProperties().remove(INIT_NO_ERROR_PROPERTY);
  }

  @Test
  public void initError() {
    assertNull(System.getProperty(INIT_NO_ERROR_PROPERTY));
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");
    assertThrows(JedisClusterOperationException.class, () -> {
      try (JedisCluster cluster = new JedisCluster(
          Collections.singleton(endpoint.getHostAndPort()),
          endpoint.getClientConfigBuilder().build())) {
        // Intentionally left empty because the exception is expected
      }
    });
  }

  @Test
  public void initNoError() {
    System.setProperty(INIT_NO_ERROR_PROPERTY, "");
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");
    try (JedisCluster cluster = new JedisCluster(
        Collections.singleton(endpoint.getHostAndPort()),
        endpoint.getClientConfigBuilder().build())) {
      assertThrows(JedisClusterOperationException.class, () -> cluster.get("foo"));
    }
  }
}
