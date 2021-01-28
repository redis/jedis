package redis.clients.jedis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.util.Pool;

public class JedisPoolAbstract extends Pool<Jedis> {

  /**
   * Using this constructor means you have to set and initialize the internalPool yourself.
   */
  @Deprecated
  public JedisPoolAbstract() {
    super();
  }

  public JedisPoolAbstract(GenericObjectPoolConfig poolConfig, PooledObjectFactory<Jedis> factory) {
    super(poolConfig, factory);
  }

  @Override
  protected void returnBrokenResource(Jedis resource) {
    super.returnBrokenResource(resource);
  }

  @Override
  protected void returnResource(Jedis resource) {
    super.returnResource(resource);
  }
}
