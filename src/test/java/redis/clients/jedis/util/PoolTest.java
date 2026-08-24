package redis.clients.jedis.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.exceptions.JedisException;

public class PoolTest {

  @Test
  public void returnBrokenResourceDoesNotThrowWhenReplacementFails() {
    FailingReplacementFactory factory = new FailingReplacementFactory();
    GenericObjectPoolConfig<Object> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);

    Pool<Object> pool = new Pool<>(config, factory);
    try {
      Object resource = pool.getResource();
      assertEquals(1, factory.created.get());
      assertEquals(1, pool.getNumActive());

      assertDoesNotThrow(() -> pool.returnBrokenResource(resource));

      assertEquals(0, pool.getNumActive());
      assertEquals(0, pool.getNumIdle());
      // destroy succeeded; only the replacement makeObject failed
      assertEquals(1, factory.destroyed.get());
      assertEquals(2, factory.created.get());

      JedisException thrown = assertThrows(JedisException.class, pool::getResource);
      assertEquals("Could not get a resource from the pool", thrown.getMessage());
    } finally {
      pool.close();
    }
  }

  @Test
  public void returnBrokenResourceReplacesObjectWhenFactorySucceeds() {
    CountingFactory factory = new CountingFactory();
    GenericObjectPoolConfig<Object> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);

    Pool<Object> pool = new Pool<>(config, factory);
    try {
      Object resource = pool.getResource();
      pool.returnBrokenResource(resource);

      assertEquals(0, pool.getNumActive());
      assertEquals(1, pool.getNumIdle());
      assertEquals(2, factory.created.get());
      assertEquals(1, factory.destroyed.get());
    } finally {
      pool.close();
    }
  }

  private static class CountingFactory extends BasePooledObjectFactory<Object> {

    final AtomicInteger created = new AtomicInteger();
    final AtomicInteger destroyed = new AtomicInteger();

    @Override
    public Object create() {
      created.incrementAndGet();
      return new Object();
    }

    @Override
    public PooledObject<Object> wrap(Object obj) {
      return new DefaultPooledObject<>(obj);
    }

    @Override
    public void destroyObject(PooledObject<Object> p) {
      destroyed.incrementAndGet();
    }
  }

  private static class FailingReplacementFactory extends CountingFactory {

    @Override
    public Object create() {
      if (created.getAndIncrement() > 0) {
        throw new RuntimeException("server unavailable");
      }
      return new Object();
    }
  }
}
