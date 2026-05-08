package redis.clients.jedis.util;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the auto-replace suppression added to {@link Pool} for commons-pool2 2.13.x, which
 * unconditionally invokes {@code addObject()} from {@code GenericObjectPool#invalidateObject} after
 * destroying a broken resource. {@link Pool#returnBrokenResource(Object)} sets a thread-local flag
 * across that call so {@link Pool#addObject()} can swallow the auto-replace failure when the
 * upstream is down, while still propagating failures from direct user calls.
 */
public class PoolUnitTest {

  /**
   * Programmable factory used by the tests below.
   * <ul>
   * <li>{@code makeFailFromCallNumber} - {@code makeObject()} starts throwing from that call.</li>
   * <li>{@code destroyThrows} - {@code destroyObject(...)} raises a RuntimeException.</li>
   * </ul>
   */
  private static final class TestFactory implements PooledObjectFactory<String> {
    final AtomicInteger makeCount = new AtomicInteger();
    final AtomicInteger destroyCount = new AtomicInteger();
    volatile int makeFailFromCallNumber = Integer.MAX_VALUE;
    volatile boolean destroyThrows = false;

    @Override
    public PooledObject<String> makeObject() {
      int n = makeCount.incrementAndGet();
      if (n >= makeFailFromCallNumber) {
        throw new JedisConnectionException("upstream is down (call #" + n + ")");
      }
      return new DefaultPooledObject<>("resource-" + n);
    }

    @Override
    public void destroyObject(PooledObject<String> p) {
      destroyCount.incrementAndGet();
      if (destroyThrows) {
        throw new RuntimeException("destroy failed");
      }
    }

    @Override
    public boolean validateObject(PooledObject<String> p) {
      return true;
    }

    @Override
    public void activateObject(PooledObject<String> p) {
    }

    @Override
    public void passivateObject(PooledObject<String> p) {
    }
  }

  private TestFactory factory;
  private Pool<String> pool;

  @BeforeEach
  public void setUp() {
    factory = new TestFactory();
    GenericObjectPoolConfig<String> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(2);
    config.setBlockWhenExhausted(false);
    pool = new Pool<>(factory, config);
  }

  @AfterEach
  public void tearDown() {
    if (pool != null && !pool.isClosed()) {
      pool.close();
    }
  }

  @Test
  public void returnBrokenResource_swallowsAutoReplaceMakeObjectFailure() {
    String resource = pool.getResource();
    factory.makeFailFromCallNumber = 2;

    assertDoesNotThrow(() -> pool.returnBrokenResource(resource));
    assertEquals(1, factory.destroyCount.get());
    assertEquals(2, factory.makeCount.get(),
      "commons-pool2 2.13.x must trigger an auto-replace makeObject() after destroy");
  }

  @Test
  public void returnBrokenResource_propagatesDestroyFailureAsJedisException() {
    String resource = pool.getResource();
    factory.destroyThrows = true;

    JedisException ex = assertThrows(JedisException.class,
      () -> pool.returnBrokenResource(resource));
    assertTrue(ex.getCause().getMessage().contains("destroy failed"));
  }

  @Test
  public void returnBrokenResource_propagatesIllegalStateForUnknownResource() {
    JedisException ex = assertThrows(JedisException.class,
      () -> pool.returnBrokenResource("never-borrowed"));
    assertTrue(ex.getCause() instanceof IllegalStateException);
  }

  @Test
  public void addObjectDirect_propagatesMakeObjectFailure() {
    factory.makeFailFromCallNumber = 1;

    assertThrows(JedisConnectionException.class, () -> pool.addObject());
  }

  @Test
  public void addObjectsDirect_wrapsMakeObjectFailureAsJedisException() {
    factory.makeFailFromCallNumber = 1;

    JedisException ex = assertThrows(JedisException.class, () -> pool.addObjects(1));
    assertTrue(ex.getCause() instanceof JedisConnectionException);
  }

  @Test
  public void invalidateObjectDirect_doesNotSuppressAutoReplaceFailure() {
    String resource = pool.getResource();
    factory.makeFailFromCallNumber = 2;

    assertThrows(JedisConnectionException.class, () -> pool.invalidateObject(resource),
      "direct invalidateObject must propagate the auto-replace failure (flag is not set)");
  }

  @Test
  public void suppressionFlagIsClearedAfterReturnBrokenResourceSuccess() {
    String resource = pool.getResource();
    pool.returnBrokenResource(resource);

    factory.makeFailFromCallNumber = factory.makeCount.get() + 1;
    assertThrows(JedisConnectionException.class, () -> pool.addObject(),
      "subsequent addObject on the same thread must propagate; the flag must not leak");
  }

  @Test
  public void suppressionFlagIsClearedAfterReturnBrokenResourceFailure() {
    String resource = pool.getResource();
    factory.destroyThrows = true;
    assertThrows(JedisException.class, () -> pool.returnBrokenResource(resource));

    factory.destroyThrows = false;
    factory.makeFailFromCallNumber = factory.makeCount.get() + 1;
    assertThrows(JedisConnectionException.class, () -> pool.addObject(),
      "subsequent addObject on the same thread must propagate; the flag must not leak");
  }
}
