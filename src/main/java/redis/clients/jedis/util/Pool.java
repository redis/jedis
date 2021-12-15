package redis.clients.jedis.util;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

public class Pool<T> extends GenericObjectPool<T> {

  // Legacy
  public Pool(GenericObjectPoolConfig<T> poolConfig, PooledObjectFactory<T> factory) {
    this(factory, poolConfig);
  }

  public Pool(final PooledObjectFactory<T> factory, final GenericObjectPoolConfig<T> poolConfig) {
    super(factory, poolConfig);
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
      super.invalidateObject(resource);
    } catch (Exception e) {
      throw new JedisException("Could not return the broken resource to the pool", e);
    }
  }

  @Override
  public void addObjects(int count) {
    try {
      for (int i = 0; i < count; i++) {
        addObject();
      }
    } catch (Exception e) {
      throw new JedisException("Error trying to add idle objects", e);
    }
  }
}
