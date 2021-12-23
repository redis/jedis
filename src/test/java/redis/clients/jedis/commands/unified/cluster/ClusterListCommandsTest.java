//package redis.clients.jedis.commands.unified.cluster;
//
//import static org.junit.Assert.assertArrayEquals;
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertNull;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import org.junit.AfterClass;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//
//import redis.clients.jedis.args.ListDirection;
//import redis.clients.jedis.resps.KeyedListElement;
//import redis.clients.jedis.commands.unified.ListCommandsTestBase;
//
//public class ClusterListCommandsTest extends ListCommandsTestBase {
//
//  private final Logger logger = LoggerFactory.getLogger(getClass());
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
//  public void rpoplpush() {
//    jedis.rpush("foo{|}", "a");
//    jedis.rpush("foo{|}", "b");
//    jedis.rpush("foo{|}", "c");
//
//    jedis.rpush("dst{|}", "foo");
//    jedis.rpush("dst{|}", "bar");
//
//    String element = jedis.rpoplpush("foo{|}", "dst{|}");
//
//    assertEquals("c", element);
//
//    List<String> srcExpected = new ArrayList<>();
//    srcExpected.add("a");
//    srcExpected.add("b");
//
//    List<String> dstExpected = new ArrayList<>();
//    dstExpected.add("c");
//    dstExpected.add("foo");
//    dstExpected.add("bar");
//
//    assertEquals(srcExpected, jedis.lrange("foo{|}", 0, 1000));
//    assertEquals(dstExpected, jedis.lrange("dst{|}", 0, 1000));
//  }
//
//  @Test
//  @Override
//  public void blpop() throws InterruptedException {
//    List<String> result = jedis.blpop(1, "foo");
//    assertNull(result);
//
//    jedis.lpush("foo", "bar");
//    result = jedis.blpop(1, "foo");
//
//    assertNotNull(result);
//    assertEquals(2, result.size());
//    assertEquals("foo", result.get(0));
//    assertEquals("bar", result.get(1));
//
//    // Multi keys
//    result = jedis.blpop(1, "{foo}", "{foo}1");
//    assertNull(result);
//
//    jedis.lpush("{foo}", "bar");
//    jedis.lpush("{foo}1", "bar1");
//    result = jedis.blpop(1, "{foo}1", "{foo}");
//
//    assertNotNull(result);
//    assertEquals(2, result.size());
//    assertEquals("{foo}1", result.get(0));
//    assertEquals("bar1", result.get(1));
//
//    // Binary
//    jedis.lpush(bfoo, bbar);
//    List<byte[]> bresult = jedis.blpop(1, bfoo);
//
//    assertNotNull(bresult);
//    assertEquals(2, bresult.size());
//    assertArrayEquals(bfoo, bresult.get(0));
//    assertArrayEquals(bbar, bresult.get(1));
//  }
//
//  @Test
//  @Override
//  public void blpopDouble() throws InterruptedException {
//    KeyedListElement result = jedis.blpop(0.1, "foo");
//    assertNull(result);
//
//    jedis.lpush("foo", "bar");
//    result = jedis.blpop(3.2, "foo");
//
//    assertNotNull(result);
//    assertEquals("foo", result.getKey());
//    assertEquals("bar", result.getElement());
//
//    // Multi keys
//    result = jedis.blpop(0.18, "{foo}", "{foo}1");
//    assertNull(result);
//
//    jedis.lpush("{foo}", "bar");
//    jedis.lpush("{foo}1", "bar1");
//    result = jedis.blpop(1d, "{foo}1", "{foo}");
//
//    assertNotNull(result);
//    assertEquals("{foo}1", result.getKey());
//    assertEquals("bar1", result.getElement());
//
//    // Binary
//    jedis.lpush(bfoo, bbar);
//    List<byte[]> bresult = jedis.blpop(3.12, bfoo);
//
//    assertNotNull(bresult);
//    assertEquals(2, bresult.size());
//    assertArrayEquals(bfoo, bresult.get(0));
//    assertArrayEquals(bbar, bresult.get(1));
//  }
//
//  @Test
//  @Override
//  public void brpop() throws InterruptedException {
//    List<String> result = jedis.brpop(1, "foo");
//    assertNull(result);
//
//    jedis.lpush("foo", "bar");
//    result = jedis.brpop(1, "foo");
//    assertNotNull(result);
//    assertEquals(2, result.size());
//    assertEquals("foo", result.get(0));
//    assertEquals("bar", result.get(1));
//
//    // Multi keys
//    result = jedis.brpop(1, "{foo}", "{foo}1");
//    assertNull(result);
//
//    jedis.lpush("{foo}", "bar");
//    jedis.lpush("{foo}1", "bar1");
//    result = jedis.brpop(1, "{foo}1", "{foo}");
//
//    assertNotNull(result);
//    assertEquals(2, result.size());
//    assertEquals("{foo}1", result.get(0));
//    assertEquals("bar1", result.get(1));
//
//    // Binary
//    jedis.lpush(bfoo, bbar);
//    List<byte[]> bresult = jedis.brpop(1, bfoo);
//    assertNotNull(bresult);
//    assertEquals(2, bresult.size());
//    assertArrayEquals(bfoo, bresult.get(0));
//    assertArrayEquals(bbar, bresult.get(1));
//  }
//
//  @Test
//  @Override
//  public void brpopDouble() throws InterruptedException {
//    KeyedListElement result = jedis.brpop(0.1, "foo");
//    assertNull(result);
//
//    jedis.lpush("foo", "bar");
//    result = jedis.brpop(3.2, "foo");
//
//    assertNotNull(result);
//    assertEquals("foo", result.getKey());
//    assertEquals("bar", result.getElement());
//
//    // Multi keys
//    result = jedis.brpop(0.18, "{foo}", "{foo}1");
//    assertNull(result);
//
//    jedis.lpush("{foo}", "bar");
//    jedis.lpush("{foo}1", "bar1");
//    result = jedis.brpop(1d, "{foo}1", "{foo}");
//
//    assertNotNull(result);
//    assertEquals("{foo}1", result.getKey());
//    assertEquals("bar1", result.getElement());
//
//    // Binary
//    jedis.lpush(bfoo, bbar);
//    List<byte[]> bresult = jedis.brpop(3.12, bfoo);
//
//    assertNotNull(bresult);
//    assertEquals(2, bresult.size());
//    assertArrayEquals(bfoo, bresult.get(0));
//    assertArrayEquals(bbar, bresult.get(1));
//  }
//
//  @Test
//  @Override
//  public void brpoplpush() {
//
//    new Thread(new Runnable() {
//      @Override
//      public void run() {
//        try {
//          Thread.sleep(100);
//        } catch (InterruptedException e) {
//          logger.error("", e);
//        }
//        jedis.lpush("foo{|}", "a");
//      }
//    }).start();
//
//    String element = jedis.brpoplpush("foo{|}", "bar{|}", 0);
//
//    assertEquals("a", element);
//    assertEquals(1, jedis.llen("bar{|}"));
//    assertEquals("a", jedis.lrange("bar{|}", 0, -1).get(0));
//  }
//
//  @Test
//  @Override
//  public void lmove() {
//    jedis.rpush("{|}foo", "bar1", "bar2", "bar3");
//    assertEquals("bar3", jedis.lmove("{|}foo", "{|}bar", ListDirection.RIGHT, ListDirection.LEFT));
//    assertEquals(Collections.singletonList("bar3"), jedis.lrange("{|}bar", 0, -1));
//    assertEquals(Arrays.asList("bar1", "bar2"), jedis.lrange("{|}foo", 0, -1));
//  }
//
//  @Test
//  @Override
//  public void blmove() {
//    new Thread(() -> {
//      try {
//        Thread.sleep(100);
//      } catch (InterruptedException e) {
//        logger.error("", e);
//      }
//      jedis.rpush("{|}foo", "bar1", "bar2", "bar3");
//    }).start();
//
//    assertEquals("bar3", jedis.blmove("{|}foo", "{|}bar", ListDirection.RIGHT, ListDirection.LEFT, 0));
//    assertEquals(Collections.singletonList("bar3"), jedis.lrange("{|}bar", 0, -1));
//    assertEquals(Arrays.asList("bar1", "bar2"), jedis.lrange("{|}foo", 0, -1));
//  }
//}
