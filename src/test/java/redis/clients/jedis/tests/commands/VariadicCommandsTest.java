package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static redis.clients.jedis.tests.utils.AssertUtil.assertByteArrayListEquals;
import static redis.clients.jedis.tests.utils.AssertUtil.assertByteArraySetEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class VariadicCommandsTest extends JedisCommandTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };
  final byte[] bfoo1 = { 0x01, 0x02, 0x03, 0x04, 0x0A };
  final byte[] bfoo2 = { 0x01, 0x02, 0x03, 0x04, 0x0B };

  @Test
  public void hdel() {
    Map<String, String> hash = new HashMap<String, String>();
    hash.put("bar", "car");
    hash.put("car", "bar");
    hash.put("foo2", "bar");
    jedis.hmset("foo", hash);

    assertEquals(0, jedis.hdel("bar", "foo", "foo1").intValue());
    assertEquals(0, jedis.hdel("foo", "foo", "foo1").intValue());
    assertEquals(2, jedis.hdel("foo", "bar", "foo2").intValue());
    assertNull(jedis.hget("foo", "bar"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    bhash.put(bfoo2, bbar);
    jedis.hmset(bfoo, bhash);

    assertEquals(0, jedis.hdel(bbar, bfoo, bfoo1).intValue());
    assertEquals(0, jedis.hdel(bfoo, bfoo, bfoo1).intValue());
    assertEquals(2, jedis.hdel(bfoo, bbar, bfoo2).intValue());
    assertNull(jedis.hget(bfoo, bbar));

  }

  @Test
  public void rpush() {
    long size = jedis.rpush("foo", "bar", "foo");
    assertEquals(2, size);

    List<String> expected = new ArrayList<String>();
    expected.add("bar");
    expected.add("foo");

    List<String> values = jedis.lrange("foo", 0, -1);
    assertEquals(expected, values);

    // Binary
    size = jedis.rpush(bfoo, bbar, bfoo);
    assertEquals(2, size);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bbar);
    bexpected.add(bfoo);

    List<byte[]> bvalues = jedis.lrange(bfoo, 0, -1);
    assertByteArrayListEquals(bexpected, bvalues);

  }

  @Test
  public void lpush() {
    long size = jedis.lpush("foo", "bar", "foo");
    assertEquals(2, size);

    List<String> expected = new ArrayList<String>();
    expected.add("foo");
    expected.add("bar");

    List<String> values = jedis.lrange("foo", 0, -1);
    assertEquals(expected, values);

    // Binary
    size = jedis.lpush(bfoo, bbar, bfoo);
    assertEquals(2, size);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bfoo);
    bexpected.add(bbar);

    List<byte[]> bvalues = jedis.lrange(bfoo, 0, -1);
    assertByteArrayListEquals(bexpected, bvalues);

  }

  @Test
  public void sadd() {
    long status = jedis.sadd("foo", "bar", "foo1");
    assertEquals(2, status);

    status = jedis.sadd("foo", "bar", "car");
    assertEquals(1, status);

    status = jedis.sadd("foo", "bar", "foo1");
    assertEquals(0, status);

    status = jedis.sadd(bfoo, bbar, bfoo1);
    assertEquals(2, status);

    status = jedis.sadd(bfoo, bbar, bcar);
    assertEquals(1, status);

    status = jedis.sadd(bfoo, bbar, bfoo1);
    assertEquals(0, status);

  }

  @Test
  public void zadd() {
    Map<String, Double> scoreMembers = new HashMap<String, Double>();
    scoreMembers.put("bar", 1d);
    scoreMembers.put("foo", 10d);

    long status = jedis.zadd("foo", scoreMembers);
    assertEquals(2, status);

    scoreMembers.clear();
    scoreMembers.put("car", 0.1d);
    scoreMembers.put("bar", 2d);

    status = jedis.zadd("foo", scoreMembers);
    assertEquals(1, status);

    Map<byte[], Double> bscoreMembers = new HashMap<byte[], Double>();
    bscoreMembers.put(bbar, 1d);
    bscoreMembers.put(bfoo, 10d);

    status = jedis.zadd(bfoo, bscoreMembers);
    assertEquals(2, status);

    bscoreMembers.clear();
    bscoreMembers.put(bcar, 0.1d);
    bscoreMembers.put(bbar, 2d);

    status = jedis.zadd(bfoo, bscoreMembers);
    assertEquals(1, status);

  }

  @Test
  public void zrem() {
    jedis.zadd("foo", 1d, "bar");
    jedis.zadd("foo", 2d, "car");
    jedis.zadd("foo", 3d, "foo1");

    long status = jedis.zrem("foo", "bar", "car");

    Set<String> expected = new LinkedHashSet<String>();
    expected.add("foo1");

    assertEquals(2, status);
    assertEquals(expected, jedis.zrange("foo", 0, 100));

    status = jedis.zrem("foo", "bar", "car");
    assertEquals(0, status);

    status = jedis.zrem("foo", "bar", "foo1");
    assertEquals(1, status);

    // Binary
    jedis.zadd(bfoo, 1d, bbar);
    jedis.zadd(bfoo, 2d, bcar);
    jedis.zadd(bfoo, 3d, bfoo1);

    status = jedis.zrem(bfoo, bbar, bcar);

    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bfoo);

    assertEquals(2, status);
    assertByteArraySetEquals(bexpected, jedis.zrange(bfoo, 0, 100));

    status = jedis.zrem(bfoo, bbar, bcar);
    assertEquals(0, status);

    status = jedis.zrem(bfoo, bbar, bfoo1);
    assertEquals(1, status);

  }
}