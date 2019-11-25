package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static redis.clients.jedis.Protocol.Command.*;

import java.util.ArrayList;
import java.util.HashMap;
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
import redis.clients.jedis.tests.HostAndPortUtil;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.SafeEncoder;

public class ClusterBinaryJedisCommandsTest {
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
  @Test
  public void testBinaryGetAndSet() {
    byte[] byteKey = "foo".getBytes();
    byte[] byteValue = "2".getBytes();
    jedisCluster.set(byteKey, byteValue);
    assertArrayEquals(byteValue, jedisCluster.get(byteKey));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testIncr() {
    byte[] byteKey = "foo".getBytes();
    byte[] byteValue = "2".getBytes();
    jedisCluster.set(byteKey, byteValue);
    jedisCluster.incr(byteKey);
    assertArrayEquals("3".getBytes(), jedisCluster.get(byteKey));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSadd() {
    byte[] byteKey = "languages".getBytes();
    byte[] firstLanguage = "java".getBytes();
    byte[] secondLanguage = "python".getBytes();
    byte[][] listLanguages = { firstLanguage, secondLanguage };
    jedisCluster.sadd(byteKey, listLanguages);
    Set<byte[]> setLanguages = jedisCluster.smembers(byteKey);
    List<String> languages = new ArrayList<>();
    for (byte[] language : setLanguages) {
      languages.add(new String(language));
    }
    assertTrue(languages.contains("java"));
    assertTrue(languages.contains("python"));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testHmset() {
    byte[] key = "jedis".getBytes();
    byte[] field = "language".getBytes();
    byte[] value = "java".getBytes();
    HashMap<byte[], byte[]> map = new HashMap();
    map.put(field, value);
    jedisCluster.hmset(key, map);
    List<byte[]> listResults = jedisCluster.hmget(key, field);
    for (byte[] result : listResults) {
      assertArrayEquals(value, result);
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testRpush() {
    byte[] value1 = "value1".getBytes();
    byte[] value2 = "value2".getBytes();
    byte[] key = "key1".getBytes();
    jedisCluster.del(key);
    jedisCluster.rpush(key, value1);
    jedisCluster.rpush(key, value2);
    assertEquals(2, (long) jedisCluster.llen(key));
  }

  @Test
  public void testKeys() {
    assertEquals(0, jedisCluster.keys("{f}o*".getBytes()).size());
    jedisCluster.set("{f}oo1".getBytes(), "bar".getBytes());
    jedisCluster.set("{f}oo2".getBytes(), "bar".getBytes());
    jedisCluster.set("{f}oo3".getBytes(), "bar".getBytes());
    assertEquals(3, jedisCluster.keys("{f}o*".getBytes()).size());
  }

  @Test
  public void testBinaryGeneralCommand(){
    byte[] key = "x".getBytes();
    byte[] value = "1".getBytes();
    jedisCluster.sendCommand("z".getBytes(), SET, key, value);
    jedisCluster.sendCommand("y".getBytes(), INCR, key);
    Object returnObj = jedisCluster.sendCommand("w".getBytes(), GET, key);
    assertEquals("2", SafeEncoder.encode((byte[])returnObj));
  }

  @Test
  public void testGeneralCommand(){
    jedisCluster.sendCommand("z", SET, "x", "1");
    jedisCluster.sendCommand("y", INCR, "x");
    Object returnObj = jedisCluster.sendCommand("w", GET, "x");
    assertEquals("2", SafeEncoder.encode((byte[])returnObj));
  }


  @Test(expected = IllegalArgumentException.class)
  public void failKeys() {
    jedisCluster.keys("*".getBytes());
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
