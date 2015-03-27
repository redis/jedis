package redis.clients.jedis.tests;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisCluster.Reset;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterException;
import redis.clients.jedis.exceptions.JedisClusterMaxRedirectionsException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.tests.utils.JedisClusterTestUtil;
import redis.clients.util.JedisClusterCRC16;

public class JedisClusterTest extends Assert {
  private static Jedis node1;
  private static Jedis node2;
  private static Jedis node3;
  private static Jedis node4;
  private String localHost = "127.0.0.1";

  private HostAndPort nodeInfo1 = HostAndPortUtil.getClusterServers().get(0);
  private HostAndPort nodeInfo2 = HostAndPortUtil.getClusterServers().get(1);
  private HostAndPort nodeInfo3 = HostAndPortUtil.getClusterServers().get(2);
  private HostAndPort nodeInfo4 = HostAndPortUtil.getClusterServers().get(3);
  protected Logger log = Logger.getLogger(getClass().getName());

  @Before
  public void setUp() throws InterruptedException {
    node1 = new Jedis(nodeInfo1.getHost(), nodeInfo1.getPort());
    node1.connect();
    node1.flushAll();

    node2 = new Jedis(nodeInfo2.getHost(), nodeInfo2.getPort());
    node2.connect();
    node2.flushAll();

    node3 = new Jedis(nodeInfo3.getHost(), nodeInfo3.getPort());
    node3.connect();
    node3.flushAll();

    node4 = new Jedis(nodeInfo4.getHost(), nodeInfo4.getPort());
    node4.connect();
    node4.flushAll();

    // ---- configure cluster

    // add nodes to cluster
    node1.clusterMeet(localHost, nodeInfo2.getPort());
    node1.clusterMeet(localHost, nodeInfo3.getPort());

    // split available slots across the three nodes
    int slotsPerNode = JedisCluster.HASHSLOTS / 3;
    int[] node1Slots = new int[slotsPerNode];
    int[] node2Slots = new int[slotsPerNode + 1];
    int[] node3Slots = new int[slotsPerNode];
    for (int i = 0, slot1 = 0, slot2 = 0, slot3 = 0; i < JedisCluster.HASHSLOTS; i++) {
      if (i < slotsPerNode) {
        node1Slots[slot1++] = i;
      } else if (i > slotsPerNode * 2) {
        node3Slots[slot3++] = i;
      } else {
        node2Slots[slot2++] = i;
      }
    }

    node1.clusterAddSlots(node1Slots);
    node2.clusterAddSlots(node2Slots);
    node3.clusterAddSlots(node3Slots);

    JedisClusterTestUtil.waitForClusterReady(node1, node2, node3);
  }

  @AfterClass
  public static void cleanUp() {
    node1.flushDB();
    node2.flushDB();
    node3.flushDB();
    node4.flushDB();
    node1.clusterReset(Reset.SOFT);
    node2.clusterReset(Reset.SOFT);
    node3.clusterReset(Reset.SOFT);
    node4.clusterReset(Reset.SOFT);
  }

  @After
  public void tearDown() throws InterruptedException {
    cleanUp();
  }

  @Test(expected = JedisMovedDataException.class)
  public void testThrowMovedException() {
    node1.set("foo", "bar");
  }

  @Test
  public void testMovedExceptionParameters() {
    try {
      node1.set("foo", "bar");
    } catch (JedisMovedDataException jme) {
      assertEquals(12182, jme.getSlot());
      assertEquals(new HostAndPort("127.0.0.1", 7381), jme.getTargetNode());
      return;
    }
    fail();
  }

  @Test(expected = JedisAskDataException.class)
  public void testThrowAskException() {
    int keySlot = JedisClusterCRC16.getSlot("test");
    String node3Id = JedisClusterTestUtil.getNodeId(node3.clusterNodes());
    node2.clusterSetSlotMigrating(keySlot, node3Id);
    node2.get("test");
  }

