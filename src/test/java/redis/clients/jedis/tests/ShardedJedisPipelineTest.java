package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Response;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.exceptions.JedisDataException;

public class ShardedJedisPipelineTest {

  private static HostAndPort redis1 = HostAndPortUtil.getRedisServers().get(0);
  private static HostAndPort redis2 = HostAndPortUtil.getRedisServers().get(1);

  private ShardedJedis jedis;

  @Before
  public void setUp() throws Exception {
    Jedis jedis = new Jedis(redis1);
    jedis.auth("foobared");
    jedis.flushAll();
    jedis.disconnect();
    jedis = new Jedis(redis2);
    jedis.auth("foobared");
    jedis.flushAll();
    jedis.disconnect();

    JedisShardInfo shardInfo1 = new JedisShardInfo(redis1);
    JedisShardInfo shardInfo2 = new JedisShardInfo(redis2);
    shardInfo1.setPassword("foobared");
    shardInfo2.setPassword("foobared");
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    shards.add(shardInfo1);
    shards.add(shardInfo2);
    this.jedis = new ShardedJedis(shards);

  }

  @Test
  public void pipeline() throws UnsupportedEncodingException {
    ShardedJedisPipeline p = jedis.pipelined();
    p.set("foo", "bar");
    p.get("foo");
    List<Object> results = p.syncAndReturnAll();

    assertEquals(2, results.size());
    assertEquals("OK", results.get(0));
    assertEquals("bar", results.get(1));
  }

  @Test
  public void pipelineResponse() {
    jedis.set("string", "foo");
    jedis.lpush("list", "foo");
    jedis.hset("hash", "foo", "bar");
    jedis.zadd("zset", 1, "foo");
    jedis.sadd("set", "foo");

    ShardedJedisPipeline p = jedis.pipelined();
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
  }

  @Test(expected = JedisDataException.class)
  public void pipelineResponseWithinPipeline() {
    jedis.set("string", "foo");

    ShardedJedisPipeline p = jedis.pipelined();
    Response<String> string = p.get("string");
    string.get();
    p.sync();
  }

  @Test
  public void canRetrieveUnsetKey() {
    ShardedJedisPipeline p = jedis.pipelined();
    Response<String> shouldNotExist = p.get(UUID.randomUUID().toString());
    p.sync();
    assertNull(shouldNotExist.get());
  }

  @Test
  public void testSyncWithNoCommandQueued() {
    JedisShardInfo shardInfo1 = new JedisShardInfo(redis1);
    JedisShardInfo shardInfo2 = new JedisShardInfo(redis2);
    shardInfo1.setPassword("foobared");
    shardInfo2.setPassword("foobared");
    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    shards.add(shardInfo1);
    shards.add(shardInfo2);

    ShardedJedis jedis2 = new ShardedJedis(shards);

    ShardedJedisPipeline pipeline = jedis2.pipelined();
    pipeline.sync();

    jedis2.close();

    jedis2 = new ShardedJedis(shards);
    pipeline = jedis2.pipelined();
    List<Object> resp = pipeline.syncAndReturnAll();
    assertTrue(resp.isEmpty());

    jedis2.close();
  }

}
