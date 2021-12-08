package redis.clients.jedis.commands.unified;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;
import static redis.clients.jedis.util.AssertUtil.assertByteArrayListEquals;
import static redis.clients.jedis.util.AssertUtil.assertByteArraySetEquals;
import static redis.clients.jedis.util.AssertUtil.assertCollectionContains;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.util.JedisByteHashMap;

public abstract class HashesCommandsTestBase extends UnifiedJedisCommandsTestBase {

  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };

  final byte[] bbar1 = { 0x05, 0x06, 0x07, 0x08, 0x0A };
  final byte[] bbar2 = { 0x05, 0x06, 0x07, 0x08, 0x0B };
  final byte[] bbar3 = { 0x05, 0x06, 0x07, 0x08, 0x0C };
  final byte[] bbarstar = { 0x05, 0x06, 0x07, 0x08, '*' };

  @Test
  public void hset() {
    assertEquals(1, jedis.hset("foo", "bar", "car"));
    assertEquals(0, jedis.hset("foo", "bar", "foo"));

    // Binary
    assertEquals(1, jedis.hset(bfoo, bbar, bcar));
    assertEquals(0, jedis.hset(bfoo, bbar, bfoo));
  }

  @Test
  public void hget() {
    jedis.hset("foo", "bar", "car");
    assertNull(jedis.hget("bar", "foo"));
    assertNull(jedis.hget("foo", "car"));
    assertEquals("car", jedis.hget("foo", "bar"));

    // Binary
    jedis.hset(bfoo, bbar, bcar);
    assertNull(jedis.hget(bbar, bfoo));
    assertNull(jedis.hget(bfoo, bcar));
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));
  }

  @Test
  public void hsetnx() {
    assertEquals(1, jedis.hsetnx("foo", "bar", "car"));
    assertEquals("car", jedis.hget("foo", "bar"));

    assertEquals(0, jedis.hsetnx("foo", "bar", "foo"));
    assertEquals("car", jedis.hget("foo", "bar"));

    assertEquals(1, jedis.hsetnx("foo", "car", "bar"));
    assertEquals("bar", jedis.hget("foo", "car"));

    // Binary
    assertEquals(1, jedis.hsetnx(bfoo, bbar, bcar));
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));

    assertEquals(0, jedis.hsetnx(bfoo, bbar, bfoo));
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));

    assertEquals(1, jedis.hsetnx(bfoo, bcar, bbar));
    assertArrayEquals(bbar, jedis.hget(bfoo, bcar));
  }

  @Test
  public void hmset() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    assertEquals("OK", jedis.hmset("foo", hash));
    assertEquals("car", jedis.hget("foo", "bar"));
    assertEquals("bar", jedis.hget("foo", "car"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    assertEquals("OK", jedis.hmset(bfoo, bhash));
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));
    assertArrayEquals(bbar, jedis.hget(bfoo, bcar));
  }

  @Test
  public void hsetVariadic() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    assertEquals(2, jedis.hset("foo", hash));
    assertEquals("car", jedis.hget("foo", "bar"));
    assertEquals("bar", jedis.hget("foo", "car"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    assertEquals(2, jedis.hset(bfoo, bhash));
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));
    assertArrayEquals(bbar, jedis.hget(bfoo, bcar));
  }

  @Test
  public void hmget() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    List<String> values = jedis.hmget("foo", "bar", "car", "foo");
    List<String> expected = new ArrayList<String>();
    expected.add("car");
    expected.add("bar");
    expected.add(null);

    assertEquals(expected, values);

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    List<byte[]> bvalues = jedis.hmget(bfoo, bbar, bcar, bfoo);
    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bcar);
    bexpected.add(bbar);
    bexpected.add(null);

    assertByteArrayListEquals(bexpected, bvalues);
  }

  @Test
  public void hincrBy() {
    assertEquals(1, jedis.hincrBy("foo", "bar", 1));
    assertEquals(0, jedis.hincrBy("foo", "bar", -1));
    assertEquals(-10, jedis.hincrBy("foo", "bar", -10));

    // Binary
    assertEquals(1, jedis.hincrBy(bfoo, bbar, 1));
    assertEquals(0, jedis.hincrBy(bfoo, bbar, -1));
    assertEquals(-10, jedis.hincrBy(bfoo, bbar, -10));
  }

  @Test
  public void hincrByFloat() {
    assertEquals(1.5d, jedis.hincrByFloat("foo", "bar", 1.5d), 0);
    assertEquals(0d, jedis.hincrByFloat("foo", "bar", -1.5d), 0);
    assertEquals(-10.7d, jedis.hincrByFloat("foo", "bar", -10.7d), 0);

    // Binary
    assertEquals(1.5d, jedis.hincrByFloat(bfoo, bbar, 1.5d), 0d);
    assertEquals(0d, jedis.hincrByFloat(bfoo, bbar, -1.5d), 0d);
    assertEquals(-10.7d, jedis.hincrByFloat(bfoo, bbar, -10.7d), 0d);
  }

  @Test
  public void hexists() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    assertFalse(jedis.hexists("bar", "foo"));
    assertFalse(jedis.hexists("foo", "foo"));
    assertTrue(jedis.hexists("foo", "bar"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    assertFalse(jedis.hexists(bbar, bfoo));
    assertFalse(jedis.hexists(bfoo, bfoo));
    assertTrue(jedis.hexists(bfoo, bbar));
  }

  @Test
  public void hdel() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    assertEquals(0, jedis.hdel("bar", "foo"));
    assertEquals(0, jedis.hdel("foo", "foo"));
    assertEquals(1, jedis.hdel("foo", "bar"));
    assertNull(jedis.hget("foo", "bar"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    assertEquals(0, jedis.hdel(bbar, bfoo));
    assertEquals(0, jedis.hdel(bfoo, bfoo));
    assertEquals(1, jedis.hdel(bfoo, bbar));
    assertNull(jedis.hget(bfoo, bbar));
  }

  @Test
  public void hlen() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    assertEquals(0, jedis.hlen("bar"));
    assertEquals(2, jedis.hlen("foo"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    assertEquals(0, jedis.hlen(bbar));
    assertEquals(2, jedis.hlen(bfoo));
  }

  @Test
  public void hkeys() {
    Map<String, String> hash = new LinkedHashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    Set<String> keys = jedis.hkeys("foo");
    Set<String> expected = new LinkedHashSet<String>();
    expected.add("bar");
    expected.add("car");
    assertEquals(expected, keys);

    // Binary
    Map<byte[], byte[]> bhash = new LinkedHashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    Set<byte[]> bkeys = jedis.hkeys(bfoo);
    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bbar);
    bexpected.add(bcar);
    assertByteArraySetEquals(bexpected, bkeys);
  }

  @Test
  public void hvals() {
    Map<String, String> hash = new LinkedHashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    List<String> vals = jedis.hvals("foo");
    assertEquals(2, vals.size());
    assertTrue(vals.contains("bar"));
    assertTrue(vals.contains("car"));

    // Binary
    Map<byte[], byte[]> bhash = new LinkedHashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    List<byte[]> bvals = jedis.hvals(bfoo);

    assertEquals(2, bvals.size());
    assertCollectionContains(bvals, bbar);
    assertCollectionContains(bvals, bcar);
  }

  @Test
  public void hgetAll() {
    Map<String, String> h = new HashMap<String, String>();
    h.put("bar", "car");
    h.put("car", "bar");
    jedis.hmset("foo", h);

    Map<String, String> hash = jedis.hgetAll("foo");
    assertEquals(2, hash.size());
    assertEquals("car", hash.get("bar"));
    assertEquals("bar", hash.get("car"));

    // Binary
    Map<byte[], byte[]> bh = new HashMap<byte[], byte[]>();
    bh.put(bbar, bcar);
    bh.put(bcar, bbar);
    jedis.hmset(bfoo, bh);
    Map<byte[], byte[]> bhash = jedis.hgetAll(bfoo);

    assertEquals(2, bhash.size());
    assertArrayEquals(bcar, bhash.get(bbar));
    assertArrayEquals(bbar, bhash.get(bcar));
  }

  @Test
  public void hscan() {
    jedis.hset("foo", "b", "b");
    jedis.hset("foo", "a", "a");

    ScanResult<Map.Entry<String, String>> result = jedis.hscan("foo", SCAN_POINTER_START);

    assertEquals(SCAN_POINTER_START, result.getCursor());
    assertFalse(result.getResult().isEmpty());

    // binary
    jedis.hset(bfoo, bbar, bcar);

    ScanResult<Map.Entry<byte[], byte[]>> bResult = jedis.hscan(bfoo, SCAN_POINTER_START_BINARY);

    assertArrayEquals(SCAN_POINTER_START_BINARY, bResult.getCursorAsBytes());
    assertFalse(bResult.getResult().isEmpty());
  }

  @Test
  public void hscanMatch() {
    ScanParams params = new ScanParams();
    params.match("a*");

    jedis.hset("foo", "b", "b");
    jedis.hset("foo", "a", "a");
    jedis.hset("foo", "aa", "aa");
    ScanResult<Map.Entry<String, String>> result = jedis.hscan("foo", SCAN_POINTER_START, params);

    assertEquals(SCAN_POINTER_START, result.getCursor());
    assertFalse(result.getResult().isEmpty());

    // binary
    params = new ScanParams();
    params.match(bbarstar);

    jedis.hset(bfoo, bbar, bcar);
    jedis.hset(bfoo, bbar1, bcar);
    jedis.hset(bfoo, bbar2, bcar);
    jedis.hset(bfoo, bbar3, bcar);

    ScanResult<Map.Entry<byte[], byte[]>> bResult = jedis.hscan(bfoo, SCAN_POINTER_START_BINARY,
      params);

    assertArrayEquals(SCAN_POINTER_START_BINARY, bResult.getCursorAsBytes());
    assertFalse(bResult.getResult().isEmpty());
  }

  @Test
  public void hscanCount() {
    ScanParams params = new ScanParams();
    params.count(2);

    for (int i = 0; i < 10; i++) {
      jedis.hset("foo", "a" + i, "a" + i);
    }

    ScanResult<Map.Entry<String, String>> result = jedis.hscan("foo", SCAN_POINTER_START, params);

    assertFalse(result.getResult().isEmpty());

    // binary
    params = new ScanParams();
    params.count(2);

    jedis.hset(bfoo, bbar, bcar);
    jedis.hset(bfoo, bbar1, bcar);
    jedis.hset(bfoo, bbar2, bcar);
    jedis.hset(bfoo, bbar3, bcar);

    ScanResult<Map.Entry<byte[], byte[]>> bResult = jedis.hscan(bfoo, SCAN_POINTER_START_BINARY,
      params);

    assertFalse(bResult.getResult().isEmpty());
  }

  @Test
  public void testHstrLen_EmptyHash() {
    Long response = jedis.hstrlen("myhash", "k1");
    assertEquals(0l, response.longValue());
  }

  @Test
  public void testHstrLen() {
    Map<String, String> values = new HashMap<>();
    values.put("key", "value");
    jedis.hmset("myhash", values);
    Long response = jedis.hstrlen("myhash", "key");
    assertEquals(5l, response.longValue());

  }

  @Test
  public void testBinaryHstrLen() {
    Map<byte[], byte[]> values = new HashMap<>();
    values.put(bbar, bcar);
    jedis.hmset(bfoo, values);
    Long response = jedis.hstrlen(bfoo, bbar);
    assertEquals(4l, response.longValue());
  }

  @Test
  public void hrandfield() {
    assertNull(jedis.hrandfield("foo"));
    assertEquals(Collections.emptyList(), jedis.hrandfield("foo", 1));
    assertEquals(Collections.emptyMap(), jedis.hrandfieldWithValues("foo", 1));

    Map<String, String> hash = new LinkedHashMap<>();
    hash.put("bar", "bar");
    hash.put("car", "car");
    hash.put("bar1", "bar1");

    jedis.hset("foo", hash);

    assertTrue(hash.containsKey(jedis.hrandfield("foo")));
    assertEquals(2, jedis.hrandfield("foo", 2).size());

    Map<String, String> actual = jedis.hrandfieldWithValues("foo", 2);
    assertNotNull(actual);
    assertEquals(2, actual.size());
    Map.Entry entry = actual.entrySet().iterator().next();
    assertEquals(hash.get(entry.getKey()), entry.getValue());

    // binary
    assertNull(jedis.hrandfield(bfoo));
    assertEquals(Collections.emptyList(), jedis.hrandfield(bfoo, 1));
    assertEquals(Collections.emptyMap(), jedis.hrandfieldWithValues(bfoo, 1));

    Map<byte[], byte[]> bhash = new JedisByteHashMap();
    bhash.put(bbar, bbar);
    bhash.put(bcar, bcar);
    bhash.put(bbar1, bbar1);

    jedis.hset(bfoo, bhash);

    assertTrue(bhash.containsKey(jedis.hrandfield(bfoo)));
    assertEquals(2, jedis.hrandfield(bfoo, 2).size());

    Map<byte[], byte[]> bactual = jedis.hrandfieldWithValues(bfoo, 2);
    assertNotNull(bactual);
    assertEquals(2, bactual.size());
    Map.Entry bentry = bactual.entrySet().iterator().next();
    assertArrayEquals(bhash.get(bentry.getKey()), (byte[]) bentry.getValue());
  }
}
