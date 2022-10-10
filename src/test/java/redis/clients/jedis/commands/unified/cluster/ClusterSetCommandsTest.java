//package redis.clients.jedis.commands.unified.cluster;
//
//import static org.junit.Assert.assertEquals;
//
//import java.util.HashSet;
//import java.util.Set;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import redis.clients.jedis.commands.unified.SetCommandsTestBase;
//
//public class ClusterSetCommandsTest extends SetCommandsTestBase {
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
//  public void smove() {
//    jedis.sadd("{.}foo", "a");
//    jedis.sadd("{.}foo", "b");
//
//    jedis.sadd("{.}bar", "c");
//
//    long status = jedis.smove("{.}foo", "{.}bar", "a");
//    assertEquals(status, 1);
//
//    Set<String> expectedSrc = new HashSet<>();
//    expectedSrc.add("b");
//
//    Set<String> expectedDst = new HashSet<>();
//    expectedDst.add("c");
//    expectedDst.add("a");
//
//    assertEquals(expectedSrc, jedis.smembers("{.}foo"));
//    assertEquals(expectedDst, jedis.smembers("{.}bar"));
//
//    status = jedis.smove("{.}foo", "{.}bar", "a");
//    assertEquals(status, 0);
//  }
//
//  @Test
//  @Override
//  public void sinter() {
//    jedis.sadd("foo{.}", "a");
//    jedis.sadd("foo{.}", "b");
//
//    jedis.sadd("bar{.}", "b");
//    jedis.sadd("bar{.}", "c");
//
//    Set<String> expected = new HashSet<>();
//    expected.add("b");
//
//    Set<String> intersection = jedis.sinter("foo{.}", "bar{.}");
//    assertEquals(expected, intersection);
//  }
//
//  @Test
//  @Override
//  public void sinterstore() {
//    jedis.sadd("foo{.}", "a");
//    jedis.sadd("foo{.}", "b");
//
//    jedis.sadd("bar{.}", "b");
//    jedis.sadd("bar{.}", "c");
//
//    Set<String> expected = new HashSet<>();
//    expected.add("b");
//
//    long status = jedis.sinterstore("car{.}", "foo{.}", "bar{.}");
//    assertEquals(1, status);
//
//    assertEquals(expected, jedis.smembers("car{.}"));
//  }
//
//  @Test
//  @Override
//  public void sunion() {
//    jedis.sadd("{.}foo", "a");
//    jedis.sadd("{.}foo", "b");
//
//    jedis.sadd("{.}bar", "b");
//    jedis.sadd("{.}bar", "c");
//
//    Set<String> expected = new HashSet<>();
//    expected.add("a");
//    expected.add("b");
//    expected.add("c");
//
//    Set<String> union = jedis.sunion("{.}foo", "{.}bar");
//    assertEquals(expected, union);
//  }
//
//  @Test
//  @Override
//  public void sunionstore() {
//    jedis.sadd("{.}foo", "a");
//    jedis.sadd("{.}foo", "b");
//
//    jedis.sadd("{.}bar", "b");
//    jedis.sadd("{.}bar", "c");
//
//    Set<String> expected = new HashSet<>();
//    expected.add("a");
//    expected.add("b");
//    expected.add("c");
//
//    long status = jedis.sunionstore("{.}car", "{.}foo", "{.}bar");
//    assertEquals(3, status);
//
//    assertEquals(expected, jedis.smembers("{.}car"));
//  }
//
//  @Test
//  @Override
//  public void sdiff() {
//    jedis.sadd("foo{.}", "x");
//    jedis.sadd("foo{.}", "a");
//    jedis.sadd("foo{.}", "b");
//    jedis.sadd("foo{.}", "c");
//
//    jedis.sadd("bar{.}", "c");
//
//    jedis.sadd("car{.}", "a");
//    jedis.sadd("car{.}", "d");
//
//    Set<String> expected = new HashSet<>();
//    expected.add("x");
//    expected.add("b");
//
//    Set<String> diff = jedis.sdiff("foo{.}", "bar{.}", "car{.}");
//    assertEquals(expected, diff);
//  }
//
//  @Test
//  @Override
//  public void sdiffstore() {
//    jedis.sadd("foo{.}", "x");
//    jedis.sadd("foo{.}", "a");
//    jedis.sadd("foo{.}", "b");
//    jedis.sadd("foo{.}", "c");
//
//    jedis.sadd("bar{.}", "c");
//
//    jedis.sadd("car{.}", "a");
//    jedis.sadd("car{.}", "d");
//
//    Set<String> expected = new HashSet<>();
//    expected.add("x");
//    expected.add("b");
//
//    long status = jedis.sdiffstore("tar{.}", "foo{.}", "bar{.}", "car{.}");
//    assertEquals(2, status);
//    assertEquals(expected, jedis.smembers("tar{.}"));
//  }
//
//}
