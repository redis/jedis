package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.ClusterReset;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterPipeline;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.utils.JedisClusterTestUtil;

public class JedisClusterPipelineTest {
  private static Jedis node1;
  private static Jedis node2;
  private static Jedis node3;
  private static Jedis node4;
  private static Jedis nodeSlave2;

  private static final int DEFAULT_TIMEOUT = 2000;
  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final JedisPoolConfig DEFAULT_CONFIG = new JedisPoolConfig();

  private HostAndPort nodeInfo1 = HostAndPortUtil.getClusterServers().get(0);
  private HostAndPort nodeInfo2 = HostAndPortUtil.getClusterServers().get(1);
  private HostAndPort nodeInfo3 = HostAndPortUtil.getClusterServers().get(2);
  private HostAndPort nodeInfo4 = HostAndPortUtil.getClusterServers().get(3);
  private HostAndPort nodeInfoSlave2 = HostAndPortUtil.getClusterServers().get(4);
  protected Logger log = Logger.getLogger(getClass().getName());

  @Before
  public void setUp() throws Exception {
    node1 = new Jedis(nodeInfo1.getHost(), nodeInfo1.getPort());
    node1.auth("cluster");
    node1.flushAll();

    node2 = new Jedis(nodeInfo2.getHost(), nodeInfo2.getPort());
    node2.auth("cluster");
    node2.flushAll();

    node3 = new Jedis(nodeInfo3.getHost(), nodeInfo3.getPort());
    node3.auth("cluster");
    node3.flushAll();

    node4 = new Jedis(nodeInfo4.getHost(), nodeInfo4.getPort());
    node4.auth("cluster");
    node4.flushAll();

    nodeSlave2 = new Jedis(nodeInfoSlave2.getHost(), nodeInfoSlave2.getPort());
    nodeSlave2.auth("cluster");
    nodeSlave2.flushAll();
    // ---- configure cluster

    // add nodes to cluster
    node1.clusterMeet("127.0.0.1", nodeInfo2.getPort());
    node1.clusterMeet("127.0.0.1", nodeInfo3.getPort());

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
    node1.clusterReset(ClusterReset.SOFT);
    node2.clusterReset(ClusterReset.SOFT);
    node3.clusterReset(ClusterReset.SOFT);
    node4.clusterReset(ClusterReset.SOFT);
  }

  @After
  public void tearDown() throws InterruptedException {
    cleanUp();
  }

  @Test
  public void pipeline() throws UnsupportedEncodingException {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    String clientName = "myAppName";
    JedisCluster jedis = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", clientName, DEFAULT_CONFIG);

    try {
      JedisClusterPipeline p = jedis.pipelined();
      p.set("foo", "bar");
      p.get("foo");
      List<Object> results = p.syncAndReturnAll();

      assertEquals(2, results.size());
      assertEquals("OK", results.get(0));
      assertEquals("bar", results.get(1));
    } finally {
      jedis.close();
    }
  }

  @Test
  public void pipelineResponse() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    String clientName = "myAppName";
    JedisCluster jedis = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", clientName, DEFAULT_CONFIG);

