package redis.clients.jedis.util;

import java.io.Closeable;
import java.util.NoSuchElementException;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisExhaustedPoolException;

public class Pool<T> extends GenericObjectPool<T> implements Closeable {

  public Pool(final GenericObjectPoolConfig<T> poolConfig, PooledObjectFactory<T> factory) {
    super(factory, poolConfig);
  }

  @Override
  public void close() {
    destroy();
  }

  public T getResource() {
    try {
      return super.borrowObject();
    } catch (JedisDataException jde) {
      throw jde;
    } catch (NoSuchElementException nse) {
      if (null == nse.getCause()) { // The exception was caused by an exhausted pool
        throw new JedisExhaustedPoolException(
            "Could not get a resource since the pool is exhausted", nse);
      }
      // Otherwise, the exception was caused by the implemented activateObject() or ValidateObject()
      throw new JedisException("Could not get a resource from the pool", nse);
    } catch (Exception e) {
      throw new JedisConnectionException("Could not get a resource from the pool", e);
    }
  }

  public void returnResource(final T resource) {
    if (resource == null) {
      return;
    }
    try {
      super.returnObject(resource);
    } catch (Exception e) {
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

  public void destroy() {
    try {
      super.close();
    } catch (Exception e) {
      throw new JedisException("Could not destroy the pool", e);
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
