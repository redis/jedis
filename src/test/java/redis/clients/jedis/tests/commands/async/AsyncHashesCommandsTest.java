package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;

import java.util.*;

public class AsyncHashesCommandsTest extends AsyncJedisCommandTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };

  @Test
  public void hset() {
    asyncJedis.hset(LONG_CALLBACK.withReset(), "foo", "bar", "car");
    long status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, status);

    asyncJedis.hset(LONG_CALLBACK.withReset(), "foo", "bar", "foo");
    status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, status);

    // Binary
    asyncJedis.hset(LONG_CALLBACK.withReset(), bfoo, bbar, bcar);
    long bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, bstatus);

    asyncJedis.hset(LONG_CALLBACK.withReset(), bfoo, bbar, bfoo);
    bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, bstatus);
  }

  @Test
  public void hget() {
    CommandWithWaiting.hset(asyncJedis, "foo", "bar", "car");

    asyncJedis.hget(STRING_CALLBACK.withReset(), "bar", "foo");
    assertEquals(null, STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hget(STRING_CALLBACK.withReset(), "foo", "car");
    assertEquals(null, STRING_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hget(STRING_CALLBACK.withReset(), "foo", "bar");
    assertEquals("car", STRING_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.hset(asyncJedis, bfoo, bbar, bcar);

    asyncJedis.hget(BYTE_ARRAY_CALLBACK.withReset(), bbar, bfoo);
    assertEquals(null, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hget(BYTE_ARRAY_CALLBACK.withReset(), bfoo, bcar);
    assertEquals(null, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hget(BYTE_ARRAY_CALLBACK.withReset(), bfoo, bbar);
    assertArrayEquals(bcar, BYTE_ARRAY_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void hsetnx() {
    asyncJedis.hsetnx(LONG_CALLBACK.withReset(), "foo", "bar", "car");
    long status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, status);
    assertEquals("car", jedis.hget("foo", "bar"));

    asyncJedis.hsetnx(LONG_CALLBACK.withReset(), "foo", "bar", "foo");
    status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, status);
    assertEquals("car", jedis.hget("foo", "bar"));

    asyncJedis.hsetnx(LONG_CALLBACK.withReset(), "foo", "car", "bar");
    status = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, status);
    assertEquals("bar", jedis.hget("foo", "car"));

    // Binary
    asyncJedis.hsetnx(LONG_CALLBACK.withReset(), bfoo, bbar, bcar);
    long bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, bstatus);
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));

    asyncJedis.hsetnx(LONG_CALLBACK.withReset(), bfoo, bbar, bfoo);
    bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, bstatus);
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));

    asyncJedis.hsetnx(LONG_CALLBACK.withReset(), bfoo, bcar, bbar);
    bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, bstatus);
    assertArrayEquals(bbar, jedis.hget(bfoo, bcar));
  }

  @Test
  public void hmset() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");

    asyncJedis.hmset(STRING_CALLBACK.withReset(), "foo", hash);
    String status = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertEquals("OK", status);
    assertEquals("car", jedis.hget("foo", "bar"));
    assertEquals("bar", jedis.hget("foo", "car"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    asyncJedis.hmset(STRING_CALLBACK.withReset(), bfoo, bhash);
    String bstatus = STRING_CALLBACK.getResponseWithWaiting(1000);
    assertEquals("OK", bstatus);
    assertArrayEquals(bcar, jedis.hget(bfoo, bbar));
    assertArrayEquals(bbar, jedis.hget(bfoo, bcar));
  }

  @Test
  public void hmget() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    CommandWithWaiting.hmset(asyncJedis, "foo", hash);

    asyncJedis.hmget(STRING_LIST_CALLBACK.withReset(), "foo", "bar", "car", "foo");
    List<String> values = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);
    List<String> expected = new ArrayList<String>();
    expected.add("car");
    expected.add("bar");
    expected.add(null);

    assertEquals(expected, values);

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    CommandWithWaiting.hmset(asyncJedis, bfoo, bhash);

    asyncJedis.hmget(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo, bbar, bcar, bfoo);
    List<byte[]> bvalues = jedis.hmget(bfoo, bbar, bcar, bfoo);
    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bcar);
    bexpected.add(bbar);
    bexpected.add(null);

    assertEquals(bexpected, bvalues);
  }

  @Test
  public void hincrBy() {
    asyncJedis.hincrBy(LONG_CALLBACK.withReset(), "foo", "bar", 1);
    long value = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, value);

    asyncJedis.hincrBy(LONG_CALLBACK.withReset(), "foo", "bar", -1);
    value = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, value);

    asyncJedis.hincrBy(LONG_CALLBACK.withReset(), "foo", "bar", -10);
    value = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(-10, value);

    // Binary
    asyncJedis.hincrBy(LONG_CALLBACK.withReset(), bfoo, bbar, 1);
    long bvalue = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, bvalue);

    asyncJedis.hincrBy(LONG_CALLBACK.withReset(), bfoo, bbar, -1);
    bvalue = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, bvalue);

    asyncJedis.hincrBy(LONG_CALLBACK.withReset(), bfoo, bbar, -10);
    bvalue = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(-10, bvalue);
  }

  @Test
  public void hexists() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    CommandWithWaiting.hmset(asyncJedis, "foo", hash);

    asyncJedis.hexists(BOOLEAN_CALLBACK.withReset(), "bar", "foo");
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hexists(BOOLEAN_CALLBACK.withReset(), "foo", "foo");
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hexists(BOOLEAN_CALLBACK.withReset(), "foo", "bar");
    assertTrue(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    CommandWithWaiting.hmset(asyncJedis, bfoo, bhash);

    asyncJedis.hexists(BOOLEAN_CALLBACK.withReset(), bbar, bfoo);
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hexists(BOOLEAN_CALLBACK.withReset(), bfoo, bfoo);
    assertFalse(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hexists(BOOLEAN_CALLBACK.withReset(), bfoo, bbar);
    assertTrue(BOOLEAN_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void hdel() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    CommandWithWaiting.hmset(asyncJedis, "foo", hash);

    asyncJedis.hdel(LONG_CALLBACK.withReset(), "bar", "foo");
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hdel(LONG_CALLBACK.withReset(), "foo", "foo");
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hdel(LONG_CALLBACK.withReset(), "foo", "bar");
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    assertEquals(null, jedis.hget("foo", "bar"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    CommandWithWaiting.hmset(asyncJedis, bfoo, bhash);

    asyncJedis.hdel(LONG_CALLBACK.withReset(), bbar, bfoo);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hdel(LONG_CALLBACK.withReset(), bfoo, bfoo);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hdel(LONG_CALLBACK.withReset(), bfoo, bbar);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    assertEquals(null, jedis.hget(bfoo, bbar));
  }

  @Test
  public void hlen() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    CommandWithWaiting.hmset(asyncJedis, "foo", hash);

    asyncJedis.hlen(LONG_CALLBACK.withReset(), "bar");
    assertEquals(0, LONG_CALLBACK.getResponseWithWaiting(1000).intValue());

    asyncJedis.hlen(LONG_CALLBACK.withReset(), "foo");
    assertEquals(2, LONG_CALLBACK.getResponseWithWaiting(1000).intValue());

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    CommandWithWaiting.hmset(asyncJedis, bfoo, bhash);

    asyncJedis.hlen(LONG_CALLBACK.withReset(), bbar);
    assertEquals(0, LONG_CALLBACK.getResponseWithWaiting(1000).intValue());

    asyncJedis.hlen(LONG_CALLBACK.withReset(), bfoo);
    assertEquals(2, LONG_CALLBACK.getResponseWithWaiting(1000).intValue());
  }

  @Test
  public void hkeys() {
    Map<String, String> hash = new LinkedHashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    CommandWithWaiting.hmset(asyncJedis, "foo", hash);

    asyncJedis.hkeys(STRING_SET_CALLBACK.withReset(), "foo");
    Set<String> keys = STRING_SET_CALLBACK.getResponseWithWaiting(1000);
    Set<String> expected = new LinkedHashSet<String>();
    expected.add("bar");
    expected.add("car");
    assertEquals(expected, keys);

    // Binary
    Map<byte[], byte[]> bhash = new LinkedHashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    CommandWithWaiting.hmset(asyncJedis, bfoo, bhash);

    asyncJedis.hkeys(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo);
    Set<byte[]> bkeys = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);
    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bbar);
    bexpected.add(bcar);
    assertEquals(bexpected, bkeys);
  }

  @Test
  public void hvals() {
    Map<String, String> hash = new LinkedHashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    CommandWithWaiting.hmset(asyncJedis, "foo", hash);

    asyncJedis.hvals(STRING_LIST_CALLBACK.withReset(), "foo");
    List<String> vals = STRING_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, vals.size());
    assertTrue(vals.contains("bar"));
    assertTrue(vals.contains("car"));

    // Binary
    Map<byte[], byte[]> bhash = new LinkedHashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    CommandWithWaiting.hmset(asyncJedis, bfoo, bhash);

    asyncJedis.hvals(BYTE_ARRAY_LIST_CALLBACK.withReset(), bfoo);
    List<byte[]> bvals = BYTE_ARRAY_LIST_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, bvals.size());
    assertTrue(arrayContains(bvals, bbar));
    assertTrue(arrayContains(bvals, bcar));
  }

  @Test
  public void hgetAll() {
    Map<String, String> h = new HashMap<String, String>();
    h.put("bar", "car");
    h.put("car", "bar");
    CommandWithWaiting.hmset(asyncJedis, "foo", h);

    asyncJedis.hgetAll(STRING_MAP_CALLBACK.withReset(), "foo");
    Map<String, String> hash = STRING_MAP_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, hash.size());
    assertEquals("car", hash.get("bar"));
    assertEquals("bar", hash.get("car"));

    // Binary
    Map<byte[], byte[]> bh = new HashMap<byte[], byte[]>();
    bh.put(bbar, bcar);
    bh.put(bcar, bbar);
    CommandWithWaiting.hmset(asyncJedis, bfoo, bh);

    asyncJedis.hgetAll(BYTE_ARRAY_MAP_CALLBACK.withReset(), bfoo);
    Map<byte[], byte[]> bhash = BYTE_ARRAY_MAP_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(2, bhash.size());
    assertArrayEquals(bcar, bhash.get(bbar));
    assertArrayEquals(bbar, bhash.get(bcar));
  }
}