    try {
      jedis.set("string", "foo");
      jedis.lpush("list", "foo");
      jedis.hset("hash", "foo", "bar");
      jedis.zadd("zset", 1, "foo");
      jedis.sadd("set", "foo");

      JedisClusterPipeline p = jedis.pipelined();
      Response<String> string = p.get("string");
      Response<Long> del = p.del("string");
      Response<String> emptyString = p.get("string");
      Response<String> list = p.lpop("list");
      Response<String> hash = p.hget("hash", "foo");
      Response<Set<String>> zset = p.zrange("zset", 0, -1);
      Response<String> set = p.spop("set");
      Response<Boolean> blist = p.exists("list");
      Response<Double> zincrby = p.zincrby("zset", 1, "foo");
      Response<Long> zcard = p.zcard("zset");
      p.lpush("list", "bar");
      Response<List<String>> lrange = p.lrange("list", 0, -1);
      Response<Map<String, String>> hgetAll = p.hgetAll("hash");
      p.sadd("set", "foo");
      Response<Set<String>> smembers = p.smembers("set");
      Response<Set<Tuple>> zrangeWithScores = p.zrangeWithScores("zset", 0, -1);
      p.sync();

      assertEquals("foo", string.get());
      assertEquals(Long.valueOf(1), del.get());
      assertNull(emptyString.get());
      assertEquals("foo", list.get());
      assertEquals("bar", hash.get());
      assertEquals("foo", zset.get().iterator().next());
      assertEquals("foo", set.get());
      assertFalse(blist.get());
      assertEquals(Double.valueOf(2), zincrby.get());
      assertEquals(Long.valueOf(1), zcard.get());
      assertEquals(1, lrange.get().size());
      assertNotNull(hgetAll.get().get("foo"));
      assertEquals(1, smembers.get().size());
      assertEquals(1, zrangeWithScores.get().size());
    } finally {
      jedis.close();
    }
  }

  @Test(expected = JedisDataException.class)
  public void pipelineResponseWithinPipeline() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    String clientName = "myAppName";
    JedisCluster jedis = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", clientName, DEFAULT_CONFIG);

    try {
      jedis.set("string", "foo");

      JedisClusterPipeline p = jedis.pipelined();
      Response<String> string = p.get("string");
      string.get();
      p.sync();
    } finally {
      jedis.close();
    }
  }

  @Test
  public void canRetrieveUnsetKey() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    String clientName = "myAppName";
    JedisCluster jedis = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", clientName, DEFAULT_CONFIG);

    try {
      JedisClusterPipeline p = jedis.pipelined();
      Response<String> shouldNotExist = p.get(UUID.randomUUID().toString());
      p.sync();
      assertNull(shouldNotExist.get());
    } finally {
      jedis.close();
    }
  }

  @Test
  public void testSyncWithNoCommandQueued() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    String clientName = "myAppName";
    JedisCluster jedis = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", clientName, DEFAULT_CONFIG);

    try {
      JedisClusterPipeline pipeline = jedis.pipelined();
      pipeline.sync();

      pipeline = jedis.pipelined();
      List<Object> resp = pipeline.syncAndReturnAll();
      assertTrue(resp.isEmpty());
    } finally {
      jedis.close();
    }
  }

  /**
   * slot->nodes 15363 node3 e
   */
  @Test
  public void testMigrating() {
    log.info("test migrate slot");
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(nodeInfo1);
    JedisCluster jedis = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_CONFIG);

    try {
      JedisClusterPipeline pipeline = jedis.pipelined();

      // Ensure the key "e" does not exist
      jedis.del("e");

      String node3Id = JedisClusterTestUtil.getNodeId(node3.clusterNodes());
      String node2Id = JedisClusterTestUtil.getNodeId(node2.clusterNodes());
      node3.clusterSetSlotMigrating(15363, node2Id);
      node2.clusterSetSlotImporting(15363, node3Id);

      pipeline.set("e", "e1");
      pipeline.sync();
      assertEquals(1, pipeline.getCounterOfAsking());

      Response<String> e1Response = pipeline.get("e");
      pipeline.sync();
      assertEquals("e1", e1Response.get());
      assertEquals(1, pipeline.getCounterOfAsking());

      pipeline.del("e");
      pipeline.sync();
      assertEquals(1, pipeline.getCounterOfAsking());

      // Ensure the key "e" does not exist
      jedis.del("e");

      node2.clusterSetSlotNode(15363, node2Id);
      node3.clusterSetSlotNode(15363, node2Id);

      pipeline.set("e", "e2");
      pipeline.sync();
      assertEquals(1, pipeline.getCounterOfMoving());
      assertEquals(0, pipeline.getCounterOfAsking());

      // Then the slot mapping cache has been updated
      Response<String> e2Response = pipeline.get("e");
      pipeline.sync();
      assertEquals("e2", e2Response.get());
      assertEquals(0, pipeline.getCounterOfMoving());
      assertEquals(0, pipeline.getCounterOfAsking());
    } finally {
      jedis.close();
    }
  }

}
