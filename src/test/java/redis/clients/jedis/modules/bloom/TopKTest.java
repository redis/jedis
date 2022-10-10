package redis.clients.jedis.modules.bloom;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

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

  @Test
  public void createTopKFilter() {
    client.topkReserve("aaa", 30, 2000, 7, 0.925);

    assertEquals(Arrays.asList(null, null), client.topkAdd("aaa", "bb", "cc"));

    assertEquals(Arrays.asList(true, false, true), client.topkQuery("aaa", "bb", "gg", "cc"));

    assertEquals(Arrays.asList(1L, 0L, 1L), client.topkCount("aaa", "bb", "gg", "cc"));

    assertEquals(new TreeSet<>(Arrays.asList("bb", "cc")), new TreeSet<>(client.topkList("aaa")));

//    assertEquals(null, client.topkIncrBy("aaa", "ff", 10));
    assertEquals(Collections.<String>singletonList(null),
        client.topkIncrBy("aaa", Collections.singletonMap("ff", 10L)));

    assertEquals(new TreeSet<>(Arrays.asList("bb", "cc", "ff")), new TreeSet<>(client.topkList("aaa")));
  }
}
