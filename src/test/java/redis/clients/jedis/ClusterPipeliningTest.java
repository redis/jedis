package redis.clients.jedis;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static redis.clients.jedis.Protocol.CLUSTER_HASHSLOTS;

import java.util.*;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.args.*;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.*;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.resps.GeoRadiusResponse;
import redis.clients.jedis.resps.StreamEntry;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.JedisClusterTestUtil;
import redis.clients.jedis.util.SafeEncoder;

public class ClusterPipeliningTest {

  private static final String LOCAL_IP = "127.0.0.1";
  private static final int DEFAULT_TIMEOUT = 2000;
  private static final int DEFAULT_REDIRECTIONS = 5;

  private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();
  private static final DefaultJedisClientConfig DEFAULT_CLIENT_CONFIG
      = DefaultJedisClientConfig.builder().password("cluster").build();

  private static Jedis node1;
  private static Jedis node2;
  private static Jedis node3;

  private HostAndPort nodeInfo1 = HostAndPorts.getClusterServers().get(0);
  private HostAndPort nodeInfo2 = HostAndPorts.getClusterServers().get(1);
  private HostAndPort nodeInfo3 = HostAndPorts.getClusterServers().get(2);
  private Set<HostAndPort> nodes = new HashSet<>(Arrays.asList(nodeInfo1, nodeInfo2, nodeInfo3));

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

    // add nodes to cluster
    node1.clusterMeet(LOCAL_IP, nodeInfo2.getPort());
    node1.clusterMeet(LOCAL_IP, nodeInfo3.getPort());

    // split available slots across the three nodes
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

