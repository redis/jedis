package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;

import java.util.*;

public class AsyncVariadicCommandsTest extends AsyncJedisCommandTestBase {
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

    CommandWithWaiting.hmset(asyncJedis, "foo", hash);

    asyncJedis.hdel(LONG_CALLBACK.withReset(), "bar", "foo", "foo1");
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hdel(LONG_CALLBACK.withReset(), "foo", "foo", "foo1");
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hdel(LONG_CALLBACK.withReset(), "foo", "bar", "foo2");
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    assertEquals(null, jedis.hget("foo", "bar"));

    // Binary
    Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>();
    bhash.put(bbar, bcar);
    bhash.put(bcar, bbar);
    bhash.put(bfoo2, bbar);

    CommandWithWaiting.hmset(asyncJedis, bfoo, bhash);

    asyncJedis.hdel(LONG_CALLBACK.withReset(), bbar, bfoo, bfoo1);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hdel(LONG_CALLBACK.withReset(), bfoo, bfoo, bfoo1);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.hdel(LONG_CALLBACK.withReset(), bfoo, bbar, bfoo2);
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    assertEquals(null, jedis.hget(bfoo, bbar));
  }

  @Test
  public void rpush() {

    asyncJedis.rpush(LONG_CALLBACK.withReset(), "foo", "bar", "foo");
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    List<String> expected = new ArrayList<String>();
    expected.add("bar");
    expected.add("foo");

    List<String> values = jedis.lrange("foo", 0, -1);
    assertEquals(expected, values);

    // Binary

    asyncJedis.rpush(LONG_CALLBACK.withReset(), bfoo, bbar, bfoo);
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bbar);
    bexpected.add(bfoo);

    List<byte[]> bvalues = jedis.lrange(bfoo, 0, -1);
    assertEquals(bexpected, bvalues);
  }

  @Test
  public void lpush() {

    asyncJedis.lpush(LONG_CALLBACK.withReset(), "foo", "bar", "foo");
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    List<String> expected = new ArrayList<String>();
    expected.add("foo");
    expected.add("bar");

    List<String> values = jedis.lrange("foo", 0, -1);
    assertEquals(expected, values);

    // Binary

    asyncJedis.lpush(LONG_CALLBACK.withReset(), bfoo, bbar, bfoo);
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bfoo);
    bexpected.add(bbar);

    List<byte[]> bvalues = jedis.lrange(bfoo, 0, -1);
    assertEquals(bexpected, bvalues);
  }

  @Test
  public void sadd() {

    asyncJedis.sadd(LONG_CALLBACK.withReset(), "foo", "bar", "foo1");
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.sadd(LONG_CALLBACK.withReset(), "foo", "bar", "car");
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.sadd(LONG_CALLBACK.withReset(), "foo", "bar", "foo1");
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    // binary

    asyncJedis.sadd(LONG_CALLBACK.withReset(), bfoo, bbar, bfoo1);
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.sadd(LONG_CALLBACK.withReset(), bfoo, bbar, bcar);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.sadd(LONG_CALLBACK.withReset(), bfoo, bbar, bfoo1);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void zadd() {
    Map<String, Double> scoreMembers = new HashMap<String, Double>();
    scoreMembers.put("bar", 1d);
    scoreMembers.put("foo", 10d);

    asyncJedis.zadd(LONG_CALLBACK.withReset(), "foo", scoreMembers);
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    scoreMembers.clear();
    scoreMembers.put("car", 0.1d);
    scoreMembers.put("bar", 2d);

    asyncJedis.zadd(LONG_CALLBACK.withReset(), "foo", scoreMembers);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    // binary
    Map<byte[], Double> bscoreMembers = new HashMap<byte[], Double>();
    bscoreMembers.put(bbar, 1d);
    bscoreMembers.put(bfoo, 10d);

    asyncJedis.zadd(LONG_CALLBACK.withReset(), bfoo, bscoreMembers);
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    bscoreMembers.clear();
    bscoreMembers.put(bcar, 0.1d);
    bscoreMembers.put(bbar, 2d);

    asyncJedis.zadd(LONG_CALLBACK.withReset(), bfoo, bscoreMembers);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void zrem() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "bar");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "car");
    CommandWithWaiting.zadd(asyncJedis, "foo", 3d, "foo1");

    asyncJedis.zrem(LONG_CALLBACK.withReset(), "foo", "bar", "car");
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    Set<String> expected = new LinkedHashSet<String>();
    expected.add("foo1");

    assertEquals(expected, jedis.zrange("foo", 0, 100));

    asyncJedis.zrem(LONG_CALLBACK.withReset(), "foo", "bar", "car");
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.zrem(LONG_CALLBACK.withReset(), "foo", "bar", "foo1");
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    // binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, bbar);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, bcar);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 3d, bfoo1);

    asyncJedis.zrem(LONG_CALLBACK.withReset(), bfoo, bbar, bcar);
    assertEquals(new Long(2), LONG_CALLBACK.getResponseWithWaiting(1000));

    // borrowed blocking API
    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bfoo1);

    assertEquals(bexpected, jedis.zrange(bfoo, 0, 100));

    asyncJedis.zrem(LONG_CALLBACK.withReset(), bfoo, bbar, bcar);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.zrem(LONG_CALLBACK.withReset(), bfoo, bbar, bfoo1);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));
  }
}