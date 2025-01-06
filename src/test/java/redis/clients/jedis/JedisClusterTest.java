package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import static redis.clients.jedis.Protocol.CLUSTER_HASHSLOTS;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Test;

import redis.clients.jedis.args.ClusterResetType;
import redis.clients.jedis.exceptions.*;
import redis.clients.jedis.util.ClientKillerUtil;
import redis.clients.jedis.util.JedisClusterTestUtil;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.Pool;

public class JedisClusterTest extends JedisClusterTestBase {

  private static final int DEFAULT_TIMEOUT = 2000;
  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();
  private static final DefaultJedisClientConfig DEFAULT_CLIENT_CONFIG
      = DefaultJedisClientConfig.builder().password("cluster").build();

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
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));

    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      assertEquals(3, jc.getClusterNodes().size());
    }

    try (JedisCluster jc2 = new JedisCluster(new HostAndPort("127.0.0.1", 7379), DEFAULT_TIMEOUT,
        DEFAULT_TIMEOUT, DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      assertEquals(3, jc2.getClusterNodes().size());
    }
  }

  @Test
  public void testDiscoverNodesAutomaticallyWithSocketConfig() {
    HostAndPort hp = new HostAndPort("127.0.0.1", 7379);

    try (JedisCluster jc = new JedisCluster(hp, DEFAULT_CLIENT_CONFIG, DEFAULT_REDIRECTIONS,
        DEFAULT_POOL_CONFIG)) {
      assertEquals(3, jc.getClusterNodes().size());
    }

    try (JedisCluster jc = new JedisCluster(Collections.singleton(hp), DEFAULT_CLIENT_CONFIG,
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      assertEquals(3, jc.getClusterNodes().size());
    }
  }

  @Test
  public void testSetClientName() {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    String clientName = "myAppName";

    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", clientName, DEFAULT_POOL_CONFIG)) {
      for (Pool<Connection> pool : jc.getClusterNodes().values()) {
        try (Jedis jedis = new Jedis(pool.getResource())) {
          assertEquals(clientName, jedis.clientGetname());
        }
      }
    }
  }

  @Test
  public void testSetClientNameWithConfig() {
    HostAndPort hp = new HostAndPort("127.0.0.1", 7379);
    String clientName = "config-pattern-app";
    try (JedisCluster jc = new JedisCluster(Collections.singleton(hp),
        DefaultJedisClientConfig.builder().password("cluster").clientName(clientName).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      jc.getClusterNodes().values().forEach(pool -> {
        try (Jedis jedis = new Jedis(pool.getResource())) {
          assertEquals(clientName, jedis.clientGetname());
        }
      });
    }
  }

  @Test
  public void testCalculateConnectionPerSlot() {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));

    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      jc.set("foo", "bar");
      jc.set("test", "test");
      assertEquals("bar", node3.get("foo"));
      assertEquals("test", node2.get("test"));
    }

    try (JedisCluster jc2 = new JedisCluster(new HostAndPort("127.0.0.1", 7379), DEFAULT_TIMEOUT,
        DEFAULT_TIMEOUT, DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      jc2.set("foo", "bar");
      jc2.set("test", "test");
      assertEquals("bar", node3.get("foo"));
      assertEquals("test", node2.get("test"));
    }
  }

  @Test
  public void testReadonlyAndReadwrite() throws Exception {
    node1.clusterMeet(LOCAL_IP, nodeInfoSlave2.getPort());
    JedisClusterTestUtil.waitForClusterReady(node1, node2, node3, nodeSlave2);

    for (String nodeInfo : node2.clusterNodes().split("\n")) {
      if (nodeInfo.contains("myself")) {
        nodeSlave2.clusterReplicate(nodeInfo.split(" ")[0]);
        break;
      }
    }

    try {
      nodeSlave2.get("test");
      fail();
    } catch (JedisMovedDataException e) {
    }
    nodeSlave2.readonly();
    nodeSlave2.get("test");

    nodeSlave2.readwrite();
    try {
      nodeSlave2.get("test");
      fail();
    } catch (JedisMovedDataException e) {
    }

    nodeSlave2.clusterReset(ClusterResetType.SOFT);
    nodeSlave2.flushDB();
  }

  @Test
  public void testReadFromReplicas() throws Exception {
    node1.clusterMeet(LOCAL_IP, nodeInfoSlave2.getPort());
    JedisClusterTestUtil.waitForClusterReady(node1, node2, node3, nodeSlave2);

    for (String nodeInfo : node2.clusterNodes().split("\n")) {
      if (nodeInfo.contains("myself")) {
        nodeSlave2.clusterReplicate(nodeInfo.split(" ")[0]);
        break;
      }
    }

    DefaultJedisClientConfig READ_REPLICAS_CLIENT_CONFIG = DefaultJedisClientConfig.builder()
        .password("cluster").readOnlyForRedisClusterReplicas().build();
    ClusterCommandObjects commandObjects = new ClusterCommandObjects();
    try (JedisCluster jedisCluster = new JedisCluster(nodeInfo1, READ_REPLICAS_CLIENT_CONFIG,
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      assertEquals("OK", jedisCluster.set("test", "read-from-replicas"));

      assertEquals("read-from-replicas", jedisCluster.executeCommandToReplica(commandObjects.get("test")));
      // TODO: ensure data being served from replica node(s)
    }

    nodeSlave2.clusterReset(ClusterResetType.SOFT);
    nodeSlave2.flushDB();
  }

  /**
   * slot->nodes 15363 node3 e
   */
  @Test
  public void testMigrate() {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(nodeInfo1);
    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      String node3Id = JedisClusterTestUtil.getNodeId(node3.clusterNodes());
      String node2Id = JedisClusterTestUtil.getNodeId(node2.clusterNodes());
      node3.clusterSetSlotMigrating(15363, node2Id);
      node2.clusterSetSlotImporting(15363, node3Id);
      try {
        node2.set("e", "e");
      } catch (JedisMovedDataException jme) {
        assertEquals(15363, jme.getSlot());
        assertEquals(new HostAndPort(LOCAL_IP, nodeInfo3.getPort()), jme.getTargetNode());
      }

      try {
        node3.set("e", "e");
      } catch (JedisAskDataException jae) {
        assertEquals(15363, jae.getSlot());
        assertEquals(new HostAndPort(LOCAL_IP, nodeInfo2.getPort()), jae.getTargetNode());
      }

      jc.set("e", "e");

      try {
        node2.get("e");
      } catch (JedisMovedDataException jme) {
        assertEquals(15363, jme.getSlot());
        assertEquals(new HostAndPort(LOCAL_IP, nodeInfo3.getPort()), jme.getTargetNode());
      }
      try {
        node3.get("e");
      } catch (JedisAskDataException jae) {
        assertEquals(15363, jae.getSlot());
        assertEquals(new HostAndPort(LOCAL_IP, nodeInfo2.getPort()), jae.getTargetNode());
      }

      assertEquals("e", jc.get("e"));

      node2.clusterSetSlotNode(15363, node2Id);
      node3.clusterSetSlotNode(15363, node2Id);
      // assertEquals("e", jc.get("e"));
      assertEquals("e", node2.get("e"));

      // assertEquals("e", node3.get("e"));
    }
  }

  @Test
  public void testMigrateToNewNode() throws InterruptedException {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(nodeInfo1);
    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      node3.clusterMeet(LOCAL_IP, nodeInfo4.getPort());

      String node3Id = JedisClusterTestUtil.getNodeId(node3.clusterNodes());
      String node4Id = JedisClusterTestUtil.getNodeId(node4.clusterNodes());
      JedisClusterTestUtil.waitForClusterReady(node4);
      node3.clusterSetSlotMigrating(15363, node4Id);
      node4.clusterSetSlotImporting(15363, node3Id);
      try {
        node4.set("e", "e");
      } catch (JedisMovedDataException jme) {
        assertEquals(15363, jme.getSlot());
        assertEquals(new HostAndPort(LOCAL_IP, nodeInfo3.getPort()), jme.getTargetNode());
      }

      try {
        node3.set("e", "e");
      } catch (JedisAskDataException jae) {
        assertEquals(15363, jae.getSlot());
        assertEquals(new HostAndPort(LOCAL_IP, nodeInfo4.getPort()), jae.getTargetNode());
      }

      try {
        node3.set("e", "e");
      } catch (JedisAskDataException jae) {
        assertEquals(15363, jae.getSlot());
        assertEquals(new HostAndPort(LOCAL_IP, nodeInfo4.getPort()), jae.getTargetNode());
      }

      jc.set("e", "e");

      try {
        node4.get("e");
      } catch (JedisMovedDataException jme) {
        assertEquals(15363, jme.getSlot());
        assertEquals(new HostAndPort(LOCAL_IP, nodeInfo3.getPort()), jme.getTargetNode());
      }
      try {
        node3.get("e");
      } catch (JedisAskDataException jae) {
        assertEquals(15363, jae.getSlot());
        assertEquals(new HostAndPort(LOCAL_IP, nodeInfo4.getPort()), jae.getTargetNode());
      }

      assertEquals("e", jc.get("e"));

      node4.clusterSetSlotNode(15363, node4Id);
      node3.clusterSetSlotNode(15363, node4Id);
      // assertEquals("e", jc.get("e"));
      assertEquals("e", node4.get("e"));

      // assertEquals("e", node3.get("e"));
    }
  }

  @Test
  public void testRecalculateSlotsWhenMoved() throws InterruptedException {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));

    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      int slot51 = JedisClusterCRC16.getSlot("51");
      node2.clusterDelSlots(slot51);
      node3.clusterDelSlots(slot51);
      node3.clusterAddSlots(slot51);

      JedisClusterTestUtil.waitForClusterReady(node1, node2, node3);
      jc.set("51", "foo");
      assertEquals("foo", jc.get("51"));
    }
  }

  @Test
  public void testAskResponse() {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));

    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      int slot51 = JedisClusterCRC16.getSlot("51");
      node3.clusterSetSlotImporting(slot51, JedisClusterTestUtil.getNodeId(node2.clusterNodes()));
      node2.clusterSetSlotMigrating(slot51, JedisClusterTestUtil.getNodeId(node3.clusterNodes()));
      jc.set("51", "foo");
      assertEquals("foo", jc.get("51"));
    }
  }

  @Test
  public void testAskResponseWithConfig() {
    HostAndPort hp = new HostAndPort("127.0.0.1", 7379);
    try (JedisCluster jc = new JedisCluster(Collections.singleton(hp), DEFAULT_CLIENT_CONFIG,
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      int slot51 = JedisClusterCRC16.getSlot("51");
      node3.clusterSetSlotImporting(slot51, JedisClusterTestUtil.getNodeId(node2.clusterNodes()));
      node2.clusterSetSlotMigrating(slot51, JedisClusterTestUtil.getNodeId(node3.clusterNodes()));
      jc.set("51", "foo");
      assertEquals("foo", jc.get("51"));
    }
  }

//  @Test(expected = JedisClusterMaxAttemptsException.class)
  @Test(expected = JedisClusterOperationException.class)
  public void testRedisClusterMaxRedirections() {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));

    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      int slot51 = JedisClusterCRC16.getSlot("51");
      // This will cause an infinite redirection loop
      node2.clusterSetSlotMigrating(slot51, JedisClusterTestUtil.getNodeId(node3.clusterNodes()));
      jc.set("51", "foo");
    }
  }

