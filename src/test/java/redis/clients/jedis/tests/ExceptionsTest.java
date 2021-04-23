package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.exceptions.*;

public class ExceptionsTest {

  private static String MESSAGE;
  private static Throwable CAUSE;

  @BeforeClass
  public static void prepare() {
    MESSAGE = "This is a test message.";
    CAUSE = new Throwable("This is a test cause.");
  }

  @Test
  public void invalidURI() {
    try {
      throw new InvalidURIException(MESSAGE);
    } catch (Exception e) {
      assertEquals(InvalidURIException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new InvalidURIException(CAUSE);
    } catch (Exception e) {
      assertEquals(InvalidURIException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new InvalidURIException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertEquals(InvalidURIException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void accessControl() {
    try {
      throw new JedisAccessControlException(MESSAGE);
    } catch (Exception e) {
      assertEquals(JedisAccessControlException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisAccessControlException(CAUSE);
    } catch (Exception e) {
      assertEquals(JedisAccessControlException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisAccessControlException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertEquals(JedisAccessControlException.class, e.getClass());
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
      assertEquals(JedisAskDataException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisAskDataException(CAUSE, hap, slot);
    } catch (Exception e) {
      assertEquals(JedisAskDataException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisAskDataException(MESSAGE, CAUSE, hap, slot);
    } catch (Exception e) {
      assertEquals(JedisAskDataException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void busy() {
    try {
      throw new JedisClusterException(MESSAGE);
    } catch (Exception e) {
      assertEquals(JedisClusterException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisClusterException(CAUSE);
    } catch (Exception e) {
      assertEquals(JedisClusterException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisClusterException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertEquals(JedisClusterException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void clusterOperation() {
    try {
      throw new JedisClusterOperationException(MESSAGE);
    } catch (Exception e) {
      assertEquals(JedisClusterOperationException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisClusterOperationException(CAUSE);
    } catch (Exception e) {
      assertEquals(JedisClusterOperationException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisClusterOperationException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertEquals(JedisClusterOperationException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void connection() {
    try {
      throw new JedisConnectionException(MESSAGE);
    } catch (Exception e) {
      assertEquals(JedisConnectionException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisConnectionException(CAUSE);
    } catch (Exception e) {
      assertEquals(JedisConnectionException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisConnectionException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertEquals(JedisConnectionException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void data() {
    try {
      throw new JedisDataException(MESSAGE);
    } catch (Exception e) {
      assertEquals(JedisDataException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisDataException(CAUSE);
    } catch (Exception e) {
      assertEquals(JedisDataException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisDataException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertEquals(JedisDataException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void jedis() {
    try {
      throw new JedisException(MESSAGE);
    } catch (Exception e) {
      assertEquals(JedisException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisException(CAUSE);
    } catch (Exception e) {
      assertEquals(JedisException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertEquals(JedisException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void exhaustedPool() {
    try {
      throw new JedisExhaustedPoolException(MESSAGE);
    } catch (Exception e) {
      assertEquals(JedisExhaustedPoolException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisExhaustedPoolException(CAUSE);
    } catch (Exception e) {
      assertEquals(JedisExhaustedPoolException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisExhaustedPoolException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertEquals(JedisExhaustedPoolException.class, e.getClass());
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
      assertEquals(JedisMovedDataException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisMovedDataException(CAUSE, hap, slot);
    } catch (Exception e) {
      assertEquals(JedisMovedDataException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisMovedDataException(MESSAGE, CAUSE, hap, slot);
    } catch (Exception e) {
      assertEquals(JedisMovedDataException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }

  @Test
  public void noScript() {
    try {
      throw new JedisNoScriptException(MESSAGE);
    } catch (Exception e) {
      assertEquals(JedisNoScriptException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisNoScriptException(CAUSE);
    } catch (Exception e) {
      assertEquals(JedisNoScriptException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisNoScriptException(MESSAGE, CAUSE);
    } catch (Exception e) {
      assertEquals(JedisNoScriptException.class, e.getClass());
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
      assertEquals(JedisRedirectionException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertNull(e.getCause());
    }

    try {
      throw new JedisRedirectionException(CAUSE, hap, slot);
    } catch (Exception e) {
      assertEquals(JedisRedirectionException.class, e.getClass());
      assertEquals(CAUSE, e.getCause());
      assertEquals(CAUSE.toString(), e.getMessage());
    }

    try {
      throw new JedisRedirectionException(MESSAGE, CAUSE, hap, slot);
    } catch (Exception e) {
      assertEquals(JedisRedirectionException.class, e.getClass());
      assertEquals(MESSAGE, e.getMessage());
      assertEquals(CAUSE, e.getCause());
    }
  }
}
