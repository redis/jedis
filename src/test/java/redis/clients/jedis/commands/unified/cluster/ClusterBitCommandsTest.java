//package redis.clients.jedis.commands.unified.cluster;
//
//import static org.junit.Assert.assertEquals;
//
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Ignore;
//import org.junit.Test;
//
//import redis.clients.jedis.args.BitOP;
//import redis.clients.jedis.exceptions.JedisDataException;
//import redis.clients.jedis.commands.unified.BitCommandsTestBase;
//
//public class ClusterBitCommandsTest extends BitCommandsTestBase {
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
//  public void bitOp() {
//    jedis.set("{key}1", "\u0060");
//    jedis.set("{key}2", "\u0044");
//
//    jedis.bitop(BitOP.AND, "resultAnd{key}", "{key}1", "{key}2");
//    String resultAnd = jedis.get("resultAnd{key}");
//    assertEquals("\u0040", resultAnd);
//
//    jedis.bitop(BitOP.OR, "resultOr{key}", "{key}1", "{key}2");
//    String resultOr = jedis.get("resultOr{key}");
//    assertEquals("\u0064", resultOr);
//
//    jedis.bitop(BitOP.XOR, "resultXor{key}", "{key}1", "{key}2");
//    String resultXor = jedis.get("resultXor{key}");
//    assertEquals("\u0024", resultXor);
//  }
//
//  @Test
//  @Override
//  public void bitOpNot() {
//    jedis.setbit("key", 0, true);
//    jedis.setbit("key", 4, true);
//
//    jedis.bitop(BitOP.NOT, "resultNot{key}", "key");
//    String resultNot = jedis.get("resultNot{key}");
//    assertEquals("\u0077", resultNot);
//  }
//
//  @Ignore
//  @Override
//  public void bitOpBinary() {
//  }
//
//  @Test(expected = JedisDataException.class)
//  @Override
//  public void bitOpNotMultiSourceShouldFail() {
//    jedis.bitop(BitOP.NOT, "{!}dest", "{!}src1", "{!}src2");
//  }
//
//}
