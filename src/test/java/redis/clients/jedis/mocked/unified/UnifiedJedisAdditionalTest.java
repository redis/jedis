package redis.clients.jedis.mocked.unified;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import redis.clients.jedis.json.JsonObjectMapper;

/**
 * {@link redis.clients.jedis.UnifiedJedis} tests that don't really fall into any category of commands.
 */
public class UnifiedJedisAdditionalTest extends UnifiedJedisTestBase {

  @Test
  public void testSetJsonObjectMapper() {
    JsonObjectMapper jsonObjectMapper = mock(JsonObjectMapper.class);

    unifiedJedis.setJsonObjectMapper(jsonObjectMapper);

    verify(commandObjects).setJsonObjectMapper(jsonObjectMapper);
  }

  @Test
  public void testSetDefaultSearchDialect() {
    int dialect = 1;

    unifiedJedis.setDefaultSearchDialect(dialect);

    verify(commandObjects).setDefaultSearchDialect(dialect);
  }

}
