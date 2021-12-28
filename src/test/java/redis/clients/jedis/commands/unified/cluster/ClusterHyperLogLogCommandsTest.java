//package redis.clients.jedis.commands.unified.cluster;
//
//import static org.junit.Assert.assertEquals;
//
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Ignore;
//import org.junit.Test;
//import redis.clients.jedis.commands.unified.HyperLogLogCommandsTestBase;
//
//public class ClusterHyperLogLogCommandsTest extends HyperLogLogCommandsTestBase {
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
//  public void pfcounts() {
//    long status = jedis.pfadd("{hll}_1", "foo", "bar", "zap");
//    assertEquals(1, status);
//    status = jedis.pfadd("{hll}_2", "foo", "bar", "zap");
//    assertEquals(1, status);
//
//    status = jedis.pfadd("{hll}_3", "foo", "bar", "baz");
//    assertEquals(1, status);
//    status = jedis.pfcount("{hll}_1");
//    assertEquals(3, status);
//    status = jedis.pfcount("{hll}_2");
//    assertEquals(3, status);
//    status = jedis.pfcount("{hll}_3");
//    assertEquals(3, status);
//
//    status = jedis.pfcount("{hll}_1", "{hll}_2");
//    assertEquals(3, status);
//
//    status = jedis.pfcount("{hll}_1", "{hll}_2", "{hll}_3");
//    assertEquals(4, status);
//  }
//
//  @Test
//  @Override
//  public void pfmerge() {
//    long status = jedis.pfadd("{hll}1", "foo", "bar", "zap", "a");
//    assertEquals(1, status);
//
//    status = jedis.pfadd("{hll}2", "a", "b", "c", "foo");
//    assertEquals(1, status);
//
//    String mergeStatus = jedis.pfmerge("{hll}3", "{hll}1", "{hll}2");
//    assertEquals("OK", mergeStatus);
//
//    status = jedis.pfcount("{hll}3");
//    assertEquals(6, status);
//  }
//
//  @Ignore
//  @Override
//  public void pfmergeBinary() {
//  }
//}
