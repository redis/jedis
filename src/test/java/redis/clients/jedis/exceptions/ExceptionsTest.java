package redis.clients.jedis.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;

public class ExceptionsTest {

  private static String MESSAGE;
  private static Throwable CAUSE;

  @BeforeClass
  public static void prepare() {
    MESSAGE = "This is a test message.";
    CAUSE = new Throwable("This is a test cause.");
  }

  @Test
  public void abortedTransaction() {
    try {
      throw new AbortedTransactionException(MESSAGE);
    } catch (Exception e) {
      assertSame(AbortedTransactionException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new AbortedTransactionException(CAUSE);
    } catch (Exception e) {
      assertSame(AbortedTransactionException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new AbortedTransactionException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertSame(AbortedTransactionException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void invalidURI() {
    try {
      throw new InvalidURIException(MESSAGE);
    } catch (Exception e) {
      assertSame(InvalidURIException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new InvalidURIException(CAUSE);
    } catch (Exception e) {
      assertSame(InvalidURIException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new InvalidURIException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertSame(InvalidURIException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void accessControl() {
    try {
      throw new JedisAccessControlException(MESSAGE);
    } catch (Exception e) {
      assertSame(JedisAccessControlException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisAccessControlException(CAUSE);
    } catch (Exception e) {
      assertSame(JedisAccessControlException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisAccessControlException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertSame(JedisAccessControlException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void askData() {
    HostAndPort hap = new HostAndPort("", 0);
    int slot = -1;

    try {
      throw new JedisAskDataException(MESSAGE, hap, slot);
    } catch (Exception e) {
      assertSame(JedisAskDataException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisAskDataException(CAUSE, hap, slot);
    } catch (Exception e) {
      assertSame(JedisAskDataException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisAskDataException(MESSAGE, CAUSE, hap, slot);
    } catch (Exception e) {
      assertSame(JedisAskDataException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void busy() {
    try {
      throw new JedisBusyException(MESSAGE);
    } catch (Exception e) {
      assertSame(JedisBusyException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisBusyException(CAUSE);
    } catch (Exception e) {
      assertSame(JedisBusyException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisBusyException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertSame(JedisBusyException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void cluster() {
    try {
      throw new JedisClusterException(MESSAGE);
    } catch (Exception e) {
      assertSame(JedisClusterException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisClusterException(CAUSE);
    } catch (Exception e) {
      assertSame(JedisClusterException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisClusterException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertSame(JedisClusterException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void clusterOperation() {
    try {
      throw new JedisClusterOperationException(MESSAGE);
    } catch (Exception e) {
      assertSame(JedisClusterOperationException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisClusterOperationException(CAUSE);
    } catch (Exception e) {
      assertSame(JedisClusterOperationException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisClusterOperationException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertSame(JedisClusterOperationException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void connection() {
    try {
      throw new JedisConnectionException(MESSAGE);
    } catch (Exception e) {
      assertSame(JedisConnectionException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisConnectionException(CAUSE);
    } catch (Exception e) {
      assertSame(JedisConnectionException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisConnectionException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertSame(JedisConnectionException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void data() {
    try {
      throw new JedisDataException(MESSAGE);
    } catch (Exception e) {
      assertSame(JedisDataException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisDataException(CAUSE);
    } catch (Exception e) {
      assertSame(JedisDataException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisDataException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertSame(JedisDataException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void jedis() {
    try {
      throw new JedisException(MESSAGE);
    } catch (Exception e) {
      assertSame(JedisException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisException(CAUSE);
    } catch (Exception e) {
      assertSame(JedisException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertSame(JedisException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void movedData() {
    HostAndPort hap = new HostAndPort("", 0);
    int slot = -1;

    try {
      throw new JedisMovedDataException(MESSAGE, hap, slot);
    } catch (Exception e) {
      assertSame(JedisMovedDataException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisMovedDataException(CAUSE, hap, slot);
    } catch (Exception e) {
      assertSame(JedisMovedDataException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisMovedDataException(MESSAGE, CAUSE, hap, slot);
    } catch (Exception e) {
      assertSame(JedisMovedDataException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void noScript() {
    try {
      throw new JedisNoScriptException(MESSAGE);
    } catch (Exception e) {
      assertSame(JedisNoScriptException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisNoScriptException(CAUSE);
    } catch (Exception e) {
      assertSame(JedisNoScriptException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisNoScriptException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertSame(JedisNoScriptException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void redirection() {
    HostAndPort hap = new HostAndPort("", 0);
    int slot = -1;

    try {
      throw new JedisRedirectionException(MESSAGE, hap, slot);
    } catch (Exception e) {
      assertSame(JedisRedirectionException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisRedirectionException(CAUSE, hap, slot);
    } catch (Exception e) {
      assertSame(JedisRedirectionException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisRedirectionException(MESSAGE, CAUSE, hap, slot);
    } catch (Exception e) {
      assertSame(JedisRedirectionException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }
}
