package redis.clients.jedis.commands.unified.bloom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.UnifiedJedisCommandsTestBase;

/**
 * Base test class for TopK commands using the UnifiedJedis pattern.
 */
@Tag("integration")
@Tag("bloom")
public abstract class TopKCommandsTestBase extends UnifiedJedisCommandsTestBase {

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("modules-docker");
  }

  public TopKCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void createTopKFilter() {
    jedis.topkReserve("aaa", 30, 2000, 7, 0.925);

    assertEquals(Arrays.asList(null, null), jedis.topkAdd("aaa", "bb", "cc"));

    assertEquals(Arrays.asList(true, false, true), jedis.topkQuery("aaa", "bb", "gg", "cc"));

    assertEquals(Arrays.asList("bb", "cc"), jedis.topkList("aaa"));

    Map<String, Long> listWithCount = jedis.topkListWithCount("aaa");
    assertEquals(2, listWithCount.size());
    listWithCount.forEach((item, count) -> {
      assertTrue(Arrays.asList("bb", "cc").contains(item));
      assertEquals(Long.valueOf(1), count);
    });

    assertNull(jedis.topkIncrBy("aaa", "ff", 5));
    assertEquals(Arrays.asList("ff", "bb", "cc"), jedis.topkList("aaa"));

    assertEquals(Collections.<String>singletonList(null),
        jedis.topkIncrBy("aaa", Collections.singletonMap("ff", 8L)));
    assertEquals(Long.valueOf(13), jedis.topkListWithCount("aaa").get("ff"));
  }
}