    JedisClusterTestUtil.waitForClusterReady(node1, node2, node3);
  }

  @AfterClass
  public static void cleanUp() {
    node1.flushDB();
    node2.flushDB();
    node3.flushDB();
    node1.clusterReset(ClusterResetType.SOFT);
    node2.clusterReset(ClusterResetType.SOFT);
    node3.clusterReset(ClusterResetType.SOFT);
  }

  @After
  public void tearDown() throws InterruptedException {
    cleanUp();
  }

  @Test
  public void clusterPipelineSync() {
    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline clusterPipeline = new ClusterPipeline(provider);

      Response<String> r1 = clusterPipeline.set("key1", "value1");
      Response<String> r2 = clusterPipeline.set("key2", "value2");
      Response<String> r3 = clusterPipeline.set("key3", "value3");
      Response<String> r4 = clusterPipeline.get("key1");
      Response<String> r5 = clusterPipeline.get("key2");
      Response<String> r6 = clusterPipeline.get("key3");

      clusterPipeline.sync();
      Assert.assertEquals("OK", r1.get());
      Assert.assertEquals("OK", r2.get());
      Assert.assertEquals("OK", r3.get());
      Assert.assertEquals("value1", r4.get());
      Assert.assertEquals("value2", r5.get());
      Assert.assertEquals("value3", r6.get());
    }
  }

  @Test
  public void constructorClientConfig() {
    try (ClusterPipeline pipe = new ClusterPipeline(nodes, DEFAULT_CLIENT_CONFIG)) {
      Response<String> r1 = pipe.set("key1", "value1");
      Response<String> r2 = pipe.set("key2", "value2");
      Response<String> r3 = pipe.set("key3", "value3");
      Response<String> r4 = pipe.get("key1");
      Response<String> r5 = pipe.get("key2");
      Response<String> r6 = pipe.get("key3");

      pipe.sync();
      Assert.assertEquals("OK", r1.get());
      Assert.assertEquals("OK", r2.get());
      Assert.assertEquals("OK", r3.get());
      Assert.assertEquals("value1", r4.get());
      Assert.assertEquals("value2", r5.get());
      Assert.assertEquals("value3", r6.get());
    }
  }

  @Test
  public void constructorPoolConfig() {
    try (ClusterPipeline pipe = new ClusterPipeline(nodes, DEFAULT_CLIENT_CONFIG, DEFAULT_POOL_CONFIG)) {
      Response<String> r1 = pipe.set("key1", "value1");
      Response<String> r2 = pipe.set("key2", "value2");
      Response<String> r3 = pipe.set("key3", "value3");
      Response<String> r4 = pipe.get("key1");
      Response<String> r5 = pipe.get("key2");
      Response<String> r6 = pipe.get("key3");

      pipe.sync();
      Assert.assertEquals("OK", r1.get());
      Assert.assertEquals("OK", r2.get());
      Assert.assertEquals("OK", r3.get());
      Assert.assertEquals("value1", r4.get());
      Assert.assertEquals("value2", r5.get());
      Assert.assertEquals("value3", r6.get());
    }
  }

  @Test
  public void pipelineResponse() {
    try (JedisCluster jc = new JedisCluster(nodes, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      jc.set("string", "foo");
      jc.lpush("list", "foo");
      jc.hset("hash", "foo", "bar");
      jc.zadd("zset", 1, "foo");
      jc.sadd("set", "foo");
      jc.setrange("setrange", 0, "0123456789");
      byte[] bytesForSetRange = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
      jc.setrange("setrangebytes".getBytes(), 0, bytesForSetRange);
    }

    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);

      Response<String> string = p.get("string");
      Response<String> list = p.lpop("list");
      Response<String> hash = p.hget("hash", "foo");
      Response<List<String>> zset = p.zrange("zset", 0, -1);
      Response<String> set = p.spop("set");
      Response<Boolean> blist = p.exists("list");
      Response<Double> zincrby = p.zincrby("zset", 1, "foo");
      Response<Long> zcard = p.zcard("zset");
      p.lpush("list", "bar");
      Response<List<String>> lrange = p.lrange("list", 0, -1);
      Response<Map<String, String>> hgetAll = p.hgetAll("hash");
      p.sadd("set", "foo");
      Response<Set<String>> smembers = p.smembers("set");
      Response<List<Tuple>> zrangeWithScores = p.zrangeWithScores("zset", 0, -1);
      Response<String> getrange = p.getrange("setrange", 1, 3);
      Response<byte[]> getrangeBytes = p.getrange("setrangebytes".getBytes(), 6, 8);
      p.sync();

      assertEquals("foo", string.get());
      assertEquals("foo", list.get());
      assertEquals("bar", hash.get());
      assertEquals("foo", zset.get().iterator().next());
      assertEquals("foo", set.get());
      assertEquals(false, blist.get());
      assertEquals(Double.valueOf(2), zincrby.get());
      assertEquals(Long.valueOf(1), zcard.get());
      assertEquals(1, lrange.get().size());
      assertNotNull(hgetAll.get().get("foo"));
      assertEquals(1, smembers.get().size());
      assertEquals(1, zrangeWithScores.get().size());
      assertEquals("123", getrange.get());
      byte[] expectedGetRangeBytes = {6, 7, 8};
      assertArrayEquals(expectedGetRangeBytes, getrangeBytes.get());
    }
  }

  @Test
  public void pipelineBinarySafeHashCommands() {
    try (JedisCluster jc = new JedisCluster(nodes, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      jc.hset("key".getBytes(), "f1".getBytes(), "v111".getBytes());
      jc.hset("key".getBytes(), "f22".getBytes(), "v2222".getBytes());
    }

    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);
      Response<Map<byte[], byte[]>> fmap = p.hgetAll("key".getBytes());
      Response<Set<byte[]>> fkeys = p.hkeys("key".getBytes());
      Response<List<byte[]>> fordered = p.hmget("key".getBytes(), "f22".getBytes(), "f1".getBytes());
      Response<List<byte[]>> fvals = p.hvals("key".getBytes());
      p.sync();

      assertNotNull(fmap.get());
      // we have to do these strange contortions because byte[] is not a very good key for a java
      // Map. It only works with equality (you need the exact key object to retrieve the value).
      // I recommend we switch to using ByteBuffer or something similar:
      // http://stackoverflow.com/questions/1058149/using-a-byte-array-as-hashmap-key-java
      Map<byte[], byte[]> map = fmap.get();
      Set<byte[]> mapKeys = map.keySet();
      Iterator<byte[]> iterMap = mapKeys.iterator();
      byte[] firstMapKey = iterMap.next();
      byte[] secondMapKey = iterMap.next();
      assertFalse(iterMap.hasNext());
      verifyHasBothValues(firstMapKey, secondMapKey, "f1".getBytes(), "f22".getBytes());
      byte[] firstMapValue = map.get(firstMapKey);
      byte[] secondMapValue = map.get(secondMapKey);
      verifyHasBothValues(firstMapValue, secondMapValue, "v111".getBytes(), "v2222".getBytes());

      assertNotNull(fkeys.get());
      Iterator<byte[]> iter = fkeys.get().iterator();
      byte[] firstKey = iter.next();
      byte[] secondKey = iter.next();
      assertFalse(iter.hasNext());
      verifyHasBothValues(firstKey, secondKey, "f1".getBytes(), "f22".getBytes());

      assertNotNull(fordered.get());
      assertArrayEquals("v2222".getBytes(), fordered.get().get(0));
      assertArrayEquals("v111".getBytes(), fordered.get().get(1));

      assertNotNull(fvals.get());
      assertEquals(2, fvals.get().size());
      byte[] firstValue = fvals.get().get(0);
      byte[] secondValue = fvals.get().get(1);
      verifyHasBothValues(firstValue, secondValue, "v111".getBytes(), "v2222".getBytes());
    }
  }

  private void verifyHasBothValues(byte[] firstKey, byte[] secondKey, byte[] value1, byte[] value2) {
    assertFalse(Arrays.equals(firstKey, secondKey));
    assertTrue(Arrays.equals(firstKey, value1) || Arrays.equals(firstKey, value2));
    assertTrue(Arrays.equals(secondKey, value1) || Arrays.equals(secondKey, value2));
  }

  @Test(expected = IllegalStateException.class)
  public void pipelineResponseWithinPipeline() {
    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);
      Response<String> string = p.get("string");
      string.get();
      p.sync();
    }
  }

  @Test
  public void pipelineWithPubSub() {
    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline pipelined = new ClusterPipeline(provider);
      Response<Long> p1 = pipelined.publish("foo", "bar");
      Response<Long> p2 = pipelined.publish("foo".getBytes(), "bar".getBytes());
      pipelined.sync();
      assertEquals(0, p1.get().longValue());
      assertEquals(0, p2.get().longValue());
    }
  }

  @Test
  public void canRetrieveUnsetKey() {
    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);
      Response<String> shouldNotExist = p.get(UUID.randomUUID().toString());
      p.sync();
      assertNull(shouldNotExist.get());
    }
  }

  @Test
  public void piplineWithError() {
    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);
      p.set("foo", "bar");
      Response<Set<String>> error = p.smembers("foo");
      Response<String> r = p.get("foo");
      p.sync();
      try {
        error.get();
        fail();
      } catch (JedisDataException e) {
        // that is fine we should be here
      }
      assertEquals(r.get(), "bar");
    }
  }

  @Test
  public void getSetParams() {
    ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG);
    ClusterPipeline p = new ClusterPipeline(provider);

    Response<String> r1 = p.set("key1", "value1");
    Response<String> r2 = p.set("key2", "value2");
    Response<String> r3 = p.set("key3", "value3");
    Response<String> r4 = p.set("key3", "value4", new SetParams().nx()); // Should not be updated
    Response<String> r5 = p.get("key1");
    Response<String> r6 = p.get("key2");
    Response<String> r7 = p.get("key3");

    p.sync();
    Assert.assertEquals("OK", r1.get());
    Assert.assertEquals("OK", r2.get());
    Assert.assertEquals("OK", r3.get());
    Assert.assertNull(r4.get());
    Assert.assertEquals("value1", r5.get());
    Assert.assertEquals("value2", r6.get());
    Assert.assertEquals("value3", r7.get());
  }

  @Test
  public void clusterPipelineSort() {
    List<String> sorted = new ArrayList<>();
    sorted.add("1");
    sorted.add("2");
    sorted.add("3");
    sorted.add("4");
    sorted.add("5");

    ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG);
    ClusterPipeline p = new ClusterPipeline(provider);

    Response<Long> r1 = p.rpush("key1", "2", "3", "5", "1", "4");
    Response<List<String>> r2 = p.sort("key1");
    Response<Long> r3 = p.sort("key1", "key1");
    Response<List<String>> r4 = p.lrange("key1", 0, 4);
    Response<List<String>> r5 = p.sort("key1", new SortingParams().limit(0, 2));
    Response<Long> r6 = p.sort("key1", new SortingParams().desc(), "key1");
    Response<List<String>> r7 = p.lrange("key1", 0, 4);

    p.sync();
    Assert.assertEquals(Long.valueOf(5), r1.get());
    Assert.assertEquals(sorted, r2.get());
    Assert.assertEquals(Long.valueOf(5), r3.get());
    Assert.assertEquals(sorted, r4.get());
    Assert.assertEquals(2, r5.get().size());
    Assert.assertEquals(Long.valueOf(5), r6.get());
    Collections.reverse(sorted);
    Assert.assertEquals(sorted, r7.get());
  }

  @Test
  public void clusterPipelineList() {
    List<String> vals = new ArrayList<>();
    vals.add("foobar");

    ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG);
    ClusterPipeline p = new ClusterPipeline(provider);

    Response<Long> r1 = p.lpush("my{list}", "hello", "hello", "foo", "foo"); // ["foo", "foo", "hello", "hello"]
    Response<Long> r2 = p.rpush("my{newlist}", "hello", "hello", "foo", "foo");  // ["hello", "hello", "foo", "foo"]
    Response<Long> r3 = p.lpos("my{list}", "foo");
    Response<Long> r4 = p.lpos("my{list}", "foo", new LPosParams().maxlen(1));
    Response<List<Long>> r5 = p.lpos("my{list}", "foo", new LPosParams().maxlen(1), 2);
    Response<String> r6 = p.ltrim("my{list}", 2, 3); // ["hello", "hello"]
    Response<Long> r7 = p.llen("my{list}");
    Response<String> r8 = p.lindex("my{list}", -1);
    Response<String> r9 = p.lset("my{list}", 1, "foobar"); // ["hello", "foobar"]
    Response<Long> r10 = p.lrem("my{list}", 1, "hello"); // ["foobar"]
    Response<List<String>> r11 = p.lrange("my{list}", 0, 10);
    Response<String> r12 = p.rpop("my{newlist}"); // ["hello", "hello", "foo"]
    Response<List<String>> r13 = p.lpop("my{list}", 1); // ["foobar"]
    Response<List<String>> r14 = p.rpop("my{newlist}", 2); // ["hello"]
    Response<Long> r15 = p.linsert("my{newlist}", ListPosition.AFTER, "hello", "world"); // ["hello", "world"]
    Response<Long> r16 = p.lpushx("myother{newlist}", "foo", "bar");
    Response<Long> r17 = p.rpushx("myother{newlist}", "foo", "bar");
    Response<String> r18 = p.rpoplpush("my{newlist}", "myother{newlist}");
    Response<String> r19 = p.lmove("my{newlist}", "myother{newlist}", ListDirection.LEFT, ListDirection.RIGHT);

    p.sync();
    Assert.assertEquals(Long.valueOf(4), r1.get());
    Assert.assertEquals(Long.valueOf(4), r2.get());
    Assert.assertEquals(Long.valueOf(0), r3.get());
    Assert.assertEquals(Long.valueOf(0), r4.get());
    Assert.assertEquals(1, r5.get().size());
    Assert.assertEquals("OK", r6.get());
    Assert.assertEquals(Long.valueOf(2), r7.get());
    Assert.assertEquals("hello", r8.get());
    Assert.assertEquals("OK", r9.get());
    Assert.assertEquals(Long.valueOf(1), r10.get());
    Assert.assertEquals(vals, r11.get());
    Assert.assertEquals("foo", r12.get());
    Assert.assertEquals(vals, r13.get());
    Assert.assertEquals(2, r14.get().size());
    Assert.assertEquals(Long.valueOf(2), r15.get());
    Assert.assertEquals(Long.valueOf(0), r16.get());
    Assert.assertEquals(Long.valueOf(0), r17.get());
    Assert.assertEquals("world", r18.get());
    Assert.assertEquals("hello", r19.get());
  }

  @Test
  public void clusterPipelineSet() {
    Set<String> diff = new HashSet<>();
    diff.add("bar");
    diff.add("foo");

    Set<String> union = new HashSet<>();
    union.add("hello");
    union.add("world");
    union.add("bar");
    union.add("foo");

    Set<String> inter = new HashSet<>();
    inter.add("world");
    inter.add("hello");

    ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG);
    ClusterPipeline p = new ClusterPipeline(provider);

    Response<Long> r1 = p.sadd("my{set}", "hello", "hello", "world", "foo", "bar");
    p.sadd("mynew{set}", "hello", "hello", "world");
    Response<Set<String>> r2 = p.sdiff("my{set}", "mynew{set}");
    Response<Long> r3 = p.sdiffstore("diffset{set}", "my{set}", "mynew{set}");
    Response<Set<String>> r4 = p.smembers("diffset{set}");
    Response<Set<String>> r5 = p.sinter("my{set}", "mynew{set}");
    Response<Long> r6 = p.sinterstore("interset{set}", "my{set}", "mynew{set}");
    Response<Set<String>> r7 = p.smembers("interset{set}");
    Response<Set<String>> r8 = p.sunion("my{set}", "mynew{set}");
    Response<Long> r9 = p.sunionstore("unionset{set}", "my{set}", "mynew{set}");
    Response<Set<String>> r10 = p.smembers("unionset{set}");
    Response<Boolean> r11 = p.sismember("my{set}", "foo");
    Response<List<Boolean>> r12 = p.smismember("my{set}", "foo", "foobar");
    Response<Long> r13 = p.srem("my{set}", "foo");
    Response<Set<String>> r14 = p.spop("my{set}", 1);
    Response<Long> r15 = p.scard("my{set}");
    Response<String> r16 = p.srandmember("my{set}");
    Response<List<String>> r17 = p.srandmember("my{set}", 2);
