package redis.clients.jedis.tests;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.tests.utils.ClientKillerUtil;
import redis.clients.util.Hashing;
import redis.clients.util.SafeEncoder;
import redis.clients.util.Sharded;

public class ShardedJedisTest extends Assert {
  private static HostAndPort redis1 = HostAndPortUtil.getRedisServers().get(0);
  private static HostAndPort redis2 = HostAndPortUtil.getRedisServers().get(1);

  /**
   * Test for "Issue - BinaryShardedJedis.disconnect() may occur memory leak". You can find more
   * detailed information at https://github.com/xetorthio/jedis/issues/808
   * @throws InterruptedException
   */
  @Test
  public void testAvoidLeaksUponDisconnect() throws InterruptedException {
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(2);
    // 6379
    JedisShardInfo shard1 = new JedisShardInfo(redis1.getHost(), redis1.getPort());
    shard1.setPassword("foobared");
    shards.add(shard1);
    // 6380
    JedisShardInfo shard2 = new JedisShardInfo(redis2.getHost(), redis2.getPort());
    shard2.setPassword("foobared");
    shards.add(shard2);

    @SuppressWarnings("resource")
    ShardedJedis shardedJedis = new ShardedJedis(shards);
    // establish the connection for two redis servers
    shardedJedis.set("a", "bar");
    JedisShardInfo ak = shardedJedis.getShardInfo("a");
    assertEquals(shard2, ak);
    shardedJedis.set("b", "bar1");
    JedisShardInfo bk = shardedJedis.getShardInfo("b");
    assertEquals(shard1, bk);

    // We set a name to the instance so it's easy to find it
    Iterator<Jedis> it = shardedJedis.getAllShards().iterator();
    Jedis deadClient = it.next();
    deadClient.clientSetname("DEAD");

    ClientKillerUtil.killClient(deadClient, "DEAD");

    assertEquals(true, deadClient.isConnected());
    assertEquals(false, deadClient.getClient().getSocket().isClosed());
    assertEquals(false, deadClient.getClient().isBroken()); // normal - not found

    shardedJedis.disconnect();

    assertEquals(false, deadClient.isConnected());
    assertEquals(true, deadClient.getClient().getSocket().isClosed());
    assertEquals(true, deadClient.getClient().isBroken());

    Jedis jedis2 = it.next();
    assertEquals(false, jedis2.isConnected());
    assertEquals(true, jedis2.getClient().getSocket().isClosed());
    assertEquals(false, jedis2.getClient().isBroken());

  }

  private List<String> getKeysDifferentShard(ShardedJedis jedis) {
    List<String> ret = new ArrayList<String>();
    JedisShardInfo first = jedis.getShardInfo("a0");
    ret.add("a0");
    for (int i = 1; i < 100; ++i) {
      JedisShardInfo actual = jedis.getShardInfo("a" + i);
      if (actual != first) {
        ret.add("a" + i);
        break;

      }

    }
    return ret;
  }

  @Test
  public void checkSharding() {
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    shards.add(new JedisShardInfo(redis1.getHost(), redis1.getPort()));
    shards.add(new JedisShardInfo(redis2.getHost(), redis2.getPort()));
    ShardedJedis jedis = new ShardedJedis(shards);
    List<String> keys = getKeysDifferentShard(jedis);
    JedisShardInfo s1 = jedis.getShardInfo(keys.get(0));
    JedisShardInfo s2 = jedis.getShardInfo(keys.get(1));
    assertNotSame(s1, s2);
  }

  @Test
  public void trySharding() {
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    JedisShardInfo si = new JedisShardInfo(redis1.getHost(), redis1.getPort());
    si.setPassword("foobared");
    shards.add(si);
    si = new JedisShardInfo(redis2.getHost(), redis2.getPort());
    si.setPassword("foobared");
    shards.add(si);
    ShardedJedis jedis = new ShardedJedis(shards);
    jedis.set("a", "bar");
    JedisShardInfo s1 = jedis.getShardInfo("a");
    jedis.set("b", "bar1");
    JedisShardInfo s2 = jedis.getShardInfo("b");
    jedis.disconnect();

    Jedis j = new Jedis(s1.getHost(), s1.getPort());
    j.auth("foobared");
    assertEquals("bar", j.get("a"));
    j.disconnect();

    j = new Jedis(s2.getHost(), s2.getPort());
    j.auth("foobared");
    assertEquals("bar1", j.get("b"));
    j.disconnect();
  }

