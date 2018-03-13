package redis.clients.jedis.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.BinaryJedisCluster;
import redis.clients.jedis.ClusterReset;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSlotBasedConnectionHandler;
import redis.clients.jedis.ReadFrom;
import redis.clients.jedis.tests.utils.JedisClusterTestUtil;
import redis.clients.jedis.util.JedisClusterCRC16;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MasterSlaveJedisClusterTest {
  private static Jedis node1;
  private static Jedis node2;
  private static Jedis node3;
  private static Jedis nodeSlave1;
  private static Jedis nodeSlave2;
  private static Jedis nodeSlave3;
  private String localHost = "127.0.0.1";

  private static final String PASSWORD = "cluster";
  private static final int DEFAULT_TIMEOUT = 2000;
  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final JedisPoolConfig DEFAULT_CONFIG = new JedisPoolConfig();

  private HostAndPort nodeInfo1 = HostAndPortUtil.getClusterServers().get(0);
  private HostAndPort nodeInfo2 = HostAndPortUtil.getClusterServers().get(1);
  private HostAndPort nodeInfo3 = HostAndPortUtil.getClusterServers().get(2);
  private HostAndPort nodeInfoSlave1 = HostAndPortUtil.getClusterServers().get(3);
  private HostAndPort nodeInfoSlave2 = HostAndPortUtil.getClusterServers().get(4);
  private HostAndPort nodeInfoSlave3 = HostAndPortUtil.getClusterServers().get(5);

  protected Logger log = LoggerFactory.getLogger(getClass().getName());

  @Before
  public void setUp() throws InterruptedException {
    node1 = new Jedis(nodeInfo1);
    node1.auth(PASSWORD);
    node1.flushAll();

    node2 = new Jedis(nodeInfo2);
    node2.auth(PASSWORD);
    node2.flushAll();

    node3 = new Jedis(nodeInfo3);
    node3.auth(PASSWORD);
    node3.flushAll();

    nodeSlave1 = new Jedis(nodeInfoSlave1);
    nodeSlave1.auth(PASSWORD);
    nodeSlave1.flushAll();

    nodeSlave2 = new Jedis(nodeInfoSlave2);
    nodeSlave2.auth(PASSWORD);
    nodeSlave2.flushAll();

    nodeSlave3 = new Jedis(nodeInfoSlave3);
    nodeSlave3.auth(PASSWORD);
    nodeSlave3.flushAll();

    // ---- configure cluster

    // add nodes to cluster
    node1.clusterMeet(localHost, nodeInfo2.getPort());
    node1.clusterMeet(localHost, nodeInfo3.getPort());
    node1.clusterMeet(localHost, nodeInfoSlave1.getPort());
    node1.clusterMeet(localHost, nodeInfoSlave2.getPort());
    node1.clusterMeet(localHost, nodeInfoSlave3.getPort());

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

    // setting replica
    String nodes = node1.clusterNodes();
    nodeSlave1.clusterReplicate(JedisClusterTestUtil.getNodeId(nodes, nodeInfo1));
    nodeSlave2.clusterReplicate(JedisClusterTestUtil.getNodeId(nodes, nodeInfo2));
    nodeSlave3.clusterReplicate(JedisClusterTestUtil.getNodeId(nodes, nodeInfo3));

    JedisClusterTestUtil.waitForReplicaReady(node1, 3);
  }

  @After
  public void tearDown() throws InterruptedException {
    node1 = new Jedis(nodeInfo1);
    node1.auth(PASSWORD);
    try {
      node1.flushAll();
    } catch (Exception e) {
      // ignore
    }
    node1.clusterReset(ClusterReset.SOFT);

    node2 = new Jedis(nodeInfo2);
    node2.auth(PASSWORD);
    try {
      node2.flushAll();
    } catch (Exception e) {
      // ignore
    }
    node2.clusterReset(ClusterReset.SOFT);

    node3 = new Jedis(nodeInfo3);
    node3.auth(PASSWORD);
    try {
      node3.flushAll();
    } catch (Exception e) {
      // ignore
    }
    node3.clusterReset(ClusterReset.SOFT);

    nodeSlave1 = new Jedis(nodeInfoSlave1);
    nodeSlave1.auth(PASSWORD);
    try {
      nodeSlave1.flushAll();
    } catch (Exception e) {
      // ignore
    }
    nodeSlave1.clusterReset(ClusterReset.SOFT);

    nodeSlave2 = new Jedis(nodeInfoSlave2);
    nodeSlave2.auth(PASSWORD);
    try {
      nodeSlave2.flushAll();
    } catch (Exception e) {
      // ignore
    }
    nodeSlave2.clusterReset(ClusterReset.SOFT);

    nodeSlave3 = new Jedis(nodeInfoSlave3);
    nodeSlave3.auth(PASSWORD);
    try {
      nodeSlave3.flushAll();
    } catch (Exception e) {
      // ignore
    }
    nodeSlave3.clusterReset(ClusterReset.SOFT);
  }

  @Test
  public void testSetupJedisClusterInfo() {
    JedisCluster jc = new JedisCluster(nodeInfo1, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, PASSWORD, "test", DEFAULT_CONFIG, ReadFrom.BOTH);
    assertEquals(6, jc.getClusterNodes(ReadFrom.BOTH).size());
    assertEquals(3, jc.getClusterNodes(ReadFrom.MASTER).size());
    assertEquals(3, jc.getClusterNodes(ReadFrom.SLAVE).size());

    List<String> nodes = Arrays.asList(nodeInfo1.toString(), nodeInfo2.toString(),
      nodeInfo3.toString());
    for (String node : nodes) {
      assertTrue(jc.getClusterNodes(ReadFrom.MASTER).containsKey(node));
    }
    nodes = Arrays.asList(nodeInfoSlave1.toString(), nodeInfoSlave2.toString(),
      nodeInfoSlave3.toString());
    for (String node : nodes) {
      assertTrue(jc.getClusterNodes(ReadFrom.SLAVE).containsKey(node));
    }

    jc = new JedisCluster(nodeInfo1, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_REDIRECTIONS,
        PASSWORD, "test", DEFAULT_CONFIG, ReadFrom.MASTER);
    assertEquals(6, jc.getClusterNodes(ReadFrom.BOTH).size());
    try {
      jc.getClusterNodes(ReadFrom.MASTER);
    } catch (UnsupportedOperationException e) {
      // ignore
    }
    try {
      jc.getClusterNodes(ReadFrom.SLAVE);
    } catch (UnsupportedOperationException e) {
      // ignore
    }
  }

  @Test
  public void testGetConnection() {
    JedisCluster jc = new JedisCluster(nodeInfo1, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, PASSWORD, "test", DEFAULT_CONFIG, ReadFrom.SLAVE);
    JedisSlotBasedConnectionHandler handler = getHandlerFromJedis(jc);

    Jedis jedis = handler.getConnection(ReadFrom.MASTER);
    assertTrue(isMasterPort(jedis.getClient().getPort()));

    jedis = handler.getConnection(ReadFrom.SLAVE);
    assertTrue(isSlavePort(jedis.getClient().getPort()));

    jedis = handler.getConnectionFromSlot(10, ReadFrom.BOTH);
    Jedis master = handler.getConnectionFromSlot(10, ReadFrom.MASTER);
    Jedis slave = handler.getConnectionFromSlot(10, ReadFrom.SLAVE);
    assertEquals(3, slave.getClient().getPort() - master.getClient().getPort());
    assertTrue(jedis.getClient().getPort() == master.getClient().getPort()
        || jedis.getClient().getPort() == slave.getClient().getPort());
  }

  @Test
  public void testSetAndGet() {
    JedisCluster jc1 = new JedisCluster(nodeInfo1, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, PASSWORD, "test", DEFAULT_CONFIG, ReadFrom.MASTER);
    JedisCluster jc2 = new JedisCluster(nodeInfo1, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, PASSWORD, "test", DEFAULT_CONFIG, ReadFrom.SLAVE);

    assertEquals("OK", jc1.set("foo", "bar"));
    assertEquals("bar", jc1.get("foo"));
    assertEquals("bar", jc2.get("foo"));
  }

  @Test
  public void testMigrate() throws InterruptedException {
    JedisCluster jc = new JedisCluster(nodeInfo1, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, PASSWORD, "test", DEFAULT_CONFIG, ReadFrom.SLAVE);
    JedisSlotBasedConnectionHandler handler = getHandlerFromJedis(jc);
    int slot = JedisClusterCRC16.getSlot("foo");

    Jedis master = handler.getConnectionFromSlot(slot, ReadFrom.MASTER);
    Jedis slave = handler.getConnectionFromSlot(slot, ReadFrom.SLAVE);

    int oldMasterPort = master.getClient().getPort();
    int oldSlavePort = slave.getClient().getPort();
    assertEquals(3, oldSlavePort - oldMasterPort);
    assertEquals(oldMasterPort, node3.getClient().getPort());

    jc.del("foo");
    node2.clusterSetSlotImporting(slot, JedisClusterTestUtil.getNodeId(node3.clusterNodes()));
    node3.clusterSetSlotMigrating(slot, JedisClusterTestUtil.getNodeId(node2.clusterNodes()));
    node1.clusterSetSlotNode(slot, JedisClusterTestUtil.getNodeId(node2.clusterNodes()));
    node2.clusterSetSlotNode(slot, JedisClusterTestUtil.getNodeId(node2.clusterNodes()));
    node3.clusterSetSlotNode(slot, JedisClusterTestUtil.getNodeId(node2.clusterNodes()));
    int i = 0;
    while (node3.clusterSlots().size() < 5 && i++ < 1000) {
      Thread.sleep(100);
    }
    jc.set("foo", "bar");
    master = handler.getConnectionFromSlot(slot, ReadFrom.MASTER);
    slave = handler.getConnectionFromSlot(slot, ReadFrom.SLAVE);
    assertEquals(nodeInfo2.getPort(), master.getClient().getPort());
    assertEquals(nodeInfoSlave2.getPort(), slave.getClient().getPort());
  }

  private JedisSlotBasedConnectionHandler getHandlerFromJedis(JedisCluster jc) {
    JedisSlotBasedConnectionHandler handler = null;
    try {
      Field field = BinaryJedisCluster.class.getDeclaredField("connectionHandler");
      field.setAccessible(true);
      handler = (JedisSlotBasedConnectionHandler) field.get(jc);
    } catch (Exception e) {
      log.error("can not obtain handler", e);
      // ignore
    }
    assertNotNull(handler);
    return handler;
  }

  private boolean isMasterPort(int port) {
    return port == nodeInfo1.getPort() || port == nodeInfo2.getPort()
        || port == nodeInfo3.getPort();
  }

  private boolean isSlavePort(int port) {
    return port == nodeInfoSlave1.getPort() || port == nodeInfoSlave2.getPort()
        || port == nodeInfoSlave3.getPort();
  }
}
