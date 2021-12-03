package redis.clients.jedis;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;

public class TupleSortedSetTest extends JedisCommandsTestBase {
  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] ba = { 0x0A };
  final byte[] bb = { 0x0B };
  final byte[] bc = { 0x0C };
  final byte[] bd = { 0x0D };
  final byte[] be = { 0x0E };
  final byte[] bf = { 0x0F };

  @Test
  public void testBinary() {
    List<Tuple> array = new ArrayList<Tuple>();

    jedis.zadd(bfoo, 0d, ba);
    array.add(new Tuple(ba, 0d));

    jedis.zadd(bfoo, 1d, bb);
    array.add(new Tuple(bb, 1d));

    List<Tuple> zrange = jedis.zrangeWithScores(bfoo, 0, -1);
    assertEquals(zrange, sorted(array));

    jedis.zadd(bfoo, -0.3, bc);
    array.add(new Tuple(bc, -0.3));

    jedis.zadd(bfoo, 0.3, bf);
    array.add(new Tuple(bf, 0.3));

    jedis.zadd(bfoo, 0.3, be);
    array.add(new Tuple(be, 0.3));

    jedis.zadd(bfoo, 0.3, bd);
    array.add(new Tuple(bd, 0.3));

    zrange = jedis.zrangeWithScores(bfoo, 0, -1);
    assertEquals(zrange, sorted(array));
  }

  @Test
  public void testString() {
    List<Tuple> array = new ArrayList<Tuple>();

    jedis.zadd("foo", 0d, "a");
    array.add(new Tuple("a", 0d));

    jedis.zadd("foo", 1d, "b");
    array.add(new Tuple("b", 1d));

    List<Tuple> range = jedis.zrangeWithScores("foo", 0, -1);
    assertEquals(range, sorted(array));

    jedis.zadd("foo", -0.3, "c");
    array.add(new Tuple("c", -0.3));

    jedis.zadd("foo", 0.3, "f");
    array.add(new Tuple("f", 0.3));

    jedis.zadd("foo", 0.3, "e");
    array.add(new Tuple("e", 0.3));

    jedis.zadd("foo", 0.3, "d");
    array.add(new Tuple("d", 0.3));

    range = jedis.zrangeWithScores("foo", 0, -1);
    assertEquals(range, sorted(array));
  }

  private List<Tuple> sorted(List<Tuple> list) {
    List<Tuple> sort = new ArrayList<>(list);
    Collections.sort(sort);
    return sort;
  }
}
