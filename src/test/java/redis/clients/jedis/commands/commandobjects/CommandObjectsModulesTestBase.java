package redis.clients.jedis.commands.commandobjects;

import org.junit.jupiter.api.extension.RegisterExtension;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.util.EnvCondition;

/**
 * Base class for tests that need a Redis Stack server.
 */
public abstract class CommandObjectsModulesTestBase extends CommandObjectsTestBase {

  @RegisterExtension
  EnvCondition envCondition = new EnvCondition();

  public CommandObjectsModulesTestBase(RedisProtocol protocol) {
    super(protocol, Endpoints.getRedisEndpoint("modules-docker"));
  }

}
