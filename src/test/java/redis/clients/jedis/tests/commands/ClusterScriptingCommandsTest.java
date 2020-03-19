package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.jedis.util.JedisClusterCRC16;

public class ClusterScriptingCommandsTest {
  private Jedis node1;
  private static Jedis node2;
  private static Jedis node3;

  private HostAndPort nodeInfo1 = HostAndPortUtil.getClusterServers().get(0);
  private HostAndPort nodeInfo2 = HostAndPortUtil.getClusterServers().get(1);
  private HostAndPort nodeInfo3 = HostAndPortUtil.getClusterServers().get(2);
  private final Set<HostAndPort> jedisClusterNode = new HashSet<>();
  JedisCluster jedisCluster;

  @Before
  public void setUp() throws InterruptedException {
    node1 = new Jedis(nodeInfo1);
    node1.auth("cluster");
    node1.flushAll();

    node2 = new Jedis(nodeInfo2);
    node2.auth("cluster");
    node2.flushAll();

    node3 = new Jedis(nodeInfo3);
    node3.auth("cluster");
    node3.flushAll();

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

    waitForClusterReady();

    jedisClusterNode.add(new HostAndPort("127.0.0.1", 7379));
    jedisCluster = new JedisCluster(jedisClusterNode, 2000, 2000, 5, "cluster", new JedisPoolConfig());

  }

  @AfterClass
  public static void cleanUp() {
    int slotTest = JedisClusterCRC16.getSlot("test");
    int slot51 = JedisClusterCRC16.getSlot("51");
    String node3Id = getNodeId(node3.clusterNodes());
    node2.clusterSetSlotNode(slotTest, node3Id);
    node2.clusterSetSlotNode(slot51, node3Id);
    node2.clusterDelSlots(slotTest, slot51);
  }

  @After
  public void tearDown() {
    // clear all slots
    int[] slotsToDelete = new int[JedisCluster.HASHSLOTS];
    for (int i = 0; i < JedisCluster.HASHSLOTS; i++) {
      slotsToDelete[i] = i;
    }
    node1.clusterDelSlots(slotsToDelete);
    node2.clusterDelSlots(slotsToDelete);
    node3.clusterDelSlots(slotsToDelete);
  }

  @SuppressWarnings("unchecked")
  @Test(expected = JedisClusterOperationException.class)
  public void testJedisClusterException() {
    String script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2],ARGV[3]}";
    List<String> keys = new ArrayList<>();
    keys.add("key1");
    keys.add("key2");
    List<String> args = new ArrayList<>();
    args.add("first");
    args.add("second");
    args.add("third");
    jedisCluster.eval(script, keys, args);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testEval2() {
    String script = "return redis.call('set',KEYS[1],'bar')";
    int numKeys = 1;
    String[] args = { "foo" };
    jedisCluster.eval(script, numKeys, args);
    assertEquals("bar", jedisCluster.get("foo"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testScriptLoadAndScriptExists() {
    String sha1 = jedisCluster.scriptLoad("return redis.call('get','foo')", "key1");
    assertTrue(jedisCluster.scriptExists(sha1, "key1"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testEvalsha() {
    String sha1 = jedisCluster.scriptLoad("return 10", "key1");
    Object o = jedisCluster.evalsha(sha1, 1, "key1");
    assertEquals("10", o.toString());
  }

  @SuppressWarnings("unchecked")
  @Test(expected = JedisClusterOperationException.class)
  public void testJedisClusterException2() {
    byte[] script = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2],ARGV[3]}".getBytes();
    List<byte[]> keys = new ArrayList<byte[]>();
    keys.add("key1".getBytes());
    keys.add("key2".getBytes());
    List<byte[]> args = new ArrayList<byte[]>();
    args.add("first".getBytes());
    args.add("second".getBytes());
    args.add("third".getBytes());
    jedisCluster.eval(script, keys, args);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBinaryEval() {
    byte[] script = "return redis.call('set',KEYS[1],'bar')".getBytes();
    byte[] args = "foo".getBytes();
    jedisCluster.eval(script, 1, args);
    assertEquals("bar", jedisCluster.get("foo"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBinaryScriptFlush() {
    byte[] byteKey = "key1".getBytes();
    jedisCluster.scriptLoad("return redis.call('get','foo')".getBytes(), byteKey);
    assertEquals("OK", jedisCluster.scriptFlush(byteKey));
  }

  @SuppressWarnings("unchecked")
  @Test(expected = JedisDataException.class)
  public void testBinaryScriptKill() {
    byte[] byteKey = "key1".getBytes();
    jedisCluster.scriptKill(byteKey);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBinaryScriptExists() {
    byte[] byteKey = "key1".getBytes();
    byte[] sha1 = jedisCluster.scriptLoad("return redis.call('get','foo')".getBytes(), byteKey);
    byte[][] arraySha1 = { sha1 };
    Long result = 1L;
    List<Long> listResult = new ArrayList<>();
    listResult.add(result);
    assertEquals(listResult, jedisCluster.scriptExists(byteKey, arraySha1));
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
