package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.commands.async.util.AsyncJUnitTestCallback;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;

import java.util.ArrayList;
import java.util.List;

public class AsyncStringValuesCommandsTest extends AsyncJedisCommandTestBase {
  @Test
  public void setAndGet() {
    asyncJedis.set(STRING_CALLBACK.withReset(), "foo", "bar");
    assertTrue(Protocol.Keyword.OK.name().equalsIgnoreCase(
      STRING_CALLBACK.getResponseWithWaiting(1000)));

    asyncJedis.get(STRING_CALLBACK.withReset(), "foo");
    assertEquals("bar", STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void getSet() {
    asyncJedis.getSet(STRING_CALLBACK.withReset(), "foo", "bar");
    assertNull(STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "foo");
    assertEquals("bar", STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void mget() {
    asyncJedis.mget(STRING_LIST_CALLBACK.withReset(), "foo", "bar");
    List<String> expected = new ArrayList<String>();
    expected.add(null);
    expected.add(null);

    assertEquals(expected, STRING_LIST_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    expected = new ArrayList<String>();
    expected.add("bar");
    expected.add(null);

    asyncJedis.mget(STRING_LIST_CALLBACK.withReset(), "foo", "bar");
    assertEquals(expected, STRING_LIST_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "bar", "foo");

    expected = new ArrayList<String>();
    expected.add("bar");
    expected.add("foo");

    asyncJedis.mget(STRING_LIST_CALLBACK.withReset(), "foo", "bar");
    assertEquals(expected, STRING_LIST_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void setnx() {

    asyncJedis.setnx(LONG_CALLBACK.withReset(), "foo", "bar");
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "foo");
    assertEquals("bar", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.setnx(LONG_CALLBACK.withReset(), "foo", "bar2");
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "foo");
    assertEquals("bar", STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void setex() {
    asyncJedis.setex(STRING_CALLBACK.withReset(), "foo", 20, "bar");
    assertEquals(Protocol.Keyword.OK.name(), STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.ttl(LONG_CALLBACK.withReset(), "foo");
    long ttl = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertTrue(ttl > 0 && ttl <= 20);
  }

  @Test
  public void mset() {
    asyncJedis.mset(STRING_CALLBACK.withReset(), "foo", "bar", "bar", "foo");
    assertEquals(Protocol.Keyword.OK.name(), STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "foo");
    assertEquals("bar", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "bar");
    assertEquals("foo", STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void msetnx() {

    asyncJedis.msetnx(LONG_CALLBACK.withReset(), "foo", "bar", "bar", "foo");
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "foo");
    assertEquals("bar", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "bar");
    assertEquals("foo", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.msetnx(LONG_CALLBACK.withReset(), "foo", "bar1", "bar2", "foo2");
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "foo");
    assertEquals("bar", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "bar");
    assertEquals("foo", STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test(expected = JedisDataException.class)
  public void incrWrongValue() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.incr(LONG_CALLBACK.withReset(), "foo");
    LONG_CALLBACK.getResponseWithWaiting(1000);
    fail("JedisDataException should be occurred");
  }

  @Test
  public void incr() {
    asyncJedis.incr(LONG_CALLBACK.withReset(), "foo");
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.incr(LONG_CALLBACK.withReset(), "foo");
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test(expected = JedisDataException.class)
  public void incrByWrongValue() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.incrBy(LONG_CALLBACK.withReset(), "foo", 2);
    LONG_CALLBACK.getResponseWithWaiting(1000);
    fail("JedisDataException should be occurred");
  }

  @Test
  public void incrBy() {
    asyncJedis.incrBy(LONG_CALLBACK.withReset(), "foo", 2);
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.incrBy(LONG_CALLBACK.withReset(), "foo", 2);
    assertEquals(new Long(4), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test(expected = JedisDataException.class)
  public void decrWrongValue() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.decr(LONG_CALLBACK.withReset(), "foo");
    LONG_CALLBACK.getResponseWithWaiting(1000);
    fail("JedisDataException should be occurred");
  }

  @Test
  public void decr() {
    asyncJedis.decr(LONG_CALLBACK.withReset(), "foo");
    assertEquals(new Long(-1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.decr(LONG_CALLBACK.withReset(), "foo");
    assertEquals(new Long(-2), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test(expected = JedisDataException.class)
  public void decrByWrongValue() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.decrBy(LONG_CALLBACK.withReset(), "foo", -2);
    LONG_CALLBACK.getResponseWithWaiting(1000);
    fail("JedisDataException should be occurred");
  }

  @Test
  public void decrBy() {
    asyncJedis.decrBy(LONG_CALLBACK.withReset(), "foo", 2);
    assertEquals(new Long(-2), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.decrBy(LONG_CALLBACK.withReset(), "foo", 2);
    assertEquals(new Long(-4), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void append() {
    asyncJedis.append(LONG_CALLBACK.withReset(), "foo", "bar");
    assertEquals(new Long(3), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "foo");
    assertEquals("bar", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.append(LONG_CALLBACK.withReset(), "foo", "bar");
    assertEquals(new Long(6), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "foo");
    assertEquals("barbar", STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void substr() {
    CommandWithWaiting.set(asyncJedis, "s", "This is a string");

    asyncJedis.substr(STRING_CALLBACK.withReset(), "s", 0, 3);
    assertEquals("This", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.substr(STRING_CALLBACK.withReset(), "s", -3, -1);
    assertEquals("ing", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.substr(STRING_CALLBACK.withReset(), "s", 0, -1);
    assertEquals("This is a string", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.substr(STRING_CALLBACK.withReset(), "s", 9, 100000);
    assertEquals(" string", STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void strlen() {
    CommandWithWaiting.set(asyncJedis, "s", "This is a string");

    asyncJedis.strlen(LONG_CALLBACK.withReset(), "s");
    assertEquals("This is a string".length(), LONG_CALLBACK.getResponseWithWaiting(1000)
        .longValue());
  }

  @Test
  public void incrLargeNumbers() {
    asyncJedis.incr(LONG_CALLBACK.withReset(), "foo");
    long value = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, value);

    asyncJedis.incrBy(LONG_CALLBACK.withReset(), "foo", Integer.MAX_VALUE);
    assertEquals(new Long(1L + Integer.MAX_VALUE), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test(expected = JedisDataException.class)
  public void incrReallyLargeNumbers() {
    CommandWithWaiting.set(asyncJedis, "foo", Long.toString(Long.MAX_VALUE));

    asyncJedis.incr(LONG_CALLBACK.withReset(), "foo");
    assertEquals(new Long(Long.MIN_VALUE), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void incrByFloat() {
    asyncJedis.incrByFloat(DOUBLE_CALLBACK.withReset(), "foo", 10.5);
    assertEquals(10.5, DOUBLE_CALLBACK.getResponseWithWaiting(1000), 0.0);

    asyncJedis.incrByFloat(DOUBLE_CALLBACK.withReset(), "foo", 0.1);
    assertEquals(10.6, DOUBLE_CALLBACK.getResponseWithWaiting(1000), 0.0);
  }

  @Test
  public void psetex() {
    AsyncJUnitTestCallback<String> callback = new AsyncJUnitTestCallback<String>();
    asyncJedis.psetex(callback, "foo", 20000, "bar");
    callback.waitForComplete(1000);

    // borrowed from blocking API
    asyncJedis.ttl(LONG_CALLBACK.withReset(), "foo");
    long ttl = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertTrue(ttl > 0 && ttl <= 20000);
  }
}