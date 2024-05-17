package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

import static redis.clients.jedis.Protocol.Command.BLPOP;
import static redis.clients.jedis.Protocol.Command.HGETALL;
import static redis.clients.jedis.Protocol.Command.GET;
import static redis.clients.jedis.Protocol.Command.LRANGE;
import static redis.clients.jedis.Protocol.Command.PING;
import static redis.clients.jedis.Protocol.Command.RPUSH;
import static redis.clients.jedis.Protocol.Command.SET;
import static redis.clients.jedis.Protocol.Command.XINFO;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;

import java.util.*;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.*;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.util.AssertUtil;
import redis.clients.jedis.util.KeyValue;

@RunWith(Parameterized.class)
public class AllKindOfValuesCommandsTest extends JedisCommandsTestBase {
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

  final byte[] bnx = { 0x6E, 0x78 };
  final byte[] bex = { 0x65, 0x78 };
  final int expireSeconds = 2;

  private static final EndpointConfig lfuEndpoint = HostAndPorts.getRedisEndpoint("standalone7-with-lfu-policy");

  public AllKindOfValuesCommandsTest(RedisProtocol redisProtocol) {
    super(redisProtocol);
  }

  @Test
  public void ping() {
    String status = jedis.ping();
    assertEquals("PONG", status);
  }

  @Test
  public void pingWithMessage() {
    String argument = "message";
    assertEquals(argument, jedis.ping(argument));

    assertArrayEquals(bfoobar, jedis.ping(bfoobar));
  }

  @Test
  public void exists() {
    String status = jedis.set("foo", "bar");
    assertEquals("OK", status);

    status = jedis.set(bfoo, bbar);
    assertEquals("OK", status);

    assertTrue(jedis.exists("foo"));

    assertTrue(jedis.exists(bfoo));

    assertEquals(1L, jedis.del("foo"));

    assertEquals(1L, jedis.del(bfoo));

    assertFalse(jedis.exists("foo"));

    assertFalse(jedis.exists(bfoo));
  }

  @Test
  public void existsMany() {
    String status = jedis.set("foo1", "bar1");
    assertEquals("OK", status);

    status = jedis.set("foo2", "bar2");
    assertEquals("OK", status);

    assertEquals(2L, jedis.exists("foo1", "foo2"));

    assertEquals(1L, jedis.del("foo1"));

    assertEquals(1L, jedis.exists("foo1", "foo2"));
  }

  @Test
  public void del() {
    jedis.set("foo1", "bar1");
    jedis.set("foo2", "bar2");
    jedis.set("foo3", "bar3");

    assertEquals(3L, jedis.del("foo1", "foo2", "foo3"));

    assertFalse(jedis.exists("foo1"));
    assertFalse(jedis.exists("foo2"));
    assertFalse(jedis.exists("foo3"));

    jedis.set("foo1", "bar1");

    assertEquals(1L, jedis.del("foo1", "foo2"));

    assertEquals(0L, jedis.del("foo1", "foo2"));

    // Binary ...
    jedis.set(bfoo1, bbar1);
    jedis.set(bfoo2, bbar2);
    jedis.set(bfoo3, bbar3);

    assertEquals(3L, jedis.del(bfoo1, bfoo2, bfoo3));

    assertFalse(jedis.exists(bfoo1));
    assertFalse(jedis.exists(bfoo2));
    assertFalse(jedis.exists(bfoo3));

    jedis.set(bfoo1, bbar1);

    assertEquals(1, jedis.del(bfoo1, bfoo2));

    assertEquals(0, jedis.del(bfoo1, bfoo2));
  }

  @Test
  public void unlink() {
    jedis.set("foo1", "bar1");
    jedis.set("foo2", "bar2");
    jedis.set("foo3", "bar3");

    assertEquals(3, jedis.unlink("foo1", "foo2", "foo3"));

    assertEquals(0, jedis.exists("foo1", "foo2", "foo3"));

    jedis.set("foo1", "bar1");

    assertEquals(1, jedis.unlink("foo1", "foo2"));

    assertEquals(0, jedis.unlink("foo1", "foo2"));

    jedis.set("foo", "bar");
    assertEquals(1, jedis.unlink("foo"));
    assertFalse(jedis.exists("foo"));

    // Binary
    jedis.set(bfoo1, bbar1);
    jedis.set(bfoo2, bbar2);
    jedis.set(bfoo3, bbar3);

    assertEquals(3, jedis.unlink(bfoo1, bfoo2, bfoo3));

    assertEquals(0, jedis.exists(bfoo1, bfoo2, bfoo3));

    jedis.set(bfoo1, bbar1);

    assertEquals(1, jedis.unlink(bfoo1, bfoo2));

    assertEquals(0, jedis.unlink(bfoo1, bfoo2));

    jedis.set(bfoo, bbar);
    assertEquals(1, jedis.unlink(bfoo));
    assertFalse(jedis.exists(bfoo));
  }

