package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;
import redis.clients.jedis.tests.commands.async.util.DoNothingCallback;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AsyncAllKindOfValuesCommandsTest extends AsyncJedisCommandTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bfoo1 = { 0x01, 0x02, 0x03, 0x04, 0x0A };
  final byte[] bfoo2 = { 0x01, 0x02, 0x03, 0x04, 0x0B };
  final byte[] bfoo3 = { 0x01, 0x02, 0x03, 0x04, 0x0C };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bbar1 = { 0x05, 0x06, 0x07, 0x08, 0x0A };
  final byte[] bbar2 = { 0x05, 0x06, 0x07, 0x08, 0x0B };
  final byte[] bbar3 = { 0x05, 0x06, 0x07, 0x08, 0x0C };

  final byte[] bfoobar = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
  final byte[] bfoostar = { 0x01, 0x02, 0x03, 0x04, '*' };
  final byte[] bbarstar = { 0x05, 0x06, 0x07, 0x08, '*' };

  @Test
  public void ping() {
    asyncJedis.ping(STRING_CALLBACK.withReset());
    assertEquals("PONG", STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void exists() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");
    CommandWithWaiting.set(asyncJedis, bfoo, bbar);

    asyncJedis.exists(BOOLEAN_CALLBACK.withReset(), "foo");
    assertTrue(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.exists(BOOLEAN_CALLBACK.withReset(), bfoo);
    assertTrue(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.del(asyncJedis, "foo");
    CommandWithWaiting.del(asyncJedis, bfoo);

    asyncJedis.exists(BOOLEAN_CALLBACK.withReset(), "foo");
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.exists(BOOLEAN_CALLBACK.withReset(), bfoo);
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void del() {
    CommandWithWaiting.set(asyncJedis, "foo1", "bar1");
    CommandWithWaiting.set(asyncJedis, "foo2", "bar2");
    CommandWithWaiting.set(asyncJedis, "foo3", "bar3");

    asyncJedis.del(LONG_CALLBACK.withReset(), "foo1", "foo2", "foo3");
    assertEquals(new Long(3), LONG_CALLBACK.getResponseWithWaiting(2000));

    asyncJedis.exists(BOOLEAN_CALLBACK.withReset(), "foo1");
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.exists(BOOLEAN_CALLBACK.withReset(), "foo2");
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.exists(BOOLEAN_CALLBACK.withReset(), "foo3");
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "foo1", "bar1");

    asyncJedis.del(LONG_CALLBACK.withReset(), "foo1", "foo2");
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.del(LONG_CALLBACK.withReset(), "foo1", "foo2");
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    // binary
    CommandWithWaiting.set(asyncJedis, "foo1", "bar1");
    CommandWithWaiting.set(asyncJedis, "foo2", "bar2");
    CommandWithWaiting.set(asyncJedis, "foo3", "bar3");

    CommandWithWaiting.set(asyncJedis, bfoo1, bbar1);
    CommandWithWaiting.set(asyncJedis, bfoo2, bbar2);
    CommandWithWaiting.set(asyncJedis, bfoo3, bbar3);

    asyncJedis.del(LONG_CALLBACK.withReset(), bfoo1, bfoo2, bfoo3);
    assertEquals(new Long(3), LONG_CALLBACK.getResponseWithWaiting(2000));

    asyncJedis.exists(BOOLEAN_CALLBACK.withReset(), bfoo1);
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.exists(BOOLEAN_CALLBACK.withReset(), bfoo2);
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.exists(BOOLEAN_CALLBACK.withReset(), bfoo3);
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, bfoo1, bbar1);

    LONG_CALLBACK.reset();
    asyncJedis.del(LONG_CALLBACK, bfoo1, bfoo2);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.del(LONG_CALLBACK.withReset(), bfoo1, bfoo2);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void type() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.type(STRING_CALLBACK.withReset(), "foo");
    STRING_CALLBACK.waitForComplete(100);
    assertEquals("string", STRING_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.set(asyncJedis, bfoo, bbar);

    asyncJedis.type(STRING_CALLBACK.withReset(), bfoo);
    assertEquals("string", STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void keys() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");
    CommandWithWaiting.set(asyncJedis, "foobar", "bar");

    asyncJedis.keys(STRING_SET_CALLBACK.withReset(), "foo*");

    Set<String> expected = new HashSet<String>();
    expected.add("foo");
    expected.add("foobar");

    assertEquals(expected, STRING_SET_CALLBACK.getResponseWithWaiting(1000));

    expected = new HashSet<String>();

    asyncJedis.keys(STRING_SET_CALLBACK.withReset(), "bar*");
    assertEquals(expected, STRING_SET_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.set(asyncJedis, bfoo, bbar);
    CommandWithWaiting.set(asyncJedis, bfoobar, bbar);

    asyncJedis.keys(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoostar);

    Set<byte[]> bkeys = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, bkeys.size());
    assertTrue(setContains(bkeys, bfoo));
    assertTrue(setContains(bkeys, bfoobar));

    asyncJedis.keys(BYTE_ARRAY_SET_CALLBACK.withReset(), bbarstar);
    assertEquals(0, STRING_SET_CALLBACK.getResponseWithWaiting(1000).size());
  }

  @Test
  public void randomKey() {
    asyncJedis.randomKey(STRING_CALLBACK.withReset());
    assertEquals(null, STRING_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.randomKey(STRING_CALLBACK.withReset());
    assertEquals("foo", STRING_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "bar", "foo");

    asyncJedis.randomKey(STRING_CALLBACK.withReset());
    String response = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertTrue(response.equals("foo") || response.equals("bar"));

    CommandWithWaiting.del(asyncJedis, "foo", "bar");
    asyncJedis.del(new DoNothingCallback<Long>(), "foo", "bar");

    // Binary
    asyncJedis.randomKey(STRING_CALLBACK.withReset());
    assertNull(STRING_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, bfoo, bbar);

    asyncJedis.randomBinaryKey(BYTE_ARRAY_CALLBACK.withReset());
    assertArrayEquals(bfoo, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, bbar, bfoo);

    asyncJedis.randomBinaryKey(BYTE_ARRAY_CALLBACK.withReset());
    byte[] randomBkey = BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000);
    assertTrue(Arrays.equals(randomBkey, bfoo) || Arrays.equals(randomBkey, bbar));
  }

  @Test
  public void rename() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.rename(STRING_CALLBACK.withReset(), "foo", "bar");
    assertEquals("OK", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "foo");
    assertEquals(null, STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "bar");
    assertEquals("bar", STRING_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.set(asyncJedis, bfoo, bbar);

    asyncJedis.rename(STRING_CALLBACK.withReset(), bfoo, bbar);
    assertEquals("OK", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(BYTE_ARRAY_CALLBACK.withReset(), bfoo);
    assertEquals(null, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(BYTE_ARRAY_CALLBACK.withReset(), bbar);
    assertArrayEquals(bbar, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void renameOldAndNewAreTheSame() {
    try {
      CommandWithWaiting.set(asyncJedis, "foo", "bar");
      asyncJedis.rename(STRING_CALLBACK.withReset(), "foo", "foo");
      STRING_CALLBACK.getResponseWithWaiting(1000);
      fail("JedisDataException expected");
    } catch (final JedisDataException e) {
    }

    // Binary
    try {
      CommandWithWaiting.set(asyncJedis, bfoo, bbar);
      asyncJedis.rename(STRING_CALLBACK.withReset(), bfoo, bfoo);
      STRING_CALLBACK.getResponseWithWaiting(1000);
      fail("JedisDataException expected");
    } catch (final JedisDataException e) {
    }
  }

  @Test
  public void renamenx() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.renamenx(LONG_CALLBACK.withReset(), "foo", "bar");
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.renamenx(LONG_CALLBACK.withReset(), "foo", "bar");
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.set(asyncJedis, bfoo, bbar);

    asyncJedis.renamenx(LONG_CALLBACK.withReset(), bfoo, bbar);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, bfoo, bbar);

    asyncJedis.renamenx(LONG_CALLBACK.withReset(), bfoo, bbar);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void dbSize() {
    asyncJedis.dbSize(LONG_CALLBACK.withReset());
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.dbSize(LONG_CALLBACK.withReset());
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.set(asyncJedis, bfoo, bbar);

    asyncJedis.dbSize(LONG_CALLBACK.withReset());
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void expire() {
    asyncJedis.expire(LONG_CALLBACK.withReset(), "foo", 20);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.expire(LONG_CALLBACK.withReset(), "foo", 20);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    asyncJedis.expire(LONG_CALLBACK.withReset(), bfoo, 20);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, bfoo, bbar);

    asyncJedis.expire(LONG_CALLBACK.withReset(), bfoo, 20);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void expireAt() {
    long unixTime = (System.currentTimeMillis() / 1000L) + 20;

    asyncJedis.expireAt(LONG_CALLBACK.withReset(), "foo", unixTime);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    unixTime = (System.currentTimeMillis() / 1000L) + 20;
    asyncJedis.expireAt(LONG_CALLBACK.withReset(), "foo", unixTime);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    // binary
    asyncJedis.expireAt(LONG_CALLBACK.withReset(), bfoo, unixTime);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, bfoo, bbar);

    unixTime = (System.currentTimeMillis() / 1000L) + 20;
    asyncJedis.expireAt(LONG_CALLBACK.withReset(), bfoo, unixTime);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void ttl() {
    asyncJedis.ttl(LONG_CALLBACK.withReset(), "foo");
    assertEquals(new Long(-2), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.ttl(LONG_CALLBACK.withReset(), "foo");
    assertEquals(new Long(-1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.expire(new DoNothingCallback<Long>(), "foo", 200);

    asyncJedis.ttl(LONG_CALLBACK.withReset(), "foo");
    Long ttl = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertTrue(ttl >= 0 && ttl <= 200);

    // Binary
    asyncJedis.ttl(LONG_CALLBACK.withReset(), bfoo);
    assertEquals(new Long(-2), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, bfoo, bbar);

    asyncJedis.ttl(LONG_CALLBACK.withReset(), bfoo);
    assertEquals(new Long(-1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.expire(new DoNothingCallback<Long>(), bfoo, 200);

    asyncJedis.ttl(LONG_CALLBACK.withReset(), bfoo);
    ttl = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertTrue(ttl >= 0 && ttl <= 200);
  }

  @Test
  public void move() {
    asyncJedis.move(LONG_CALLBACK.withReset(), "foo", 1);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.move(LONG_CALLBACK.withReset(), "foo", 1);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(STRING_CALLBACK.withReset(), "foo");
    assertEquals(null, STRING_CALLBACK.getResponseWithWaiting(1000));

    // we can't check with AsyncJedis, so we borrow Blocking API
    jedis.select(1);
    assertEquals("bar", jedis.get("foo"));

    // Binary
    jedis.select(0);

    asyncJedis.move(LONG_CALLBACK.withReset(), bfoo, 1);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, bfoo, bbar);

    asyncJedis.move(LONG_CALLBACK.withReset(), bfoo, 1);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.get(BYTE_ARRAY_CALLBACK.withReset(), bfoo);
    assertEquals(null, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));

    // we can't check with AsyncJedis, so we borrow Blocking API
    jedis.select(1);
    assertArrayEquals(bbar, jedis.get(bfoo));
  }

  @Test
  public void flushDB() {
    CommandWithWaiting.set(asyncJedis, "foo", "bar");
    CommandWithWaiting.set(asyncJedis, "bar", "foo");
    CommandWithWaiting.move(asyncJedis, "bar", 1);

    asyncJedis.flushDB(STRING_CALLBACK.withReset());
    assertEquals("OK", STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.dbSize(LONG_CALLBACK.withReset());
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void persist() {
    CommandWithWaiting.setex(asyncJedis, "foo", 60 * 60, "bar");

    asyncJedis.persist(LONG_CALLBACK.withReset(), "foo");
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.ttl(LONG_CALLBACK.withReset(), "foo");
    assertEquals(new Long(-1), LONG_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.setex(asyncJedis, bfoo, 60 * 60, bbar);
    asyncJedis.setex(new DoNothingCallback<String>(), bfoo, 60 * 60, bbar);

    asyncJedis.persist(LONG_CALLBACK.withReset(), bfoo);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.ttl(LONG_CALLBACK.withReset(), bfoo);
    assertEquals(new Long(-1), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void echo() {
    asyncJedis.echo(STRING_CALLBACK.withReset(), "hello world");
    assertEquals("hello world", STRING_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void dumpAndRestore() {
    CommandWithWaiting.set(asyncJedis, "foo1", "bar1");

    asyncJedis.dump(BYTE_ARRAY_CALLBACK.withReset(), "foo1");
    asyncJedis.restore(new DoNothingCallback<String>(), "foo2", 0,
      BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.exists(BOOLEAN_CALLBACK.withReset(), "foo2");
    assertTrue("foo2", BOOLEAN_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void pexpire() {
    asyncJedis.pexpire(LONG_CALLBACK.withReset(), "foo", 10000);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.pexpire(LONG_CALLBACK.withReset(), "foo", 10000);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void pexpireAt() {
    long unixTime = (System.currentTimeMillis()) + 10000;

    asyncJedis.pexpireAt(LONG_CALLBACK.withReset(), "foo", unixTime);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    unixTime = (System.currentTimeMillis()) + 10000;
    asyncJedis.pexpire(LONG_CALLBACK.withReset(), "foo", unixTime);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void pttl() {
    asyncJedis.pttl(LONG_CALLBACK.withReset(), "foo");
    assertEquals(new Long(-2), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.set(asyncJedis, "foo", "bar");

    asyncJedis.pttl(LONG_CALLBACK.withReset(), "foo");
    assertEquals(new Long(-1), LONG_CALLBACK.getResponseWithWaiting(1000));

    CommandWithWaiting.pexpire(asyncJedis, "foo", 20000);
    asyncJedis.pexpire(new DoNothingCallback<Long>(), "foo", 20000);

    asyncJedis.pttl(LONG_CALLBACK.withReset(), "foo");

    Long pttl = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertTrue(pttl >= 0 && pttl <= 20000);
  }
}
