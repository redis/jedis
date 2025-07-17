package redis.clients.jedis.commands.unified.pooled;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.BaseRedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.StreamsBinaryCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.unified.pooled.PooledCommandsTestHelper#testParamsProvider")
public class PooledStreamsBinaryCommandsTest extends StreamsBinaryCommandsTestBase {

  public PooledStreamsBinaryCommandsTest(RedisProtocol protocol, Class<? extends BaseRedisClient> clientType) {
    super(protocol, clientType);
  }

  @Override
  public void setUpTestClient() {
    jedis = PooledCommandsTestHelper.getCleanClient(protocol, clientType);
  }

  @AfterEach
  public void tearDown() {
    try {
      jedis.close();
    } catch (Exception e) {
      logger.warn("Exception while closing jedis", e);
    }
  }

}