  @Test
  public void type() {
    jedis.set("foo", "bar");
    assertEquals("string", jedis.type("foo"));

    // Binary
    jedis.set(bfoo, bbar);
    assertEquals("string", jedis.type(bfoo));
  }

  @Test
  public void keys() {
    jedis.set("foo", "bar");
    jedis.set("foobar", "bar");

    Set<String> keys = jedis.keys("foo*");
    AssertUtil.assertCollectionContains(keys, "foo");
    AssertUtil.assertCollectionContains(keys, "foobar");

    assertEquals(Collections.emptySet(), jedis.keys("bar*"));

    // Binary
    jedis.set(bfoo, bbar);
    jedis.set(bfoobar, bbar);

    Set<byte[]> bkeys = jedis.keys(bfoostar);
    AssertUtil.assertByteArrayCollectionContains(bkeys, bfoo);
    AssertUtil.assertByteArrayCollectionContains(bkeys, bfoobar);

    assertEquals(Collections.emptySet(), jedis.keys(bbarstar));
  }

  @Test
  public void randomKey() {
    assertNull(jedis.randomKey());

    jedis.set("foo", "bar");

    assertEquals("foo", jedis.randomKey());

    jedis.set("bar", "foo");

    String randomkey = jedis.randomKey();
    assertTrue(randomkey.equals("foo") || randomkey.equals("bar"));

    // Binary
    jedis.del("foo");
    jedis.del("bar");
    assertNull(jedis.randomKey());

    jedis.set(bfoo, bbar);

    assertArrayEquals(bfoo, jedis.randomBinaryKey());

    jedis.set(bbar, bfoo);

    byte[] randomBkey = jedis.randomBinaryKey();
    assertTrue(Arrays.equals(randomBkey, bfoo) || Arrays.equals(randomBkey, bbar));

  }

  @Test
  public void rename() {
    jedis.set("foo", "bar");
    String status = jedis.rename("foo", "bar");
    assertEquals("OK", status);

    assertNull(jedis.get("foo"));

    assertEquals("bar", jedis.get("bar"));

    // Binary
    jedis.set(bfoo, bbar);
    String bstatus = jedis.rename(bfoo, bbar);
    assertEquals("OK", bstatus);

    assertNull(jedis.get(bfoo));

    assertArrayEquals(bbar, jedis.get(bbar));
  }

  @Test
  public void renameOldAndNewAreTheSame() {
    assertEquals("OK", jedis.set("foo", "bar"));
    assertEquals("OK", jedis.rename("foo", "foo"));

    // Binary
    assertEquals("OK", jedis.set(bfoo, bbar));
    assertEquals("OK", jedis.rename(bfoo, bfoo));
  }

  @Test
  public void renamenx() {
    jedis.set("foo", "bar");
    assertEquals(1, jedis.renamenx("foo", "bar"));

    jedis.set("foo", "bar");
    assertEquals(0, jedis.renamenx("foo", "bar"));

    // Binary
    jedis.set(bfoo, bbar);
    assertEquals(1, jedis.renamenx(bfoo, bbar));

    jedis.set(bfoo, bbar);
    assertEquals(0, jedis.renamenx(bfoo, bbar));

  }

  @Test
  public void dbSize() {
    assertEquals(0, jedis.dbSize());

    jedis.set("foo", "bar");
    assertEquals(1, jedis.dbSize());

    // Binary
    jedis.set(bfoo, bbar);
    assertEquals(2, jedis.dbSize());
  }

  @Test
  public void expire() {
    assertEquals(0, jedis.expire("foo", 20L));

    jedis.set("foo", "bar");
    assertEquals(1, jedis.expire("foo", 20L));
    assertEquals(0, jedis.expire("foo", 20L, ExpiryOption.NX));

    // Binary
    assertEquals(0, jedis.expire(bfoo, 20L));

    jedis.set(bfoo, bbar);
    assertEquals(1, jedis.expire(bfoo, 20L));
    assertEquals(0, jedis.expire(bfoo, 20L, ExpiryOption.NX));
  }

  @Test
  public void expireAt() {
    long unixTime = (System.currentTimeMillis() / 1000L) + 20;

    assertEquals(0, jedis.expireAt("foo", unixTime));

    jedis.set("foo", "bar");
    unixTime = (System.currentTimeMillis() / 1000L) + 20;
    assertEquals(1, jedis.expireAt("foo", unixTime));
    assertEquals(1, jedis.expireAt("foo", unixTime, ExpiryOption.XX));

    // Binary
    assertEquals(0, jedis.expireAt(bfoo, unixTime));

    jedis.set(bfoo, bbar);
    unixTime = (System.currentTimeMillis() / 1000L) + 20;
    assertEquals(1, jedis.expireAt(bfoo, unixTime));
    assertEquals(1, jedis.expireAt(bfoo, unixTime, ExpiryOption.XX));
  }

