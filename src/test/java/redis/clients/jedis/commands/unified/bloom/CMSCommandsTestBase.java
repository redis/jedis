package redis.clients.jedis.commands.unified.bloom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;
import redis.clients.jedis.exceptions.JedisException;

/**
 * Base test class for Count-Min-Sketch commands using the UnifiedJedis pattern.
 */
@Tag("bloom")
public abstract class CMSCommandsTestBase extends UnifiedJedisCommandsTestBase {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public CMSCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testInitByDim() {
    jedis.cmsInitByDim("cms1", 16L, 4L);
    Map<String, Object> info = jedis.cmsInfo("cms1");
    assertEquals(16L, info.get("width"));
    assertEquals(4L, info.get("depth"));
    assertEquals(0L, info.get("count"));
  }

  @Test
  public void testInitByProb() {
    jedis.cmsInitByProb("cms2", 0.01, 0.01);
    Map<String, Object> info = jedis.cmsInfo("cms2");
    assertEquals(200L, info.get("width"));
    assertEquals(7L, info.get("depth"));
    assertEquals(0L, info.get("count"));
  }

  @Test
  public void testKeyAlreadyExists() {
    jedis.cmsInitByDim("dup", 16L, 4L);
    JedisException thrown = assertThrows(JedisException.class, () -> {
      jedis.cmsInitByDim("dup", 8L, 6L);
    });
    assertEquals("CMS: key already exists", thrown.getMessage());
  }

  @Test
  public void testIncrBy() {
    jedis.cmsInitByDim("cms3", 1000L, 5L);
    long resp = jedis.cmsIncrBy("cms3", "foo", 5L);
    assertEquals(5L, resp);

    Map<String, Object> info = jedis.cmsInfo("cms3");
    assertEquals(1000L, info.get("width"));
    assertEquals(5L, info.get("depth"));
    assertEquals(5L, info.get("count"));
  }

  @Test
  public void testIncrByMultipleArgs() {
    jedis.cmsInitByDim("cms4", 1000L, 5L);
    jedis.cmsIncrBy("cms4", "foo", 5L);

    Map<String, Long> itemIncrements = new LinkedHashMap<>();
    itemIncrements.put("foo", 5L);
    itemIncrements.put("bar", 15L);

    List<Long> resp = jedis.cmsIncrBy("cms4", itemIncrements);
    assertEquals(Arrays.asList(10L, 15L), resp);

    Map<String, Object> info = jedis.cmsInfo("cms4");
    assertEquals(1000L, info.get("width"));
    assertEquals(5L, info.get("depth"));
    assertEquals(25L, info.get("count"));
  }

  @Test
  public void testQuery() {
    jedis.cmsInitByDim("cms5", 1000L, 5L);

    Map<String, Long> itemIncrements = new HashMap<>();
    itemIncrements.put("foo", 10L);
    itemIncrements.put("bar", 15L);

    jedis.cmsIncrBy("cms5", itemIncrements);

    List<Long> resp = jedis.cmsQuery("cms5", "foo", "bar");
    assertEquals(Arrays.asList(10L, 15L), resp);
  }

  @Test
  public void testMerge() {
    jedis.cmsInitByDim("{key}A", 1000L, 5L);
    jedis.cmsInitByDim("{key}B", 1000L, 5L);
    jedis.cmsInitByDim("{key}C", 1000L, 5L);

    Map<String, Long> aValues = new HashMap<>();
    aValues.put("foo", 5L);
    aValues.put("bar", 3L);
    aValues.put("baz", 9L);
    jedis.cmsIncrBy("{key}A", aValues);

    Map<String, Long> bValues = new HashMap<>();
    bValues.put("foo", 2L);
    bValues.put("bar", 3L);
    bValues.put("baz", 1L);
    jedis.cmsIncrBy("{key}B", bValues);

    List<Long> q1 = jedis.cmsQuery("{key}A", "foo", "bar", "baz");
    assertEquals(Arrays.asList(5L, 3L, 9L), q1);

    List<Long> q2 = jedis.cmsQuery("{key}B", "foo", "bar", "baz");
    assertEquals(Arrays.asList(2L, 3L, 1L), q2);

    jedis.cmsMerge("{key}C", "{key}A", "{key}B");

    List<Long> q3 = jedis.cmsQuery("{key}C", "foo", "bar", "baz");
    assertEquals(Arrays.asList(7L, 6L, 10L), q3);

    Map<String, Long> keysAndWeights = new HashMap<>();
    keysAndWeights.put("{key}A", 1L);
    keysAndWeights.put("{key}B", 2L);
    jedis.cmsMerge("{key}C", keysAndWeights);

    List<Long> q4 = jedis.cmsQuery("{key}C", "foo", "bar", "baz");
    assertEquals(Arrays.asList(9L, 9L, 11L), q4);

    keysAndWeights.clear();
    keysAndWeights.put("{key}A", 2L);
    keysAndWeights.put("{key}B", 3L);
    jedis.cmsMerge("{key}C", keysAndWeights);

    List<Long> q5 = jedis.cmsQuery("{key}C", "foo", "bar", "baz");
    assertEquals(Arrays.asList(16L, 15L, 21L), q5);
  }
}