  @Test
  public void tryShardingWithMurmure() {
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    JedisShardInfo si = new JedisShardInfo(redis1.getHost(), redis1.getPort());
    si.setPassword("foobared");
    shards.add(si);
    si = new JedisShardInfo(redis2.getHost(), redis2.getPort());
    si.setPassword("foobared");
    shards.add(si);
    ShardedJedis jedis = new ShardedJedis(shards, Hashing.MURMUR_HASH);
    jedis.set("a", "bar");
    JedisShardInfo s1 = jedis.getShardInfo("a");
    jedis.set("b", "bar1");
    JedisShardInfo s2 = jedis.getShardInfo("b");
    jedis.disconnect();

    Jedis j = new Jedis(s1.getHost(), s1.getPort());
    j.auth("foobared");
    assertEquals("bar", j.get("a"));
    j.disconnect();

    j = new Jedis(s2.getHost(), s2.getPort());
    j.auth("foobared");
    assertEquals("bar1", j.get("b"));
    j.disconnect();
  }

  @Test
  public void checkKeyTags() {
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    shards.add(new JedisShardInfo(redis1.getHost(), redis1.getPort()));
    shards.add(new JedisShardInfo(redis2.getHost(), redis2.getPort()));
    ShardedJedis jedis = new ShardedJedis(shards, ShardedJedis.DEFAULT_KEY_TAG_PATTERN);

    assertEquals(jedis.getKeyTag("foo"), "foo");
    assertEquals(jedis.getKeyTag("foo{bar}"), "bar");
    assertEquals(jedis.getKeyTag("foo{bar}}"), "bar"); // default pattern is
    // non greedy
    assertEquals(jedis.getKeyTag("{bar}foo"), "bar"); // Key tag may appear
    // anywhere
    assertEquals(jedis.getKeyTag("f{bar}oo"), "bar"); // Key tag may appear
    // anywhere

    JedisShardInfo s1 = jedis.getShardInfo("abc{bar}");
    JedisShardInfo s2 = jedis.getShardInfo("foo{bar}");
    assertSame(s1, s2);

    List<String> keys = getKeysDifferentShard(jedis);
    JedisShardInfo s3 = jedis.getShardInfo(keys.get(0));
    JedisShardInfo s4 = jedis.getShardInfo(keys.get(1));
    assertNotSame(s3, s4);

    ShardedJedis jedis2 = new ShardedJedis(shards);

    assertEquals(jedis2.getKeyTag("foo"), "foo");
    assertNotSame(jedis2.getKeyTag("foo{bar}"), "bar");

    JedisShardInfo s5 = jedis2.getShardInfo(keys.get(0) + "{bar}");
    JedisShardInfo s6 = jedis2.getShardInfo(keys.get(1) + "{bar}");
    assertNotSame(s5, s6);
  }

  @Test
  public void shardedPipeline() {
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    shards.add(new JedisShardInfo(redis1.getHost(), redis1.getPort()));
    shards.add(new JedisShardInfo(redis2.getHost(), redis2.getPort()));
    shards.get(0).setPassword("foobared");
    shards.get(1).setPassword("foobared");
    ShardedJedis jedis = new ShardedJedis(shards);

    final List<String> keys = getKeysDifferentShard(jedis);
    jedis.set(keys.get(0), "a");
    jedis.set(keys.get(1), "b");

    assertNotSame(jedis.getShard(keys.get(0)), jedis.getShard(keys.get(1)));

    List<Object> results = jedis.pipelined(new ShardedJedisPipeline() {
      public void execute() {
        get(keys.get(0));
        get(keys.get(1));
      }
    });

    List<Object> expected = new ArrayList<Object>(2);
    expected.add(SafeEncoder.encode("a"));
    expected.add(SafeEncoder.encode("b"));

    assertEquals(2, results.size());
    assertArrayEquals(SafeEncoder.encode("a"), (byte[]) results.get(0));
    assertArrayEquals(SafeEncoder.encode("b"), (byte[]) results.get(1));
  }

