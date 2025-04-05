package redis.clients.jedis.modules.bloom;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

@RunWith(Parameterized.class)
public class TopKTest extends RedisModuleCommandsTestBase {

  @BeforeClass
  public static void prepare() {
    RedisModuleCommandsTestBase.prepare();
  }
//
//  @AfterClass
//  public static void tearDown() {
////    RedisModuleCommandsTestBase.tearDown();
//  }

  public TopKTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void createTopKFilter() {
    client.topkReserve("aaa", 30, 2000, 7, 0.925);

    assertEquals(Arrays.asList(null, null), client.topkAdd("aaa", "bb", "cc"));

    assertEquals(Arrays.asList(true, false, true), client.topkQuery("aaa", "bb", "gg", "cc"));

    assertEquals(Arrays.asList("bb", "cc"), client.topkList("aaa"));

    Map<String, Long> listWithCount = client.topkListWithCount("aaa");
    assertEquals(2, listWithCount.size());
    listWithCount.forEach((item, count) -> {
      assertTrue(Arrays.asList("bb", "cc").contains(item));
      assertEquals(Long.valueOf(1), count);
    });

    assertEquals(null, client.topkIncrBy("aaa", "ff", 5));
    assertEquals(Arrays.asList("ff", "bb", "cc"), client.topkList("aaa"));

    assertEquals(Collections.<String>singletonList(null),
        client.topkIncrBy("aaa", Collections.singletonMap("ff", 8L)));
    assertEquals(Long.valueOf(13), client.topkListWithCount("aaa").get("ff"));
  }
}
