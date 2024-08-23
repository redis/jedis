package redis.clients.jedis.commands.unified.cluster;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static redis.clients.jedis.util.AssertUtil.assertByteArrayListEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.SortedSetCommandsTestBase;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZParams;
import redis.clients.jedis.params.ZRangeParams;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.KeyValue;

@RunWith(Parameterized.class)
public class ClusterSortedSetCommandsTest extends SortedSetCommandsTestBase {

  final byte[] bfoo = { 0x01, 0x02, 0x03, 0x04 };
  final byte[] bfoo_same_hashslot = { 0x01, 0x02, 0x03, 0x04, 0x03, 0x00, 0x03, 0x1b };
  final byte[] ba = { 0x0A };
  final byte[] bb = { 0x0B };
  final byte[] bc = { 0x0C };

  public ClusterSortedSetCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Before
  public void setUp() {
    jedis = ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @After
  public void tearDown() {
    jedis.close();
    ClusterCommandsTestHelper.clearClusterData();
  }

  @Test
  @Override
  public void zunion() {
    jedis.zadd("{:}foo", 1, "a");
    jedis.zadd("{:}foo", 2, "b");
    jedis.zadd("{:}bar", 2, "a");
    jedis.zadd("{:}bar", 2, "b");

    ZParams params = new ZParams();
    params.weights(2, 2.5);
    params.aggregate(ZParams.Aggregate.SUM);

    assertThat(jedis.zunion(params, "{:}foo", "{:}bar"),
        containsInAnyOrder("a", "b"));

    assertThat(jedis.zunionWithScores(params, "{:}foo", "{:}bar"),
        containsInAnyOrder(
            new Tuple("b", new Double(9)),
            new Tuple("a", new Double(7))
        ));
  }

  @Test
  @Override
  public void zunionstore() {
    jedis.zadd("{:}foo", 1, "a");
    jedis.zadd("{:}foo", 2, "b");
    jedis.zadd("{:}bar", 2, "a");
    jedis.zadd("{:}bar", 2, "b");

    assertEquals(2, jedis.zunionstore("{:}dst", "{:}foo", "{:}bar"));

    List<Tuple> expected = new ArrayList<>();
    expected.add(new Tuple("a", new Double(3)));
    expected.add(new Tuple("b", new Double(4)));
    assertEquals(expected, jedis.zrangeWithScores("{:}dst", 0, 100));
  }

  @Test
  @Override
  public void zunionstoreParams() {
    jedis.zadd("{:}foo", 1, "a");
    jedis.zadd("{:}foo", 2, "b");
    jedis.zadd("{:}bar", 2, "a");
    jedis.zadd("{:}bar", 2, "b");

    ZParams params = new ZParams();
    params.weights(2, 2.5);
    params.aggregate(ZParams.Aggregate.SUM);

    assertEquals(2, jedis.zunionstore("{:}dst", params, "{:}foo", "{:}bar"));

    List<Tuple> expected = new ArrayList<>();
    expected.add(new Tuple("a", new Double(7)));
    expected.add(new Tuple("b", new Double(9)));
    assertEquals(expected, jedis.zrangeWithScores("{:}dst", 0, 100));
  }

  @Test
  @Override
  public void zinter() {
    jedis.zadd("foo{:}", 1, "a");
    jedis.zadd("foo{:}", 2, "b");
    jedis.zadd("bar{:}", 2, "a");

    ZParams params = new ZParams();
    params.weights(2, 2.5);
    params.aggregate(ZParams.Aggregate.SUM);
    assertThat(jedis.zinter(params, "foo{:}", "bar{:}"),
        containsInAnyOrder("a"));

    assertThat(jedis.zinterWithScores(params, "foo{:}", "bar{:}"),
        containsInAnyOrder(new Tuple("a", new Double(7))));
  }

  @Test
  @Override
  public void zinterstore() {
    jedis.zadd("foo{:}", 1, "a");
    jedis.zadd("foo{:}", 2, "b");
    jedis.zadd("bar{:}", 2, "a");

    assertEquals(1, jedis.zinterstore("dst{:}", "foo{:}", "bar{:}"));

    assertEquals(Collections.singletonList(new Tuple("a", new Double(3))),
        jedis.zrangeWithScores("dst{:}", 0, 100));
  }

  @Test
  @Override
  public void zintertoreParams() {
    jedis.zadd("foo{:}", 1, "a");
    jedis.zadd("foo{:}", 2, "b");
    jedis.zadd("bar{:}", 2, "a");

    ZParams params = new ZParams();
    params.weights(2, 2.5);
    params.aggregate(ZParams.Aggregate.SUM);

    assertEquals(1, jedis.zinterstore("dst{:}", params, "foo{:}", "bar{:}"));

    assertEquals(Collections.singletonList(new Tuple("a", new Double(7))),
        jedis.zrangeWithScores("dst{:}", 0, 100));
  }

  @Test
  @Override
  public void bzpopmax() {
    assertNull(jedis.bzpopmax(1, "f{:}oo", "b{:}ar"));

    jedis.zadd("f{:}oo", 1d, "a", ZAddParams.zAddParams().nx());
    jedis.zadd("f{:}oo", 10d, "b", ZAddParams.zAddParams().nx());
    jedis.zadd("b{:}ar", 0.1d, "c", ZAddParams.zAddParams().nx());
    assertEquals(new KeyValue<>("f{:}oo", new Tuple("b", 10d)), jedis.bzpopmax(0, "f{:}oo", "b{:}ar"));
  }

  @Test
  @Override
  public void bzpopmin() {
    assertNull(jedis.bzpopmin(1, "ba{:}r", "fo{:}o"));

    jedis.zadd("fo{:}o", 1d, "a", ZAddParams.zAddParams().nx());
    jedis.zadd("fo{:}o", 10d, "b", ZAddParams.zAddParams().nx());
    jedis.zadd("ba{:}r", 0.1d, "c", ZAddParams.zAddParams().nx());
    assertEquals(new KeyValue<>("ba{:}r", new Tuple("c", 0.1d)), jedis.bzpopmin(0, "ba{:}r", "fo{:}o"));
  }

  @Test
  @Override
  public void zdiff() {
    jedis.zadd("{:}foo", 1.0, "a");
    jedis.zadd("{:}foo", 2.0, "b");
    jedis.zadd("{:}bar", 1.0, "a");

    assertEquals(0, jedis.zdiff("{bar}1", "{bar}2").size());

    assertThat(jedis.zdiff("{:}foo", "{:}bar"),
        containsInAnyOrder("b"));

    assertThat(jedis.zdiffWithScores("{:}foo", "{:}bar"),
        containsInAnyOrder(new Tuple("b", 2.0d)));
  }

  @Test
  @Override
  public void zdiffstore() {
    jedis.zadd("foo{:}", 1.0, "a");
    jedis.zadd("foo{:}", 2.0, "b");
    jedis.zadd("bar{:}", 1.0, "a");

    assertEquals(0, jedis.zdiffstore("{bar}3", "{bar}1", "{bar}2"));
    assertEquals(1, jedis.zdiffstore("bar{:}3", "foo{:}", "bar{:}"));
    assertEquals(Collections.singletonList("b"), jedis.zrange("bar{:}3", 0, -1));
  }

  @Test
  public void zrangestore() {
    jedis.zadd("foo{.}", 1, "aa");
    jedis.zadd("foo{.}", 2, "c");
    jedis.zadd("foo{.}", 3, "bb");

    long stored = jedis.zrangestore("bar{.}", "foo{.}", ZRangeParams.zrangeByScoreParams(1, 2));
    assertEquals(2, stored);

    List<String> range = jedis.zrange("bar{.}", 0, -1);
    List<String> expected = new ArrayList<>();
    expected.add("aa");
    expected.add("c");
    assertEquals(expected, range);

    // Binary
    jedis.zadd(bfoo, 1d, ba);
    jedis.zadd(bfoo, 10d, bb);
    jedis.zadd(bfoo, 0.1d, bc);
    jedis.zadd(bfoo, 2d, ba);

    long bstored = jedis.zrangestore(bfoo_same_hashslot, bfoo, ZRangeParams.zrangeParams(0, 1).rev());
    assertEquals(2, bstored);

    List<byte[]> brange = jedis.zrevrange(bfoo_same_hashslot, 0, 1);
    List<byte[]> bexpected = new ArrayList<>();
    bexpected.add(bb);
    bexpected.add(ba);
    assertByteArrayListEquals(bexpected, brange);
  }

  @Test
  public void zintercard() {
    jedis.zadd("foo{.}", 1, "a");
    jedis.zadd("foo{.}", 2, "b");
    jedis.zadd("bar{.}", 2, "a");
    jedis.zadd("bar{.}", 1, "b");

    assertEquals(2, jedis.zintercard("foo{.}", "bar{.}"));
    assertEquals(1, jedis.zintercard(1, "foo{.}", "bar{.}"));

    // Binary
    jedis.zadd(bfoo, 1, ba);
    jedis.zadd(bfoo, 2, bb);
    jedis.zadd(bfoo_same_hashslot, 2, ba);
    jedis.zadd(bfoo_same_hashslot, 2, bb);

    assertEquals(2, jedis.zintercard(bfoo, bfoo_same_hashslot));
    assertEquals(1, jedis.zintercard(1, bfoo, bfoo_same_hashslot));
  }

}
