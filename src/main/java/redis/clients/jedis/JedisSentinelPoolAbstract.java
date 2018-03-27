package redis.clients.jedis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

public abstract class JedisSentinelPoolAbstract extends JedisPoolAbstract {

  public JedisSentinelPoolAbstract() {
    super();
  }

  public JedisSentinelPoolAbstract(GenericObjectPoolConfig poolConfig,
      PooledObjectFactory<Jedis> factory) {
    super(poolConfig, factory);
  }

  public abstract Jedis getResourceReadOnly();
}
