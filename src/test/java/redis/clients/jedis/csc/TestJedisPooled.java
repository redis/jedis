package redis.clients.jedis.csc;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.Connection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;

public class TestJedisPooled extends JedisPooled {

  public TestJedisPooled(final HostAndPort hostAndPort, final JedisClientConfig clientConfig, Cache clientSideCache) {
    super(hostAndPort, clientConfig, clientSideCache);
  }

  public TestJedisPooled(redis.clients.jedis.HostAndPort hnp, JedisClientConfig jedisClientConfig,
      Cache guava, GenericObjectPoolConfig<Connection> genericObjectPoolConfig) {
    super(hnp, jedisClientConfig, guava, genericObjectPoolConfig);
  }

}
