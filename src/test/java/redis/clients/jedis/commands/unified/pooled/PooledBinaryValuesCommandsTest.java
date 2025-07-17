package redis.clients.jedis.commands.unified.pooled;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.BaseRedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.BinaryValuesCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.unified.pooled.PooledCommandsTestHelper#testParamsProvider")
public class PooledBinaryValuesCommandsTest extends BinaryValuesCommandsTestBase {

  public PooledBinaryValuesCommandsTest(RedisProtocol protocol, Class<? extends BaseRedisClient> clientType) {
    super(protocol, clientType);
  }

  @BeforeEach
  public void setUp() {
    jedis = PooledCommandsTestHelper.getCleanClient(protocol, clientType);
  }

  @AfterEach
  public void cleanUp() {
    try {
      jedis.close();
    } catch (Exception e) {
      logger.warn("Exception while closing jedis", e);
    }
  }
}