//  @Test(expected = JedisClusterMaxAttemptsException.class)
  @Test(expected = JedisClusterOperationException.class)
  public void testRedisClusterMaxRedirectionsWithConfig() {
    HostAndPort hp = new HostAndPort("127.0.0.1", 7379);
    try (JedisCluster jc = new JedisCluster(Collections.singleton(hp), DEFAULT_CLIENT_CONFIG,
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      int slot51 = JedisClusterCRC16.getSlot("51");
      // This will cause an infinite redirection loop
      node2.clusterSetSlotMigrating(slot51, JedisClusterTestUtil.getNodeId(node3.clusterNodes()));
      jc.set("51", "foo");
    }
  }

  @Test
  public void testClusterForgetNode() throws InterruptedException {
    // at first, join node4 to cluster
    node1.clusterMeet("127.0.0.1", nodeInfo4.getPort());
    node2.clusterMeet("127.0.0.1", nodeInfo4.getPort());
    node3.clusterMeet("127.0.0.1", nodeInfo4.getPort());

    String node4Id = JedisClusterTestUtil.getNodeId(node4.clusterNodes());

    JedisClusterTestUtil.assertNodeIsKnown(node1, node4Id, 1000);
    JedisClusterTestUtil.assertNodeIsKnown(node2, node4Id, 1000);
    JedisClusterTestUtil.assertNodeIsKnown(node3, node4Id, 1000);

    assertNodeHandshakeEnded(node1, 1000);
    assertNodeHandshakeEnded(node2, 1000);
    assertNodeHandshakeEnded(node3, 1000);

    assertEquals(4, node1.clusterNodes().split("\n").length);
    assertEquals(4, node2.clusterNodes().split("\n").length);
    assertEquals(4, node3.clusterNodes().split("\n").length);

    // do cluster forget
    node1.clusterForget(node4Id);
    node2.clusterForget(node4Id);
    node3.clusterForget(node4Id);

    JedisClusterTestUtil.assertNodeIsUnknown(node1, node4Id, 1000);
    JedisClusterTestUtil.assertNodeIsUnknown(node2, node4Id, 1000);
    JedisClusterTestUtil.assertNodeIsUnknown(node3, node4Id, 1000);

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
  public void testClusterCountKeysInSlot() {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort(nodeInfo1.getHost(), nodeInfo1.getPort()));

    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {

      int count = 5;
      for (int index = 0; index < count; index++) {
        jc.set("foo{bar}" + index, "hello");
      }

      int slot = JedisClusterCRC16.getSlot("foo{bar}");
      assertEquals(count, node1.clusterCountKeysInSlot(slot));
    }
  }

  @Test
  public void testStableSlotWhenMigratingNodeOrImportingNodeIsNotSpecified()
      throws InterruptedException {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort(nodeInfo1.getHost(), nodeInfo1.getPort()));

    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {

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
  }

  @Test(expected = JedisException.class)
  public void testIfPoolConfigAppliesToClusterPools() {
    GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(0);
    config.setMaxWait(Duration.ofMillis(DEFAULT_TIMEOUT));
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", config)) {
      jc.set("52", "poolTestValue");
    }
  }

  @Test
  public void testCloseable() throws IOException {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort(nodeInfo1.getHost(), nodeInfo1.getPort()));

    JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG);
    jc.set("51", "foo");
    jc.close();

    assertEquals(0, jc.getClusterNodes().size());
  }

  @Test
  public void testCloseableWithConfig() {
    HostAndPort hp = nodeInfo1;
    try (JedisCluster jc = new JedisCluster(hp, DEFAULT_CLIENT_CONFIG, DEFAULT_REDIRECTIONS,
        DEFAULT_POOL_CONFIG)) {
      jc.set("51", "foo");
      jc.close();

      assertEquals(0, jc.getClusterNodes().size());
    }
  }

  @Test
  public void testJedisClusterTimeout() {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort(nodeInfo1.getHost(), nodeInfo1.getPort()));

    try (JedisCluster jc = new JedisCluster(jedisClusterNode, 4000, 4000, DEFAULT_REDIRECTIONS,
        "cluster", DEFAULT_POOL_CONFIG)) {

      for (Pool<Connection> pool : jc.getClusterNodes().values()) {
        try (Connection conn = pool.getResource()) {
          assertEquals(4000, conn.getSoTimeout());
        }
      }
    }
  }

  @Test
  public void testJedisClusterTimeoutWithConfig() {
    HostAndPort hp = nodeInfo1;
    try (JedisCluster jc = new JedisCluster(hp, DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(4000).socketTimeoutMillis(4000).password("cluster").build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {

      jc.getClusterNodes().values().forEach(pool -> {
        try (Connection conn = pool.getResource()) {
          assertEquals(4000, conn.getSoTimeout());
        }
      });
    }
  }

  @Test
  public void testJedisClusterRunsWithMultithreaded() throws InterruptedException,
      ExecutionException, IOException {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    final JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG);
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

  @Test(timeout = DEFAULT_TIMEOUT * 2)
  public void testReturnConnectionOnJedisConnectionException() throws InterruptedException {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    ConnectionPoolConfig config = new ConnectionPoolConfig();
    config.setMaxTotal(1);
    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", config)) {

      try (Connection c = jc.getClusterNodes().get("127.0.0.1:7380").getResource()) {
        Jedis j = new Jedis(c);
        ClientKillerUtil.tagClient(j, "DEAD");
        ClientKillerUtil.killClient(j, "DEAD");
      }

      jc.get("test");
    }
  }

  @Test(expected = JedisClusterOperationException.class, timeout = DEFAULT_TIMEOUT)
  public void testReturnConnectionOnRedirection() {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    ConnectionPoolConfig config = new ConnectionPoolConfig();
    config.setMaxTotal(1);
    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", config)) {

      // This will cause an infinite redirection between node 2 and 3
      node3.clusterSetSlotMigrating(15363, JedisClusterTestUtil.getNodeId(node2.clusterNodes()));
      jc.get("e");
    }
  }

  @Test
  public void testLocalhostNodeNotAddedWhen127Present() {
    HostAndPort localhost = new HostAndPort("localhost", 7379);
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    // cluster node is defined as 127.0.0.1; adding localhost should work,
    // but shouldn't show up.
    jedisClusterNode.add(localhost);
    ConnectionPoolConfig config = new ConnectionPoolConfig();
    config.setMaxTotal(1);

    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", config)) {
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertFalse(clusterNodes.containsKey(JedisClusterInfoCache.getNodeKey(localhost)));
    }
  }

  @Test
  public void testInvalidStartNodeNotAdded() {
    HostAndPort invalidHost = new HostAndPort("not-a-real-host", 7379);
    Set<HostAndPort> jedisClusterNode = new LinkedHashSet<HostAndPort>();
    jedisClusterNode.add(invalidHost);
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    ConnectionPoolConfig config = new ConnectionPoolConfig();
    config.setMaxTotal(1);
    try (JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", config)) {
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertFalse(clusterNodes.containsKey(JedisClusterInfoCache.getNodeKey(invalidHost)));
    }
  }

  @Test
  public void clusterLinks2() throws InterruptedException {
    Set<String> mapKeys = new HashSet<>(Arrays.asList("direction", "node", "create-time", "events",
        "send-buffer-allocated", "send-buffer-used"));

    List<Map<String, Object>> links = node1.clusterLinks();
    assertNotNull(links);
    assertTrue(links.size() >= 3);
    for (Map<String, Object> link : links) {
      assertEquals(6, link.size());
      assertEquals(mapKeys, link.keySet());
    }
  }

  @Test
  public void clusterRefreshNodes() throws Exception {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(nodeInfo1);
    jedisClusterNode.add(nodeInfo2);
    jedisClusterNode.add(nodeInfo3);

    try (JedisCluster cluster = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT,
        DEFAULT_TIMEOUT, DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      assertEquals(3, cluster.getClusterNodes().size());
      cleanUp(); // cleanup and add node4

      // at first, join node4 to cluster
      node1.clusterMeet(LOCAL_IP, nodeInfo2.getPort());
      node1.clusterMeet(LOCAL_IP, nodeInfo3.getPort());
      node1.clusterMeet(LOCAL_IP, nodeInfo4.getPort());
      // split available slots across the three nodes
      int slotsPerNode = CLUSTER_HASHSLOTS / 4;
      int[] node1Slots = new int[slotsPerNode];
      int[] node2Slots = new int[slotsPerNode];
      int[] node3Slots = new int[slotsPerNode];
      int[] node4Slots = new int[slotsPerNode];
      for (int i = 0, slot1 = 0, slot2 = 0, slot3 = 0, slot4 = 0; i < CLUSTER_HASHSLOTS; i++) {
        if (i < slotsPerNode) {
          node1Slots[slot1++] = i;
        } else if (i >= slotsPerNode && i < slotsPerNode*2) {
          node2Slots[slot2++] = i;
        } else if (i >= slotsPerNode*2 && i < slotsPerNode*3) {
          node3Slots[slot3++] = i;
        } else {
          node4Slots[slot4++] = i;
        }
      }

      node1.clusterAddSlots(node1Slots);
      node2.clusterAddSlots(node2Slots);
      node3.clusterAddSlots(node3Slots);
      node4.clusterAddSlots(node4Slots);
      JedisClusterTestUtil.waitForClusterReady(node1, node2, node3, node4);

      // cluster.set("key", "value"); will get JedisMovedDataException and renewSlotCache
      cluster.set("key", "value");

      assertEquals(4, cluster.getClusterNodes().size());
      String nodeKey4 = LOCAL_IP + ":" + nodeInfo4.getPort();
      assertTrue(cluster.getClusterNodes().keySet().contains(nodeKey4));

      // make 4 nodes to 3 nodes
      cleanUp();
      setUp();
      // cluster.set("bar", "foo") will get JedisMovedDataException and renewSlotCache
      cluster.set("bar", "foo");
      assertEquals(3, cluster.getClusterNodes().size());
    }
  }

  @Test(timeout = 30_000)
  public void clusterPeriodTopologyRefreshTest() throws Exception {
    Set<HostAndPort> jedisClusterNode = new HashSet<>();
    jedisClusterNode.add(nodeInfo1);
    jedisClusterNode.add(nodeInfo2);
    jedisClusterNode.add(nodeInfo3);

    // we set topologyRefreshPeriod is 1s
    Duration topologyRefreshPeriod = Duration.ofSeconds(1);
    try (JedisCluster cluster = new JedisCluster(jedisClusterNode, DEFAULT_CLIENT_CONFIG, DEFAULT_POOL_CONFIG,
        topologyRefreshPeriod, DEFAULT_REDIRECTIONS, Duration.ofSeconds(10))) {
      assertEquals(3, cluster.getClusterNodes().size());
      cleanUp(); // cleanup and add node4

      // at first, join node4 to cluster
      node1.clusterMeet(LOCAL_IP, nodeInfo2.getPort());
      node1.clusterMeet(LOCAL_IP, nodeInfo3.getPort());
      node1.clusterMeet(LOCAL_IP, nodeInfo4.getPort());
      // split available slots across the three nodes
      int slotsPerNode = CLUSTER_HASHSLOTS / 4;
      int[] node1Slots = new int[slotsPerNode];
      int[] node2Slots = new int[slotsPerNode];
      int[] node3Slots = new int[slotsPerNode];
      int[] node4Slots = new int[slotsPerNode];
      for (int i = 0, slot1 = 0, slot2 = 0, slot3 = 0, slot4 = 0; i < CLUSTER_HASHSLOTS; i++) {
        if (i < slotsPerNode) {
          node1Slots[slot1++] = i;
        } else if (i >= slotsPerNode && i < slotsPerNode*2) {
          node2Slots[slot2++] = i;
        } else if (i >= slotsPerNode*2 && i < slotsPerNode*3) {
          node3Slots[slot3++] = i;
        } else {
          node4Slots[slot4++] = i;
        }
      }

      node1.clusterAddSlots(node1Slots);
      node2.clusterAddSlots(node2Slots);
      node3.clusterAddSlots(node3Slots);
      node4.clusterAddSlots(node4Slots);
      JedisClusterTestUtil.waitForClusterReady(node1, node2, node3, node4);

      // Now we just wait topologyRefreshPeriod * 3 (executor will delay) for cluster topology refresh (3 -> 4)
      Thread.sleep(topologyRefreshPeriod.toMillis() * 3);

      assertEquals(4, cluster.getClusterNodes().size());
      String nodeKey4 = LOCAL_IP + ":" + nodeInfo4.getPort();
      assertTrue(cluster.getClusterNodes().keySet().contains(nodeKey4));

      // make 4 nodes to 3 nodes
      cleanUp();
      setUp();

      // Now we just wait topologyRefreshPeriod * 3 (executor will delay) for cluster topology refresh (4 -> 3)
      Thread.sleep(topologyRefreshPeriod.toMillis() * 3);
      assertEquals(3, cluster.getClusterNodes().size());
    }
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