  @Test
  public void expireTime() {
    long unixTime;

    jedis.set("foo", "bar");
    unixTime = (System.currentTimeMillis() / 1000L) + 20;
    jedis.expireAt("foo", unixTime);
    assertEquals(unixTime, jedis.expireTime("foo"), 0.0001);

    // Binary
    jedis.set(bfoo, bbar);
    unixTime = (System.currentTimeMillis() / 1000L) + 20;
    jedis.expireAt(bfoo, unixTime);
    assertEquals(unixTime, jedis.expireTime(bfoo), 0.0001);
  }

  @Test
  public void ttl() {
    assertEquals(-2, jedis.ttl("foo"));

    jedis.set("foo", "bar");
    assertEquals(-1, jedis.ttl("foo"));

    jedis.expire("foo", 20);
    long ttl = jedis.ttl("foo");
    assertTrue(ttl >= 0 && ttl <= 20);

    // Binary
    assertEquals(-2, jedis.ttl(bfoo));

    jedis.set(bfoo, bbar);
    assertEquals(-1, jedis.ttl(bfoo));

    jedis.expire(bfoo, 20);
    long bttl = jedis.ttl(bfoo);
    assertTrue(bttl >= 0 && bttl <= 20);
  }

  @Test
  public void touch() throws Exception {
    assertEquals(0, jedis.touch("foo1", "foo2", "foo3"));

    jedis.set("foo1", "bar1");

    Thread.sleep(1100); // little over 1 sec
    assertTrue(jedis.objectIdletime("foo1") > 0);

    assertEquals(1, jedis.touch("foo1"));
    assertEquals(0L, jedis.objectIdletime("foo1").longValue());

    assertEquals(1, jedis.touch("foo1", "foo2", "foo3"));

    jedis.set("foo2", "bar2");

    jedis.set("foo3", "bar3");

    assertEquals(3, jedis.touch("foo1", "foo2", "foo3"));

    // Binary
    assertEquals(0, jedis.touch(bfoo1, bfoo2, bfoo3));

    jedis.set(bfoo1, bbar1);

    Thread.sleep(1100); // little over 1 sec
    assertTrue(jedis.objectIdletime(bfoo1) > 0);

    assertEquals(1, jedis.touch(bfoo1));
    assertEquals(0L, jedis.objectIdletime(bfoo1).longValue());

    assertEquals(1, jedis.touch(bfoo1, bfoo2, bfoo3));

    jedis.set(bfoo2, bbar2);

    jedis.set(bfoo3, bbar3);

    assertEquals(3, jedis.touch(bfoo1, bfoo2, bfoo3));

  }

  @Test
  public void select() {
    jedis.set("foo", "bar");
    String status = jedis.select(1);
    assertEquals("OK", status);
    assertNull(jedis.get("foo"));
    status = jedis.select(0);
    assertEquals("OK", status);
    assertEquals("bar", jedis.get("foo"));
    // Binary
    jedis.set(bfoo, bbar);
    String bstatus = jedis.select(1);
    assertEquals("OK", bstatus);
    assertNull(jedis.get(bfoo));
    bstatus = jedis.select(0);
    assertEquals("OK", bstatus);
    assertArrayEquals(bbar, jedis.get(bfoo));
  }

  @Test
  public void getDB() {
    assertEquals(0, jedis.getDB());
    jedis.select(1);
    assertEquals(1, jedis.getDB());
  }

  @Test
  public void move() {
    assertEquals(0, jedis.move("foo", 1));

    jedis.set("foo", "bar");
    assertEquals(1, jedis.move("foo", 1));
    assertNull(jedis.get("foo"));

    jedis.select(1);
    assertEquals("bar", jedis.get("foo"));

    // Binary
    jedis.select(0);
    assertEquals(0, jedis.move(bfoo, 1));

    jedis.set(bfoo, bbar);
    assertEquals(1, jedis.move(bfoo, 1));
    assertNull(jedis.get(bfoo));

    jedis.select(1);
    assertArrayEquals(bbar, jedis.get(bfoo));

  }

  @Test
  public void swapDB() {
    jedis.set("foo1", "bar1");
    jedis.select(1);
    assertNull(jedis.get("foo1"));
    jedis.set("foo2", "bar2");
    String status = jedis.swapDB(0, 1);
    assertEquals("OK", status);
    assertEquals("bar1", jedis.get("foo1"));
    assertNull(jedis.get("foo2"));
    jedis.select(0);
    assertNull(jedis.get("foo1"));
    assertEquals("bar2", jedis.get("foo2"));

    // Binary
    jedis.set(bfoo1, bbar1);
    jedis.select(1);
    assertArrayEquals(null, jedis.get(bfoo1));
    jedis.set(bfoo2, bbar2);
    status = jedis.swapDB(0, 1);
    assertEquals("OK", status);
    assertArrayEquals(bbar1, jedis.get(bfoo1));
    assertArrayEquals(null, jedis.get(bfoo2));
    jedis.select(0);
    assertArrayEquals(null, jedis.get(bfoo1));
    assertArrayEquals(bbar2, jedis.get(bfoo2));
  }

