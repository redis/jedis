package redis.clients.jedis.tests.commands;

import static redis.clients.jedis.Protocol.Command.CLUSTER;
import static redis.clients.jedis.Protocol.Command.FLUSHALL;

import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import redis.clients.jedis.BuilderFactory;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
//import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisX;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.jedis.util.JedisClusterCRC16;

public abstract class ClusterJedisCommandsTestBase {
//  private Jedis node1;
//  private static Jedis node2;
//  private static Jedis node3;
  private static Connection node1;
  private static Connection node2;
  private static Connection node3;

  private HostAndPort nodeInfo1 = HostAndPortUtil.getClusterServers().get(0);
  private HostAndPort nodeInfo2 = HostAndPortUtil.getClusterServers().get(1);
  private HostAndPort nodeInfo3 = HostAndPortUtil.getClusterServers().get(2);

  private final Set<HostAndPort> jedisClusterNode = new HashSet<>();
//  JedisCluster jedisCluster;
  JedisX jedisCluster;

  @Before
  public void setUp() throws InterruptedException {
//    node1 = new Jedis(nodeInfo1);
//    node1.auth("cluster");
//    node1.flushAll();
    node1 = new Connection(nodeInfo1, DefaultJedisClientConfig.builder().password("cluster").build());
    node1.executeCommand(FLUSHALL);

//    node2 = new Jedis(nodeInfo2);
//    node2.auth("cluster");
//    node2.flushAll();
    node2 = new Connection(nodeInfo2, DefaultJedisClientConfig.builder().password("cluster").build());
    node2.executeCommand(FLUSHALL);

//    node3 = new Jedis(nodeInfo3);
//    node3.auth("cluster");
//    node3.flushAll();
    node3 = new Connection(nodeInfo3, DefaultJedisClientConfig.builder().password("cluster").build());
    node3.executeCommand(FLUSHALL);

    // ---- configure cluster

    // add nodes to cluster
//    node1.clusterMeet("127.0.0.1", nodeInfo2.getPort());
//    node1.clusterMeet("127.0.0.1", nodeInfo3.getPort());
    node1.executeCommand(new CommandArguments(CLUSTER).add("MEET").add("127.0.0.1").add(nodeInfo2.getPort()));
    node1.executeCommand(new CommandArguments(CLUSTER).add("MEET").add("127.0.0.1").add(nodeInfo3.getPort()));

    // split available slots across the three nodes
//    int slotsPerNode = JedisCluster.HASHSLOTS / 3;
    int slotsPerNode = Protocol.CLUSTER_HASHSLOTS / 3;
    int[] node1Slots = new int[slotsPerNode];
    int[] node2Slots = new int[slotsPerNode + 1];
    int[] node3Slots = new int[slotsPerNode];
//    for (int i = 0, slot1 = 0, slot2 = 0, slot3 = 0; i < JedisCluster.HASHSLOTS; i++) {
    for (int i = 0, slot1 = 0, slot2 = 0, slot3 = 0; i < Protocol.CLUSTER_HASHSLOTS; i++) {
      if (i < slotsPerNode) {
        node1Slots[slot1++] = i;
      } else if (i > slotsPerNode * 2) {
        node3Slots[slot3++] = i;
      } else {
        node2Slots[slot2++] = i;
      }
    }

//    node1.clusterAddSlots(node1Slots);
//    node2.clusterAddSlots(node2Slots);
//    node3.clusterAddSlots(node3Slots);
    node1.executeCommand(new CommandArguments(CLUSTER).add("ADDSLOTS").addObjects(node1Slots));
    node2.executeCommand(new CommandArguments(CLUSTER).add("ADDSLOTS").addObjects(node2Slots));
    node3.executeCommand(new CommandArguments(CLUSTER).add("ADDSLOTS").addObjects(node3Slots));

    waitForClusterReady();

    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
//    jedisCluster = new JedisCluster(jedisClusterNode, 2000, 2000, 5, "cluster", new JedisPoolConfig());
    jedisCluster = new JedisX(jedisClusterNode, DefaultJedisClientConfig.builder().password("cluster").build(), 5);

  }

  @AfterClass
  public static void cleanUp() {
    int slotTest = JedisClusterCRC16.getSlot("test");
    int slot51 = JedisClusterCRC16.getSlot("51");
//    String node3Id = getNodeId(node3.clusterNodes());
    String node3Id = getNodeId(node3.executeCommand(new CommandObject<>(
        new CommandArguments(CLUSTER).add(Protocol.ClusterKeyword.NODES),
        BuilderFactory.STRING)));
//    node2.clusterSetSlotNode(slotTest, node3Id);
//    node2.clusterSetSlotNode(slot51, node3Id);
    node2.executeCommand(new CommandArguments(CLUSTER).add("SETSLOT").add(slotTest).add("NODE").add(node3Id));
    node2.executeCommand(new CommandArguments(CLUSTER).add("SETSLOT").add(slot51).add("NODE").add(node3Id));
//    node2.clusterDelSlots(slotTest, slot51);
    node2.executeCommand(new CommandArguments(CLUSTER).add("DELSLOTS").add(slotTest).add(slot51));
  }

  @After
  public void tearDown() {
    // clear all slots
    int[] slotsToDelete = new int[Protocol.CLUSTER_HASHSLOTS];
    for (int i = 0; i < Protocol.CLUSTER_HASHSLOTS; i++) {
      slotsToDelete[i] = i;
    }
//    node1.clusterDelSlots(slotsToDelete);
//    node2.clusterDelSlots(slotsToDelete);
//    node3.clusterDelSlots(slotsToDelete);
    node1.executeCommand(new CommandArguments(CLUSTER).add("DELSLOTS").addObjects(slotsToDelete));
    node2.executeCommand(new CommandArguments(CLUSTER).add("DELSLOTS").addObjects(slotsToDelete));
    node3.executeCommand(new CommandArguments(CLUSTER).add("DELSLOTS").addObjects(slotsToDelete));
  }

  private static String getNodeId(String infoOutput) {
    for (String infoLine : infoOutput.split("\n")) {
      if (infoLine.contains("myself")) {
        return infoLine.split(" ")[0];
      }
    }
    return "";
  }

  private void waitForClusterReady() throws InterruptedException {
    boolean clusterOk = false;
    while (!clusterOk) {
//      if (node1.clusterInfo().split("\n")[0].contains("ok")
//          && node2.clusterInfo().split("\n")[0].contains("ok")
//          && node3.clusterInfo().split("\n")[0].contains("ok")) {
      if (node1.executeCommand(new CommandObject<>(new CommandArguments(CLUSTER).add("INFO"), BuilderFactory.STRING)).split("\n")[0].contains("ok")
          && node2.executeCommand(new CommandObject<>(new CommandArguments(CLUSTER).add("INFO"), BuilderFactory.STRING)).split("\n")[0].contains("ok")
          && node3.executeCommand(new CommandObject<>(new CommandArguments(CLUSTER).add("INFO"), BuilderFactory.STRING)).split("\n")[0].contains("ok")) {
        clusterOk = true;
      }
      Thread.sleep(50);
    }
  }
}
