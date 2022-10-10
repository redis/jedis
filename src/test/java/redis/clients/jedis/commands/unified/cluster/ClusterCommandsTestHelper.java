//package redis.clients.jedis.commands.unified.cluster;
//
//import static redis.clients.jedis.Protocol.CLUSTER_HASHSLOTS;
//
//import java.util.Collections;
//
//import redis.clients.jedis.HostAndPort;
//import redis.clients.jedis.Jedis;
//import redis.clients.jedis.JedisCluster;
//import redis.clients.jedis.args.ClusterResetType;
//import redis.clients.jedis.HostAndPorts;
//import redis.clients.jedis.util.JedisClusterTestUtil;
//
//public class ClusterCommandsTestHelper {
//
//  private static final HostAndPort nodeInfo1 = HostAndPorts.getClusterServers().get(0);
//  private static final HostAndPort nodeInfo2 = HostAndPorts.getClusterServers().get(1);
//  private static final HostAndPort nodeInfo3 = HostAndPorts.getClusterServers().get(2);
//
//  private static Jedis node1;
//  private static Jedis node2;
//  private static Jedis node3;
//
//  static JedisCluster initAndGetCluster() throws InterruptedException {
//
//    node1 = new Jedis(nodeInfo1);
//    node1.auth("cluster");
//    node1.flushAll();
//
//    node2 = new Jedis(nodeInfo2);
//    node2.auth("cluster");
//    node2.flushAll();
//
//    node3 = new Jedis(nodeInfo3);
//    node3.auth("cluster");
//    node3.flushAll();
//
//    // ---- configure cluster
//    // add nodes to cluster
//    node1.clusterMeet("127.0.0.1", nodeInfo2.getPort());
//    node1.clusterMeet("127.0.0.1", nodeInfo3.getPort());
//
//    // split available slots across the three nodes
//    int slotsPerNode = CLUSTER_HASHSLOTS / 3;
//    int[] node1Slots = new int[slotsPerNode];
//    int[] node2Slots = new int[slotsPerNode + 1];
//    int[] node3Slots = new int[slotsPerNode];
//    for (int i = 0, slot1 = 0, slot2 = 0, slot3 = 0; i < CLUSTER_HASHSLOTS; i++) {
//      if (i < slotsPerNode) {
//        node1Slots[slot1++] = i;
//      } else if (i > slotsPerNode * 2) {
//        node3Slots[slot3++] = i;
//      } else {
//        node2Slots[slot2++] = i;
//      }
//    }
//
//    node1.clusterAddSlots(node1Slots);
//    node2.clusterAddSlots(node2Slots);
//    node3.clusterAddSlots(node3Slots);
//
//    JedisClusterTestUtil.waitForClusterReady(node1, node2, node2);
//
//    return new JedisCluster(Collections.singleton(
//        new HostAndPort("127.0.0.1", nodeInfo1.getPort())), null, "cluster");
//  }
//
//  static void tearClusterDown() {
//    node1.flushDB();
//    node2.flushDB();
//    node3.flushDB();
//    node1.clusterReset(ClusterResetType.SOFT);
//    node2.clusterReset(ClusterResetType.SOFT);
//    node3.clusterReset(ClusterResetType.SOFT);
//  }
//
//  static void clearClusterData() {
//    node1.flushDB();
//    node2.flushDB();
//    node3.flushDB();
//  }
//}
