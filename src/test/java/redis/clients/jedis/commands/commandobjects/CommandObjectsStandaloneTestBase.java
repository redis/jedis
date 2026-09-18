package redis.clients.jedis.commands.commandobjects;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.EnvCondition;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;

/**
 * Base class for tests that use the standalone client.
 */
@Tag("integration")
public abstract class CommandObjectsStandaloneTestBase extends CommandObjectsTestBase {

  @RegisterExtension
  RedisVersionCondition redisVersionCondition = new RedisVersionCondition(() -> endpoint);
  @RegisterExtension
  EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(() -> endpoint);
  @RegisterExtension
  static EnvCondition envCondition = new EnvCondition();

  public CommandObjectsStandaloneTestBase(RedisProtocol protocol) {
    super(protocol, Endpoints.getRedisEndpoint("standalone0"));
  }

}
