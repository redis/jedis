package redis.clients.jedis.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public class PoolTest {

  /**
   * Creates plain objects while the "server" is up and fails to create anything once it is down,
   * which is what a pool sees when Redis becomes unreachable.
   */
  private static class ServerBackedFactory extends BasePooledObjectFactory<Object> {

    private final AtomicInteger destroyed = new AtomicInteger();
    private volatile boolean serverUp = true;

    @Override
    public Object create() {
      if (!serverUp) {
        throw new JedisConnectionException("Failed to connect to localhost:6379.");
      }
      return new Object();
    }

    @Override
    public PooledObject<Object> wrap(Object obj) {
      return new DefaultPooledObject<>(obj);
    }

    @Override
    public void destroyObject(PooledObject<Object> pooledObject) {
      destroyed.incrementAndGet();
    }
  }

  private static GenericObjectPoolConfig<Object> config() {
    GenericObjectPoolConfig<Object> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(4);
    return config;
  }

  @Test
  public void returnBrokenResourceSucceedsWhenTheReplacementCannotBeCreated() {
    ServerBackedFactory factory = new ServerBackedFactory();
    try (Pool<Object> pool = new Pool<>(factory, config())) {
      Object resource = pool.getResource();
      factory.serverUp = false;

      pool.returnBrokenResource(resource);

      assertEquals(0, pool.getNumActive());
      assertEquals(0, pool.getNumIdle());
      assertEquals(1, factory.destroyed.get());
    }
  }

  @Test
  public void returnBrokenResourceDestroysTheResourceWhileTheServerIsUp() {
    ServerBackedFactory factory = new ServerBackedFactory();
    try (Pool<Object> pool = new Pool<>(factory, config())) {
      Object resource = pool.getResource();

      pool.returnBrokenResource(resource);

      assertEquals(0, pool.getNumActive());
      assertEquals(1, factory.destroyed.get());
    }
  }

  @Test
  public void returnBrokenResourceRejectsAResourceThatIsNotPooled() {
    try (Pool<Object> pool = new Pool<>(new ServerBackedFactory(), config())) {
      JedisException e = assertThrows(JedisException.class,
        () -> pool.returnBrokenResource(new Object()));

      assertInstanceOf(IllegalStateException.class, e.getCause());
    }
  }

  @Test
  public void returnBrokenResourceIgnoresNull() {
    try (Pool<Object> pool = new Pool<>(new ServerBackedFactory(), config())) {
      pool.returnBrokenResource(null);

      assertEquals(0, pool.getNumActive());
    }
  }
}
