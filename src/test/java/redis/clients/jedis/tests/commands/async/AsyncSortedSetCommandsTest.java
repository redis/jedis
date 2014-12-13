package redis.clients.jedis.tests.commands.async;

import org.junit.Test;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.ZParams;
import redis.clients.jedis.tests.commands.async.util.CommandWithWaiting;
import redis.clients.util.SafeEncoder;

import java.util.LinkedHashSet;
import java.util.Set;

public class AsyncSortedSetCommandsTest extends AsyncJedisCommandTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bbar = { 0x05, 0x06, 0x07, 0x08 };
  final byte[] bcar = { 0x09, 0x0A, 0x0B, 0x0C };
  final byte[] ba = { 0x0A };
  final byte[] bb = { 0x0B };
  final byte[] bc = { 0x0C };

  @Test
  public void zadd() {
    asyncJedis.zadd(LONG_CALLBACK.withReset(), "foo", 1d, "a");
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.zadd(LONG_CALLBACK.withReset(), "foo", 10d, "b");
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.zadd(LONG_CALLBACK.withReset(), "foo", 0.1d, "c");
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.zadd(LONG_CALLBACK.withReset(), "foo", 2d, "a");
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    asyncJedis.zadd(LONG_CALLBACK.withReset(), bfoo, 1d, ba);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.zadd(LONG_CALLBACK.withReset(), bfoo, 10d, bb);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.zadd(LONG_CALLBACK.withReset(), bfoo, 0.1d, bc);
    assertEquals(new Long(1), LONG_CALLBACK.getResponseWithWaiting(1000));

    asyncJedis.zadd(LONG_CALLBACK.withReset(), bfoo, 2d, ba);
    assertEquals(new Long(0), LONG_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void zrange() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 10d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 0.1d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "a");

    Set<String> expected = new LinkedHashSet<String>();
    expected.add("c");
    expected.add("a");

    asyncJedis.zrange(STRING_SET_CALLBACK.withReset(), "foo", 0, 1);
    Set<String> range = STRING_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range);

    expected.add("b");

    asyncJedis.zrange(STRING_SET_CALLBACK.withReset(), "foo", 0, 100);
    range = STRING_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bc);
    bexpected.add(ba);

    asyncJedis.zrange(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, 0, 1);
    Set<byte[]> brange = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange);

    bexpected.add(bb);
    asyncJedis.zrange(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, 0, 100);
    brange = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange);
  }

  @Test
  public void zrevrange() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 10d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 0.1d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "a");

    Set<String> expected = new LinkedHashSet<String>();
    expected.add("b");
    expected.add("a");

    asyncJedis.zrevrange(STRING_SET_CALLBACK.withReset(), "foo", 0, 1);
    Set<String> range = STRING_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range);

    expected.add("c");

    asyncJedis.zrevrange(STRING_SET_CALLBACK.withReset(), "foo", 0, 100);
    range = STRING_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bb);
    bexpected.add(ba);

    asyncJedis.zrevrange(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, 0, 1);
    Set<byte[]> brange = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange);

    bexpected.add(bc);

    asyncJedis.zrevrange(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, 0, 100);
    brange = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange);
  }

  @Test
  public void zrem() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "b");

    asyncJedis.zrem(LONG_CALLBACK.withReset(), "foo", "a");
    long status = LONG_CALLBACK.getResponseWithWaiting(1000);

    Set<String> expected = new LinkedHashSet<String>();
    expected.add("b");

    assertEquals(1, status);
    assertEquals(expected, jedis.zrange("foo", 0, 100));

    asyncJedis.zrem(LONG_CALLBACK.withReset(), "foo", "bar");
    status = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(0, status);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, bb);

    asyncJedis.zrem(LONG_CALLBACK.withReset(), bfoo, ba);
    long bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);

    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bb);

    assertEquals(1, bstatus);
    assertEquals(bexpected, jedis.zrange(bfoo, 0, 100));

    asyncJedis.zrem(LONG_CALLBACK.withReset(), bfoo, bbar);
    bstatus = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, bstatus);
  }

  @Test
  public void zincrby() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "b");

    asyncJedis.zincrby(DOUBLE_CALLBACK.withReset(), "foo", 2d, "a");
    double score = DOUBLE_CALLBACK.getResponseWithWaiting(1000);

    Set<String> expected = new LinkedHashSet<String>();
    expected.add("a");
    expected.add("b");

    assertEquals(3d, score, 0);
    asyncJedis.zrange(STRING_SET_CALLBACK.withReset(), "foo", 0, 100);
    assertEquals(expected, STRING_SET_CALLBACK.getResponseWithWaiting(1000));

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, bb);

    asyncJedis.zincrby(DOUBLE_CALLBACK.withReset(), bfoo, 2d, ba);
    double bscore = DOUBLE_CALLBACK.getResponseWithWaiting(1000);

    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bb);
    bexpected.add(ba);

    assertEquals(3d, bscore, 0);
    asyncJedis.zrange(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, 0, 100);
    assertEquals(bexpected, BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000));
  }

  @Test
  public void zrank() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "b");

    asyncJedis.zrank(LONG_CALLBACK.withReset(), "foo", "a");
    Long rank = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(new Long(0), rank);

    asyncJedis.zrank(LONG_CALLBACK.withReset(), "foo", "b");
    rank = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(new Long(1), rank);

    asyncJedis.zrank(LONG_CALLBACK.withReset(), "car", "b");
    rank = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertNull(rank);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, bb);

    asyncJedis.zrank(LONG_CALLBACK.withReset(), bfoo, ba);
    Long brank = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(new Long(0), brank);

    asyncJedis.zrank(LONG_CALLBACK.withReset(), bfoo, bb);
    brank = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(new Long(1), brank);

    asyncJedis.zrank(LONG_CALLBACK.withReset(), bcar, bb);
    brank = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertNull(brank);
  }

  @Test
  public void zrevrank() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "b");

    asyncJedis.zrevrank(LONG_CALLBACK.withReset(), "foo", "a");
    long rank = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, rank);

    asyncJedis.zrevrank(LONG_CALLBACK.withReset(), "foo", "b");
    rank = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, rank);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, bb);

    asyncJedis.zrevrank(LONG_CALLBACK.withReset(), bfoo, ba);
    long brank = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(1, brank);

    asyncJedis.zrevrank(LONG_CALLBACK.withReset(), bfoo, bb);
    brank = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(0, brank);
  }

  @Test
  public void zrangeWithScores() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 10d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 0.1d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "a");

    Set<Tuple> expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("c", 0.1d));
    expected.add(new Tuple("a", 2d));

    asyncJedis.zrangeWithScores(TUPLE_SET_CALLBACK.withReset(), "foo", 0, 1);
    Set<Tuple> range = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range);

    expected.add(new Tuple("b", 10d));

    asyncJedis.zrangeWithScores(TUPLE_SET_CALLBACK.withReset(), "foo", 0, 100);
    range = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    Set<Tuple> bexpected = new LinkedHashSet<Tuple>();
    bexpected.add(new Tuple(bc, 0.1d));
    bexpected.add(new Tuple(ba, 2d));

    asyncJedis.zrangeWithScores(TUPLE_BINARY_SET_CALLBACK.withReset(), bfoo, 0, 1);
    Set<Tuple> brange = TUPLE_BINARY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange);

    bexpected.add(new Tuple(bb, 10d));

    asyncJedis.zrangeWithScores(TUPLE_BINARY_SET_CALLBACK.withReset(), bfoo, 0, 100);
    brange = TUPLE_BINARY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange);
  }

  @Test
  public void zrevrangeWithScores() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 10d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 0.1d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "a");

    Set<Tuple> expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("b", 10d));
    expected.add(new Tuple("a", 2d));

    asyncJedis.zrevrangeWithScores(TUPLE_SET_CALLBACK.withReset(), "foo", 0, 1);
    Set<Tuple> range = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range);

    expected.add(new Tuple("c", 0.1d));

    asyncJedis.zrevrangeWithScores(TUPLE_SET_CALLBACK.withReset(), "foo", 0, 100);
    range = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    Set<Tuple> bexpected = new LinkedHashSet<Tuple>();
    bexpected.add(new Tuple(bb, 10d));
    bexpected.add(new Tuple(ba, 2d));

    asyncJedis.zrevrangeWithScores(TUPLE_BINARY_SET_CALLBACK.withReset(), bfoo, 0, 1);
    Set<Tuple> brange = TUPLE_BINARY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange);

    bexpected.add(new Tuple(bc, 0.1d));
    asyncJedis.zrevrangeWithScores(TUPLE_BINARY_SET_CALLBACK.withReset(), bfoo, 0, 100);
    brange = TUPLE_BINARY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange);
  }

  @Test
  public void zcard() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 10d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 0.1d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "a");

    asyncJedis.zcard(LONG_CALLBACK.withReset(), "foo");
    long size = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(3, size);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    asyncJedis.zcard(LONG_CALLBACK.withReset(), bfoo);
    long bsize = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(3, bsize);
  }

  @Test
  public void zscore() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 10d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 0.1d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "a");

    asyncJedis.zscore(DOUBLE_CALLBACK.withReset(), "foo", "b");
    Double score = DOUBLE_CALLBACK.getResponseWithWaiting(1000);
    assertEquals((Double) 10d, score);

    asyncJedis.zscore(DOUBLE_CALLBACK.withReset(), "foo", "c");
    score = DOUBLE_CALLBACK.getResponseWithWaiting(1000);
    assertEquals((Double) 0.1d, score);

    asyncJedis.zscore(DOUBLE_CALLBACK.withReset(), "foo", "s");
    score = DOUBLE_CALLBACK.getResponseWithWaiting(1000);
    assertNull(score);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    asyncJedis.zscore(DOUBLE_CALLBACK.withReset(), bfoo, bb);
    score = DOUBLE_CALLBACK.getResponseWithWaiting(1000);
    assertEquals((Double) 10d, score);

    asyncJedis.zscore(DOUBLE_CALLBACK.withReset(), bfoo, bc);
    score = DOUBLE_CALLBACK.getResponseWithWaiting(1000);
    assertEquals((Double) 0.1d, score);

    asyncJedis.zscore(DOUBLE_CALLBACK.withReset(), bfoo, SafeEncoder.encode("s"));
    score = DOUBLE_CALLBACK.getResponseWithWaiting(1000);
    assertNull(score);
  }

  @Test
  public void zcount() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 10d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 0.1d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "a");

    asyncJedis.zcount(LONG_CALLBACK.withReset(), "foo", 0.01d, 2.1d);
    long result = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, result);

    asyncJedis.zcount(LONG_CALLBACK.withReset(), "foo", "(0.01", "+inf");
    result = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(3, result);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    asyncJedis.zcount(LONG_CALLBACK.withReset(), bfoo, 0.01d, 2.1d);
    long bresult = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(2, bresult);

    asyncJedis.zcount(LONG_CALLBACK.withReset(), bfoo, SafeEncoder.encode("(0.01"),
      SafeEncoder.encode("+inf"));
    bresult = LONG_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(3, bresult);
  }

  @Test
  public void zrangebyscore() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 10d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 0.1d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "a");

    asyncJedis.zrangeByScore(STRING_SET_CALLBACK.withReset(), "foo", 0d, 2d);
    Set<String> range = STRING_SET_CALLBACK.getResponseWithWaiting(2000);

    Set<String> expected = new LinkedHashSet<String>();
    expected.add("c");
    expected.add("a");

    assertEquals(expected, range);

    asyncJedis.zrangeByScore(STRING_SET_CALLBACK.withReset(), "foo", 0d, 2d, 0, 1);
    range = STRING_SET_CALLBACK.getResponseWithWaiting(1000);

    expected = new LinkedHashSet<String>();
    expected.add("c");

    assertEquals(expected, range);

    asyncJedis.zrangeByScore(STRING_SET_CALLBACK.withReset(), "foo", 0d, 2d, 1, 1);
    range = STRING_SET_CALLBACK.getResponseWithWaiting(1000);

    asyncJedis.zrangeByScore(STRING_SET_CALLBACK.withReset(), "foo", "-inf", "(2");
    Set<String> range2 = STRING_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(expected, range2);

    expected = new LinkedHashSet<String>();
    expected.add("a");

    assertEquals(expected, range);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    asyncJedis.zrangeByScore(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, 0d, 2d);
    Set<byte[]> brange = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(2000);

    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bc);
    bexpected.add(ba);

    assertEquals(bexpected, brange);

    asyncJedis.zrangeByScore(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, 0d, 2d, 0, 1);
    brange = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);

    bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bc);

    assertEquals(bexpected, brange);

    asyncJedis.zrangeByScore(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, 0d, 2d, 1, 1);
    brange = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);

    asyncJedis.zrangeByScore(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, SafeEncoder.encode("-inf"),
      SafeEncoder.encode("(2"));
    Set<byte[]> brange2 = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);
    assertEquals(bexpected, brange2);

    bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(ba);

    assertEquals(bexpected, brange);
  }

  @Test
  public void zrevrangebyscore() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1.0d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2.0d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 3.0d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 4.0d, "d");
    CommandWithWaiting.zadd(asyncJedis, "foo", 5.0d, "e");

    asyncJedis.zrevrangeByScore(STRING_SET_CALLBACK.withReset(), "foo", 3d,
      Double.NEGATIVE_INFINITY, 0, 1);
    Set<String> range = STRING_SET_CALLBACK.getResponseWithWaiting(2000);
    Set<String> expected = new LinkedHashSet<String>();
    expected.add("c");

    assertEquals(expected, range);

    asyncJedis.zrevrangeByScore(STRING_SET_CALLBACK.withReset(), "foo", 3.5d,
      Double.NEGATIVE_INFINITY, 0, 2);
    range = STRING_SET_CALLBACK.getResponseWithWaiting(1000);

    expected = new LinkedHashSet<String>();
    expected.add("c");
    expected.add("b");

    assertEquals(expected, range);

    asyncJedis.zrevrangeByScore(STRING_SET_CALLBACK.withReset(), "foo", 3.5d,
      Double.NEGATIVE_INFINITY, 1, 1);
    range = STRING_SET_CALLBACK.getResponseWithWaiting(1000);
    expected = new LinkedHashSet<String>();
    expected.add("b");

    assertEquals(expected, range);

    asyncJedis.zrevrangeByScore(STRING_SET_CALLBACK.withReset(), "foo", 4d, 2d);
    range = STRING_SET_CALLBACK.getResponseWithWaiting(1000);

    expected = new LinkedHashSet<String>();
    expected.add("d");
    expected.add("c");
    expected.add("b");

    assertEquals(expected, range);

    asyncJedis.zrevrangeByScore(STRING_SET_CALLBACK.withReset(), "foo", "+inf", "(4");
    range = STRING_SET_CALLBACK.getResponseWithWaiting(1000);

    expected = new LinkedHashSet<String>();
    expected.add("e");

    assertEquals(expected, range);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    asyncJedis.zrevrangeByScore(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, 2d, 0d);
    Set<byte[]> brange = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(2000);

    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bc);
    bexpected.add(ba);

    assertEquals(bexpected, brange);

    asyncJedis.zrevrangeByScore(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, 2d, 0d, 0, 1);
    brange = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);

    bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(ba);

    assertEquals(bexpected, brange);

    asyncJedis.zrevrangeByScore(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo,
      SafeEncoder.encode("+inf"), SafeEncoder.encode("(2"));
    Set<byte[]> brange2 = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);

    bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bb);

    assertEquals(bexpected, brange2);

    asyncJedis.zrevrangeByScore(BYTE_ARRAY_SET_CALLBACK.withReset(), bfoo, 2d, 0d, 1, 1);
    brange = BYTE_ARRAY_SET_CALLBACK.getResponseWithWaiting(1000);
    bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bc);

    assertEquals(bexpected, brange);
  }

  @Test
  public void zrangebyscoreWithScores() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 10d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 0.1d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "a");

    asyncJedis.zrangeByScoreWithScores(TUPLE_SET_CALLBACK.withReset(), "foo", 0d, 2d);
    Set<Tuple> range = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);

    Set<Tuple> expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("c", 0.1d));
    expected.add(new Tuple("a", 2d));

    assertEquals(expected, range);

    asyncJedis.zrangeByScoreWithScores(TUPLE_SET_CALLBACK.withReset(), "foo", 0d, 2d, 0, 1);
    range = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);

    expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("c", 0.1d));

    assertEquals(expected, range);

    asyncJedis.zrangeByScoreWithScores(TUPLE_SET_CALLBACK.withReset(), "foo", 0d, 2d, 1, 1);
    range = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);

    expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("a", 2d));

    assertEquals(expected, range);

    // Binary

    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    asyncJedis.zrangeByScoreWithScores(TUPLE_SET_CALLBACK.withReset(), bfoo, 0d, 2d);
    Set<Tuple> brange = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);

    Set<Tuple> bexpected = new LinkedHashSet<Tuple>();
    bexpected.add(new Tuple(bc, 0.1d));
    bexpected.add(new Tuple(ba, 2d));

    assertEquals(bexpected, brange);

    asyncJedis.zrangeByScoreWithScores(TUPLE_SET_CALLBACK.withReset(), bfoo, 0d, 2d, 0, 1);
    brange = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);

    bexpected = new LinkedHashSet<Tuple>();
    bexpected.add(new Tuple(bc, 0.1d));

    assertEquals(bexpected, brange);

    asyncJedis.zrangeByScoreWithScores(TUPLE_SET_CALLBACK.withReset(), bfoo, 0d, 2d, 1, 1);
    brange = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);

    bexpected = new LinkedHashSet<Tuple>();
    bexpected.add(new Tuple(ba, 2d));

    assertEquals(bexpected, brange);
  }

  @Test
  public void zrevrangebyscoreWithScores() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1.0d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2.0d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 3.0d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 4.0d, "d");
    CommandWithWaiting.zadd(asyncJedis, "foo", 5.0d, "e");

    asyncJedis.zrevrangeByScoreWithScores(TUPLE_SET_CALLBACK.withReset(), "foo", 3d,
      Double.NEGATIVE_INFINITY, 0, 1);
    Set<Tuple> range = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);
    Set<Tuple> expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("c", 3.0d));

    assertEquals(expected, range);

    asyncJedis.zrevrangeByScoreWithScores(TUPLE_SET_CALLBACK.withReset(), "foo", 3.5d,
      Double.NEGATIVE_INFINITY, 0, 2);
    range = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);
    expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("c", 3.0d));
    expected.add(new Tuple("b", 2.0d));

    assertEquals(expected, range);

    asyncJedis.zrevrangeByScoreWithScores(TUPLE_SET_CALLBACK.withReset(), "foo", 3.5d,
      Double.NEGATIVE_INFINITY, 1, 1);
    range = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);
    expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("b", 2.0d));

    assertEquals(expected, range);

    asyncJedis.zrevrangeByScoreWithScores(TUPLE_SET_CALLBACK.withReset(), "foo", 4d, 2d);
    range = TUPLE_SET_CALLBACK.getResponseWithWaiting(1000);
    expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("d", 4.0d));
    expected.add(new Tuple("c", 3.0d));
    expected.add(new Tuple("b", 2.0d));

    assertEquals(expected, range);

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    asyncJedis.zrevrangeByScoreWithScores(TUPLE_BINARY_SET_CALLBACK.withReset(), bfoo, 2d, 0d);
    Set<Tuple> brange = TUPLE_BINARY_SET_CALLBACK.getResponseWithWaiting(1000);

    Set<Tuple> bexpected = new LinkedHashSet<Tuple>();
    bexpected.add(new Tuple(bc, 0.1d));
    bexpected.add(new Tuple(ba, 2d));

    assertEquals(bexpected, brange);

    asyncJedis
        .zrevrangeByScoreWithScores(TUPLE_BINARY_SET_CALLBACK.withReset(), bfoo, 2d, 0d, 0, 1);
    brange = TUPLE_BINARY_SET_CALLBACK.getResponseWithWaiting(1000);

    bexpected = new LinkedHashSet<Tuple>();
    bexpected.add(new Tuple(ba, 2d));

    assertEquals(bexpected, brange);

    asyncJedis
        .zrevrangeByScoreWithScores(TUPLE_BINARY_SET_CALLBACK.withReset(), bfoo, 2d, 0d, 1, 1);
    brange = TUPLE_BINARY_SET_CALLBACK.getResponseWithWaiting(1000);

    bexpected = new LinkedHashSet<Tuple>();
    bexpected.add(new Tuple(bc, 0.1d));

    assertEquals(bexpected, brange);
  }

  @Test
  public void zremrangeByRank() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 10d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 0.1d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "a");

    asyncJedis.zremrangeByRank(LONG_CALLBACK.withReset(), "foo", 0, 0);
    long result = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(1, result);

    Set<String> expected = new LinkedHashSet<String>();
    expected.add("a");
    expected.add("b");

    // borrowed blocking API
    assertEquals(expected, jedis.zrange("foo", 0, 100));

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    asyncJedis.zremrangeByRank(LONG_CALLBACK.withReset(), bfoo, 0, 0);
    long bresult = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(1, bresult);

    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(ba);
    bexpected.add(bb);

    // borrowed blocking API
    assertEquals(bexpected, jedis.zrange(bfoo, 0, 100));
  }

  @Test
  public void zremrangeByScore() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1d, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 10d, "b");
    CommandWithWaiting.zadd(asyncJedis, "foo", 0.1d, "c");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2d, "a");

    asyncJedis.zremrangeByScore(LONG_CALLBACK.withReset(), "foo", 0, 2);
    long result = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(2, result);

    Set<String> expected = new LinkedHashSet<String>();
    expected.add("b");

    // borrowed blocking API
    assertEquals(expected, jedis.zrange("foo", 0, 100));

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1d, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 10d, bb);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 0.1d, bc);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2d, ba);

    asyncJedis.zremrangeByScore(LONG_CALLBACK.withReset(), bfoo, 0, 2);
    long bresult = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(2, bresult);

    Set<byte[]> bexpected = new LinkedHashSet<byte[]>();
    bexpected.add(bb);

    // borrowed blocking API
    assertEquals(bexpected, jedis.zrange(bfoo, 0, 100));
  }

  @Test
  public void zunionstore() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2, "b");
    CommandWithWaiting.zadd(asyncJedis, "bar", 2, "a");
    CommandWithWaiting.zadd(asyncJedis, "bar", 2, "b");

    asyncJedis.zunionstore(LONG_CALLBACK.withReset(), "dst", "foo", "bar");
    long result = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(2, result);

    Set<Tuple> expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("b", new Double(4)));
    expected.add(new Tuple("a", new Double(3)));

    // blocking API
    assertEquals(expected, jedis.zrangeWithScores("dst", 0, 100));

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2, bb);
    CommandWithWaiting.zadd(asyncJedis, bbar, 2, ba);
    CommandWithWaiting.zadd(asyncJedis, bbar, 2, bb);

    asyncJedis.zunionstore(LONG_CALLBACK.withReset(), SafeEncoder.encode("dst"), bfoo, bbar);
    long bresult = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(2, bresult);

    Set<Tuple> bexpected = new LinkedHashSet<Tuple>();
    bexpected.add(new Tuple(bb, new Double(4)));
    bexpected.add(new Tuple(ba, new Double(3)));

    // blocking API
    assertEquals(bexpected, jedis.zrangeWithScores(SafeEncoder.encode("dst"), 0, 100));
  }

  @Test
  public void zunionstoreParams() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2, "b");
    CommandWithWaiting.zadd(asyncJedis, "bar", 2, "a");
    CommandWithWaiting.zadd(asyncJedis, "bar", 2, "b");

    ZParams params = new ZParams();
    params.weights(2, 2);
    params.aggregate(ZParams.Aggregate.SUM);

    asyncJedis.zunionstore(LONG_CALLBACK.withReset(), "dst", params, "foo", "bar");
    long result = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(2, result);

    Set<Tuple> expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("b", new Double(8)));
    expected.add(new Tuple("a", new Double(6)));

    // blocking API
    assertEquals(expected, jedis.zrangeWithScores("dst", 0, 100));

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2, bb);
    CommandWithWaiting.zadd(asyncJedis, bbar, 2, ba);
    CommandWithWaiting.zadd(asyncJedis, bbar, 2, bb);

    ZParams bparams = new ZParams();
    bparams.weights(2, 2);
    bparams.aggregate(ZParams.Aggregate.SUM);

    asyncJedis.zunionstore(LONG_CALLBACK.withReset(), SafeEncoder.encode("dst"), bparams, bfoo,
      bbar);
    long bresult = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(2, bresult);

    Set<Tuple> bexpected = new LinkedHashSet<Tuple>();
    bexpected.add(new Tuple(bb, new Double(8)));
    bexpected.add(new Tuple(ba, new Double(6)));

    // blocking API
    assertEquals(bexpected, jedis.zrangeWithScores(SafeEncoder.encode("dst"), 0, 100));
  }

  @Test
  public void zinterstore() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2, "b");
    CommandWithWaiting.zadd(asyncJedis, "bar", 2, "a");

    asyncJedis.zinterstore(LONG_CALLBACK.withReset(), "dst", "foo", "bar");
    long result = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(1, result);

    Set<Tuple> expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("a", new Double(3)));

    // blocking API
    assertEquals(expected, jedis.zrangeWithScores("dst", 0, 100));

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2, bb);
    CommandWithWaiting.zadd(asyncJedis, bbar, 2, ba);

    asyncJedis.zinterstore(LONG_CALLBACK.withReset(), SafeEncoder.encode("dst"), bfoo, bbar);
    long bresult = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(1, bresult);

    Set<Tuple> bexpected = new LinkedHashSet<Tuple>();
    bexpected.add(new Tuple(ba, new Double(3)));

    // blocking API
    assertEquals(bexpected, jedis.zrangeWithScores(SafeEncoder.encode("dst"), 0, 100));
  }

  @Test
  public void zintertoreParams() {
    CommandWithWaiting.zadd(asyncJedis, "foo", 1, "a");
    CommandWithWaiting.zadd(asyncJedis, "foo", 2, "b");
    CommandWithWaiting.zadd(asyncJedis, "bar", 2, "a");

    ZParams params = new ZParams();
    params.weights(2, 2);
    params.aggregate(ZParams.Aggregate.SUM);

    asyncJedis.zinterstore(LONG_CALLBACK.withReset(), "dst", params, "foo", "bar");
    long result = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(1, result);

    Set<Tuple> expected = new LinkedHashSet<Tuple>();
    expected.add(new Tuple("a", new Double(6)));

    // blocking API
    assertEquals(expected, jedis.zrangeWithScores("dst", 0, 100));

    // Binary
    CommandWithWaiting.zadd(asyncJedis, bfoo, 1, ba);
    CommandWithWaiting.zadd(asyncJedis, bfoo, 2, bb);
    CommandWithWaiting.zadd(asyncJedis, bbar, 2, ba);

    ZParams bparams = new ZParams();
    bparams.weights(2, 2);
    bparams.aggregate(ZParams.Aggregate.SUM);

    asyncJedis.zinterstore(LONG_CALLBACK.withReset(), SafeEncoder.encode("dst"), bparams, bfoo,
      bbar);
    long bresult = LONG_CALLBACK.getResponseWithWaiting(1000);

    assertEquals(1, bresult);

    Set<Tuple> bexpected = new LinkedHashSet<Tuple>();
    bexpected.add(new Tuple(ba, new Double(6)));

    // blocking API
    assertEquals(bexpected, jedis.zrangeWithScores(SafeEncoder.encode("dst"), 0, 100));
  }
}