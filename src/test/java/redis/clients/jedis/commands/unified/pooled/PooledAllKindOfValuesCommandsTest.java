package redis.clients.jedis.commands.unified.pooled;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.BaseRedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.AllKindOfValuesCommandsTestBase;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.unified.pooled.PooledCommandsTestHelper#testParamsProvider")
public class PooledAllKindOfValuesCommandsTest extends AllKindOfValuesCommandsTestBase {

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(PooledCommandsTestHelper.nodeInfo);
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(PooledCommandsTestHelper.nodeInfo);

  public PooledAllKindOfValuesCommandsTest(RedisProtocol protocol, Class<? extends BaseRedisClient> clientType) {
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
