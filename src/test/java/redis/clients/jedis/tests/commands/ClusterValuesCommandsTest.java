package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.exceptions.JedisDataException;

public class ClusterValuesCommandsTest extends ClusterJedisCommandsTestBase {

  @Test
  public void testHincrByFloat() {
    Double value = jedisCluster.hincrByFloat("foo", "bar", 1.5d);
    assertEquals((Double) 1.5d, value);
    value = jedisCluster.hincrByFloat("foo", "bar", -1.5d);
    assertEquals((Double) 0d, value);
    value = jedisCluster.hincrByFloat("foo", "bar", -10.7d);
    assertEquals(Double.valueOf(-10.7d), value);
  }

  @Test
  public void pipeline() {
    try (Pipeline p = jedisCluster.startPipeline("foo")) {
      p.set("foo", "bar");
      p.get("foo");
      List<Object> results = p.syncAndReturnAll();

      assertEquals(2, results.size());
      assertEquals("OK", results.get(0));
      assertEquals("bar", results.get(1));
    }
  }

  @Test
  public void pipelineResponse() {
    jedisCluster.set("{hashtag}.string", "foo");
    jedisCluster.lpush("{hashtag}.list", "foo");
    jedisCluster.hset("{hashtag}.hash", "foo", "bar");
    jedisCluster.zadd("{hashtag}.zset", 1, "foo");
    jedisCluster.sadd("{hashtag}.set", "foo");
    jedisCluster.setrange("{hashtag}.setrange", 0, "0123456789");
    byte[] bytesForSetRange = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    jedisCluster.setrange("{hashtag}.setrangebytes".getBytes(), 0, bytesForSetRange);

    try (Pipeline p = jedisCluster.startPipeline("{hashtag}")) {
      Response<String> string = p.get("{hashtag}.string");
      Response<String> list = p.lpop("{hashtag}.list");
      Response<String> hash = p.hget("{hashtag}.hash", "foo");
      Response<Set<String>> zset = p.zrange("{hashtag}.zset", 0, -1);
      Response<String> set = p.spop("{hashtag}.set");
      Response<Boolean> blist = p.exists("{hashtag}.list");
      Response<Double> zincrby = p.zincrby("{hashtag}.zset", 1, "foo");
      Response<Long> zcard = p.zcard("{hashtag}.zset");
      p.lpush("{hashtag}.list", "bar");
      Response<List<String>> lrange = p.lrange("{hashtag}.list", 0, -1);
      Response<Map<String, String>> hgetAll = p.hgetAll("{hashtag}.hash");
      p.sadd("{hashtag}.set", "foo");
      Response<Set<String>> smembers = p.smembers("{hashtag}.set");
      Response<Set<Tuple>> zrangeWithScores = p.zrangeWithScores("{hashtag}.zset", 0, -1);
      Response<String> getrange = p.getrange("{hashtag}.setrange", 1, 3);
      Response<byte[]> getrangeBytes = p.getrange("{hashtag}.setrangebytes".getBytes(), 6, 8);
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
  public void pipelineSelect() {
    try (Pipeline p = jedisCluster.startPipeline("select")) {
      p.select(1);
      List<Object> resp = p.syncAndReturnAll();
      assertEquals(JedisDataException.class, resp.get(0).getClass());
    }
  }

  @Test
  public void pipelineResponseWithData() {
    jedisCluster.zadd("zset", 1, "foo");

    try (Pipeline p = jedisCluster.startPipeline("zset")) {
      Response<Double> score = p.zscore("zset", "foo");
      p.sync();

      assertNotNull(score.get());
    }
  }

  @Test
  public void pipelineResponseWithoutData() {
    jedisCluster.zadd("zset", 1, "foo");

    try (Pipeline p = jedisCluster.startPipeline("zset")) {
      Response<Double> score = p.zscore("zset", "bar");
      p.sync();

      assertNull(score.get());
    }
  }

  @Test(expected = JedisDataException.class)
  public void pipelineResponseWithinPipeline() {
    jedisCluster.set("string", "foo");

    try (Pipeline p = jedisCluster.startPipeline("string")) {
      Response<String> string = p.get("string");
      string.get();
      p.sync();
    }
  }
}
