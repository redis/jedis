package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import io.redis.test.annotations.SinceRedisVersion;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.*;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.args.ClusterResetType;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.resps.ClusterShardInfo;
import redis.clients.jedis.resps.ClusterShardNodeInfo;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.JedisClusterTestUtil;
import redis.clients.jedis.util.RedisVersionRule;

public class ClusterCommandsTest {

  private static Jedis node1;
  private static Jedis node2;

  private static HostAndPort nodeInfo1 = HostAndPorts.getClusterServers().get(0);
  private static HostAndPort nodeInfo2 = HostAndPorts.getClusterServers().get(1);

  @Rule
  public RedisVersionRule versionRule = new RedisVersionRule(nodeInfo1,
      DefaultJedisClientConfig.builder().password("cluster").build());

  @Before
  public void setUp() throws Exception {
    node1 = new Jedis(nodeInfo1);
    node1.auth("cluster");
    node1.flushAll();

    node2 = new Jedis(nodeInfo2);
    node2.auth("cluster");
    node2.flushAll();
  }

  @After
  public void tearDown() {
    node1.disconnect();
    node2.disconnect();
  }

  @BeforeClass
  public static void resetRedisBefore() {
    removeSlots();
  }

  @AfterClass
  public static void resetRedisAfter() {
    removeSlots();
  }

  public static void removeSlots() {
    try (Jedis node = new Jedis(nodeInfo1)) {
      node.auth("cluster");
      node.clusterReset(ClusterResetType.SOFT);
    }
    try (Jedis node = new Jedis(nodeInfo2)) {
      node.auth("cluster");
      node.clusterReset(ClusterResetType.SOFT);
    }
  }

  @Test
  public void testClusterSoftReset() {
    node1.clusterMeet("127.0.0.1", nodeInfo2.getPort());
    assertTrue(node1.clusterNodes().split("\n").length > 1);
    node1.clusterReset(ClusterResetType.SOFT);
    assertEquals(1, node1.clusterNodes().split("\n").length);
  }

  @Test
  public void testClusterHardReset() {
    String nodeId = JedisClusterTestUtil.getNodeId(node1.clusterNodes());
    node1.clusterReset(ClusterResetType.HARD);
    String newNodeId = JedisClusterTestUtil.getNodeId(node1.clusterNodes());
    assertNotEquals(nodeId, newNodeId);
  }

  @Test
  public void clusterSetSlotImporting() {
    node2.clusterAddSlots(6000);
    String[] nodes = node1.clusterNodes().split("\n");
    String nodeId = nodes[0].split(" ")[0];
    String status = node1.clusterSetSlotImporting(6000, nodeId);
    assertEquals("OK", status);
    node2.clusterDelSlots(6000);
  }

  @Test
  public void clusterNodes() {
    String nodes = node1.clusterNodes();
    assertTrue(nodes.split("\n").length > 0);
  }
//
//  @Test
//  public void clusterMeet() {
//    String status = node1.clusterMeet("127.0.0.1", nodeInfo2.getPort());
//    assertEquals("OK", status);
//  }

  @Test
  public void clusterAddSlotsAndDelSlots() {
    assertEquals("OK", node1.clusterAddSlots(1, 2, 3, 4, 5));
    assertEquals("OK", node1.clusterDelSlots(1, 2, 3, 4, 5));
  }

  @Test
  public void clusterInfo() {
    String info = node1.clusterInfo();
    assertNotNull(info);
  }

  @Test
  @SinceRedisVersion("7.0.0")
  public void addAndDelSlotsRange() {
    // test add
    assertEquals("OK", node1.clusterAddSlotsRange(100, 105));
    String clusterNodes = node1.clusterNodes();
    assertTrue(clusterNodes.contains("connected 100-105"));

    assertEquals("OK", node1.clusterAddSlotsRange(110, 120));
    clusterNodes = node1.clusterNodes();
    assertTrue(clusterNodes.contains("connected 100-105 110-120"));

    // test del
    assertEquals("OK", node1.clusterDelSlotsRange(100, 105));
    clusterNodes = node1.clusterNodes();
    assertTrue(clusterNodes.contains("connected 110-120"));

    assertEquals("OK", node1.clusterDelSlotsRange(110, 120));
  }

  @Test
  public void clusterGetKeysInSlot() {
    node1.clusterAddSlots(500);
    List<String> keys = node1.clusterGetKeysInSlot(500, 1);
    assertEquals(0, keys.size());
    node1.clusterDelSlots(500);
  }

  @Test
  public void clusterGetKeysInSlotBinary() {
    node1.clusterAddSlots(501);
    List<byte[]> keys = node1.clusterGetKeysInSlotBinary(501, 1);
    assertEquals(0, keys.size());
    node1.clusterDelSlots(501);
  }

