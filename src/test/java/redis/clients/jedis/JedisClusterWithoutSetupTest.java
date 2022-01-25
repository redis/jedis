package redis.clients.jedis;

import org.junit.Test;

import redis.clients.jedis.exceptions.JedisClusterOperationException;

public class JedisClusterWithoutSetupTest {

  @Test(expected = JedisClusterOperationException.class)
  public void uselessStartNodes() {
    new JedisCluster(new HostAndPort("127.0.0.1", 7378));
  }
}
