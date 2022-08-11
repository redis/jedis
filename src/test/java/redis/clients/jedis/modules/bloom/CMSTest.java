package redis.clients.jedis.modules.bloom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

/**
 * Tests for the Count-Min-Sketch Implementation
 */
public class CMSTest extends RedisModuleCommandsTestBase {

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }
//
//  @AfterClass
//  public static void tearDown() {
////    RedisModuleCommandsTestBase.tearDown();
//  }

  @Test
  public void testInitByDim() {
    client.cmsInitByDim("cms1", 16L, 4L);
    Map<String, Object> info = client.cmsInfo("cms1");
    assertEquals(16L, info.get("width"));
    assertEquals(4L, info.get("depth"));
    assertEquals(0L, info.get("count"));
  }

  @Test
  public void testInitByProb() {
    client.cmsInitByProb("cms2", 0.01, 0.01);
    Map<String, Object> info = client.cmsInfo("cms2");
    assertEquals(200L, info.get("width"));
    assertEquals(7L, info.get("depth"));
    assertEquals(0L, info.get("count"));
  }

  @Test
  public void testKeyAlreadyExists() {
    client.cmsInitByDim("dup", 16L, 4L);
    JedisException thrown = assertThrows(JedisException.class, () -> {
      client.cmsInitByDim("dup", 8L, 6L);
    });
    assertEquals("CMS: key already exists", thrown.getMessage());
  }

  @Test
  public void testIncrBy() {
    client.cmsInitByDim("cms3", 1000L, 5L);
    long resp = client.cmsIncrBy("cms3", "foo", 5L);
    assertEquals(5L, resp);

    Map<String, Object> info = client.cmsInfo("cms3");
    assertEquals(1000L, info.get("width"));
    assertEquals(5L, info.get("depth"));
    assertEquals(5L, info.get("count"));
  }

  @Test
  public void testIncrByMultipleArgs() {
    client.cmsInitByDim("cms4", 1000L, 5L);
    client.cmsIncrBy("cms4", "foo", 5L);

    Map<String, Long> itemIncrements = new LinkedHashMap<>();
    itemIncrements.put("foo", 5L);
    itemIncrements.put("bar", 15L);

    List<Long> resp = client.cmsIncrBy("cms4", itemIncrements);
//    assertArrayEquals(new Long[] { 15L, 10L }, resp.toArray(new Long[0]));
    assertEquals(Arrays.asList(10L, 15L), resp);

    Map<String, Object> info = client.cmsInfo("cms4");
    assertEquals(1000L, info.get("width"));
    assertEquals(5L, info.get("depth"));
    assertEquals(25L, info.get("count"));
  }

  @Test
  public void testQuery() {
    client.cmsInitByDim("cms5", 1000L, 5L);

    Map<String, Long> itemIncrements = new HashMap<>();
    itemIncrements.put("foo", 10L);
    itemIncrements.put("bar", 15L);

    client.cmsIncrBy("cms5", itemIncrements);

    List<Long> resp = client.cmsQuery("cms5", "foo", "bar");
    assertEquals(Arrays.asList(10L, 15L), resp);
  }

  @Test
  public void testMerge() {
    client.cmsInitByDim("A", 1000L, 5L);
    client.cmsInitByDim("B", 1000L, 5L);
    client.cmsInitByDim("C", 1000L, 5L);

    Map<String, Long> aValues = new HashMap<>();
    aValues.put("foo", 5L);
    aValues.put("bar", 3L);
    aValues.put("baz", 9L);

    client.cmsIncrBy("A", aValues);

    Map<String, Long> bValues = new HashMap<>();
    bValues.put("foo", 2L);
    bValues.put("bar", 3L);
    bValues.put("baz", 1L);

    client.cmsIncrBy("B", bValues);

    List<Long> q1 = client.cmsQuery("A", "foo", "bar", "baz");
    assertEquals(Arrays.asList(5L, 3L, 9L), q1);

    List<Long> q2 = client.cmsQuery("B", "foo", "bar", "baz");
    assertEquals(Arrays.asList(2L, 3L, 1L), q2);

    client.cmsMerge("C", "A", "B");

    List<Long> q3 = client.cmsQuery("C", "foo", "bar", "baz");
    assertEquals(Arrays.asList(7L, 6L, 10L), q3);

    Map<String, Long> keysAndWeights = new HashMap<>();
    keysAndWeights.put("A", 1L);
    keysAndWeights.put("B", 2L);

    client.cmsMerge("C", keysAndWeights);

    List<Long> q4 = client.cmsQuery("C", "foo", "bar", "baz");
    assertEquals(Arrays.asList(9L, 9L, 11L), q4);

    keysAndWeights.clear();
    keysAndWeights.put("A", 2L);
    keysAndWeights.put("B", 3L);

    client.cmsMerge("C", keysAndWeights);

    List<Long> q5 = client.cmsQuery("C", "foo", "bar", "baz");
    assertEquals(Arrays.asList(16L, 15L, 21L), q5);
  }
}