  @Test
  public void flushDB() {
    jedis.set("foo", "bar");
    assertEquals(1, jedis.dbSize());
    jedis.set("bar", "foo");
    jedis.move("bar", 1);
    String status = jedis.flushDB();
    assertEquals("OK", status);
    assertEquals(0, jedis.dbSize());
    jedis.select(1);
    assertEquals(1, jedis.dbSize());
    assertEquals("OK", jedis.flushDB(FlushMode.SYNC));
    assertEquals(0, jedis.dbSize());

    // Binary
    jedis.select(0);
    jedis.set(bfoo, bbar);
    assertEquals(1, jedis.dbSize());
    jedis.set(bbar, bfoo);
    jedis.move(bbar, 1);
    String bstatus = jedis.flushDB();
    assertEquals("OK", bstatus);
    assertEquals(0, jedis.dbSize());
    jedis.select(1);
    assertEquals(1, jedis.dbSize());
    assertEquals("OK", jedis.flushDB(FlushMode.ASYNC));
    assertEquals(0, jedis.dbSize());
  }

  @Test
  public void flushAll() {
    jedis.set("foo", "bar");
    assertEquals(1, jedis.dbSize());
    jedis.set("bar", "foo");
    jedis.move("bar", 1);
    String status = jedis.flushAll();
    assertEquals("OK", status);
    assertEquals(0, jedis.dbSize());
    jedis.select(1);
    assertEquals(0, jedis.dbSize());
    jedis.set("foo", "bar");
    assertEquals(1, jedis.dbSize());
    assertEquals("OK", jedis.flushAll(FlushMode.SYNC));
    assertEquals(0, jedis.dbSize());

    // Binary
    jedis.select(0);
    jedis.set(bfoo, bbar);
    assertEquals(1, jedis.dbSize());
    jedis.set(bbar, bfoo);
    jedis.move(bbar, 1);
    String bstatus = jedis.flushAll();
    assertEquals("OK", bstatus);
    assertEquals(0, jedis.dbSize());
    jedis.select(1);
    assertEquals(0, jedis.dbSize());
    jedis.set(bfoo, bbar);
    assertEquals(1, jedis.dbSize());
    assertEquals("OK", jedis.flushAll(FlushMode.ASYNC));
    assertEquals(0, jedis.dbSize());
  }

  @Test
  public void persist() {
    jedis.setex("foo", 60 * 60, "bar");
    assertTrue(jedis.ttl("foo") > 0);
    assertEquals(1, jedis.persist("foo"));
    assertEquals(-1, jedis.ttl("foo"));

    // Binary
    jedis.setex(bfoo, 60 * 60, bbar);
    assertTrue(jedis.ttl(bfoo) > 0);
    assertEquals(1, jedis.persist(bfoo));
    assertEquals(-1, jedis.ttl(bfoo));
  }

  @Test
  public void echo() {
    String result = jedis.echo("hello world");
    assertEquals("hello world", result);

    // Binary
    byte[] bresult = jedis.echo(SafeEncoder.encode("hello world"));
    assertArrayEquals(SafeEncoder.encode("hello world"), bresult);
  }

  @Test
  public void dumpAndRestore() {
    jedis.set("foo1", "bar");
    byte[] sv = jedis.dump("foo1");
    jedis.restore("foo2", 0, sv);
    assertEquals("bar", jedis.get("foo2"));

    jedis.set(bfoo1, bbar);
    sv = jedis.dump(bfoo1);
    jedis.restore(bfoo2, 0, sv);
    assertArrayEquals(bbar, jedis.get(bfoo2));
  }

  @Test
  public void restoreParams() {
    // take a separate instance
    Jedis jedis2 = new Jedis(endpoint.getHost(), endpoint.getPort(), 500);
    jedis2.auth(endpoint.getPassword());
    jedis2.flushAll();

    jedis2.set("foo", "bar");
    jedis.set("from", "a");
    byte[] serialized = jedis.dump("from");

    try {
      jedis2.restore("foo", 0, serialized);
      fail("Simple restore on a existing key should fail");
    } catch (JedisDataException e) {
      // should be here
    }
    try {
      jedis2.restore("foo", 0, serialized, RestoreParams.restoreParams());
      fail("Simple restore on a existing key should fail");
    } catch (JedisDataException e) {
      // should be here
    }
    assertEquals("bar", jedis2.get("foo"));

    jedis2.restore("foo", 1000, serialized, RestoreParams.restoreParams().replace());
    assertEquals("a", jedis2.get("foo"));
    assertTrue(jedis2.pttl("foo") <= 1000);

    jedis2.restore("bar", System.currentTimeMillis() + 1000, serialized, RestoreParams.restoreParams().replace().absTtl());
    assertTrue(jedis2.pttl("bar") <= 1000);

    jedis2.restore("bar1", 1000, serialized, RestoreParams.restoreParams().replace().idleTime(1000));
    assertEquals(1000, jedis2.objectIdletime("bar1").longValue());
    jedis2.close();

    Jedis lfuJedis = new Jedis(lfuEndpoint.getHostAndPort(), lfuEndpoint.getClientConfigBuilder().timeoutMillis(500).build());;
    lfuJedis.restore("bar1", 1000, serialized, RestoreParams.restoreParams().replace().frequency(90));
    assertEquals(90, lfuJedis.objectFreq("bar1").longValue());
    lfuJedis.close();
  }

