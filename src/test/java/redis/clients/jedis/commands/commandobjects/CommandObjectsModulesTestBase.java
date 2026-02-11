package redis.clients.jedis.commands.commandobjects;

import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;

/**
 * Base class for tests that need a Redis Stack server.
 */
public abstract class CommandObjectsModulesTestBase extends CommandObjectsTestBase {

  public CommandObjectsModulesTestBase(RedisProtocol protocol) {
    super(protocol, Endpoints.getRedisEndpoint(Endpoints.MODULES_DOCKER));
  }

}
