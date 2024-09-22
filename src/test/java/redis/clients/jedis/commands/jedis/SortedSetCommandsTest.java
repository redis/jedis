package redis.clients.jedis.commands.jedis;

import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START;
import static redis.clients.jedis.params.ScanParams.SCAN_POINTER_START_BINARY;
import static redis.clients.jedis.util.AssertUtil.assertByteArrayListEquals;

import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.SortedSetOption;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.AssertUtil;
import redis.clients.jedis.util.KeyValue;
import redis.clients.jedis.util.SafeEncoder;

@RunWith(Parameterized.class)
public class SortedSetCommandsTest extends JedisCommandsTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };
  final byte[] ba = { 0x0A };
  final byte[] bb = { 0x0B };
  final byte[] bc = { 0x0C };
  final byte[] bInclusiveB = { 0x5B, 0x0B };
  final byte[] bExclusiveC = { 0x28, 0x0C };
  final byte[] bLexMinusInf = { 0x2D };
  final byte[] bLexPlusInf = { 0x2B };

  final byte[] bbar1 = { 0x05, 0x06, 0x07, 0x08, 0x0A };
  final byte[] bbar2 = { 0x05, 0x06, 0x07, 0x08, 0x0B };
  final byte[] bbar3 = { 0x05, 0x06, 0x07, 0x08, 0x0C };
  final byte[] bbarstar = { 0x05, 0x06, 0x07, 0x08, '*' };

  public SortedSetCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void zadd() {
    assertEquals(1, jedis.zadd("foo", 1d, "a"));

    assertEquals(1, jedis.zadd("foo", 10d, "b"));

    assertEquals(1, jedis.zadd("foo", 0.1d, "c"));

    assertEquals(0, jedis.zadd("foo", 2d, "a"));

    // Binary
    assertEquals(1, jedis.zadd(bfoo, 1d, ba));

    assertEquals(1, jedis.zadd(bfoo, 10d, bb));

    assertEquals(1, jedis.zadd(bfoo, 0.1d, bc));

    assertEquals(0, jedis.zadd(bfoo, 2d, ba));
  }

  @Test
  public void zaddWithParams() {
    jedis.del("foo");

    // xx: never add new member
    assertEquals(0L, jedis.zadd("foo", 1d, "a", ZAddParams.zAddParams().xx()));

    jedis.zadd("foo", 1d, "a");
    // nx: never update current member
    assertEquals(0L, jedis.zadd("foo", 2d, "a", ZAddParams.zAddParams().nx()));
    assertEquals(Double.valueOf(1d), jedis.zscore("foo", "a"));

    Map<String, Double> scoreMembers = new HashMap<String, Double>();
    scoreMembers.put("a", 2d);
    scoreMembers.put("b", 1d);
    // ch: return count of members not only added, but also updated
    assertEquals(2L, jedis.zadd("foo", scoreMembers, ZAddParams.zAddParams().ch()));

    // lt: only update existing elements if the new score is less than the current score.
    jedis.zadd("foo", 3d, "a", ZAddParams.zAddParams().lt());
    assertEquals(Double.valueOf(2d), jedis.zscore("foo", "a"));
    jedis.zadd("foo", 1d, "a", ZAddParams.zAddParams().lt());
    assertEquals(Double.valueOf(1d), jedis.zscore("foo", "a"));

    // gt: only update existing elements if the new score is greater than the current score.
    jedis.zadd("foo", 0d, "b", ZAddParams.zAddParams().gt());
    assertEquals(Double.valueOf(1d), jedis.zscore("foo", "b"));
    jedis.zadd("foo", 2d, "b", ZAddParams.zAddParams().gt());
    assertEquals(Double.valueOf(2d), jedis.zscore("foo", "b"));

    // incr: don't update already existing elements.
    assertNull(jedis.zaddIncr("foo", 1d, "b", ZAddParams.zAddParams().nx()));
    assertEquals(Double.valueOf(2d), jedis.zscore("foo", "b"));
    // incr: update elements that already exist.
    assertEquals(Double.valueOf(3d), jedis.zaddIncr("foo", 1d,"b", ZAddParams.zAddParams().xx()));
    assertEquals(Double.valueOf(3d), jedis.zscore("foo", "b"));

    // binary
    jedis.del(bfoo);

    // xx: never add new member
    assertEquals(0L, jedis.zadd(bfoo, 1d, ba, ZAddParams.zAddParams().xx()));

    jedis.zadd(bfoo, 1d, ba);
    // nx: never update current member
    assertEquals(0L, jedis.zadd(bfoo, 2d, ba, ZAddParams.zAddParams().nx()));
    assertEquals(Double.valueOf(1d), jedis.zscore(bfoo, ba));

    Map<byte[], Double> binaryScoreMembers = new HashMap<byte[], Double>();
    binaryScoreMembers.put(ba, 2d);
    binaryScoreMembers.put(bb, 1d);
    // ch: return count of members not only added, but also updated
    assertEquals(2L, jedis.zadd(bfoo, binaryScoreMembers, ZAddParams.zAddParams().ch()));

    // lt: only update existing elements if the new score is less than the current score.
    jedis.zadd(bfoo, 3d, ba, ZAddParams.zAddParams().lt());
    assertEquals(Double.valueOf(2d), jedis.zscore(bfoo, ba));
    jedis.zadd(bfoo, 1d, ba, ZAddParams.zAddParams().lt());
    assertEquals(Double.valueOf(1d), jedis.zscore(bfoo, ba));

    // gt: only update existing elements if the new score is greater than the current score.
    jedis.zadd(bfoo, 0d, bb, ZAddParams.zAddParams().gt());
    assertEquals(Double.valueOf(1d), jedis.zscore(bfoo, bb));
    jedis.zadd(bfoo, 2d, bb, ZAddParams.zAddParams().gt());
    assertEquals(Double.valueOf(2d), jedis.zscore(bfoo, bb));

    // incr: don't update already existing elements.
    assertNull(jedis.zaddIncr(bfoo, 1d, bb, ZAddParams.zAddParams().nx()));
    assertEquals(Double.valueOf(2d), jedis.zscore(bfoo, bb));
    // incr: update elements that already exist.
    assertEquals(Double.valueOf(3d), jedis.zaddIncr(bfoo, 1d, bb, ZAddParams.zAddParams().xx()));
    assertEquals(Double.valueOf(3d), jedis.zscore(bfoo, bb));
  }

  @Test
  public void zrange() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "a");

    List<String> expected = new ArrayList<String>();
    expected.add("c");
    expected.add("a");

    List<String> range = jedis.zrange("foo", 0, 1);
    assertEquals(expected, range);

    expected.add("b");
    range = jedis.zrange("foo", 0, 100);
    assertEquals(expected, range);

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bc);
    bexpected.add(ba);

    List<byte[]> brange = jedis.zrange(bfoo, 0, 1);
    assertByteArrayListEquals(bexpected, brange);

    bexpected.add(bb);
    brange = jedis.zrange(bfoo, 0, 100);
    assertByteArrayListEquals(bexpected, brange);
  }

  @Test
  public void zrangeByLex() {
    jedis.zadd("foo", 1, "aa");
    jedis.zadd("foo", 1, "c");
    jedis.zadd("foo", 1, "bb");
    jedis.zadd("foo", 1, "d");

    List<String> expected = new ArrayList<String>();
    expected.add("bb");
    expected.add("c");

    // exclusive aa ~ inclusive c
    assertEquals(expected, jedis.zrangeByLex("foo", "(aa", "[c"));

    expected.clear();
    expected.add("bb");
    expected.add("c");

    // with LIMIT
    assertEquals(expected, jedis.zrangeByLex("foo", "-", "+", 1, 2));
  }

  @Test
  public void zrangeByLexBinary() {
    // binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 1, bc);
    jedis.zadd(bfoo, 1, bb);

    List<byte[]> bExpected = new ArrayList<byte[]>();
    bExpected.add(bb);

    assertByteArrayListEquals(bExpected, jedis.zrangeByLex(bfoo, bInclusiveB, bExclusiveC));

    bExpected.clear();
    bExpected.add(ba);
    bExpected.add(bb);

    // with LIMIT
    assertByteArrayListEquals(bExpected, jedis.zrangeByLex(bfoo, bLexMinusInf, bLexPlusInf, 0, 2));
  }

  @Test
  public void zrevrangeByLex() {
    jedis.zadd("foo", 1, "aa");
    jedis.zadd("foo", 1, "c");
    jedis.zadd("foo", 1, "bb");
    jedis.zadd("foo", 1, "d");

    List<String> expected = new ArrayList<String>();
    expected.add("c");
    expected.add("bb");

    // exclusive aa ~ inclusive c
    assertEquals(expected, jedis.zrevrangeByLex("foo", "[c", "(aa"));

    expected.clear();
    expected.add("c");
    expected.add("bb");

    // with LIMIT
    assertEquals(expected, jedis.zrevrangeByLex("foo", "+", "-", 1, 2));
  }

  @Test
  public void zrevrangeByLexBinary() {
    // binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 1, bc);
    jedis.zadd(bfoo, 1, bb);

    List<byte[]> bExpected = new ArrayList<byte[]>();
    bExpected.add(bb);

    assertByteArrayListEquals(bExpected, jedis.zrevrangeByLex(bfoo, bExclusiveC, bInclusiveB));

    bExpected.clear();
    bExpected.add(bc);
    bExpected.add(bb);

    // with LIMIT
    assertByteArrayListEquals(bExpected, jedis.zrevrangeByLex(bfoo, bLexPlusInf, bLexMinusInf, 0, 2));
  }

  @Test
  public void zrevrange() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "a");

    List<String> expected = new ArrayList<String>();
    expected.add("b");
    expected.add("a");

    List<String> range = jedis.zrevrange("foo", 0, 1);
    assertEquals(expected, range);

    expected.add("c");
    range = jedis.zrevrange("foo", 0, 100);
    assertEquals(expected, range);

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bb);
    bexpected.add(ba);

    List<byte[]> brange = jedis.zrevrange(bfoo, 0, 1);
    assertByteArrayListEquals(bexpected, brange);

    bexpected.add(bc);
    brange = jedis.zrevrange(bfoo, 0, 100);
    assertByteArrayListEquals(bexpected, brange);
  }

  @Test
  public void zrem() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 2d, "b");

    assertEquals(1, jedis.zrem("foo", "a"));

    List<String> expected = new ArrayList<String>();
    expected.add("b");

    assertEquals(expected, jedis.zrange("foo", 0, 100));

    assertEquals(0, jedis.zrem("foo", "bar"));

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 2d, bb);

    assertEquals(1, jedis.zrem(bfoo, ba));

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bb);

    assertByteArrayListEquals(bexpected, jedis.zrange(bfoo, 0, 100));

    assertEquals(0, jedis.zrem(bfoo, bbar));
  }

  @Test
  public void zincrby() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 2d, "b");

    assertEquals(3d, jedis.zincrby("foo", 2d, "a"), 0);

    List<String> expected = new ArrayList<String>();
    expected.add("b");
    expected.add("a");

    assertEquals(expected, jedis.zrange("foo", 0, 100));

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 2d, bb);

    assertEquals(3d, jedis.zincrby(bfoo, 2d, ba), 0);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bb);
    bexpected.add(ba);

    assertByteArrayListEquals(bexpected, jedis.zrange(bfoo, 0, 100));
  }

  @Test
  public void zincrbyWithParams() {
    jedis.del("foo");

    // xx: never add new member
    assertNull(jedis.zincrby("foo", 2d, "a", ZIncrByParams.zIncrByParams().xx()));

    jedis.zadd("foo", 2d, "a");

    // nx: never update current member
    assertNull(jedis.zincrby("foo", 1d, "a", ZIncrByParams.zIncrByParams().nx()));
    assertEquals(Double.valueOf(2d), jedis.zscore("foo", "a"));

    // Binary

    jedis.del(bfoo);

    // xx: never add new member
    assertNull(jedis.zincrby(bfoo, 2d, ba, ZIncrByParams.zIncrByParams().xx()));

    jedis.zadd(bfoo, 2d, ba);

    // nx: never update current member
    assertNull(jedis.zincrby(bfoo, 1d, ba, ZIncrByParams.zIncrByParams().nx()));
    assertEquals(Double.valueOf(2d), jedis.zscore(bfoo, ba));
  }

  @Test
  public void zrank() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 2d, "b");

    long rank = jedis.zrank("foo", "a");
    assertEquals(0, rank);

    rank = jedis.zrank("foo", "b");
    assertEquals(1, rank);

    assertNull(jedis.zrank("car", "b"));

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 2d, bb);

    long brank = jedis.zrank(bfoo, ba);
    assertEquals(0, brank);

    brank = jedis.zrank(bfoo, bb);
    assertEquals(1, brank);

    assertNull(jedis.zrank(bcar, bb));
  }

  @Test
  public void zrankWithScore() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 2d, "b");

    KeyValue<Long, Double> keyValue = jedis.zrankWithScore("foo", "a");
    assertEquals(Long.valueOf(0), keyValue.getKey());
    assertEquals(Double.valueOf(1d), keyValue.getValue());

    keyValue = jedis.zrankWithScore("foo", "b");
    assertEquals(Long.valueOf(1), keyValue.getKey());
    assertEquals(Double.valueOf(2d), keyValue.getValue());

    assertNull(jedis.zrankWithScore("car", "b"));

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 2d, bb);

    keyValue = jedis.zrankWithScore(bfoo, ba);
    assertEquals(Long.valueOf(0), keyValue.getKey());
    assertEquals(Double.valueOf(1d), keyValue.getValue());

    keyValue = jedis.zrankWithScore(bfoo, bb);
    assertEquals(Long.valueOf(1), keyValue.getKey());
    assertEquals(Double.valueOf(2d), keyValue.getValue());

    assertNull(jedis.zrankWithScore(bcar, bb));
  }

  @Test
  public void zrevrank() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 2d, "b");

    long rank = jedis.zrevrank("foo", "a");
    assertEquals(1, rank);

    rank = jedis.zrevrank("foo", "b");
    assertEquals(0, rank);

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 2d, bb);

    long brank = jedis.zrevrank(bfoo, ba);
    assertEquals(1, brank);

    brank = jedis.zrevrank(bfoo, bb);
    assertEquals(0, brank);
  }

  @Test
  public void zrangeWithScores() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "a");

    List<Tuple> expected = new ArrayList<Tuple>();
    expected.add(new Tuple("c", 0.1d));
    expected.add(new Tuple("a", 2d));

    List<Tuple> range = jedis.zrangeWithScores("foo", 0, 1);
    assertEquals(expected, range);

    expected.add(new Tuple("b", 10d));
    range = jedis.zrangeWithScores("foo", 0, 100);
    assertEquals(expected, range);

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    List<Tuple> bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(bc, 0.1d));
    bexpected.add(new Tuple(ba, 2d));

    List<Tuple> brange = jedis.zrangeWithScores(bfoo, 0, 1);
    assertEquals(bexpected, brange);

    bexpected.add(new Tuple(bb, 10d));
    brange = jedis.zrangeWithScores(bfoo, 0, 100);
    assertEquals(bexpected, brange);

  }

  @Test
  public void zrevrangeWithScores() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "a");

    List<Tuple> expected = new ArrayList<Tuple>();
    expected.add(new Tuple("b", 10d));
    expected.add(new Tuple("a", 2d));

    List<Tuple> range = jedis.zrevrangeWithScores("foo", 0, 1);
    assertEquals(expected, range);

    expected.add(new Tuple("c", 0.1d));
    range = jedis.zrevrangeWithScores("foo", 0, 100);
    assertEquals(expected, range);

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    List<Tuple> bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(bb, 10d));
    bexpected.add(new Tuple(ba, 2d));

    List<Tuple> brange = jedis.zrevrangeWithScores(bfoo, 0, 1);
    assertEquals(bexpected, brange);

    bexpected.add(new Tuple(bc, 0.1d));
    brange = jedis.zrevrangeWithScores(bfoo, 0, 100);
    assertEquals(bexpected, brange);
  }

  @Test
  public void zcard() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "a");

    assertEquals(3, jedis.zcard("foo"));

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    assertEquals(3, jedis.zcard(bfoo));
  }

  @Test
  public void zscore() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "a");

    assertEquals((Double) 10d, jedis.zscore("foo", "b"));

    assertEquals((Double) 0.1d, jedis.zscore("foo", "c"));

    assertNull(jedis.zscore("foo", "s"));

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    assertEquals((Double) 10d, jedis.zscore(bfoo, bb));

    assertEquals((Double) 0.1d, jedis.zscore(bfoo, bc));

    assertNull(jedis.zscore(bfoo, SafeEncoder.encode("s")));

  }

  @Test
  public void zmscore() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "a");

    assertEquals(Arrays.asList(10d, 0.1d, null), jedis.zmscore("foo", "b", "c", "s"));

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    assertEquals(Arrays.asList(10d, 0.1d, null),
      jedis.zmscore(bfoo, bb, bc, SafeEncoder.encode("s")));
  }

  @Test
  public void zpopmax() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "d");

    Tuple actual = jedis.zpopmax("foo");
    Tuple expected = new Tuple("b", 10d);
    assertEquals(expected, actual);

    actual = jedis.zpopmax("foo");
    expected = new Tuple("d", 2d);
    assertEquals(expected, actual);

    actual = jedis.zpopmax("foo");
    expected = new Tuple("a", 1d);
    assertEquals(expected, actual);

    actual = jedis.zpopmax("foo");
    expected = new Tuple("c", 0.1d);
    assertEquals(expected, actual);

    // Empty
    actual = jedis.zpopmax("foo");
    assertNull(actual);

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    // First
    actual = jedis.zpopmax(bfoo);
    expected = new Tuple(bb, 10d);
    assertEquals(expected, actual);

    // Second
    actual = jedis.zpopmax(bfoo);
    expected = new Tuple(ba, 2d);
    assertEquals(expected, actual);

    // Third
    actual = jedis.zpopmax(bfoo);
    expected = new Tuple(bc, 0.1d);
    assertEquals(expected, actual);

    // Empty
    actual = jedis.zpopmax(bfoo);
    assertNull(actual);
  }

  @Test
  public void zpopmaxWithCount() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "d");
    jedis.zadd("foo", 0.03, "e");

    List<Tuple> actual = jedis.zpopmax("foo", 2);
    assertEquals(2, actual.size());

    List<Tuple> expected = new ArrayList<Tuple>();
    expected.add(new Tuple("b", 10d));
    expected.add(new Tuple("d", 2d));
    assertEquals(expected, actual);

    actual = jedis.zpopmax("foo", 3);
    assertEquals(3, actual.size());

    expected.clear();
    expected.add(new Tuple("a", 1d));
    expected.add(new Tuple("c", 0.1d));
    expected.add(new Tuple("e", 0.03d));
    assertEquals(expected, actual);

    // Empty
    actual = jedis.zpopmax("foo", 1);
    expected.clear();
    assertEquals(expected, actual);

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    // First
    actual = jedis.zpopmax(bfoo, 1);
    expected.clear();
    expected.add(new Tuple(bb, 10d));
    assertEquals(expected, actual);

    // Second
    actual = jedis.zpopmax(bfoo, 1);
    expected.clear();
    expected.add(new Tuple(ba, 2d));
    assertEquals(expected, actual);

    // Last 2 (just 1, because 1 was overwritten)
    actual = jedis.zpopmax(bfoo, 1);
    expected.clear();
    expected.add(new Tuple(bc, 0.1d));
    assertEquals(expected, actual);

    // Empty
    actual = jedis.zpopmax(bfoo, 1);
    expected.clear();
    assertEquals(expected, actual);
  }

  @Test
  public void zpopmin() {

    jedis.zadd("foo", 1d, "a", ZAddParams.zAddParams().nx());
    jedis.zadd("foo", 10d, "b", ZAddParams.zAddParams().nx());
    jedis.zadd("foo", 0.1d, "c", ZAddParams.zAddParams().nx());
    jedis.zadd("foo", 2d, "a", ZAddParams.zAddParams().nx());

    List<Tuple> range = jedis.zpopmin("foo", 2);

    List<Tuple> expected = new ArrayList<Tuple>();
    expected.add(new Tuple("c", 0.1d));
    expected.add(new Tuple("a", 1d));

    assertEquals(expected, range);

    assertEquals(new Tuple("b", 10d), jedis.zpopmin("foo"));

    // Binary

    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    List<Tuple> brange = jedis.zpopmin(bfoo, 2);

    List<Tuple> bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(bc, 0.1d));
    bexpected.add(new Tuple(ba, 2d));

    assertEquals(bexpected, brange);

    assertEquals(new Tuple(bb, 10d), jedis.zpopmin(bfoo));
  }

  @Test
  public void zcount() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "a");

    assertEquals(2, jedis.zcount("foo", 0.01d, 2.1d));

    assertEquals(3, jedis.zcount("foo", "(0.01", "+inf"));

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    assertEquals(2, jedis.zcount(bfoo, 0.01d, 2.1d));

    assertEquals(3, jedis.zcount(bfoo, SafeEncoder.encode("(0.01"), SafeEncoder.encode("+inf")));
  }

  @Test
  public void zlexcount() {
    jedis.zadd("foo", 1, "a");
    jedis.zadd("foo", 1, "b");
    jedis.zadd("foo", 1, "c");
    jedis.zadd("foo", 1, "aa");

    assertEquals(2, jedis.zlexcount("foo", "[aa", "(c"));

    assertEquals(4, jedis.zlexcount("foo", "-", "+"));

    assertEquals(3, jedis.zlexcount("foo", "-", "(c"));

    assertEquals(3, jedis.zlexcount("foo", "[aa", "+"));
  }

  @Test
  public void zlexcountBinary() {
    // Binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 1, bc);
    jedis.zadd(bfoo, 1, bb);

    assertEquals(1, jedis.zlexcount(bfoo, bInclusiveB, bExclusiveC));

    assertEquals(3, jedis.zlexcount(bfoo, bLexMinusInf, bLexPlusInf));
  }

  @Test
  public void zrangebyscore() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "a");

    List<String> range = jedis.zrangeByScore("foo", 0d, 2d);

    List<String> expected = new ArrayList<String>();
    expected.add("c");
    expected.add("a");

    assertEquals(expected, range);

    range = jedis.zrangeByScore("foo", 0d, 2d, 0, 1);

    expected = new ArrayList<String>();
    expected.add("c");

    assertEquals(expected, range);

    range = jedis.zrangeByScore("foo", 0d, 2d, 1, 1);
    List<String> range2 = jedis.zrangeByScore("foo", "-inf", "(2");
    assertEquals(expected, range2);

    expected = new ArrayList<String>();
    expected.add("a");

    assertEquals(expected, range);

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    List<byte[]> brange = jedis.zrangeByScore(bfoo, 0d, 2d);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bc);
    bexpected.add(ba);

    assertByteArrayListEquals(bexpected, brange);

    brange = jedis.zrangeByScore(bfoo, 0d, 2d, 0, 1);

    bexpected = new ArrayList<byte[]>();
    bexpected.add(bc);

    assertByteArrayListEquals(bexpected, brange);

    brange = jedis.zrangeByScore(bfoo, 0d, 2d, 1, 1);
    List<byte[]> brange2 = jedis.zrangeByScore(bfoo, SafeEncoder.encode("-inf"),
      SafeEncoder.encode("(2"));
    assertByteArrayListEquals(bexpected, brange2);

    bexpected = new ArrayList<byte[]>();
    bexpected.add(ba);

    assertByteArrayListEquals(bexpected, brange);

  }

  @Test
  public void zrevrangebyscore() {
    jedis.zadd("foo", 1.0d, "a");
    jedis.zadd("foo", 2.0d, "b");
    jedis.zadd("foo", 3.0d, "c");
    jedis.zadd("foo", 4.0d, "d");
    jedis.zadd("foo", 5.0d, "e");

    List<String> range = jedis.zrevrangeByScore("foo", 3d, Double.NEGATIVE_INFINITY, 0, 1);
    List<String> expected = new ArrayList<String>();
    expected.add("c");

    assertEquals(expected, range);

    range = jedis.zrevrangeByScore("foo", 3.5d, Double.NEGATIVE_INFINITY, 0, 2);
    expected = new ArrayList<String>();
    expected.add("c");
    expected.add("b");

    assertEquals(expected, range);

    range = jedis.zrevrangeByScore("foo", 3.5d, Double.NEGATIVE_INFINITY, 1, 1);
    expected = new ArrayList<String>();
    expected.add("b");

    assertEquals(expected, range);

    range = jedis.zrevrangeByScore("foo", 4d, 2d);
    expected = new ArrayList<String>();
    expected.add("d");
    expected.add("c");
    expected.add("b");

    assertEquals(expected, range);

    range = jedis.zrevrangeByScore("foo", "4", "2", 0, 2);
    expected = new ArrayList<String>();
    expected.add("d");
    expected.add("c");

    assertEquals(expected, range);

    range = jedis.zrevrangeByScore("foo", "+inf", "(4");
    expected = new ArrayList<String>();
    expected.add("e");

    assertEquals(expected, range);

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    List<byte[]> brange = jedis.zrevrangeByScore(bfoo, 2d, 0d);

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(ba);
    bexpected.add(bc);

    assertByteArrayListEquals(bexpected, brange);

    brange = jedis.zrevrangeByScore(bfoo, 2d, 0d, 0, 1);

    bexpected = new ArrayList<byte[]>();
    bexpected.add(ba);

    assertByteArrayListEquals(bexpected, brange);

    List<byte[]> brange2 = jedis.zrevrangeByScore(bfoo, SafeEncoder.encode("+inf"),
      SafeEncoder.encode("(2"));

    bexpected = new ArrayList<byte[]>();
    bexpected.add(bb);

    assertByteArrayListEquals(bexpected, brange2);

    brange = jedis.zrevrangeByScore(bfoo, 2d, 0d, 1, 1);
    bexpected = new ArrayList<byte[]>();
    bexpected.add(bc);

    assertByteArrayListEquals(bexpected, brange);
  }

  @Test
  public void zrangebyscoreWithScores() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "a");

    List<Tuple> range = jedis.zrangeByScoreWithScores("foo", 0d, 2d);

    List<Tuple> expected = new ArrayList<Tuple>();
    expected.add(new Tuple("c", 0.1d));
    expected.add(new Tuple("a", 2d));

    assertEquals(expected, range);

    range = jedis.zrangeByScoreWithScores("foo", 0d, 2d, 0, 1);

    expected = new ArrayList<Tuple>();
    expected.add(new Tuple("c", 0.1d));

    assertEquals(expected, range);

    range = jedis.zrangeByScoreWithScores("foo", 0d, 2d, 1, 1);

    expected = new ArrayList<Tuple>();
    expected.add(new Tuple("a", 2d));

    assertEquals(expected, range);

    // Binary

    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    List<Tuple> brange = jedis.zrangeByScoreWithScores(bfoo, 0d, 2d);

    List<Tuple> bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(bc, 0.1d));
    bexpected.add(new Tuple(ba, 2d));

    assertEquals(bexpected, brange);

    brange = jedis.zrangeByScoreWithScores(bfoo, 0d, 2d, 0, 1);

    bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(bc, 0.1d));

    assertEquals(bexpected, brange);

    brange = jedis.zrangeByScoreWithScores(bfoo, 0d, 2d, 1, 1);

    bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(ba, 2d));

    assertEquals(bexpected, brange);
  }

  @Test
  public void zrangeParams() {
    jedis.zadd("foo", 1, "aa");
    jedis.zadd("foo", 1, "c");
    jedis.zadd("foo", 1, "bb");
    jedis.zadd("foo", 1, "d");

    List<String> expected = new ArrayList<String>();
    expected.add("c");
    expected.add("bb");

    assertEquals(expected, jedis.zrange("foo", ZRangeParams.zrangeByLexParams("[c", "(aa").rev()));
    assertNotNull(jedis.zrangeWithScores("foo", ZRangeParams.zrangeByScoreParams(0, 1)));

    // Binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 1, bc);
    jedis.zadd(bfoo, 1, bb);

    List<byte[]> bExpected = new ArrayList<byte[]>();
    bExpected.add(bb);

    assertByteArrayListEquals(bExpected, jedis.zrange(bfoo, ZRangeParams.zrangeByLexParams(bExclusiveC, bInclusiveB).rev()));
    assertNotNull(jedis.zrangeWithScores(bfoo, ZRangeParams.zrangeByScoreParams(0, 1).limit(0, 3)));
  }

  @Test
  public void zrangestore() {
    jedis.zadd("foo", 1, "aa");
    jedis.zadd("foo", 2, "c");
    jedis.zadd("foo", 3, "bb");

    long stored = jedis.zrangestore("bar", "foo", ZRangeParams.zrangeByScoreParams(1, 2));
    assertEquals(2, stored);

    List<String> range = jedis.zrange("bar", 0, -1);
    List<String> expected = new ArrayList<>();
    expected.add("aa");
    expected.add("c");
    assertEquals(expected, range);

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    long bstored = jedis.zrangestore(bbar, bfoo, ZRangeParams.zrangeParams(0, 1).rev());
    assertEquals(2, bstored);

    List<byte[]> brange = jedis.zrevrange(bbar, 0, 1);
    List<byte[]> bexpected = new ArrayList<>();
    bexpected.add(bb);
    bexpected.add(ba);
    assertByteArrayListEquals(bexpected, brange);
  }

  @Test
  public void zrevrangebyscoreWithScores() {
    jedis.zadd("foo", 1.0d, "a");
    jedis.zadd("foo", 2.0d, "b");
    jedis.zadd("foo", 3.0d, "c");
    jedis.zadd("foo", 4.0d, "d");
    jedis.zadd("foo", 5.0d, "e");

    List<Tuple> range = jedis.zrevrangeByScoreWithScores("foo", 3d, Double.NEGATIVE_INFINITY, 0, 1);
    List<Tuple> expected = new ArrayList<Tuple>();
    expected.add(new Tuple("c", 3.0d));

    assertEquals(expected, range);

    range = jedis.zrevrangeByScoreWithScores("foo", 3.5d, Double.NEGATIVE_INFINITY, 0, 2);
    expected = new ArrayList<Tuple>();
    expected.add(new Tuple("c", 3.0d));
    expected.add(new Tuple("b", 2.0d));

    assertEquals(expected, range);

    range = jedis.zrevrangeByScoreWithScores("foo", 3.5d, Double.NEGATIVE_INFINITY, 1, 1);
    expected = new ArrayList<Tuple>();
    expected.add(new Tuple("b", 2.0d));

    assertEquals(expected, range);

    range = jedis.zrevrangeByScoreWithScores("foo", 4d, 2d);
    expected = new ArrayList<Tuple>();
    expected.add(new Tuple("d", 4.0d));
    expected.add(new Tuple("c", 3.0d));
    expected.add(new Tuple("b", 2.0d));

    assertEquals(expected, range);

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    List<Tuple> brange = jedis.zrevrangeByScoreWithScores(bfoo, 2d, 0d);

    List<Tuple> bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(ba, 2d));
    bexpected.add(new Tuple(bc, 0.1d));

    assertEquals(bexpected, brange);

    brange = jedis.zrevrangeByScoreWithScores(bfoo, 2d, 0d, 0, 1);

    bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(ba, 2d));

    assertEquals(bexpected, brange);

    brange = jedis.zrevrangeByScoreWithScores(bfoo, 2d, 0d, 1, 1);

    bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(bc, 0.1d));

    assertEquals(bexpected, brange);
  }

  @Test
  public void zremrangeByRank() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "a");

    assertEquals(1, jedis.zremrangeByRank("foo", 0, 0));

    List<String> expected = new ArrayList<String>();
    expected.add("a");
    expected.add("b");

    assertEquals(expected, jedis.zrange("foo", 0, 100));

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    assertEquals(1, jedis.zremrangeByRank(bfoo, 0, 0));

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(ba);
    bexpected.add(bb);

    assertByteArrayListEquals(bexpected, jedis.zrange(bfoo, 0, 100));

  }

  @Test
  public void zremrangeByScore() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 10d, "b");
    jedis.zadd("foo", 0.1d, "c");
    jedis.zadd("foo", 2d, "a");

    assertEquals(2, jedis.zremrangeByScore("foo", 0, 2));

    List<String> expected = new ArrayList<String>();
    expected.add("b");

    assertEquals(expected, jedis.zrange("foo", 0, 100));

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    assertEquals(2, jedis.zremrangeByScore(bfoo, 0, 2));

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(bb);

    assertByteArrayListEquals(bexpected, jedis.zrange(bfoo, 0, 100));
  }

  @Test
  public void zremrangeByScoreExclusive() {
    jedis.zadd("foo", 1d, "a");
    jedis.zadd("foo", 0d, "c");
    jedis.zadd("foo", 2d, "b");

    assertEquals(1, jedis.zremrangeByScore("foo", "(0", "(2"));

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 0d, bc);
    jedis.zadd(bfoo, 2d, bb);

    assertEquals(1, jedis.zremrangeByScore(bfoo, "(0".getBytes(), "(2".getBytes()));
  }

  @Test
  public void zremrangeByLex() {
    jedis.zadd("foo", 1, "a");
    jedis.zadd("foo", 1, "b");
    jedis.zadd("foo", 1, "c");
    jedis.zadd("foo", 1, "aa");

    assertEquals(2, jedis.zremrangeByLex("foo", "[aa", "(c"));

    List<String> expected = new ArrayList<String>();
    expected.add("a");
    expected.add("c");

    assertEquals(expected, jedis.zrangeByLex("foo", "-", "+"));
  }

  @Test
  public void zremrangeByLexBinary() {
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 1, bc);
    jedis.zadd(bfoo, 1, bb);

    assertEquals(1, jedis.zremrangeByLex(bfoo, bInclusiveB, bExclusiveC));

    List<byte[]> bexpected = new ArrayList<byte[]>();
    bexpected.add(ba);
    bexpected.add(bc);

    assertByteArrayListEquals(bexpected, jedis.zrangeByLex(bfoo, bLexMinusInf, bLexPlusInf));
  }

  @Test
  public void zunion() {
    jedis.zadd("foo", 1, "a");
    jedis.zadd("foo", 2, "b");
    jedis.zadd("bar", 2, "a");
    jedis.zadd("bar", 2, "b");

    ZParams params = new ZParams();
    params.weights(2, 2.5);
    params.aggregate(ZParams.Aggregate.SUM);

    List<String> expected = new ArrayList<>();
    expected.add("a");
    expected.add("b");
    assertEquals(expected, jedis.zunion(params, "foo", "bar"));

    List<Tuple> expectedTuple = new ArrayList<>();
    expectedTuple.add(new Tuple("a", new Double(7)));
    expectedTuple.add(new Tuple("b", new Double(9)));
    assertEquals(expectedTuple, jedis.zunionWithScores(params, "foo", "bar"));

    // Binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 2, bb);
    jedis.zadd(bbar, 2, ba);
    jedis.zadd(bbar, 2, bb);

    List<byte[]> bexpected = new ArrayList<>();
    bexpected.add(ba);
    bexpected.add(bb);
    AssertUtil.assertByteArrayListEquals(bexpected, jedis.zunion(params, bfoo, bbar));

    List<Tuple> bexpectedTuple = new ArrayList<>();
    bexpectedTuple.add(new Tuple(ba, new Double(7)));
    bexpectedTuple.add(new Tuple(bb, new Double(9)));
    assertEquals(bexpectedTuple, jedis.zunionWithScores(params, bfoo, bbar));
  }

  @Test
  public void zunionstore() {
    jedis.zadd("foo", 1, "a");
    jedis.zadd("foo", 2, "b");
    jedis.zadd("bar", 2, "a");
    jedis.zadd("bar", 2, "b");

    assertEquals(2, jedis.zunionstore("dst", "foo", "bar"));

    List<Tuple> expected = new ArrayList<Tuple>();
    expected.add(new Tuple("a", new Double(3)));
    expected.add(new Tuple("b", new Double(4)));

    assertEquals(expected, jedis.zrangeWithScores("dst", 0, 100));

    // Binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 2, bb);
    jedis.zadd(bbar, 2, ba);
    jedis.zadd(bbar, 2, bb);

    assertEquals(2, jedis.zunionstore(SafeEncoder.encode("dst"), bfoo, bbar));

    List<Tuple> bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(ba, new Double(3)));
    bexpected.add(new Tuple(bb, new Double(4)));

    assertEquals(bexpected, jedis.zrangeWithScores(SafeEncoder.encode("dst"), 0, 100));
  }

  @Test
  public void zunionstoreParams() {
    jedis.zadd("foo", 1, "a");
    jedis.zadd("foo", 2, "b");
    jedis.zadd("bar", 2, "a");
    jedis.zadd("bar", 2, "b");

    ZParams params = new ZParams();
    params.weights(2, 2.5);
    params.aggregate(ZParams.Aggregate.SUM);

    assertEquals(2, jedis.zunionstore("dst", params, "foo", "bar"));

    List<Tuple> expected = new ArrayList<Tuple>();
    expected.add(new Tuple("a", new Double(7)));
    expected.add(new Tuple("b", new Double(9)));

    assertEquals(expected, jedis.zrangeWithScores("dst", 0, 100));

    // Binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 2, bb);
    jedis.zadd(bbar, 2, ba);
    jedis.zadd(bbar, 2, bb);

    ZParams bparams = new ZParams();
    bparams.weights(2, 2.5);
    bparams.aggregate(ZParams.Aggregate.SUM);

    assertEquals(2, jedis.zunionstore(SafeEncoder.encode("dst"), bparams, bfoo, bbar));

    List<Tuple> bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(ba, new Double(7)));
    bexpected.add(new Tuple(bb, new Double(9)));

    assertEquals(bexpected, jedis.zrangeWithScores(SafeEncoder.encode("dst"), 0, 100));
  }

  @Test
  public void zinter() {
    jedis.zadd("foo", 1, "a");
    jedis.zadd("foo", 2, "b");
    jedis.zadd("bar", 2, "a");

    ZParams params = new ZParams();
    params.weights(2, 2.5);
    params.aggregate(ZParams.Aggregate.SUM);
    assertEquals(singletonList("a"), jedis.zinter(params, "foo", "bar"));

    assertEquals(singletonList(new Tuple("a", new Double(7))),
      jedis.zinterWithScores(params, "foo", "bar"));

    // Binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 2, bb);
    jedis.zadd(bbar, 2, ba);

    ZParams bparams = new ZParams();
    bparams.weights(2, 2.5);
    bparams.aggregate(ZParams.Aggregate.SUM);
    AssertUtil.assertByteArrayListEquals(singletonList(ba), jedis.zinter(params, bfoo, bbar));

    assertEquals(singletonList(new Tuple(ba, new Double(7))),
      jedis.zinterWithScores(bparams, bfoo, bbar));
  }

  @Test
  public void zinterstore() {
    jedis.zadd("foo", 1, "a");
    jedis.zadd("foo", 2, "b");
    jedis.zadd("bar", 2, "a");

    assertEquals(1, jedis.zinterstore("dst", "foo", "bar"));

    List<Tuple> expected = new ArrayList<Tuple>();
    expected.add(new Tuple("a", new Double(3)));

    assertEquals(expected, jedis.zrangeWithScores("dst", 0, 100));

    // Binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 2, bb);
    jedis.zadd(bbar, 2, ba);

    assertEquals(1, jedis.zinterstore(SafeEncoder.encode("dst"), bfoo, bbar));

    List<Tuple> bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(ba, new Double(3)));

    assertEquals(bexpected, jedis.zrangeWithScores(SafeEncoder.encode("dst"), 0, 100));
  }

  @Test
  public void zintertoreParams() {
    jedis.zadd("foo", 1, "a");
    jedis.zadd("foo", 2, "b");
    jedis.zadd("bar", 2, "a");

    ZParams params = new ZParams();
    params.weights(2, 2.5);
    params.aggregate(ZParams.Aggregate.SUM);

    assertEquals(1, jedis.zinterstore("dst", params, "foo", "bar"));

    List<Tuple> expected = new ArrayList<Tuple>();
    expected.add(new Tuple("a", new Double(7)));

    assertEquals(expected, jedis.zrangeWithScores("dst", 0, 100));

    // Binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 2, bb);
    jedis.zadd(bbar, 2, ba);

    ZParams bparams = new ZParams();
    bparams.weights(2, 2.5);
    bparams.aggregate(ZParams.Aggregate.SUM);

    assertEquals(1, jedis.zinterstore(SafeEncoder.encode("dst"), bparams, bfoo, bbar));

    List<Tuple> bexpected = new ArrayList<Tuple>();
    bexpected.add(new Tuple(ba, new Double(7)));

    assertEquals(bexpected, jedis.zrangeWithScores(SafeEncoder.encode("dst"), 0, 100));
  }

  @Test
  public void zintercard() {
    jedis.zadd("foo", 1, "a");
    jedis.zadd("foo", 2, "b");
    jedis.zadd("bar", 2, "a");
    jedis.zadd("bar", 1, "b");

    assertEquals(2, jedis.zintercard("foo", "bar"));
    assertEquals(1, jedis.zintercard(1, "foo", "bar"));

    // Binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 2, bb);
    jedis.zadd(bbar, 2, ba);
    jedis.zadd(bbar, 2, bb);

    assertEquals(2, jedis.zintercard(bfoo, bbar));
    assertEquals(1, jedis.zintercard(1, bfoo, bbar));
  }

  @Test
  public void zscan() {
    jedis.zadd("foo", 1, "a");
    jedis.zadd("foo", 2, "b");

    ScanResult<Tuple> result = jedis.zscan("foo", SCAN_POINTER_START);

    assertEquals(SCAN_POINTER_START, result.getCursor());
    assertFalse(result.getResult().isEmpty());

    // binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 1, bb);

    ScanResult<Tuple> bResult = jedis.zscan(bfoo, SCAN_POINTER_START_BINARY);

    assertArrayEquals(SCAN_POINTER_START_BINARY, bResult.getCursorAsBytes());
    assertFalse(bResult.getResult().isEmpty());
  }

  @Test
  public void zscanMatch() {
    ScanParams params = new ScanParams();
    params.match("a*");

    jedis.zadd("foo", 2, "b");
    jedis.zadd("foo", 1, "a");
    jedis.zadd("foo", 11, "aa");
    ScanResult<Tuple> result = jedis.zscan("foo", SCAN_POINTER_START, params);

    assertEquals(SCAN_POINTER_START, result.getCursor());
    assertFalse(result.getResult().isEmpty());

    // binary
    params = new ScanParams();
    params.match(bbarstar);

    jedis.zadd(bfoo, 2, bbar1);
    jedis.zadd(bfoo, 1, bbar2);
    jedis.zadd(bfoo, 11, bbar3);
    ScanResult<Tuple> bResult = jedis.zscan(bfoo, SCAN_POINTER_START_BINARY, params);

    assertArrayEquals(SCAN_POINTER_START_BINARY, bResult.getCursorAsBytes());
    assertFalse(bResult.getResult().isEmpty());

  }

  @Test
  public void zscanCount() {
    ScanParams params = new ScanParams();
    params.count(2);

    jedis.zadd("foo", 1, "a1");
    jedis.zadd("foo", 2, "a2");
    jedis.zadd("foo", 3, "a3");
    jedis.zadd("foo", 4, "a4");
    jedis.zadd("foo", 5, "a5");

    ScanResult<Tuple> result = jedis.zscan("foo", SCAN_POINTER_START, params);

    assertFalse(result.getResult().isEmpty());

    // binary
    params = new ScanParams();
    params.count(2);

    jedis.zadd(bfoo, 2, bbar1);
    jedis.zadd(bfoo, 1, bbar2);
    jedis.zadd(bfoo, 11, bbar3);

    ScanResult<Tuple> bResult = jedis.zscan(bfoo, SCAN_POINTER_START_BINARY, params);

    assertFalse(bResult.getResult().isEmpty());
  }

  @Test
  public void infinity() {
    jedis.zadd("key", Double.POSITIVE_INFINITY, "pos");
    assertEquals(Double.POSITIVE_INFINITY, jedis.zscore("key", "pos"), 0d);
    jedis.zadd("key", Double.NEGATIVE_INFINITY, "neg");
    assertEquals(Double.NEGATIVE_INFINITY, jedis.zscore("key", "neg"), 0d);
    jedis.zadd("key", 0d, "zero");

    List<Tuple> set = jedis.zrangeWithScores("key", 0, -1);
    Iterator<Tuple> itr = set.iterator();
    assertEquals(Double.NEGATIVE_INFINITY, itr.next().getScore(), 0d);
    assertEquals(0d, itr.next().getScore(), 0d);
    assertEquals(Double.POSITIVE_INFINITY, itr.next().getScore(), 0d);
  }

  @Test
  public void bzpopmax() {
    assertNull(jedis.bzpopmax(1, "foo", "bar"));

    jedis.zadd("foo", 1d, "a", ZAddParams.zAddParams().nx());
    jedis.zadd("foo", 10d, "b", ZAddParams.zAddParams().nx());
    jedis.zadd("bar", 0.1d, "c", ZAddParams.zAddParams().nx());
    assertEquals(new KeyValue<>("foo", new Tuple("b", 10d)), jedis.bzpopmax(0, "foo", "bar"));

    // Binary
    assertNull(jedis.bzpopmax(1, bfoo, bbar));

    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bbar, 0.1d, bc);
    KeyValue<byte[], Tuple> actual = jedis.bzpopmax(0, bfoo, bbar);
    assertArrayEquals(bfoo, actual.getKey());
    assertEquals(new Tuple(bb, 10d), actual.getValue());
  }

  @Test
  public void bzpopmin() {
    assertNull(jedis.bzpopmin(1, "bar", "foo"));

    jedis.zadd("foo", 1d, "a", ZAddParams.zAddParams().nx());
    jedis.zadd("foo", 10d, "b", ZAddParams.zAddParams().nx());
    jedis.zadd("bar", 0.1d, "c", ZAddParams.zAddParams().nx());
    assertEquals(new KeyValue<>("bar", new Tuple("c", 0.1)), jedis.bzpopmin(0, "bar", "foo"));

    // Binary
    assertNull(jedis.bzpopmin(1, bbar, bfoo));

    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bbar, 0.1d, bc);
    KeyValue<byte[], Tuple> actual = jedis.bzpopmin(0, bbar, bfoo);
    assertArrayEquals(bbar, (byte[]) actual.getKey());
    assertEquals(new Tuple(bc, 0.1), actual.getValue());
  }

  @Test
  public void zdiff() {
    jedis.zadd("foo", 1.0, "a");
    jedis.zadd("foo", 2.0, "b");
    jedis.zadd("bar", 1.0, "a");

    assertEquals(0, jedis.zdiff("bar1", "bar2").size());
    assertEquals(singletonList("b"), jedis.zdiff("foo", "bar"));
    assertEquals(singletonList(new Tuple("b", 2.0d)), jedis.zdiffWithScores("foo", "bar"));

    // binary

    jedis.zadd(bfoo, 1.0, ba);
    jedis.zadd(bfoo, 2.0, bb);
    jedis.zadd(bbar, 1.0, ba);

    assertEquals(0, jedis.zdiff(bbar1, bbar2).size());
    List<byte[]> bactual = jedis.zdiff(bfoo, bbar);
    assertEquals(1, bactual.size());
    assertArrayEquals(bb, bactual.iterator().next());
    assertEquals(singletonList(new Tuple(bb, 2.0d)), jedis.zdiffWithScores(bfoo, bbar));
  }

  @Test
  public void zdiffstore() {
    jedis.zadd("foo", 1.0, "a");
    jedis.zadd("foo", 2.0, "b");
    jedis.zadd("bar", 1.0, "a");

    assertEquals(0, jedis.zdiffstore("bar3", "bar1", "bar2"));
    assertEquals(1, jedis.zdiffstore("bar3", "foo", "bar"));
    assertEquals(singletonList("b"), jedis.zrange("bar3", 0, -1));

    // binary

    jedis.zadd(bfoo, 1.0, ba);
    jedis.zadd(bfoo, 2.0, bb);
    jedis.zadd(bbar, 1.0, ba);

    assertEquals(0, jedis.zdiffstore(bbar3, bbar1, bbar2));
    assertEquals(1, jedis.zdiffstore(bbar3, bfoo, bbar));
    List<byte[]> bactual = jedis.zrange(bbar3, 0, -1);
    assertArrayEquals(bb, bactual.iterator().next());
  }

  @Test
  public void zrandmember() {
    assertNull(jedis.zrandmember("foo"));
    assertEquals(Collections.emptyList(), jedis.zrandmember("foo", 1));
    assertEquals(Collections.emptyList(), jedis.zrandmemberWithScores("foo", 1));

    Map<String, Double> hash = new HashMap<>();
    hash.put("bar1", 1d);
    hash.put("bar2", 10d);
    hash.put("bar3", 0.1d);
    jedis.zadd("foo", hash);

    AssertUtil.assertCollectionContains(hash.keySet(), jedis.zrandmember("foo"));
    assertEquals(2, jedis.zrandmember("foo", 2).size());

    List<Tuple> actual = jedis.zrandmemberWithScores("foo", 2);
    assertEquals(2, actual.size());
    actual.forEach(t -> assertEquals(hash.get(t.getElement()), t.getScore(), 0d));

    // Binary
    assertNull(jedis.zrandmember(bfoo));
    assertEquals(Collections.emptyList(), jedis.zrandmember(bfoo, 1));
    assertEquals(Collections.emptyList(), jedis.zrandmemberWithScores(bfoo, 1));

    Map<byte[], Double> bhash = new HashMap<>();
    bhash.put(bbar1, 1d);
    bhash.put(bbar2, 10d);
    bhash.put(bbar3, 0.1d);
    jedis.zadd(bfoo, bhash);

    AssertUtil.assertByteArrayCollectionContains(bhash.keySet(), jedis.zrandmember(bfoo));
    assertEquals(2, jedis.zrandmember(bfoo, 2).size());

    List<Tuple> bactual = jedis.zrandmemberWithScores(bfoo, 2);
    assertEquals(2, bactual.size());
    bactual.forEach(t -> assertEquals(getScoreFromByteMap(bhash, t.getBinaryElement()), t.getScore(), 0d));
  }

  private Double getScoreFromByteMap(Map<byte[], Double> bhash, byte[] key) {
    for (Map.Entry<byte[], Double> en : bhash.entrySet()) {
      if (Arrays.equals(en.getKey(), key)) {
        return en.getValue();
      }
    }
    return null;
  }

  @Test
  public void zmpop() {
    jedis.zadd("foo", 1d, "a", ZAddParams.zAddParams().nx());
    jedis.zadd("foo", 10d, "b", ZAddParams.zAddParams().nx());
    jedis.zadd("foo", 0.1d, "c", ZAddParams.zAddParams().nx());
    jedis.zadd("foo", 2d, "a", ZAddParams.zAddParams().nx());

    KeyValue<String, List<Tuple>> single = jedis.zmpop(SortedSetOption.MAX, "foo");
    KeyValue<String, List<Tuple>> range = jedis.zmpop(SortedSetOption.MIN, 2, "foo");

    assertEquals(new Tuple("b", 10d), single.getValue().get(0));
    assertEquals(2, range.getValue().size());
    assertNull(jedis.zmpop(SortedSetOption.MAX, "foo"));
  }

  @Test
  public void bzmpopSimple() {
    jedis.zadd("foo", 1d, "a", ZAddParams.zAddParams().nx());
    jedis.zadd("foo", 10d, "b", ZAddParams.zAddParams().nx());
    jedis.zadd("foo", 0.1d, "c", ZAddParams.zAddParams().nx());
    jedis.zadd("foo", 2d, "a", ZAddParams.zAddParams().nx());

    KeyValue<String, List<Tuple>> single = jedis.bzmpop(1L, SortedSetOption.MAX, "foo");
    KeyValue<String, List<Tuple>> range = jedis.bzmpop(1L, SortedSetOption.MIN, 2, "foo");

    assertEquals(new Tuple("b", 10d), single.getValue().get(0));
    assertEquals(2, range.getValue().size());
    assertNull(jedis.bzmpop(1L, SortedSetOption.MAX, "foo"));
  }
}