  @Test
  public void pexpire() {
    assertEquals(0, jedis.pexpire("foo", 10000));

    jedis.set("foo1", "bar1");
    assertEquals(1, jedis.pexpire("foo1", 10000));

    jedis.set("foo2", "bar2");
    assertEquals(1, jedis.pexpire("foo2", 200000000000L));
    assertEquals(0, jedis.pexpire("foo2", 10000000, ExpiryOption.NX));
    assertEquals(1, jedis.pexpire("foo2", 10000000, ExpiryOption.XX));
    assertEquals(0, jedis.pexpire("foo2", 10000, ExpiryOption.GT));
    assertEquals(1, jedis.pexpire("foo2", 10000, ExpiryOption.LT));

    long pttl = jedis.pttl("foo2");
    assertTrue(pttl > 100L);

    // Binary
    assertEquals(0, jedis.pexpire(bfoo, 10000));

    jedis.set(bfoo, bbar);
    assertEquals(1, jedis.pexpire(bfoo, 10000));
    assertEquals(0, jedis.pexpire(bfoo, 10000, ExpiryOption.NX));
  }

  @Test
  public void pexpireAt() {
    long unixTime = (System.currentTimeMillis()) + 10000;

    assertEquals(0, jedis.pexpireAt("foo", unixTime));

    jedis.set("foo", "bar");
    assertEquals(1, jedis.pexpireAt("foo", unixTime));

    // Binary
    assertEquals(0, jedis.pexpireAt(bfoo, unixTime));

    jedis.set(bfoo, bbar);
    assertEquals(1, jedis.pexpireAt(bfoo, unixTime));
  }

  @Test
  public void pexpireTime() {
    long unixTime = (System.currentTimeMillis()) + 10000;

    jedis.set("foo", "bar");
    jedis.pexpireAt("foo", unixTime);
    assertEquals(unixTime, jedis.pexpireTime("foo"), 0.0001);

    // Binary
    jedis.set(bfoo, bbar);
    jedis.pexpireAt(bfoo, unixTime);
    assertEquals(unixTime, jedis.pexpireTime(bfoo), 0.0001);
  }

  @Test
  public void pttl() {
    assertEquals(-2, jedis.pttl("foo"));

    jedis.set("foo", "bar");
    assertEquals(-1, jedis.pttl("foo"));

    jedis.pexpire("foo", 20000);
    long pttl = jedis.pttl("foo");
    assertTrue(pttl >= 0 && pttl <= 20000);

    // Binary
    assertEquals(-2, jedis.pttl(bfoo));

    jedis.set(bfoo, bbar);
    assertEquals(-1, jedis.pttl(bfoo));

    jedis.pexpire(bfoo, 20000);
    pttl = jedis.pttl(bfoo);
    assertTrue(pttl >= 0 && pttl <= 20000);
  }

  @Test
  public void psetex() {
    long pttl;

    jedis.psetex("foo", 200000000000L, "bar");
    pttl = jedis.pttl("foo");
    assertTrue(pttl > 100000000000L);

    // Binary
    jedis.psetex(bfoo, 200000000000L, bbar);
    pttl = jedis.pttl(bfoo);
    assertTrue(pttl > 100000000000L);
  }

  @Test
  public void scan() {
    jedis.set("b", "b");
    jedis.set("a", "a");

    ScanResult<String> result = jedis.scan(SCAN_POINTER_START);

    assertEquals(SCAN_POINTER_START, result.getCursor());
    assertFalse(result.getResult().isEmpty());

    // binary
    ScanResult<byte[]> bResult = jedis.scan(SCAN_POINTER_START_BINARY);

    assertArrayEquals(SCAN_POINTER_START_BINARY, bResult.getCursorAsBytes());
    assertFalse(bResult.getResult().isEmpty());
  }

  @Test
  public void scanMatch() {
    ScanParams params = new ScanParams();
    params.match("a*");

    jedis.set("b", "b");
    jedis.set("a", "a");
    jedis.set("aa", "aa");
    ScanResult<String> result = jedis.scan(SCAN_POINTER_START, params);

    assertEquals(SCAN_POINTER_START, result.getCursor());
    assertFalse(result.getResult().isEmpty());

    // binary
    params = new ScanParams();
    params.match(bfoostar);

    jedis.set(bfoo1, bbar);
    jedis.set(bfoo2, bbar);
    jedis.set(bfoo3, bbar);

    ScanResult<byte[]> bResult = jedis.scan(SCAN_POINTER_START_BINARY, params);

    assertArrayEquals(SCAN_POINTER_START_BINARY, bResult.getCursorAsBytes());
    assertFalse(bResult.getResult().isEmpty());
  }