  @Test
  public void clusterSetSlotNode() {
    String[] nodes = node1.clusterNodes().split("\n");
    String nodeId = nodes[0].split(" ")[0];
    String status = node1.clusterSetSlotNode(10000, nodeId);
    assertEquals("OK", status);
  }

  @Test
  public void clusterSetSlotMigrating() {
    node1.clusterAddSlots(5000);
    String[] nodes = node1.clusterNodes().split("\n");
    String nodeId = nodes[0].split(" ")[0];
    String status = node1.clusterSetSlotMigrating(5000, nodeId);
    assertEquals("OK", status);
    node1.clusterDelSlots(5000);
  }

  @Test
  public void clusterSlots() {
    // please see cluster slot output format from below commit
    // @see: https://github.com/antirez/redis/commit/e14829de3025ffb0d3294e5e5a1553afd9f10b60
    assertEquals("OK", node1.clusterAddSlots(3000, 3001, 3002));

    List<Object> slots = node1.clusterSlots();
    assertNotNull(slots);
    assertTrue(slots.size() > 0);

    for (Object slotInfoObj : slots) {
      assertNotNull(slotInfoObj);
      List<Object> slotInfo = (List<Object>) slotInfoObj;
      assertTrue(slotInfo.size() >= 2);

      assertTrue(slotInfo.get(0) instanceof Long);
      assertTrue(slotInfo.get(1) instanceof Long);

      if (slotInfo.size() > 2) {
        // assigned slots
        assertTrue(slotInfo.get(2) instanceof List);
      }
    }
    node1.clusterDelSlots(3000, 3001, 3002);
  }

  @Test
  @SinceRedisVersion("7.0.0")
  public void clusterShards() {
    assertEquals("OK", node1.clusterAddSlots(3100, 3101, 3102, 3105));

    List<ClusterShardInfo> shards = node1.clusterShards();
    assertNotNull(shards);
    assertTrue(shards.size() > 0);

    for (ClusterShardInfo shardInfo : shards) {
      assertNotNull(shardInfo);

      assertTrue(shardInfo.getSlots().size() > 1);
      for (List<Long> slotRange : shardInfo.getSlots()) {
        assertEquals(2, slotRange.size());
      }

      for (ClusterShardNodeInfo nodeInfo : shardInfo.getNodes()) {
        assertNotNull(nodeInfo.getId());
        assertNotNull(nodeInfo.getEndpoint());
        assertNotNull(nodeInfo.getIp());
        assertNull(nodeInfo.getHostname());
        assertNotNull(nodeInfo.getPort());
        assertNotNull(nodeInfo.getTlsPort()); // currently we are always starting Redis server with `tls-port`
        assertNotNull(nodeInfo.getRole());
        assertNotNull(nodeInfo.getReplicationOffset());
        assertNotNull(nodeInfo.getHealth());
      }
    }
    node1.clusterDelSlots(3100, 3101, 3102, 3105);
  }

  @Test
  @SinceRedisVersion("7.0.0")
  public void clusterLinks() throws InterruptedException {
    List<Map<String, Object>> links = node1.clusterLinks();
    assertNotNull(links);
    assertEquals(0, links.size());
  }

  @Test
  public void testClusterKeySlot() {
    // It assumes JedisClusterCRC16 is correctly implemented
    assertEquals(JedisClusterCRC16.getSlot("{user1000}.following"),
      node1.clusterKeySlot("{user1000}.following"));
    assertEquals(JedisClusterCRC16.getSlot("foo{bar}{zap}"),
        node1.clusterKeySlot("foo{bar}{zap}"));
    assertEquals(JedisClusterCRC16.getSlot("foo{}{bar}"),
        node1.clusterKeySlot("foo{}{bar}"));
    assertEquals(JedisClusterCRC16.getSlot("foo{{bar}}zap"),
        node1.clusterKeySlot("foo{{bar}}zap"));
  }

  @Test
  public void clusterCountFailureReports() {
    assertEquals(0, node1.clusterCountFailureReports(node1.clusterMyId()));
  }

  @Test
  public void clusterMyId() {
    MatcherAssert.assertThat(node1.clusterMyId(), Matchers.not(Matchers.isEmptyOrNullString()));
  }

  @Test
  @SinceRedisVersion("7.2.0")
  public void clusterMyShardId() {
    MatcherAssert.assertThat(node1.clusterMyShardId(), Matchers.not(Matchers.isEmptyOrNullString()));
  }

  @Test
  public void testClusterEpoch() {
    try {
      assertEquals("OK", node1.clusterSetConfigEpoch(1));
    } catch (JedisDataException jde) {
      assertEquals("ERR The user can assign a config epoch only when the node does not know any other node.", jde.getMessage());
    }
  }

  @Test
  public void ClusterBumpEpoch() {
    MatcherAssert.assertThat(node1.clusterBumpEpoch(), Matchers.matchesPattern("^BUMPED|STILL [0-9]+$"));
  }

}
