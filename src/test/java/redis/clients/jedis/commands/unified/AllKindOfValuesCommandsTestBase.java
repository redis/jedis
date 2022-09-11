package redis.clients.jedis.commands.unified;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import static redis.clients.jedis.Protocol.Command.BLPOP;
import static redis.clients.jedis.Protocol.Command.GET;
import static redis.clients.jedis.Protocol.Command.HGETALL;
import static redis.clients.jedis.Protocol.Command.LRANGE;
import static redis.clients.jedis.Protocol.Command.PING;
import static redis.clients.jedis.Protocol.Command.RPUSH;
import static redis.clients.jedis.Protocol.Command.SET;
import static redis.clients.jedis.Protocol.Command.XINFO;
import static redis.clients.jedis.util.SafeEncoder.encode;
import static redis.clients.jedis.util.SafeEncoder.encodeObject;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;
import static redis.clients.jedis.params.SetParams.setParams;
import static redis.clients.jedis.util.AssertUtil.assertByteArrayListEquals;
import static redis.clients.jedis.util.AssertUtil.assertCollectionContains;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.exceptions.JedisDataException;

public abstract class AllKindOfValuesCommandsTestBase extends UnifiedJedisCommandsTestBase {

  protected final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  protected final byte[] bfoo1 = { 0x01, 0x02, 0x03, 0x04, 0x0A };
  protected final byte[] bfoo2 = { 0x01, 0x02, 0x03, 0x04, 0x0B };
  protected final byte[] bfoo3 = { 0x01, 0x02, 0x03, 0x04, 0x0C };
  protected final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  protected final byte[] bbar1 = { 0x05, 0x06, 0x07, 0x08, 0x0A };
  protected final byte[] bbar2 = { 0x05, 0x06, 0x07, 0x08, 0x0B };
  protected final byte[] bbar3 = { 0x05, 0x06, 0x07, 0x08, 0x0C };

