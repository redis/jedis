package redis.clients.jedis.modules;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;

@RunWith(Parameterized.class)
public class RedisModulesControlCommandsTest extends JedisCommandsTestBase {

  public RedisModulesControlCommandsTest(RedisProtocol redisProtocol) {
    super(redisProtocol);
  }

  @Test
  public void dialectConfig() {
    final String configParam = "search-default-dialect";
    // confirm default
    assertEquals(Collections.singletonMap(configParam, "1"), jedis.configGet(configParam));

    try {
      assertEquals("OK", jedis.configSet(configParam, "2"));
      assertEquals(Collections.singletonMap(configParam, "2"), jedis.configGet(configParam));

    } finally {
      // restore to default
      assertEquals("OK", jedis.configSet(configParam, "1"));
    }
  }
}
