package redis.clients.jedis.commands.unified.client.bloom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import io.redis.test.annotations.SinceRedisVersion;

import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.bloom.CmsCellSize;
import redis.clients.jedis.commands.unified.client.RedisClientCommandsTestHelper;
import redis.clients.jedis.commands.unified.bloom.CMSCommandsTestBase;
import redis.clients.jedis.exceptions.JedisDataException;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class CMSRedisClientCommandsIT extends CMSCommandsTestBase {

  public CMSRedisClientCommandsIT(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return RedisClientCommandsTestHelper.getClient(protocol, endpoint);
  }

  @Test
  @SinceRedisVersion("8.11.0")
  public void testIncrBySyncAndReturnAllReportsError() {
    jedis.cmsInitByDim("cms8", 16L, 4L, CmsCellSize.ONE_BYTE);
    jedis.cmsIncrBy("cms8", "foo", 250L);

    try (Pipeline pipeline = (Pipeline) jedis.pipelined()) {
      pipeline.cmsIncrBy("cms8", Collections.singletonMap("foo", 10L));
      List<Object> all = pipeline.syncAndReturnAll();

      assertEquals(1, all.size());
      JedisDataException error = assertInstanceOf(JedisDataException.class, all.get(0));
      assertEquals("CMS: INCRBY overflow", error.getMessage());
    }
  }

  @Test
  @SinceRedisVersion("8.11.0")
  public void testIncrByInTransactionReportsError() {
    jedis.cmsInitByDim("cms9", 16L, 4L, CmsCellSize.ONE_BYTE);
    jedis.cmsIncrBy("cms9", "foo", 250L);

    AbstractTransaction transaction = jedis.multi();
    transaction.cmsIncrBy("cms9", Collections.singletonMap("foo", 10L));
    List<Object> exec = transaction.exec();

    assertEquals(1, exec.size());
    JedisDataException error = assertInstanceOf(JedisDataException.class, exec.get(0));
    assertEquals("CMS: INCRBY overflow", error.getMessage());
    assertEquals(250L, jedis.cmsQuery("cms9", "foo").get(0));
  }
}
