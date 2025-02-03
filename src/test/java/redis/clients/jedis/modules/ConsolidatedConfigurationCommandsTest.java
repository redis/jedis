package redis.clients.jedis.modules;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import io.redis.test.annotations.SinceRedisVersion;
import java.util.Collections;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;

@SinceRedisVersion(value = "7.9.0")
@RunWith(Parameterized.class)
public class ConsolidatedConfigurationCommandsTest extends RedisModuleCommandsTestBase {

  public ConsolidatedConfigurationCommandsTest(RedisProtocol redisProtocol) {
    super(redisProtocol);
  }

  @Test
  public void setSearchConfigGloballyTest() {
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

  @Test
  public void setReadOnlySearchConfigTest() {
    JedisDataException de = assertThrows(JedisDataException.class,
        () -> jedis.configSet("search-max-doctablesize", "10"));
    assertThat(de.getMessage(), Matchers.not(Matchers.emptyOrNullString()));
  }

  @Test
  public void getSearchConfigSettingTest() {
    assertThat(jedis.configGet("search-timeout"), aMapWithSize(1));
  }

  @Test
  public void getTSConfigSettingTest() {
    assertThat(jedis.configGet("ts-retention-policy"), aMapWithSize(1));
  }

  @Test
  public void getBFConfigSettingTest() {
    assertThat(jedis.configGet("bf-error-rate"), aMapWithSize(1));
  }

  @Test
  public void getCFConfigSettingTest() {
    assertThat(jedis.configGet("cf-initial-size"), aMapWithSize(1));
  }

  @Test
  public void getAllConfigSettings() {
    assertThat(jedis.configGet("*").size(), Matchers.greaterThan(0));
  }
}
