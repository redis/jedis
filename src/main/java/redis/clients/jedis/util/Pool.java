package redis.clients.jedis.util;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.exceptions.JedisException;

public class Pool<T> extends GenericObjectPool<T> {

  private static final Logger log = LoggerFactory.getLogger(Pool.class);

  /**
   * Set while the current thread is executing inside {@link #returnBrokenResource(Object)}.
   * <p>
   * commons-pool2 2.13.0 changed {@link GenericObjectPool#invalidateObject(Object)} to call
   * {@link GenericObjectPool#addObject()} unconditionally after destroying the invalidated
   * instance, in order to keep the pool warm. When the upstream is unreachable that implicit
   * replacement attempt now fails with the factory's exception, which prior to 2.13 did not
   * surface from {@code returnBrokenResource}. This flag lets the {@link #addObject()} override
   * recognise the implicit call and swallow its failure, restoring the pre-2.13 contract for
   * the broken-return path while preserving full error semantics for every other caller of
   * {@code addObject()} / {@link #addObjects(int)}.
   */
  private final ThreadLocal<Boolean> inReturnBrokenResource =
      ThreadLocal.withInitial(() -> Boolean.FALSE);

  // Legacy
  public Pool(GenericObjectPoolConfig<T> poolConfig, PooledObjectFactory<T> factory) {
    this(factory, poolConfig);
  }

  public Pool(final PooledObjectFactory<T> factory, final GenericObjectPoolConfig<T> poolConfig) {
    super(factory, poolConfig);
  }

  public Pool(final PooledObjectFactory<T> factory) {
    super(factory);
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
    inReturnBrokenResource.set(Boolean.TRUE);
    try {
      super.invalidateObject(resource);
    } catch (Exception e) {
      throw new JedisException("Could not return the broken resource to the pool", e);
    } finally {
      inReturnBrokenResource.remove();
    }
  }

  /**
   * When invoked transitively from {@link #returnBrokenResource(Object)}, swallows failures
   * originating from the implicit "ensure-liveness" replacement that commons-pool2 2.13.x
   * performs at the end of {@link GenericObjectPool#invalidateObject(Object)}. Direct callers
   * (user code, {@link #addObjects(int)}) keep full error semantics.
   */
  @Override
  public void addObject() throws Exception {
    if (Boolean.TRUE.equals(inReturnBrokenResource.get())) {
      try {
        super.addObject();
      } catch (Exception e) {
        if (log.isTraceEnabled()) {
          log.trace("Suppressed addObject() failure during returnBrokenResource", e);
        }
      }
    } else {
      super.addObject();
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
