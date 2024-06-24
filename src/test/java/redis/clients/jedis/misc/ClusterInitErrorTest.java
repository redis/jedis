package redis.clients.jedis.misc;

import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

public class ClusterInitErrorTest {

  private static final String INIT_NO_ERROR_PROPERTY = "jedis.cluster.initNoError";

  @After
  public void cleanUp() {
    System.getProperties().remove(INIT_NO_ERROR_PROPERTY);
  }

  @Test(expected = JedisClusterOperationException.class)
  public void initError() {
    Assert.assertNull(System.getProperty(INIT_NO_ERROR_PROPERTY));
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");
    try (JedisCluster cluster = new JedisCluster(
        Collections.singleton(endpoint.getHostAndPort()),
        endpoint.getClientConfigBuilder().build())) {
      throw new IllegalStateException("should not reach here");
    }
  }

  @Test
  public void initNoError() {
    System.setProperty(INIT_NO_ERROR_PROPERTY, "");
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");
    try (JedisCluster cluster = new JedisCluster(
        Collections.singleton(endpoint.getHostAndPort()),
        endpoint.getClientConfigBuilder().build())) {
      Assert.assertThrows(JedisClusterOperationException.class, () -> cluster.get("foo"));
    }
  }
}
