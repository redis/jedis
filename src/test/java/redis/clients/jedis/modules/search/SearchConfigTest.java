package redis.clients.jedis.modules.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.Map;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.RedisProtocol;
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
    Assume.assumeFalse(protocol == RedisProtocol.RESP3); // pending update

    Map<String, Object> map = client.ftConfigGet("TIMEOUT");
    assertEquals(1, map.size());
    String value = (String) map.get("TIMEOUT");
    assertNotNull(value);

    assertEquals("OK", client.ftConfigSet("timeout", value));
  }

  @Test
  public void configOnTimeout() {
    Assume.assumeFalse(protocol == RedisProtocol.RESP3); // pending update

    // confirm default
    assertEquals(Collections.singletonMap("ON_TIMEOUT", "return"), client.ftConfigGet("ON_TIMEOUT"));

    assertEquals("OK", client.ftConfigSet("ON_TIMEOUT", "fail"));
    assertEquals(Collections.singletonMap("ON_TIMEOUT", "fail"), client.ftConfigGet("ON_TIMEOUT"));

    // restore to default
    assertEquals("OK", client.ftConfigSet("ON_TIMEOUT", "return"));
  }

  @Test
  public void dialectConfig() {
    Assume.assumeFalse(protocol == RedisProtocol.RESP3); // pending update

    // confirm default
    assertEquals(Collections.singletonMap("DEFAULT_DIALECT", "1"), client.ftConfigGet("DEFAULT_DIALECT"));

    assertEquals("OK", client.ftConfigSet("DEFAULT_DIALECT", "2"));
    assertEquals(Collections.singletonMap("DEFAULT_DIALECT", "2"), client.ftConfigGet("DEFAULT_DIALECT"));

    // restore to default
    assertEquals("OK", client.ftConfigSet("DEFAULT_DIALECT", "1"));
  }
}