  @Test
  public void scanCount() {
    ScanParams params = new ScanParams();
    params.count(2);

    for (int i = 0; i < 10; i++) {
      jedis.set("a" + i, "a" + i);
    }

    ScanResult<String> result = jedis.scan(SCAN_POINTER_START, params);

    assertFalse(result.getResult().isEmpty());

    // binary
    params = new ScanParams();
    params.count(2);

    jedis.set(bfoo1, bbar);
    jedis.set(bfoo2, bbar);
    jedis.set(bfoo3, bbar);

    ScanResult<byte[]> bResult = jedis.scan(SCAN_POINTER_START_BINARY, params);

    assertFalse(bResult.getResult().isEmpty());
  }

  @Test
  public void scanType() {
    ScanParams noParams = new ScanParams();
    ScanParams pagingParams = new ScanParams().count(4);

    jedis.set("a", "a");
    jedis.hset("b", "b", "b");
    jedis.set("c", "c");
    jedis.sadd("d", "d");
    jedis.set("e", "e");
    jedis.zadd("f", 0d, "f");
    jedis.set("g", "g");

    // string
    ScanResult<String> scanResult;

    scanResult = jedis.scan(SCAN_POINTER_START, pagingParams, "string");
    assertFalse(scanResult.isCompleteIteration());
    int page1Count = scanResult.getResult().size();
    scanResult = jedis.scan(scanResult.getCursor(), pagingParams, "string");
    assertTrue(scanResult.isCompleteIteration());
    int page2Count = scanResult.getResult().size();
    assertEquals(4, page1Count + page2Count);


    scanResult = jedis.scan(SCAN_POINTER_START, noParams, "hash");
    assertEquals(Collections.singletonList("b"), scanResult.getResult());
    scanResult = jedis.scan(SCAN_POINTER_START, noParams, "set");
    assertEquals(Collections.singletonList("d"), scanResult.getResult());
    scanResult = jedis.scan(SCAN_POINTER_START, noParams, "zset");
    assertEquals(Collections.singletonList("f"), scanResult.getResult());

    // binary
    final byte[] string = "string".getBytes();
    final byte[] hash = "hash".getBytes();
    final byte[] set = "set".getBytes();
    final byte[] zset = "zset".getBytes();

    ScanResult<byte[]> binaryResult;

    jedis.set("a", "a");
    jedis.hset("b", "b", "b");
    jedis.set("c", "c");
    jedis.sadd("d", "d");
    jedis.set("e", "e");
    jedis.zadd("f", 0d, "f");
    jedis.set("g", "g");

    binaryResult = jedis.scan(SCAN_POINTER_START_BINARY, pagingParams, string);
    assertFalse(binaryResult.isCompleteIteration());
    page1Count = binaryResult.getResult().size();
    binaryResult = jedis.scan(binaryResult.getCursorAsBytes(), pagingParams, string);
    assertTrue(binaryResult.isCompleteIteration());
    page2Count = binaryResult.getResult().size();
    assertEquals(4, page1Count + page2Count);

    binaryResult = jedis.scan(SCAN_POINTER_START_BINARY, noParams, hash);
    AssertUtil.assertByteArrayListEquals(Collections.singletonList(new byte[]{98}), binaryResult.getResult());
    binaryResult = jedis.scan(SCAN_POINTER_START_BINARY, noParams, set);
    AssertUtil.assertByteArrayListEquals(Collections.singletonList(new byte[]{100}), binaryResult.getResult());
    binaryResult = jedis.scan(SCAN_POINTER_START_BINARY, noParams, zset);
    AssertUtil.assertByteArrayListEquals(Collections.singletonList(new byte[]{102}), binaryResult.getResult());
  }

  @Test
  public void scanIsCompleteIteration() {
    for (int i = 0; i < 100; i++) {
      jedis.set("a" + i, "a" + i);
    }

    ScanResult<String> result = jedis.scan(SCAN_POINTER_START);
    // note: in theory Redis would be allowed to already return all results on the 1st scan,
    // but in practice this never happens for data sets greater than a few tens
    // see: https://redis.io/commands/scan#number-of-elements-returned-at-every-scan-call
    assertFalse(result.isCompleteIteration());

    result = scanCompletely(result.getCursor());

    assertNotNull(result);
    assertTrue(result.isCompleteIteration());
  }

  private ScanResult<String> scanCompletely(String cursor) {
    ScanResult<String> scanResult;
    do {
      scanResult = jedis.scan(cursor);
      cursor = scanResult.getCursor();
    } while (!SCAN_POINTER_START.equals(scanResult.getCursor()));

    return scanResult;
  }

