package redis.clients.jedis.bloom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.exceptions.JedisDataException;

public class RedisBloomBuilderFactoryTest {

  @Test
  public void cmsIncrByPassesPlainCounts() {
    List<Object> data = Arrays.asList(10L, 15L);

    List<Long> result = RedisBloomBuilderFactory.CMS_INCRBY_LONG_LIST.build(data);

    assertEquals(Arrays.asList(10L, 15L), result);
  }

  @Test
  public void cmsIncrByFailsAsAWholeOnARejectedItem() {
    // the first item was applied, the second one was rejected; the call still fails
    JedisDataException rejected = new JedisDataException("CMS: INCRBY overflow");
    List<Object> data = Arrays.asList(1L, rejected);

    JedisDataException thrown = assertThrows(JedisDataException.class,
      () -> RedisBloomBuilderFactory.CMS_INCRBY_LONG_LIST.build(data));

    assertSame(rejected, thrown);
  }

  @Test
  public void cmsIncrByKeepsNull() {
    assertNull(RedisBloomBuilderFactory.CMS_INCRBY_LONG_LIST.build(null));
  }
}
