package redis.clients.jedis.tests;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

import redis.clients.jedis.Tuple;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;

import static org.junit.Assert.assertEquals;

public class TupleSortedSetTest extends JedisCommandTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] ba = { 0x0A };
  final byte[] bb = { 0x0B };
  final byte[] bc = { 0x0C };
  final byte[] bd = { 0x0D };

  @Test
  public void testBinary() {
    SortedSet<Tuple> sortedSet = new TreeSet<Tuple>();

    jedis.zadd(bfoo, 0d, ba);
    sortedSet.add(new Tuple(ba, 0d));

    jedis.zadd(bfoo, 1d, bb);
    sortedSet.add(new Tuple(bb, 1d));

    Set<Tuple> zrange = jedis.zrangeWithScores(bfoo, 0, -1);
    assertEquals(sortedSet, zrange);

    jedis.zadd(bfoo, -0.3, bc);
    sortedSet.add(new Tuple(bc, -0.3));

    jedis.zadd(bfoo, 0.3, bd);
    sortedSet.add(new Tuple(bd, 0.3));

    zrange = jedis.zrangeWithScores(bfoo, 0, -1);
    assertEquals(sortedSet, zrange);
  }

  @Test
  public void testString() {
    SortedSet<Tuple> sortedSet = new TreeSet<Tuple>();

    jedis.zadd("foo", 0d, "a");
    sortedSet.add(new Tuple("a", 0d));

    jedis.zadd("foo", 1d, "b");
    sortedSet.add(new Tuple("b", 1d));

    Set<Tuple> range = jedis.zrangeWithScores("foo", 0, -1);
    assertEquals(sortedSet, range);

    jedis.zadd("foo", -0.3, "c");
    sortedSet.add(new Tuple("c", -0.3));

    jedis.zadd("foo", 0.3, "d");
    sortedSet.add(new Tuple("d", 0.3));

    range = jedis.zrangeWithScores("foo", 0, -1);
    assertEquals(sortedSet, range);
  }
}
