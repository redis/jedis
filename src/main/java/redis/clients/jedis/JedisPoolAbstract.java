package redis.clients.jedis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.util.Pool;

public class JedisPoolAbstract extends Pool<Jedis> {

  protected Logger log = LoggerFactory.getLogger(getClass().getName());

  public JedisPoolAbstract() {
    super();
  }

  public JedisPoolAbstract(GenericObjectPoolConfig poolConfig, PooledObjectFactory<Jedis> factory) {
    super(poolConfig, factory);
  }

  public void prepareInternalPool() {
    try {
      this.internalPool.preparePool();
    } catch (Exception e) {
      log.warn("exception occurred warming jedis internal connection pool", e);
    }
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