  @Test
  public void setNxExAndGet() {
    assertEquals("OK", jedis.set("hello", "world", SetParams.setParams().nx().ex(expireSeconds)));
    assertEquals("world", jedis.get("hello"));

    assertNull(jedis.set("hello", "bar", SetParams.setParams().nx().ex(expireSeconds)));
    assertEquals("world", jedis.get("hello"));

    long ttl = jedis.ttl("hello");
    assertTrue(ttl > 0 && ttl <= expireSeconds);

    // binary
    byte[] bworld = { 0x77, 0x6F, 0x72, 0x6C, 0x64 };
    byte[] bhello = { 0x68, 0x65, 0x6C, 0x6C, 0x6F };

    assertEquals("OK", jedis.set(bworld, bhello, SetParams.setParams().nx().ex(expireSeconds)));
    assertArrayEquals(bhello, jedis.get(bworld));

    assertNull(jedis.set(bworld, bbar, SetParams.setParams().nx().ex(expireSeconds)));
    assertArrayEquals(bhello, jedis.get(bworld));

    long bttl = jedis.ttl(bworld);
    assertTrue(bttl > 0 && bttl <= expireSeconds);
  }

  @Test
  public void setGetOptionTest() {
    assertEquals("OK", jedis.set("hello", "world"));

    // GET old value
    assertEquals("world", jedis.setGet("hello", "jedis"));

    assertEquals("jedis", jedis.get("hello"));

    // GET null value
    assertNull(jedis.setGet("key", "value"));
  }

  @Test
  public void setGet() {
    assertEquals("OK", jedis.set("hello", "world"));

    // GET old value
    assertEquals("world", jedis.setGet("hello", "jedis", SetParams.setParams()));

    assertEquals("jedis", jedis.get("hello"));

    // GET null value
    assertNull(jedis.setGet("key", "value", SetParams.setParams()));
  }

  @Test
  public void sendCommandTest() {
    Object obj = jedis.sendCommand(SET, "x", "1");
    String returnValue = SafeEncoder.encode((byte[]) obj);
    assertEquals("OK", returnValue);
    obj = jedis.sendCommand(GET, "x");
    returnValue = SafeEncoder.encode((byte[]) obj);
    assertEquals("1", returnValue);

    jedis.sendCommand(RPUSH, "foo", "a");
    jedis.sendCommand(RPUSH, "foo", "b");
    jedis.sendCommand(RPUSH, "foo", "c");

    obj = jedis.sendCommand(LRANGE, "foo", "0", "2");
    List<byte[]> list = (List<byte[]>) obj;
    List<byte[]> expected = new ArrayList<>(3);
    expected.add("a".getBytes());
    expected.add("b".getBytes());
    expected.add("c".getBytes());
    for (int i = 0; i < 3; i++)
      assertArrayEquals(expected.get(i), list.get(i));

    assertEquals("PONG", SafeEncoder.encode((byte[]) jedis.sendCommand(PING)));
  }

  @Test
  public void sendBlockingCommandTest() {
    assertNull(jedis.sendBlockingCommand(BLPOP, "foo", Long.toString(1L)));

    jedis.sendCommand(RPUSH, "foo", "bar");
    assertEquals(Arrays.asList("foo", "bar"),
      SafeEncoder.encodeObject(jedis.sendBlockingCommand(BLPOP, "foo", Long.toString(1L))));

    assertNull(jedis.sendBlockingCommand(BLPOP, "foo", Long.toString(1L)));
  }

  @Test
  public void encodeCompleteResponsePing() {
    assertEquals("PONG", SafeEncoder.encodeObject(jedis.sendCommand(PING)));
  }

  @Test
  public void encodeCompleteResponseHgetall() {
    Assume.assumeFalse(protocol == RedisProtocol.RESP3);

    HashMap<String, String> entries = new HashMap<>();
    entries.put("foo", "bar");
    entries.put("foo2", "bar2");
    jedis.hset("hash:test:encode", entries);

    List encodeObj = (List) SafeEncoder.encodeObject(jedis.sendCommand(HGETALL, "hash:test:encode"));

    assertEquals(4, encodeObj.size());
    entries.forEach((k, v) -> {
      assertThat((Iterable<String>) encodeObj, Matchers.hasItem(k));
      assertEquals(v, findValueFromMapAsList(encodeObj, k));
    });
  }

  @Test
  public void encodeCompleteResponseHgetallResp3() {
    Assume.assumeTrue(protocol == RedisProtocol.RESP3);

    HashMap<String, String> entries = new HashMap<>();
    entries.put("foo", "bar");
    entries.put("foo2", "bar2");
    jedis.hset("hash:test:encode", entries);

    List<KeyValue> encodeObj = (List<KeyValue>) SafeEncoder.encodeObject(jedis.sendCommand(HGETALL, "hash:test:encode"));

    assertEquals(2, encodeObj.size());
    encodeObj.forEach(kv -> {
      assertThat(entries, Matchers.hasEntry(kv.getKey(), kv.getValue()));
    });
  }

