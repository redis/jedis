package redis.clients.jedis.commands.unified.pooled;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.unified.StreamsBinaryCommandsTestBase;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class PooledStreamsBinaryCommandsTest extends StreamsBinaryCommandsTestBase {

  public PooledStreamsBinaryCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  public void setUpTestClient() {
    jedis = PooledCommandsTestHelper.getPooled(protocol);
  }

  @AfterEach
  public void tearDown() {
    jedis.close();
  }

}
