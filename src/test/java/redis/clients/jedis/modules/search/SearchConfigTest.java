package redis.clients.jedis.modules.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.modules.RedisModuleCommandsTestBase;

public class SearchConfigTest extends RedisModuleCommandsTestBase {

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
  public void config() {
    Map<String, String> map = client.ftConfigGet("TIMEOUT");
    assertEquals(1, map.size());
    String value = map.get("TIMEOUT");
    assertNotNull(value);

    assertEquals("OK", client.ftConfigSet("timeout", value));
  }

  @Test
  public void configOnTimeout() {
    // confirm default
    assertEquals(Collections.singletonMap("ON_TIMEOUT", "return"), client.ftConfigGet("ON_TIMEOUT"));

    assertEquals("OK", client.ftConfigSet("ON_TIMEOUT", "fail"));
    assertEquals(Collections.singletonMap("ON_TIMEOUT", "fail"), client.ftConfigGet("ON_TIMEOUT"));

    // restore to default
    assertEquals("OK", client.ftConfigSet("ON_TIMEOUT", "return"));
  }

  @Test
  public void dialectConfig() {
    // confirm default
    assertEquals(Collections.singletonMap("DEFAULT_DIALECT", "1"), client.ftConfigGet("DEFAULT_DIALECT"));

    assertEquals("OK", client.ftConfigSet("DEFAULT_DIALECT", "2"));
    assertEquals(Collections.singletonMap("DEFAULT_DIALECT", "2"), client.ftConfigGet("DEFAULT_DIALECT"));

    // restore to default
    assertEquals("OK", client.ftConfigSet("DEFAULT_DIALECT", "1"));
  }
}
