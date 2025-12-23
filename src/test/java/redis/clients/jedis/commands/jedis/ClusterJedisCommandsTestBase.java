package redis.clients.jedis.commands.jedis;

import static redis.clients.jedis.Protocol.CLUSTER_HASHSLOTS;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.RedisVersionCondition;

@Tag("integration")
public abstract class ClusterJedisCommandsTestBase {

  private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("cluster-unbound");

  private Jedis node1;
  private static Jedis node2;
  private static Jedis node3;

  private HostAndPort nodeInfo1 = endpoint.getHostsAndPorts().get(0);
  private HostAndPort nodeInfo2 = endpoint.getHostsAndPorts().get(1);
  private HostAndPort nodeInfo3 = endpoint.getHostsAndPorts().get(2);
  private final Set<HostAndPort> jedisClusterNode = new HashSet<>();
  RedisClusterClient cluster;

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(nodeInfo1, endpoint.getClientConfigBuilder().build());
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(nodeInfo1, endpoint.getClientConfigBuilder().build());

  @BeforeEach
  public void setUp() throws InterruptedException {
    node1 = new Jedis(nodeInfo1);
    node1.auth(endpoint.getPassword());
    node1.flushAll();

    node2 = new Jedis(nodeInfo2);
    node2.auth(endpoint.getPassword());
    node2.flushAll();

    node3 = new Jedis(nodeInfo3);
    node3.auth(endpoint.getPassword());
    node3.flushAll();

    // ---- configure cluster

    // add nodes to cluster
    node1.clusterMeet(nodeInfo2.getHost(), nodeInfo2.getPort());
    node1.clusterMeet(nodeInfo3.getHost(), nodeInfo3.getPort());

    // split available slots across the three nodes
//    int slotsPerNode = JedisCluster.HASHSLOTS / 3;
    int slotsPerNode = CLUSTER_HASHSLOTS / 3;
    int[] node1Slots = new int[slotsPerNode];
    int[] node2Slots = new int[slotsPerNode + 1];
    int[] node3Slots = new int[slotsPerNode];
    for (int i = 0, slot1 = 0, slot2 = 0, slot3 = 0; i < CLUSTER_HASHSLOTS; i++) {
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

    waitForClusterReady();

    jedisClusterNode.add(nodeInfo1);
    cluster = RedisClusterClient.builder()
        .nodes(jedisClusterNode)
        .clientConfig(endpoint.getClientConfigBuilder().build())
        .build();
  }

  @AfterAll
  public static void cleanUp() {
    int slotTest = JedisClusterCRC16.getSlot("test");
    int slot51 = JedisClusterCRC16.getSlot("51");
    if (node3 != null) {
      String node3Id = getNodeId(node3.clusterNodes());
      node2.clusterSetSlotNode(slotTest, node3Id);
      node2.clusterSetSlotNode(slot51, node3Id);
      node2.clusterDelSlots(slotTest, slot51);
    }
  }

  @AfterEach
  public void tearDown() {
    // clear all slots
    int[] slotsToDelete = new int[CLUSTER_HASHSLOTS];
    for (int i = 0; i < CLUSTER_HASHSLOTS; i++) {
      slotsToDelete[i] = i;
    }
    node1.clusterDelSlots(slotsToDelete);
    node2.clusterDelSlots(slotsToDelete);
    node3.clusterDelSlots(slotsToDelete);
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
      if (node1.clusterInfo().split("\n")[0].contains("ok")
          && node2.clusterInfo().split("\n")[0].contains("ok")
          && node3.clusterInfo().split("\n")[0].contains("ok")) {
        clusterOk = true;
      }
      Thread.sleep(50);
    }
  }
}