  @Test
  public void testMD5Sharding() {
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(3);
    shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT));
    shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 1));
    shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 2));
    Sharded<Jedis, JedisShardInfo> sharded = new Sharded<Jedis, JedisShardInfo>(shards, Hashing.MD5);
    int shard_6379 = 0;
    int shard_6380 = 0;
    int shard_6381 = 0;
    for (int i = 0; i < 1000; i++) {
      JedisShardInfo jedisShardInfo = sharded.getShardInfo(Integer.toString(i));
      switch (jedisShardInfo.getPort()) {
      case 6379:
        shard_6379++;
        break;
      case 6380:
        shard_6380++;
        break;
      case 6381:
        shard_6381++;
        break;
      default:
        fail("Attempting to use a non-defined shard!!:" + jedisShardInfo);
        break;
      }
    }
    assertTrue(shard_6379 > 300 && shard_6379 < 400);
    assertTrue(shard_6380 > 300 && shard_6380 < 400);
    assertTrue(shard_6381 > 300 && shard_6381 < 400);
  }

  @Test
  public void testMurmurSharding() {
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(3);
    shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT));
    shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 1));
    shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 2));
    Sharded<Jedis, JedisShardInfo> sharded = new Sharded<Jedis, JedisShardInfo>(shards,
        Hashing.MURMUR_HASH);
    int shard_6379 = 0;
    int shard_6380 = 0;
    int shard_6381 = 0;
    for (int i = 0; i < 1000; i++) {
      JedisShardInfo jedisShardInfo = sharded.getShardInfo(Integer.toString(i));
      switch (jedisShardInfo.getPort()) {
      case 6379:
        shard_6379++;
        break;
      case 6380:
        shard_6380++;
        break;
      case 6381:
        shard_6381++;
        break;
      default:
        fail("Attempting to use a non-defined shard!!:" + jedisShardInfo);
        break;
      }
    }
    assertTrue(shard_6379 > 300 && shard_6379 < 400);
    assertTrue(shard_6380 > 300 && shard_6380 < 400);
    assertTrue(shard_6381 > 300 && shard_6381 < 400);
  }

  @Test
  public void testMasterSlaveShardingConsistency() {
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(3);
    shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT));
    shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 1));
    shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 2));
    Sharded<Jedis, JedisShardInfo> sharded = new Sharded<Jedis, JedisShardInfo>(shards,
        Hashing.MURMUR_HASH);

    List<JedisShardInfo> otherShards = new ArrayList<JedisShardInfo>(3);
    otherShards.add(new JedisShardInfo("otherhost", Protocol.DEFAULT_PORT));
    otherShards.add(new JedisShardInfo("otherhost", Protocol.DEFAULT_PORT + 1));
    otherShards.add(new JedisShardInfo("otherhost", Protocol.DEFAULT_PORT + 2));
    Sharded<Jedis, JedisShardInfo> sharded2 = new Sharded<Jedis, JedisShardInfo>(otherShards,
        Hashing.MURMUR_HASH);

    for (int i = 0; i < 1000; i++) {
      JedisShardInfo jedisShardInfo = sharded.getShardInfo(Integer.toString(i));
      JedisShardInfo jedisShardInfo2 = sharded2.getShardInfo(Integer.toString(i));
      assertEquals(shards.indexOf(jedisShardInfo), otherShards.indexOf(jedisShardInfo2));
    }

  }

  @Test
  public void testMasterSlaveShardingConsistencyWithShardNaming() {
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>(3);
    shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT, "HOST1:1234"));
    shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 1, "HOST2:1234"));
    shards.add(new JedisShardInfo("localhost", Protocol.DEFAULT_PORT + 2, "HOST3:1234"));
    Sharded<Jedis, JedisShardInfo> sharded = new Sharded<Jedis, JedisShardInfo>(shards,
        Hashing.MURMUR_HASH);

    List<JedisShardInfo> otherShards = new ArrayList<JedisShardInfo>(3);
    otherShards.add(new JedisShardInfo("otherhost", Protocol.DEFAULT_PORT, "HOST2:1234"));
    otherShards.add(new JedisShardInfo("otherhost", Protocol.DEFAULT_PORT + 1, "HOST3:1234"));
    otherShards.add(new JedisShardInfo("otherhost", Protocol.DEFAULT_PORT + 2, "HOST1:1234"));
    Sharded<Jedis, JedisShardInfo> sharded2 = new Sharded<Jedis, JedisShardInfo>(otherShards,
        Hashing.MURMUR_HASH);

    for (int i = 0; i < 1000; i++) {
      JedisShardInfo jedisShardInfo = sharded.getShardInfo(Integer.toString(i));
      JedisShardInfo jedisShardInfo2 = sharded2.getShardInfo(Integer.toString(i));
      assertEquals(jedisShardInfo.getName(), jedisShardInfo2.getName());
    }
  }

  @Test
  public void checkCloseable() {
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    shards.add(new JedisShardInfo(redis1.getHost(), redis1.getPort()));
    shards.add(new JedisShardInfo(redis2.getHost(), redis2.getPort()));
    shards.get(0).setPassword("foobared");
    shards.get(1).setPassword("foobared");

    ShardedJedis jedisShard = new ShardedJedis(shards);
    try {
      jedisShard.set("shard_closeable", "true");
    } finally {
      jedisShard.close();
    }

    for (Jedis jedis : jedisShard.getAllShards()) {
      assertTrue(!jedis.isConnected());
    }
  }

}