  @Test
  public void testDiscoverNodesAutomatically() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    JedisCluster jc = new JedisCluster(jedisClusterNode);
    assertEquals(3, jc.getClusterNodes().size());
  }

  @Test
  public void testCalculateConnectionPerSlot() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    JedisCluster jc = new JedisCluster(jedisClusterNode);
    jc.set("foo", "bar");
    jc.set("test", "test");
    assertEquals("bar", node3.get("foo"));
    assertEquals("test", node2.get("test"));
  }

  /**
   * slot->nodes 15363 node3 e
   */
  @Test
  public void testMigrate() {
    log.info("test migrate slot");
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(nodeInfo1);
    JedisCluster jc = new JedisCluster(jedisClusterNode);
    String node3Id = JedisClusterTestUtil.getNodeId(node3.clusterNodes());
    String node2Id = JedisClusterTestUtil.getNodeId(node2.clusterNodes());
    node3.clusterSetSlotMigrating(15363, node2Id);
    node2.clusterSetSlotImporting(15363, node3Id);
    try {
      node2.set("e", "e");
    } catch (JedisMovedDataException jme) {
      assertEquals(15363, jme.getSlot());
      assertEquals(new HostAndPort(localHost, nodeInfo3.getPort()), jme.getTargetNode());
    }

    try {
      node3.set("e", "e");
    } catch (JedisAskDataException jae) {
      assertEquals(15363, jae.getSlot());
      assertEquals(new HostAndPort(localHost, nodeInfo2.getPort()), jae.getTargetNode());
    }

    jc.set("e", "e");

    try {
      node2.get("e");
    } catch (JedisMovedDataException jme) {
      assertEquals(15363, jme.getSlot());
      assertEquals(new HostAndPort(localHost, nodeInfo3.getPort()), jme.getTargetNode());
    }
    try {
      node3.get("e");
    } catch (JedisAskDataException jae) {
      assertEquals(15363, jae.getSlot());
      assertEquals(new HostAndPort(localHost, nodeInfo2.getPort()), jae.getTargetNode());
    }

    assertEquals("e", jc.get("e"));

    node2.clusterSetSlotNode(15363, node2Id);
    node3.clusterSetSlotNode(15363, node2Id);
    // assertEquals("e", jc.get("e"));
    assertEquals("e", node2.get("e"));

    // assertEquals("e", node3.get("e"));

  }

  @Test
  public void testMigrateToNewNode() throws InterruptedException {
    log.info("test migrate slot to new node");
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(nodeInfo1);
    JedisCluster jc = new JedisCluster(jedisClusterNode);
    node4.clusterMeet(localHost, nodeInfo1.getPort());

    String node3Id = JedisClusterTestUtil.getNodeId(node3.clusterNodes());
    String node4Id = JedisClusterTestUtil.getNodeId(node4.clusterNodes());
    JedisClusterTestUtil.waitForClusterReady(node4);
    node3.clusterSetSlotMigrating(15363, node4Id);
    node4.clusterSetSlotImporting(15363, node3Id);
    try {
      node4.set("e", "e");
    } catch (JedisMovedDataException jme) {
      assertEquals(15363, jme.getSlot());
      assertEquals(new HostAndPort(localHost, nodeInfo3.getPort()), jme.getTargetNode());
    }

    try {
      node3.set("e", "e");
    } catch (JedisAskDataException jae) {
      assertEquals(15363, jae.getSlot());
      assertEquals(new HostAndPort(localHost, nodeInfo4.getPort()), jae.getTargetNode());
    }

    jc.set("e", "e");

    try {
      node4.get("e");
    } catch (JedisMovedDataException jme) {
      assertEquals(15363, jme.getSlot());
      assertEquals(new HostAndPort(localHost, nodeInfo3.getPort()), jme.getTargetNode());
    }
    try {
      node3.get("e");
    } catch (JedisAskDataException jae) {
      assertEquals(15363, jae.getSlot());
      assertEquals(new HostAndPort(localHost, nodeInfo4.getPort()), jae.getTargetNode());
    }

    assertEquals("e", jc.get("e"));

    node4.clusterSetSlotNode(15363, node4Id);
    node3.clusterSetSlotNode(15363, node4Id);
    // assertEquals("e", jc.get("e"));
    assertEquals("e", node4.get("e"));

    // assertEquals("e", node3.get("e"));

  }

  @Test
  public void testRecalculateSlotsWhenMoved() throws InterruptedException {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    JedisCluster jc = new JedisCluster(jedisClusterNode);
    int slot51 = JedisClusterCRC16.getSlot("51");
    node2.clusterDelSlots(slot51);
    node3.clusterDelSlots(slot51);
    node3.clusterAddSlots(slot51);

    JedisClusterTestUtil.waitForClusterReady(node1, node2, node3);
    jc.set("51", "foo");
    assertEquals("foo", jc.get("51"));
  }

  @Test
  public void testAskResponse() throws InterruptedException {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    JedisCluster jc = new JedisCluster(jedisClusterNode);
    int slot51 = JedisClusterCRC16.getSlot("51");
    node3.clusterSetSlotImporting(slot51, JedisClusterTestUtil.getNodeId(node2.clusterNodes()));
    node2.clusterSetSlotMigrating(slot51, JedisClusterTestUtil.getNodeId(node3.clusterNodes()));
    jc.set("51", "foo");
    assertEquals("foo", jc.get("51"));
  }

  @Test(expected = JedisClusterException.class)
  public void testThrowExceptionWithoutKey() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    JedisCluster jc = new JedisCluster(jedisClusterNode);
    jc.ping();
  }

  @Test(expected = JedisClusterMaxRedirectionsException.class)
  public void testRedisClusterMaxRedirections() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    JedisCluster jc = new JedisCluster(jedisClusterNode);
    int slot51 = JedisClusterCRC16.getSlot("51");
    // This will cause an infinite redirection loop
    node2.clusterSetSlotMigrating(slot51, JedisClusterTestUtil.getNodeId(node3.clusterNodes()));
    jc.set("51", "foo");
  }

  @Test
  public void testRedisHashtag() {
    assertEquals(JedisClusterCRC16.getSlot("{bar"), JedisClusterCRC16.getSlot("foo{{bar}}zap"));
    assertEquals(JedisClusterCRC16.getSlot("{user1000}.following"),
      JedisClusterCRC16.getSlot("{user1000}.followers"));
    assertNotEquals(JedisClusterCRC16.getSlot("foo{}{bar}"), JedisClusterCRC16.getSlot("bar"));
    assertEquals(JedisClusterCRC16.getSlot("foo{bar}{zap}"), JedisClusterCRC16.getSlot("bar"));
  }

  @Test
  public void testClusterForgetNode() throws InterruptedException {
    // at first, join node4 to cluster
    node1.clusterMeet("127.0.0.1", nodeInfo4.getPort());

    String node7Id = JedisClusterTestUtil.getNodeId(node4.clusterNodes());

    JedisClusterTestUtil.assertNodeIsKnown(node3, node7Id, 1000);
    JedisClusterTestUtil.assertNodeIsKnown(node2, node7Id, 1000);
    JedisClusterTestUtil.assertNodeIsKnown(node1, node7Id, 1000);

    assertNodeHandshakeEnded(node3, 1000);
    assertNodeHandshakeEnded(node2, 1000);
    assertNodeHandshakeEnded(node1, 1000);

    assertEquals(4, node1.clusterNodes().split("\n").length);
    assertEquals(4, node2.clusterNodes().split("\n").length);
    assertEquals(4, node3.clusterNodes().split("\n").length);

    // do cluster forget
    node1.clusterForget(node7Id);
    node2.clusterForget(node7Id);
    node3.clusterForget(node7Id);

    JedisClusterTestUtil.assertNodeIsUnknown(node1, node7Id, 1000);
    JedisClusterTestUtil.assertNodeIsUnknown(node2, node7Id, 1000);
    JedisClusterTestUtil.assertNodeIsUnknown(node3, node7Id, 1000);

    assertEquals(3, node1.clusterNodes().split("\n").length);
    assertEquals(3, node2.clusterNodes().split("\n").length);
    assertEquals(3, node3.clusterNodes().split("\n").length);
  }

  @Test
  public void testClusterFlushSlots() {
    String slotRange = getNodeServingSlotRange(node1.clusterNodes());
    assertNotNull(slotRange);

    try {
      node1.clusterFlushSlots();
      assertNull(getNodeServingSlotRange(node1.clusterNodes()));
    } finally {
      // rollback
      String[] rangeInfo = slotRange.split("-");
      int lower = Integer.parseInt(rangeInfo[0]);
      int upper = Integer.parseInt(rangeInfo[1]);

      int[] node1Slots = new int[upper - lower + 1];
      for (int i = 0; lower <= upper;) {
        node1Slots[i++] = lower++;
      }
      node1.clusterAddSlots(node1Slots);
    }
  }

  @Test
  public void testClusterKeySlot() {
    // It assumes JedisClusterCRC16 is correctly implemented
    assertEquals(node1.clusterKeySlot("foo{bar}zap}").intValue(),
      JedisClusterCRC16.getSlot("foo{bar}zap"));
    assertEquals(node1.clusterKeySlot("{user1000}.following").intValue(),
      JedisClusterCRC16.getSlot("{user1000}.following"));
  }

  @Test
  public void testClusterCountKeysInSlot() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort(nodeInfo1.getHost(), nodeInfo1.getPort()));
    JedisCluster jc = new JedisCluster(jedisClusterNode);

    for (int index = 0; index < 5; index++) {
      jc.set("foo{bar}" + index, "hello");
    }

    int slot = JedisClusterCRC16.getSlot("foo{bar}");
    assertEquals(5, node1.clusterCountKeysInSlot(slot).intValue());
  }

  @Test
  public void testStableSlotWhenMigratingNodeOrImportingNodeIsNotSpecified()
      throws InterruptedException {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort(nodeInfo1.getHost(), nodeInfo1.getPort()));
    JedisCluster jc = new JedisCluster(jedisClusterNode);

    int slot51 = JedisClusterCRC16.getSlot("51");
    jc.set("51", "foo");
    // node2 is responsible of taking care of slot51 (7186)

    node3.clusterSetSlotImporting(slot51, JedisClusterTestUtil.getNodeId(node2.clusterNodes()));
    assertEquals("foo", jc.get("51"));
    node3.clusterSetSlotStable(slot51);
    assertEquals("foo", jc.get("51"));

    node2.clusterSetSlotMigrating(slot51, JedisClusterTestUtil.getNodeId(node3.clusterNodes()));
    // assertEquals("foo", jc.get("51")); // it leads Max Redirections
    node2.clusterSetSlotStable(slot51);
    assertEquals("foo", jc.get("51"));
  }

  @Test(expected = JedisConnectionException.class)
  public void testIfPoolConfigAppliesToClusterPools() {
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMaxTotal(0);
    config.setMaxWaitMillis(2000);
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    JedisCluster jc = new JedisCluster(jedisClusterNode, config);
    jc.set("52", "poolTestValue");
  }

  @Test
  public void testCloseable() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort(nodeInfo1.getHost(), nodeInfo1.getPort()));

    JedisCluster jc = null;
    try {
      jc = new JedisCluster(jedisClusterNode);
      jc.set("51", "foo");
    } finally {
      if (jc != null) {
        jc.close();
      }
    }

    Iterator<JedisPool> poolIterator = jc.getClusterNodes().values().iterator();
    while (poolIterator.hasNext()) {
      JedisPool pool = poolIterator.next();
      try {
        pool.getResource();
        fail("JedisCluster's internal pools should be already destroyed");
      } catch (JedisConnectionException e) {
        // ok to go...
      }
    }
  }

  @Test
  public void testJedisClusterTimeout() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort(nodeInfo1.getHost(), nodeInfo1.getPort()));

    JedisCluster jc = new JedisCluster(jedisClusterNode, 4000);

    for (JedisPool pool : jc.getClusterNodes().values()) {
      Jedis jedis = pool.getResource();
      assertEquals(jedis.getClient().getConnectionTimeout(), 4000);
      assertEquals(jedis.getClient().getSoTimeout(), 4000);
      jedis.close();
    }

  }

  @Test
  public void testJedisClusterRunsWithMultithreaded() throws InterruptedException,
      ExecutionException {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    final JedisCluster jc = new JedisCluster(jedisClusterNode);
    jc.set("foo", "bar");

    ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 100, 0, TimeUnit.SECONDS,
        new ArrayBlockingQueue<Runnable>(10));
    List<Future<String>> futures = new ArrayList<Future<String>>();
    for (int i = 0; i < 50; i++) {
      executor.submit(new Callable<String>() {
        @Override
        public String call() throws Exception {
          // FIXME : invalidate slot cache from JedisCluster to test
          // random connection also does work
          return jc.get("foo");
        }
      });
    }

    for (Future<String> future : futures) {
      String value = future.get();
      assertEquals("bar", value);
    }

    jc.close();
  }

  private static String getNodeServingSlotRange(String infoOutput) {
    // f4f3dc4befda352a4e0beccf29f5e8828438705d 127.0.0.1:7380 master - 0
    // 1394372400827 0 connected 5461-10922
    for (String infoLine : infoOutput.split("\n")) {
      if (infoLine.contains("myself")) {
        try {
          return infoLine.split(" ")[8];
        } catch (ArrayIndexOutOfBoundsException e) {
          return null;
        }
      }
    }
    return null;
  }

  private void assertNodeHandshakeEnded(Jedis node, int timeoutMs) {
    int sleepInterval = 100;
    for (int sleepTime = 0; sleepTime <= timeoutMs; sleepTime += sleepInterval) {
      boolean isHandshaking = isAnyNodeHandshaking(node);
      if (!isHandshaking) return;

      try {
        Thread.sleep(sleepInterval);
      } catch (InterruptedException e) {
      }
    }

    throw new JedisException("Node handshaking is not ended");
  }

  private boolean isAnyNodeHandshaking(Jedis node) {
    String infoOutput = node.clusterNodes();
    for (String infoLine : infoOutput.split("\n")) {
      if (infoLine.contains("handshake")) {
        return true;
      }
    }
    return false;
  }

}
