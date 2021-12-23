//package redis.clients.jedis.commands.unified.cluster;
//
//import static org.junit.Assert.assertEquals;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Set;
//
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import redis.clients.jedis.params.ZAddParams;
//import redis.clients.jedis.params.ZParams;
//import redis.clients.jedis.resps.KeyedZSetElement;
//import redis.clients.jedis.resps.Tuple;
//import redis.clients.jedis.commands.unified.SortedSetCommandsTestBase;
//
//public class ClusterSortedSetCommandsTest extends SortedSetCommandsTestBase {
//
//  @BeforeClass
//  public static void prepare() throws InterruptedException {
//    jedis = ClusterCommandsTestHelper.initAndGetCluster();
//  }
//
//  @AfterClass
//  public static void closeCluster() {
//    jedis.close();
//  }
//
//  @AfterClass
//  public static void resetCluster() {
//    ClusterCommandsTestHelper.tearClusterDown();
//  }
//
//  @Before
//  public void setUp() {
//    ClusterCommandsTestHelper.clearClusterData();
//  }
//
//  @Test
//  @Override
//  public void zunion() {
//    jedis.zadd("{:}foo", 1, "a");
//    jedis.zadd("{:}foo", 2, "b");
//    jedis.zadd("{:}bar", 2, "a");
//    jedis.zadd("{:}bar", 2, "b");
//
//    ZParams params = new ZParams();
//    params.weights(2, 2.5);
//    params.aggregate(ZParams.Aggregate.SUM);
//    Set<String> expected = new LinkedHashSet<>();
//    expected.add("a");
//    expected.add("b");
//    assertEquals(expected, jedis.zunion(params, "{:}foo", "{:}bar"));
//
//    Set<Tuple> expectedTuple = new LinkedHashSet<>();
//    expectedTuple.add(new Tuple("b", new Double(9)));
//    expectedTuple.add(new Tuple("a", new Double(7)));
//    assertEquals(expectedTuple, jedis.zunionWithScores(params, "{:}foo", "{:}bar"));
//  }
//
//  @Test
//  @Override
//  public void zunionstore() {
//    jedis.zadd("{:}foo", 1, "a");
//    jedis.zadd("{:}foo", 2, "b");
//    jedis.zadd("{:}bar", 2, "a");
//    jedis.zadd("{:}bar", 2, "b");
//
//    assertEquals(2, jedis.zunionstore("{:}dst", "{:}foo", "{:}bar"));
//
//    List<Tuple> expected = new ArrayList<>();
//    expected.add(new Tuple("a", new Double(3)));
//    expected.add(new Tuple("b", new Double(4)));
//    assertEquals(expected, jedis.zrangeWithScores("{:}dst", 0, 100));
//  }
//
//  @Test
//  @Override
//  public void zunionstoreParams() {
//    jedis.zadd("{:}foo", 1, "a");
//    jedis.zadd("{:}foo", 2, "b");
//    jedis.zadd("{:}bar", 2, "a");
//    jedis.zadd("{:}bar", 2, "b");
//
//    ZParams params = new ZParams();
//    params.weights(2, 2.5);
//    params.aggregate(ZParams.Aggregate.SUM);
//
//    assertEquals(2, jedis.zunionstore("{:}dst", params, "{:}foo", "{:}bar"));
//
//    List<Tuple> expected = new ArrayList<>();
//    expected.add(new Tuple("a", new Double(7)));
//    expected.add(new Tuple("b", new Double(9)));
//    assertEquals(expected, jedis.zrangeWithScores("{:}dst", 0, 100));
//  }
//
//  @Test
//  @Override
//  public void zinter() {
//    jedis.zadd("foo{:}", 1, "a");
//    jedis.zadd("foo{:}", 2, "b");
//    jedis.zadd("bar{:}", 2, "a");
//
//    ZParams params = new ZParams();
//    params.weights(2, 2.5);
//    params.aggregate(ZParams.Aggregate.SUM);
//    assertEquals(Collections.singleton("a"), jedis.zinter(params, "foo{:}", "bar{:}"));
//
//    assertEquals(Collections.singleton(new Tuple("a", new Double(7))),
//      jedis.zinterWithScores(params, "foo{:}", "bar{:}"));
//  }
//
//  @Test
//  @Override
//  public void zinterstore() {
//    jedis.zadd("foo{:}", 1, "a");
//    jedis.zadd("foo{:}", 2, "b");
//    jedis.zadd("bar{:}", 2, "a");
//
//    assertEquals(1, jedis.zinterstore("dst{:}", "foo{:}", "bar{:}"));
//
//    assertEquals(Collections.singletonList(new Tuple("a", new Double(3))),
//        jedis.zrangeWithScores("dst{:}", 0, 100));
//  }
//
//  @Test
//  @Override
//  public void zintertoreParams() {
//    jedis.zadd("foo{:}", 1, "a");
//    jedis.zadd("foo{:}", 2, "b");
//    jedis.zadd("bar{:}", 2, "a");
//
//    ZParams params = new ZParams();
//    params.weights(2, 2.5);
//    params.aggregate(ZParams.Aggregate.SUM);
//
//    assertEquals(1, jedis.zinterstore("dst{:}", params, "foo{:}", "bar{:}"));
//
//    assertEquals(Collections.singletonList(new Tuple("a", new Double(7))),
//        jedis.zrangeWithScores("dst{:}", 0, 100));
//  }
//
//  @Test
//  @Override
//  public void bzpopmax() {
//    jedis.zadd("f{:}oo", 1d, "a", ZAddParams.zAddParams().nx());
//    jedis.zadd("f{:}oo", 10d, "b", ZAddParams.zAddParams().nx());
//    jedis.zadd("b{:}ar", 0.1d, "c", ZAddParams.zAddParams().nx());
//    assertEquals(new KeyedZSetElement("f{:}oo", "b", 10d), jedis.bzpopmax(0, "f{:}oo", "b{:}ar"));
//  }
//
//  @Test
//  @Override
//  public void bzpopmin() {
//    jedis.zadd("fo{:}o", 1d, "a", ZAddParams.zAddParams().nx());
//    jedis.zadd("fo{:}o", 10d, "b", ZAddParams.zAddParams().nx());
//    jedis.zadd("ba{:}r", 0.1d, "c", ZAddParams.zAddParams().nx());
//    assertEquals(new KeyedZSetElement("ba{:}r", "c", 0.1d), jedis.bzpopmin(0, "ba{:}r", "fo{:}o"));
//  }
//
//  @Test
//  @Override
//  public void zdiff() {
//    jedis.zadd("{:}foo", 1.0, "a");
//    jedis.zadd("{:}foo", 2.0, "b");
//    jedis.zadd("{:}bar", 1.0, "a");
//
//    assertEquals(0, jedis.zdiff("{bar}1", "{bar}2").size());
//    assertEquals(Collections.singleton("b"), jedis.zdiff("{:}foo", "{:}bar"));
//    assertEquals(Collections.singleton(new Tuple("b", 2.0d)), jedis.zdiffWithScores("{:}foo", "{:}bar"));
//  }
//
//  @Test
//  @Override
//  public void zdiffStore() {
//    jedis.zadd("foo{:}", 1.0, "a");
//    jedis.zadd("foo{:}", 2.0, "b");
//    jedis.zadd("bar{:}", 1.0, "a");
//
//    assertEquals(0, jedis.zdiffStore("{bar}3", "{bar}1", "{bar}2"));
//    assertEquals(1, jedis.zdiffStore("bar{:}3", "foo{:}", "bar{:}"));
//    assertEquals(Collections.singletonList("b"), jedis.zrange("bar{:}3", 0, -1));
//  }
//
//}
