//package redis.clients.jedis.commands.unified.cluster;
//
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Ignore;
//import redis.clients.jedis.commands.unified.BinaryValuesCommandsTestBase;
//
//public class ClusterBinaryValuesCommandsTest extends BinaryValuesCommandsTestBase {
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
//  @Ignore
//  @Override
//  public void mget() {
//  }
//
//  @Ignore
//  @Override
//  public void mset() {
//  }
//
//  @Ignore
//  @Override
//  public void msetnx() {
//  }
//}