//    Response<Long> r18 = p.smove("my{set}", "mynew{set}", "hello");

    p.sync();
    Assert.assertEquals(Long.valueOf(4), r1.get());
    Assert.assertEquals(diff, r2.get());
    Assert.assertEquals(Long.valueOf(diff.size()), r3.get());
    Assert.assertEquals(diff, r4.get());
    Assert.assertEquals(inter, r5.get());
    Assert.assertEquals(Long.valueOf(inter.size()), r6.get());
    Assert.assertEquals(inter, r7.get());
    Assert.assertEquals(union, r8.get());
    Assert.assertEquals(Long.valueOf(union.size()), r9.get());
    Assert.assertEquals(union, r10.get());
    Assert.assertTrue(r11.get());
    Assert.assertTrue(r12.get().get(0) && !r12.get().get(1));
    Assert.assertEquals(Long.valueOf(1), r13.get());
    Assert.assertTrue(union.containsAll(r14.get()));
    Assert.assertEquals(Long.valueOf(2), r15.get());
    Assert.assertTrue(union.contains(r16.get()));
    Assert.assertTrue(union.containsAll(r17.get()));
//    Assert.assertEquals(Long.valueOf(1), r18.get());
  }

  @Test
  public void clusterPipelineSortedSet() {
    Map<String, Double> hm = new HashMap<>();
    hm.put("a1", 1d);
    hm.put("a2", 2d);
    hm.put("a3", 3d);

    Set<String> members = new HashSet<>(hm.keySet());

    Tuple max = new Tuple("a3", 3d);
    Tuple min = new Tuple("a1", 1d);

    ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG);
    ClusterPipeline p = new ClusterPipeline(provider);

    Response<Long> r1 = p.zadd("myset", hm);
    Response<Long> r2 = p.zrank("myset", "a3");
    Response<Long> r3 = p.zrevrank("myset", "a3");
    Response<Long> r4 = p.zrem("myset", "a1");
    Response<Long> r5 = p.zadd("myset", 1d, "a1");
    Response<Long> r6 = p.zadd("myotherset", 2d, "a1", new ZAddParams().nx());
    Response<Double> r7 = p.zaddIncr("myset", 3d, "a4", new ZAddParams().xx()); // Should not update
    Response<List<String>> r8 = p.zrevrange("myset", 0, 0);
    Response<List<Tuple>> r9 = p.zrevrangeWithScores("myset", 0, 0);
    Response<String> r10 = p.zrandmember("myset");
    Response<List<String>> r11 = p.zrandmember("myset", 2);
    Response<List<Tuple>> r12 = p.zrandmemberWithScores("myset", 1);
    Response<Double> r13 = p.zscore("myset", "a1");
    Response<List<Double>> r14 = p.zmscore("myset", "a1", "a2");
    Response<Tuple> r15 = p.zpopmax("myset");
    Response<Tuple> r16 = p.zpopmin("myset");
    Response<Long> r17 = p.zcount("myotherset", 2, 5);
    Response<Long> r18 = p.zcount("myotherset", "(2", "5");
    p.zadd("myset", hm, new ZAddParams().nx()); // return the elements that were popped
    Response<List<Tuple>> r19 = p.zpopmax("myset", 2);
    Response<List<Tuple>> r20 = p.zpopmin("myset", 1);

    p.sync();
    Assert.assertEquals(Long.valueOf(3), r1.get());
    Assert.assertEquals(Long.valueOf(2), r2.get());
    Assert.assertEquals(Long.valueOf(0), r3.get());
    Assert.assertEquals(Long.valueOf(1), r4.get());
    Assert.assertEquals(Long.valueOf(1), r5.get());
    Assert.assertEquals(Long.valueOf(1), r6.get());
    Assert.assertNull(r7.get());
    Assert.assertTrue(r8.get().size() == 1 && r8.get().contains("a3"));
    Assert.assertTrue(r9.get().size() == 1 && r9.get().contains(max));
    Assert.assertTrue(members.contains(r10.get()));
    Assert.assertTrue(members.containsAll(r11.get()));
    assertEquals(1, r12.get().size());
    Assert.assertEquals(Double.valueOf(1), r13.get());
    Assert.assertTrue(hm.values().containsAll(r14.get()));
    Assert.assertEquals(max, r15.get());
    Assert.assertEquals(min, r16.get());
    Assert.assertEquals(Long.valueOf(1), r17.get());
    Assert.assertEquals(Long.valueOf(0), r18.get());
    Assert.assertTrue(r19.get().size() == 2 && r19.get().contains(max));
    Assert.assertTrue(r20.get().size() == 1 && r20.get().contains(min));
  }

  @Test
  public void clusterPipelineHash() {
    Map<String, String> hm = new HashMap<>();
    hm.put("field2", "2");
    hm.put("field3", "5");

    Set<String> keys = new HashSet<>();
    keys.add("field2");

    List<String> vals = new ArrayList<>();
    vals.add("3.5");

    List<String> vals2 = new ArrayList<>();
    vals2.add("hello");
    vals2.add(null);

    ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG);
    ClusterPipeline p = new ClusterPipeline(provider);

    Response<Long> r1 = p.hset("myhash", "field1", "hello");
    Response<Long> r2 = p.hsetnx("myhash", "field1", "hello");
    Response<String> r3 = p.hget("myhash", "field1");
    Response<Long> r4 = p.hset("myotherhash", hm);
    Response<String> r5 = p.hmset("mynewhash", hm);
    p.hincrBy("mynewhash", "field2", 1);
    Response<Double> r6 = p.hincrByFloat("mynewhash", "field2", 0.5);
    Response<Long> r7 = p.hlen("myhash");
    Response<Long> r8 = p.hdel("mynewhash", "field3");
    Response<Boolean> r9 = p.hexists("mynewhash", "field3");
    Response<Set<String>> r10 = p.hkeys("mynewhash");
    Response<List<String>> r11 = p.hvals("mynewhash");
    Response<List<String>> r12 = p.hmget("myhash", "field1", "field2");
    Response<String> r13 = p.hrandfield("myotherhash");
    Response<List<String>> r14 = p.hrandfield("myotherhash", 2);
    Response<Map<String, String>> r15 = p.hrandfieldWithValues("myotherhash", 2);
    Response<Long> r16 = p.hstrlen("myhash", "field1");

    p.sync();
    Assert.assertEquals(Long.valueOf(1), r1.get());
    Assert.assertEquals(Long.valueOf(0), r2.get());
    Assert.assertEquals("hello", r3.get());
    Assert.assertEquals(Long.valueOf(2), r4.get());
    Assert.assertEquals("OK", r5.get());
    Assert.assertEquals(Double.valueOf(3.5), r6.get());
    Assert.assertEquals(Long.valueOf(1), r7.get());
    Assert.assertEquals(Long.valueOf(1), r8.get());
    Assert.assertFalse(r9.get());
    Assert.assertEquals(keys, r10.get());
    Assert.assertEquals(vals, r11.get());
    Assert.assertEquals(vals2, r12.get());
    Assert.assertTrue(hm.keySet().contains(r13.get()));
    Assert.assertEquals(2, r14.get().size());
    Assert.assertTrue(r15.get().containsKey("field3") && r15.get().containsValue("5"));
    Assert.assertEquals(Long.valueOf(5), r16.get());
  }

  @Test
  public void clusterPipelineGeo() {
    Map<String, GeoCoordinate> hm = new HashMap<>();
    hm.put("place1", new GeoCoordinate(2.1909389952632, 41.433791470673));
    hm.put("place2", new GeoCoordinate(2.1873744593677, 41.406342043777));

    List<GeoCoordinate> values = new ArrayList<>();
    values.add(new GeoCoordinate(2.19093829393386841, 41.43379028184083523));
    values.add(new GeoCoordinate(2.18737632036209106, 41.40634178640635099));

    List<String> hashValues = new ArrayList<>();
    hashValues.add("sp3e9yg3kd0");
    hashValues.add("sp3e9cbc3t0");
    hashValues.add(null);

    GeoRadiusParam params = new GeoRadiusParam().withCoord().withHash().withDist();
    GeoRadiusParam params2 = new GeoRadiusParam().count(1, true);
    GeoRadiusStoreParam storeParams = new GeoRadiusStoreParam().store("radius{#}");

    GeoRadiusResponse expectedResponse = new GeoRadiusResponse("place1".getBytes());
    expectedResponse.setCoordinate(new GeoCoordinate(2.19093829393386841, 41.43379028184083523));
    expectedResponse.setDistance(0.0881);
    expectedResponse.setRawScore(3471609698139488L);

    ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG);
    ClusterPipeline p = new ClusterPipeline(provider);

    Response<Long> r1 = p.geoadd("barcelona", hm);
    p.geoadd("barcelona{#}", new GeoAddParams().nx(), hm);
    Response<Double> r2 = p.geodist("barcelona", "place1", "place2");
    Response<Double> r3 = p.geodist("barcelona", "place1", "place2", GeoUnit.KM);
    Response<List<String>> r4 = p.geohash("barcelona", "place1", "place2", "place3");
    Response<List<GeoCoordinate>> r5 = p.geopos("barcelona", "place1", "place2");
    Response<List<GeoRadiusResponse>> r6 = p.georadius("barcelona", 2.191, 41.433, 1000, GeoUnit.M);
    Response<List<GeoRadiusResponse>> r7 = p.georadiusReadonly("barcelona", 2.191, 41.433, 1000, GeoUnit.M);
    Response<List<GeoRadiusResponse>> r8 = p.georadius("barcelona", 2.191, 41.433, 1, GeoUnit.KM, params);
    Response<List<GeoRadiusResponse>> r9 = p.georadiusReadonly("barcelona", 2.191, 41.433, 1, GeoUnit.KM, params);
    Response<Long> r10 = p.georadiusStore("barcelona{#}", 2.191, 41.433, 1000, GeoUnit.M, params2, storeParams);
    Response<List<String>> r11 = p.zrange("radius{#}", 0, -1);
    Response<List<GeoRadiusResponse>> r12 = p.georadiusByMember("barcelona", "place1", 4, GeoUnit.KM);
    Response<List<GeoRadiusResponse>> r13 = p.georadiusByMemberReadonly("barcelona", "place1", 4, GeoUnit.KM);
    Response<List<GeoRadiusResponse>> r14 = p.georadiusByMember("barcelona", "place1", 4, GeoUnit.KM, params2);
    Response<List<GeoRadiusResponse>> r15 = p.georadiusByMemberReadonly("barcelona", "place1", 4, GeoUnit.KM, params2);
    Response<Long> r16 = p.georadiusByMemberStore("barcelona{#}", "place1", 4, GeoUnit.KM, params2, storeParams);
    Response<List<String>> r17 = p.zrange("radius{#}", 0, -1);

    p.sync();
    Assert.assertEquals(Long.valueOf(2), r1.get());
    Assert.assertEquals(Double.valueOf(3067.4157), r2.get());
    Assert.assertEquals(Double.valueOf(3.0674), r3.get());
    Assert.assertEquals(hashValues, r4.get());
    Assert.assertEquals(values, r5.get());
    Assert.assertTrue(r6.get().size() == 1 && r6.get().get(0).getMemberByString().equals("place1"));
    Assert.assertTrue(r7.get().size() == 1 && r7.get().get(0).getMemberByString().equals("place1"));
    Assert.assertEquals(expectedResponse, r8.get().get(0));
    Assert.assertEquals(expectedResponse, r9.get().get(0));
    Assert.assertEquals(Long.valueOf(1), r10.get());
    Assert.assertTrue(r11.get().size() == 1 && r11.get().contains("place1"));
    Assert.assertTrue(r12.get().size() == 2 && r12.get().get(0).getMemberByString().equals("place2"));
    Assert.assertTrue(r13.get().size() == 2 && r13.get().get(0).getMemberByString().equals("place2"));
    Assert.assertTrue(r14.get().size() == 1 && r14.get().get(0).getMemberByString().equals("place2"));
    Assert.assertTrue(r15.get().size() == 1 && r15.get().get(0).getMemberByString().equals("place2"));
    Assert.assertEquals(Long.valueOf(1), r16.get());
    Assert.assertTrue(r17.get().size() == 1 && r17.get().contains("place2"));
  }

  @Test
  public void clusterPipelineHyperLogLog() {
    ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG);
    ClusterPipeline p = new ClusterPipeline(provider);

    Response<Long> r1 = p.pfadd("{hll}_1", "foo", "bar", "zap", "a");
    Response<Long> r2 = p.pfadd("{hll}_2", "foo", "bar", "zap");
    Response<Long> r3 = p.pfcount("{hll}_1", "{hll}_2");
    Response<String> r4 = p.pfmerge("{hll}3", "{hll}_1", "{hll}_2");
    Response<Long> r5 = p.pfcount("{hll}3");

    p.sync();
    Assert.assertEquals(Long.valueOf(1), r1.get());
    Assert.assertEquals(Long.valueOf(1), r2.get());
    Assert.assertEquals(Long.valueOf(4), r3.get());
    Assert.assertEquals("OK", r4.get());
    Assert.assertEquals(Long.valueOf(4), r5.get());
  }

  @Test
  public void clusterPipelineStringsAndBits() {
    List<Long> fieldRes = new ArrayList<>();
    fieldRes.add(1L);
    fieldRes.add(0L);

    ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG);
    ClusterPipeline p = new ClusterPipeline(provider);

    Response<String> r1 = p.set("{mykey}", "foobar"); // foobar = 66 6f 6f 62 61 72
    p.set("my{otherkey}", "foo");
    Response<String> r2 = p.substr("{mykey}", 0, 2);
    Response<Long> r3 = p.strlen("{mykey}");
    Response<Long> r4 = p.bitcount("my{otherkey}");
    Response<Long> r5 = p.bitcount("my{otherkey}", 1, 1);
    Response<Long> r6 = p.bitpos("{mykey}", true);
    Response<Long> r7 = p.bitpos("{mykey}", false, new BitPosParams(1, 2));
    Response<List<Long>> r8 = p.bitfield("mynew{key}", "INCRBY", "i5", "100", "1", "GET", "u4", "0");
    Response<List<Long>> r9 = p.bitfieldReadonly("hello", "GET", "i8", "17");
    p.set("myother{mykey}", "abcdef");
    Response<Long> r10 = p.bitop(BitOP.AND, "dest{mykey}", "{mykey}", "myother{mykey}");
    Response<String> r11 = p.get("dest{mykey}");
    Response<Boolean> r12 = p.setbit("my{otherkey}", 7, true);
    Response<Boolean> r13 = p.getbit("my{otherkey}", 7);

    p.sync();
    Assert.assertEquals("OK", r1.get());
    Assert.assertEquals("foo", r2.get());
    Assert.assertEquals(Long.valueOf(6), r3.get());
    Assert.assertEquals(Long.valueOf(16), r4.get());
    Assert.assertEquals(Long.valueOf(6), r5.get());
    Assert.assertEquals(Long.valueOf(1), r6.get());
    Assert.assertEquals(Long.valueOf(8), r7.get());
    Assert.assertEquals(fieldRes, r8.get());
    Assert.assertEquals(fieldRes.subList(1, 2), r9.get());
    Assert.assertEquals(Long.valueOf(6), r10.get());
    Assert.assertEquals("`bc`ab", r11.get());
    Assert.assertFalse(r12.get());
    Assert.assertTrue(r13.get());
  }

  @Test
  public void clusterPipelineStream() {
    Map<String, String> hm = new HashMap<>();
    hm.put("one", "one");
    hm.put("two", "two");
    hm.put("three", "three");

    StreamEntryID streamId1 = new StreamEntryID("1638277876711-0");
    StreamEntryID streamId2 = new StreamEntryID("1638277959731-0");

    ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG);
    ClusterPipeline p = new ClusterPipeline(provider);

    Response<StreamEntryID> r1 = p.xadd("mystream", streamId1, hm);
    Response<StreamEntryID> r2 = p.xadd("mystream", new XAddParams().id(new StreamEntryID("1638277959731-0")).maxLen(2).approximateTrimming(), hm);
    Response<Long> r3 = p.xlen("mystream");
    Response<List<StreamEntry>> r4 = p.xrange("mystream", streamId1, streamId2);
    Response<List<StreamEntry>> r5 = p.xrange("mystream", streamId1, streamId2, 1);
    Response<List<StreamEntry>> r6 = p.xrevrange("mystream", streamId1, streamId2);
    Response<List<StreamEntry>> r7 = p.xrevrange("mystream", streamId1, streamId2, 1);
    Response<String> r8 = p.xgroupCreate("mystream", "group", streamId1, false);
    Response<String> r9 = p.xgroupSetID("mystream", "group", streamId2);
    // More stream commands are missing

    p.sync();
    Assert.assertEquals(streamId1, r1.get());
    Assert.assertEquals(streamId2, r2.get());
    Assert.assertEquals(Long.valueOf(2), r3.get());
    Assert.assertTrue(r4.get().size() == 2
        && r4.get().get(0).getID().compareTo(streamId1) == 0
        && r4.get().get(1).getID().compareTo(streamId2) == 0);
    Assert.assertTrue(r5.get().size() == 1 && r5.get().get(0).getID().compareTo(streamId1) == 0);
    Assert.assertTrue(r6.get().size() == 2
        && r6.get().get(1).getID().compareTo(streamId1) == 0
        && r6.get().get(0).getID().compareTo(streamId2) == 0);
    Assert.assertTrue(r7.get().size() == 1 && r7.get().get(0).getID().compareTo(streamId2) == 0);
    Assert.assertEquals("OK", r8.get());
    Assert.assertEquals("OK", r9.get());
  }

  @Test
  public void testEval() {
    String script = "return 'success!'";

    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);
      Response<Object> result = p.eval(script);
      p.sync();

      assertEquals("success!", result.get());
    }
  }

  @Test
  public void testEvalWithBinary() {
    String script = "return 'success!'";

    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);
      Response<Object> result = p.eval(SafeEncoder.encode(script));
      p.sync();

      assertArrayEquals(SafeEncoder.encode("success!"), (byte[]) result.get());
    }
  }

  @Test
  public void testEvalKeyAndArg() {
    String key = "test";
    String arg = "3";
    String script = "redis.call('INCRBY', KEYS[1], ARGV[1]) redis.call('INCRBY', KEYS[1], ARGV[1])";

    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);
      p.set(key, "0");
      Response<Object> result0 = p.eval(script, Arrays.asList(key), Arrays.asList(arg));
      p.incr(key);
      Response<Object> result1 = p.eval(script, Arrays.asList(key), Arrays.asList(arg));
      Response<String> result2 = p.get(key);
      p.sync();

      assertNull(result0.get());
      assertNull(result1.get());
      assertEquals("13", result2.get());
    }
  }

  @Test
  public void testEvalKeyAndArgWithBinary() {
    // binary
    byte[] bKey = SafeEncoder.encode("test");
    byte[] bArg = SafeEncoder.encode("3");
    byte[] bScript = SafeEncoder.encode("redis.call('INCRBY', KEYS[1], ARGV[1]) redis.call('INCRBY', KEYS[1], ARGV[1])");

    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline bP = new ClusterPipeline(provider);
      bP.set(bKey, SafeEncoder.encode("0"));
      Response<Object> bResult0 = bP.eval(bScript, Arrays.asList(bKey), Arrays.asList(bArg));
      bP.incr(bKey);
      Response<Object> bResult1 = bP.eval(bScript, Arrays.asList(bKey), Arrays.asList(bArg));
      Response<byte[]> bResult2 = bP.get(bKey);
      bP.sync();

      assertNull(bResult0.get());
      assertNull(bResult1.get());
      assertArrayEquals(SafeEncoder.encode("13"), bResult2.get());
    }
  }

  @Test
  public void testEvalNestedLists() {
    String script = "return { {KEYS[1]} , {2} }";

    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);
      Response<Object> result = p.eval(script, 1, "key1");
      p.sync();

      List<?> results = (List<?>) result.get();
      MatcherAssert.assertThat((List<String>) results.get(0), listWithItem("key1"));
      MatcherAssert.assertThat((List<Long>) results.get(1), listWithItem(2L));
    }
  }

  @Test
  public void testEvalNestedListsWithBinary() {
    byte[] bScript = SafeEncoder.encode("return { {KEYS[1]} , {2} }");
    byte[] bKey = SafeEncoder.encode("key1");

    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);
      Response<Object> result = p.eval(bScript, 1, bKey);
      p.sync();

      List<?> results = (List<?>) result.get();
      MatcherAssert.assertThat((List<byte[]>) results.get(0), listWithItem(bKey));
      MatcherAssert.assertThat((List<Long>) results.get(1), listWithItem(2L));
    }
  }

  @Test
  public void testEvalsha() {
    String script = "return 'success!'";
    String sha1;
    try (JedisCluster jc = new JedisCluster(nodes, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      sha1 = jc.scriptLoad(script, "sampleKey");
      assertTrue(jc.scriptExists(sha1, "sampleKey"));
    }

    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);
      Response<Object> result = p.evalsha(sha1, 1, "sampleKey");
      p.sync();

      assertEquals("success!", result.get());
    }
  }

  @Test
  public void testEvalshaKeyAndArg() {
    String key = "test";
    String arg = "3";
    String script = "redis.call('INCRBY', KEYS[1], ARGV[1]) redis.call('INCRBY', KEYS[1], ARGV[1])";
    String sha1;
    try (JedisCluster jc = new JedisCluster(nodes, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      sha1 = jc.scriptLoad(script, key);
      assertTrue(jc.scriptExists(sha1, key));
    }

    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);
      p.set(key, "0");
      Response<Object> result0 = p.evalsha(sha1, Arrays.asList(key), Arrays.asList(arg));
      p.incr(key);
      Response<Object> result1 = p.evalsha(sha1, Arrays.asList(key), Arrays.asList(arg));
      Response<String> result2 = p.get(key);
      p.sync();

      assertNull(result0.get());
      assertNull(result1.get());
      assertEquals("13", result2.get());
    }
  }

  @Test
  public void testEvalshaKeyAndArgWithBinary() {
    byte[] bKey = SafeEncoder.encode("test");
    byte[] bArg = SafeEncoder.encode("3");
    String script = "redis.call('INCRBY', KEYS[1], ARGV[1]) redis.call('INCRBY', KEYS[1], ARGV[1])";
    byte[] bScript = SafeEncoder.encode(script);
    byte[] bSha1;
    try (JedisCluster jc = new JedisCluster(nodes, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "cluster", DEFAULT_POOL_CONFIG)) {
      bSha1 = jc.scriptLoad(bScript, bKey);
      assertTrue(jc.scriptExists(bSha1, bKey));
    }

    try (ClusterConnectionProvider provider = new ClusterConnectionProvider(nodes, DEFAULT_CLIENT_CONFIG)) {
      ClusterPipeline p = new ClusterPipeline(provider);
      p.set(bKey, SafeEncoder.encode("0"));
      Response<Object> result0 = p.evalsha(bSha1, Arrays.asList(bKey), Arrays.asList(bArg));
      p.incr(bKey);
      Response<Object> result1 = p.evalsha(bSha1, Arrays.asList(bKey), Arrays.asList(bArg));
      Response<byte[]> result2 = p.get(bKey);
      p.sync();

      assertNull(result0.get());
      assertNull(result1.get());
      assertArrayEquals(SafeEncoder.encode("13"), result2.get());
    }
  }

  private <T> Matcher<Iterable<? super T>> listWithItem(T expected) {
    return CoreMatchers.<T>hasItem(equalTo(expected));
  }
}