  @Test
  public void encodeCompleteResponseXinfoStream() {
    Assume.assumeFalse(protocol == RedisProtocol.RESP3);

    HashMap<String, String> entry = new HashMap<>();
    entry.put("foo", "bar");
    StreamEntryID entryID = jedis.xadd("mystream", StreamEntryID.NEW_ENTRY, entry);
    jedis.xgroupCreate("mystream", "mygroup", null, false);

    Object obj = jedis.sendCommand(XINFO, "STREAM", "mystream");

    List encodeObj = (List) SafeEncoder.encodeObject(obj);

    assertThat(encodeObj.size(), Matchers.greaterThanOrEqualTo(14));
    assertEquals("must have even number of elements", 0, encodeObj.size() % 2); // must be even

    assertEquals(1L, findValueFromMapAsList(encodeObj, "length"));
    assertEquals(entryID.toString(), findValueFromMapAsList(encodeObj, "last-generated-id"));

    List<String> entryAsList = new ArrayList<>(2);
    entryAsList.add("foo");
    entryAsList.add("bar");

    assertEquals(entryAsList, ((List) findValueFromMapAsList(encodeObj, "first-entry")).get(1));
    assertEquals(entryAsList, ((List) findValueFromMapAsList(encodeObj, "last-entry")).get(1));
  }

  @Test
  public void encodeCompleteResponseXinfoStreamResp3() {
    Assume.assumeTrue(protocol == RedisProtocol.RESP3);

    HashMap<String, String> entry = new HashMap<>();
    entry.put("foo", "bar");
    StreamEntryID entryID = jedis.xadd("mystream", StreamEntryID.NEW_ENTRY, entry);
    jedis.xgroupCreate("mystream", "mygroup", null, false);

    Object obj = jedis.sendCommand(XINFO, "STREAM", "mystream");

    List<KeyValue> encodeObj = (List<KeyValue>) SafeEncoder.encodeObject(obj);

    assertThat(encodeObj.size(), Matchers.greaterThanOrEqualTo(7));

    assertEquals(1L, findValueFromMapAsKeyValueList(encodeObj, "length"));
    assertEquals(entryID.toString(), findValueFromMapAsKeyValueList(encodeObj, "last-generated-id"));

    List<String> entryAsList = new ArrayList<>(2);
    entryAsList.add("foo");
    entryAsList.add("bar");

    assertEquals(entryAsList, ((List) findValueFromMapAsKeyValueList(encodeObj, "first-entry")).get(1));
    assertEquals(entryAsList, ((List) findValueFromMapAsKeyValueList(encodeObj, "last-entry")).get(1));
  }

  private Object findValueFromMapAsList(List list, Object key) {
    for (int i = 0; i < list.size(); i += 2) {
      if (key.equals(list.get(i))) {
        return list.get(i + 1);
      }
    }
    return null;
  }

  private Object findValueFromMapAsKeyValueList(List<KeyValue> list, Object key) {
    for (KeyValue kv : list) {
      if (key.equals(kv.getKey())) {
        return kv.getValue();
      }
    }
    return null;
  }

  @Test
  public void copy() {
    assertFalse(jedis.copy("unknown", "foo", false));

    jedis.set("foo1", "bar");
    assertTrue(jedis.copy("foo1", "foo2", false));
    assertEquals("bar", jedis.get("foo2"));

    // with destinationDb
    assertTrue(jedis.copy("foo1", "foo3", 2, false));
    jedis.select(2);
    assertEquals("bar", jedis.get("foo3"));
    jedis.select(0); // getting back to original db, for next tests

    // replace
    jedis.set("foo1", "bar1");
    assertTrue(jedis.copy("foo1", "foo2", true));
    assertEquals("bar1", jedis.get("foo2"));

    // Binary
    assertFalse(jedis.copy(bfoobar, bfoo, false));

    jedis.set(bfoo1, bbar);
    assertTrue(jedis.copy(bfoo1, bfoo2, false));
    assertArrayEquals(bbar, jedis.get(bfoo2));

    // with destinationDb
    assertTrue(jedis.copy(bfoo1, bfoo3, 3, false));
    jedis.select(3);
    assertArrayEquals(bbar, jedis.get(bfoo3));
    jedis.select(0); // getting back to original db, for next tests

    // replace
    jedis.set(bfoo1, bbar1);
    assertTrue(jedis.copy(bfoo1, bfoo2, true));
    assertArrayEquals(bbar1, jedis.get(bfoo2));
  }

  @Test
  public void reset() {
    // response test
    String status = jedis.reset();
    assertEquals("RESET", status);

    // auth reset
    String counter = "counter";
    Exception ex1 = assertThrows(JedisDataException.class, () -> {
      jedis.set(counter, "1");
    });
    assertEquals("NOAUTH Authentication required.", ex1.getMessage());

    // multi reset
    jedis.auth(endpoint.getPassword());
    jedis.set(counter, "1");

    Transaction trans = jedis.multi();
    trans.incr(counter);
    jedis.reset();

    Exception ex2 = assertThrows(JedisDataException.class, trans::exec);
    assertEquals("EXECABORT Transaction discarded because of: NOAUTH Authentication required.", ex2.getMessage());

    jedis.auth(endpoint.getPassword());
    assertEquals("1", jedis.get(counter));
  }
}