  protected final byte[] bfoobar = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08 };
  protected final byte[] bfoostar = { 0x01, 0x02, 0x03, 0x04, '*' };
  protected final byte[] bbarstar = { 0x05, 0x06, 0x07, 0x08, '*' };

  protected final byte[] bnx = { 0x6E, 0x78 };
  protected final byte[] bex = { 0x65, 0x78 };
  final int expireSeconds = 2;

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
    Set<String> expected = new HashSet<>();
    expected.add("foo");
    expected.add("foobar");
    assertEquals(expected, keys);

    expected = new HashSet<>();
    keys = jedis.keys("bar*");

    assertEquals(expected, keys);

    // Binary
    jedis.set(bfoo, bbar);
    jedis.set(bfoobar, bbar);

    Set<byte[]> bkeys = jedis.keys(bfoostar);
    assertEquals(2, bkeys.size());
    assertCollectionContains(bkeys, bfoo);
    assertCollectionContains(bkeys, bfoobar);

    bkeys = jedis.keys(bbarstar);

    assertEquals(0, bkeys.size());
  }

  @Test
  public void randomKey() {
    assertNull(jedis.randomKey());

    for (int i = 0; i < 100; i++) {
      jedis.set("foo" + i, "bar"+i);
    }

    String key = jedis.randomKey();
    assertNotNull(key);
    assertTrue(key.startsWith("foo"));
    assertEquals(key.replace("foo", "bar"), jedis.get(key));
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
    jedis.set("foo", "bar");
    jedis.set("from", "a");
    byte[] serialized = jedis.dump("from");

    try {
      jedis.restore("foo", 0, serialized);
      fail("Simple restore on a existing key should fail");
    } catch (JedisDataException e) {
      // should be here
    }
    try {
      jedis.restore("foo", 0, serialized, RestoreParams.restoreParams());
      fail("Simple restore on a existing key should fail");
    } catch (JedisDataException e) {
      // should be here
    }
    assertEquals("bar", jedis.get("foo"));

    jedis.restore("foo", 1000, serialized, RestoreParams.restoreParams().replace());
    assertEquals("a", jedis.get("foo"));
    assertTrue(jedis.pttl("foo") <= 1000);

    jedis.restore("bar", System.currentTimeMillis() + 1000, serialized, RestoreParams.restoreParams().replace().absTtl());
    assertTrue(jedis.pttl("bar") <= 1000);

    jedis.restore("bar1", 1000, serialized, RestoreParams.restoreParams().replace().idleTime(1000));
    assertEquals(1000, jedis.objectIdletime("bar1").longValue());
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
    assertByteArrayListEquals(Collections.singletonList(new byte[]{98}), binaryResult.getResult());
    binaryResult = jedis.scan(SCAN_POINTER_START_BINARY, noParams, set);
    assertByteArrayListEquals(Collections.singletonList(new byte[]{100}), binaryResult.getResult());
    binaryResult = jedis.scan(SCAN_POINTER_START_BINARY, noParams, zset);
    assertByteArrayListEquals(Collections.singletonList(new byte[]{102}), binaryResult.getResult());
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
    assertEquals("OK", jedis.set("hello", "world", setParams().nx().ex(expireSeconds)));
    assertEquals("world", jedis.get("hello"));

    assertNull(jedis.set("hello", "bar", setParams().nx().ex(expireSeconds)));
    assertEquals("world", jedis.get("hello"));

    long ttl = jedis.ttl("hello");
    assertTrue(ttl > 0 && ttl <= expireSeconds);

    // binary
    byte[] bworld = { 0x77, 0x6F, 0x72, 0x6C, 0x64 };
    byte[] bhello = { 0x68, 0x65, 0x6C, 0x6C, 0x6F };

    assertEquals("OK", jedis.set(bworld, bhello, setParams().nx().ex(expireSeconds)));
    assertArrayEquals(bhello, jedis.get(bworld));

    assertNull(jedis.set(bworld, bbar, setParams().nx().ex(expireSeconds)));
    assertArrayEquals(bhello, jedis.get(bworld));

    long bttl = jedis.ttl(bworld);
    assertTrue(bttl > 0 && bttl <= expireSeconds);
  }

  @Test
  public void setGetOptionTest() {
    assertEquals("OK", jedis.set("hello", "world"));

    // GET old value
    assertEquals("world", jedis.set("hello", "jedis", setParams().get()));

    assertEquals("jedis", jedis.get("hello"));

    // GET null value
    assertNull(jedis.set("key", "value", setParams().get()));
  }

  @Test
  public void setGet() {
    assertEquals("OK", jedis.set("hello", "world"));

    // GET old value
    assertEquals("world", jedis.setGet("hello", "jedis", setParams()));

    assertEquals("jedis", jedis.get("hello"));

    // GET null value
    assertNull(jedis.setGet("key", "value", setParams()));
  }

  @Test
  public void sendCommandTest() {
    Object obj = jedis.sendCommand(SET, "x", "1");
    String returnValue = encode((byte[]) obj);
    assertEquals("OK", returnValue);
    obj = jedis.sendCommand(GET, "x");
    returnValue = encode((byte[]) obj);
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

    assertEquals("PONG", encode((byte[]) jedis.sendCommand(PING)));
  }

  @Test
  public void sendBlockingCommandTest() {
    assertNull(jedis.sendBlockingCommand(BLPOP, "foo", Long.toString(1L)));

    jedis.sendCommand(RPUSH, "foo", "bar");
    assertEquals(Arrays.asList("foo", "bar"),
      encodeObject(jedis.sendBlockingCommand(BLPOP, "foo", Long.toString(1L))));

    assertNull(jedis.sendBlockingCommand(BLPOP, "foo", Long.toString(1L)));
  }

  @Test
  public void encodeCompleteResponse() {
    HashMap<String, String> entry = new HashMap<>();
    entry.put("foo", "bar");
    StreamEntryID entryID = jedis.xadd("mystream", StreamEntryID.NEW_ENTRY, entry);
    jedis.xgroupCreate("mystream", "mygroup", null, false);

    Object obj = jedis.sendCommand(XINFO, "STREAM", "mystream");
    List encodeObj = (List) encodeObject(obj);

    assertTrue(encodeObj.size() >= 14);
    assertEquals(0, encodeObj.size() % 2); // must be even

    assertEquals(1L, findValueFromMapAsList(encodeObj, "length"));
    assertEquals(entryID.toString(), findValueFromMapAsList(encodeObj, "last-generated-id"));

    List<String> entryAsList = new ArrayList<>(2);
    entryAsList.add("foo");
    entryAsList.add("bar");

    assertEquals(entryAsList, ((List) findValueFromMapAsList(encodeObj, "first-entry")).get(1));
    assertEquals(entryAsList, ((List) findValueFromMapAsList(encodeObj, "last-entry")).get(1));

    assertEquals("PONG", encodeObject(jedis.sendCommand(PING)));

    entry.put("foo2", "bar2");
    jedis.hset("hash:test:encode", entry);
    encodeObj = (List) encodeObject(jedis.sendCommand(HGETALL, "hash:test:encode"));

    assertEquals(4, encodeObj.size());
    assertTrue(encodeObj.contains("foo"));
    assertTrue(encodeObj.contains("foo2"));
  }

  private Object findValueFromMapAsList(List list, Object key) {
    for (int i = 0; i < list.size(); i += 2) {
      if (key.equals(list.get(i))) {
        return list.get(i + 1);
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

    // replace
    jedis.set("foo1", "bar1");
    assertTrue(jedis.copy("foo1", "foo2", true));
    assertEquals("bar1", jedis.get("foo2"));

    // Binary
    assertFalse(jedis.copy(bfoobar, bfoo, false));

    jedis.set(bfoo1, bbar);
    assertTrue(jedis.copy(bfoo1, bfoo2, false));
    assertArrayEquals(bbar, jedis.get(bfoo2));

    // replace
    jedis.set(bfoo1, bbar1);
    assertTrue(jedis.copy(bfoo1, bfoo2, true));
    assertArrayEquals(bbar1, jedis.get(bfoo2));
  }
}
