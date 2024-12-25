package redis.clients.jedis.util;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;
import today.bonfire.oss.sop.PoolObject;
import today.bonfire.oss.sop.PooledObjectFactory;
import today.bonfire.oss.sop.SimpleObjectPool;
import today.bonfire.oss.sop.SimpleObjectPoolConfig;

public class Pool<T extends PoolObject> extends SimpleObjectPool<T> {

  private final static JedisPoolConfig defaultPoolConfig = JedisPoolConfig.builder().build();

  public Pool(SimpleObjectPoolConfig poolConfig, PooledObjectFactory<T> factory) {
    this(factory, poolConfig);
  }

  public Pool(final PooledObjectFactory<T> factory, final SimpleObjectPoolConfig poolConfig) {
    super(poolConfig, factory);
  }

  public Pool(final PooledObjectFactory<T> factory) {
    super(defaultPoolConfig, factory);
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
      return super.borrowObject();
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

  public int getNumActive() {
    return super.borrowedObjectsCount();
  }

  public int getNumIdle() {
    return super.idleObjectCount();
  }

  public int getNumWaiters() {
    return super.waitingCount();
  }
}
