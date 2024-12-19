package redis.clients.jedis.util;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;
import today.bonfire.oss.sop.PoolEntity;
import today.bonfire.oss.sop.PooledObjectFactory;
import today.bonfire.oss.sop.SimpleObjectPool;

import java.time.Duration;

public class Pool<T extends PoolEntity> extends SimpleObjectPool<T> {

  private final static JedisPoolConfig defaultPoolConfig = new JedisPoolConfig();

  public Pool(JedisPoolConfig poolConfig, PooledObjectFactory<T> factory) {
    this(factory, poolConfig);
  }

  public Pool(final PooledObjectFactory<T> factory, final JedisPoolConfig poolConfig) {
    super(poolConfig.getMaxTotal(),
          poolConfig.getMinIdle(),
          poolConfig.getMinEvictableIdleTimeMillis(),
          poolConfig.getMaxWaitMillis(),
          factory);
  }

  public Pool(final PooledObjectFactory<T> factory) {
    super(defaultPoolConfig.getMaxTotal(),
          defaultPoolConfig.getMinIdle(),
          defaultPoolConfig.getMinEvictableIdleTimeMillis(),
          defaultPoolConfig.getMaxWaitMillis(),
          factory);
  }

  @Override
  public void close() {
    destroy();
  }

  public void destroy() {
    try {
      super.close();
    } catch (RuntimeException e) {
      throw new JedisException("Could not destroy the pool", e);
    }
  }

  public T getResource() {
    try {
      return super.borrowObject(Duration.ofMillis(10000)); // Default timeout 5 seconds
    } catch (JedisException je) {
      throw je;
    } catch (Exception e) {
      throw new JedisException("Could not get a resource from the pool", e);
    }
  }

  public void returnResource(final T resource) {
    if (resource == null) {
      return;
    }
    try {
      super.returnObject(resource);
    } catch (RuntimeException e) {
      throw new JedisException("Could not return the resource to the pool", e);
    }
  }

  public void returnBrokenResource(final T resource) {
    if (resource == null) {
      return;
    }
    try {
      super.returnObject(resource, true);
    } catch (Exception e) {
      throw new JedisException("Could not return the broken resource to the pool", e);
    }
  }

  protected void clear() {
    super.destroyAllIdleObjects();
  }
}
