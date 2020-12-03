package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static redis.clients.jedis.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.ScanParams.SCAN_POINTER_START_BINARY;
import static redis.clients.jedis.tests.utils.AssertUtil.assertByteArrayListEquals;
import static redis.clients.jedis.tests.utils.AssertUtil.assertByteArraySetEquals;
import static redis.clients.jedis.tests.utils.AssertUtil.assertCollectionContains;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

public class HashesCommandsTest extends JedisCommandTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };

  final byte[] bbar1 = { 0x05, 0x06, 0x07, 0x08, 0x0A };
  final byte[] bbar2 = { 0x05, 0x06, 0x07, 0x08, 0x0B };
  final byte[] bbar3 = { 0x05, 0x06, 0x07, 0x08, 0x0C };
  final byte[] bbarstar = { 0x05, 0x06, 0x07, 0x08, '*' };

  @Test
  public void hset() {
    long status = jedis.hset("foo", "bar", "car");
    assertEquals(1, status);
    status = jedis.hset("foo", "bar", "foo");
    assertEquals(0, status);

    // Binary
    long bstatus = jedis.hset(bfoo, bbar, bcar);
    assertEquals(1, bstatus);
    bstatus = jedis.hset(bfoo, bbar, bfoo);
    assertEquals(0, bstatus);

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
    long status = jedis.hsetnx("foo", "bar", "car");
    assertEquals(1, status);
    assertEquals("car", jedis.hget("foo", "bar"));

    status = jedis.hsetnx("foo", "bar", "foo");
    assertEquals(0, status);
    assertEquals("car", jedis.hget("foo", "bar"));

    status = jedis.hsetnx("foo", "car", "bar");
    assertEquals(1, status);
    assertEquals("bar", jedis.hget("foo", "car"));

    // Binary
    long bstatus = jedis.hsetnx(bfoo, bbar, bcar);
    assertEquals(1, bstatus);
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));

    bstatus = jedis.hsetnx(bfoo, bbar, bfoo);
    assertEquals(0, bstatus);
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));

    bstatus = jedis.hsetnx(bfoo, bcar, bbar);
    assertEquals(1, bstatus);
    assertArrayEquals(bbar, jedis.hget(bfoo, bcar));

  }

  @Test
  public void hmset() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    String status = jedis.hmset("foo", hash);
    assertEquals("OK", status);
    assertEquals("car", jedis.hget("foo", "bar"));
    assertEquals("bar", jedis.hget("foo", "car"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    String bstatus = jedis.hmset(bfoo, bhash);
    assertEquals("OK", bstatus);
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));
    assertArrayEquals(bbar, jedis.hget(bfoo, bcar));

  }

  @Test
  public void hsetVariadic() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    long status = jedis.hset("foo", hash);
    assertEquals(2, status);
    assertEquals("car", jedis.hget("foo", "bar"));
    assertEquals("bar", jedis.hget("foo", "car"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    status = jedis.hset(bfoo, bhash);
    assertEquals(2, status);
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
    long value = jedis.hincrBy("foo", "bar", 1);
    assertEquals(1, value);
    value = jedis.hincrBy("foo", "bar", -1);
    assertEquals(0, value);
    value = jedis.hincrBy("foo", "bar", -10);
    assertEquals(-10, value);

    // Binary
    long bvalue = jedis.hincrBy(bfoo, bbar, 1);
    assertEquals(1, bvalue);
    bvalue = jedis.hincrBy(bfoo, bbar, -1);
    assertEquals(0, bvalue);
    bvalue = jedis.hincrBy(bfoo, bbar, -10);
    assertEquals(-10, bvalue);

  }

  @Test
  public void hincrByFloat() {
    Double value = jedis.hincrByFloat("foo", "bar", 1.5d);
    assertEquals((Double) 1.5d, value);
    value = jedis.hincrByFloat("foo", "bar", -1.5d);
    assertEquals((Double) 0d, value);
    value = jedis.hincrByFloat("foo", "bar", -10.7d);
    assertEquals(Double.valueOf(-10.7d), value);

    // Binary
    double bvalue = jedis.hincrByFloat(bfoo, bbar, 1.5d);
    assertEquals(1.5d, bvalue, 0d);
    bvalue = jedis.hincrByFloat(bfoo, bbar, -1.5d);
    assertEquals(0d, bvalue, 0d);
    bvalue = jedis.hincrByFloat(bfoo, bbar, -10.7d);
    assertEquals(-10.7d, bvalue, 0d);

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

    assertEquals(0, jedis.hdel("bar", "foo").intValue());
    assertEquals(0, jedis.hdel("foo", "foo").intValue());
    assertEquals(1, jedis.hdel("foo", "bar").intValue());
    assertNull(jedis.hget("foo", "bar"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    assertEquals(0, jedis.hdel(bbar, bfoo).intValue());
    assertEquals(0, jedis.hdel(bfoo, bfoo).intValue());
    assertEquals(1, jedis.hdel(bfoo, bbar).intValue());
    assertNull(jedis.hget(bfoo, bbar));

  }

  @Test
  public void hlen() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    jedis.hmset("foo", hash);

    assertEquals(0, jedis.hlen("bar").intValue());
    assertEquals(2, jedis.hlen("foo").intValue());

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    jedis.hmset(bfoo, bhash);

    assertEquals(0, jedis.hlen(bbar).intValue());
    assertEquals(2, jedis.hlen(bfoo).intValue());

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
  public void hgetAllPipeline() {
    Map<byte[], byte[]> bh = new HashMap<byte[], byte[]>();
    bh.put(bbar, bcar);
    bh.put(bcar, bbar);
    jedis.hmset(bfoo, bh);
    Pipeline pipeline = jedis.pipelined();
    Response<Map<byte[], byte[]>> bhashResponse = pipeline.hgetAll(bfoo);
    pipeline.sync();
    Map<byte[], byte[]> bhash = bhashResponse.get();

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

}
