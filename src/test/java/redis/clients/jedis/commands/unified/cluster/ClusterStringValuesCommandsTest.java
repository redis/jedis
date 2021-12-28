//package redis.clients.jedis.commands.unified.cluster;
//
//import static org.junit.Assert.assertEquals;
//
//import java.util.ArrayList;
//import java.util.List;
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import redis.clients.jedis.commands.unified.StringValuesCommandsTestBase;
//
//public class ClusterStringValuesCommandsTest extends StringValuesCommandsTestBase {
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
//  public void mget() {
//    List<String> values = jedis.mget("foo{^}", "bar{^}");
//    List<String> expected = new ArrayList<>();
//    expected.add(null);
//    expected.add(null);
//
//    assertEquals(expected, values);
//
//    jedis.set("foo{^}", "bar");
//
//    expected = new ArrayList<>();
//    expected.add("bar");
//    expected.add(null);
//    values = jedis.mget("foo{^}", "bar{^}");
//
//    assertEquals(expected, values);
//
//    jedis.set("bar{^}", "foo");
//
//    expected = new ArrayList<>();
//    expected.add("bar");
//    expected.add("foo");
//    values = jedis.mget("foo{^}", "bar{^}");
//
//    assertEquals(expected, values);
//  }
//
//  @Test
//  @Override
//  public void mset() {
//    String status = jedis.mset("{^}foo", "bar", "{^}bar", "foo");
//    assertEquals("OK", status);
//    assertEquals("bar", jedis.get("{^}foo"));
//    assertEquals("foo", jedis.get("{^}bar"));
//  }
//
//  @Test
//  @Override
//  public void msetnx() {
//    assertEquals(1, jedis.msetnx("{^}foo", "bar", "{^}bar", "foo"));
//    assertEquals("bar", jedis.get("{^}foo"));
//    assertEquals("foo", jedis.get("{^}bar"));
//
//    assertEquals(0, jedis.msetnx("{^}foo", "bar1", "{^}bar2", "foo2"));
//    assertEquals("bar", jedis.get("{^}foo"));
//    assertEquals("foo", jedis.get("{^}bar"));
//  }
//
//}
