//package redis.clients.jedis.commands.unified.cluster;
//
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import redis.clients.jedis.commands.unified.HashesCommandsTestBase;
//
//public class ClusterHashesCommandsTest extends HashesCommandsTestBase {
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
//}
