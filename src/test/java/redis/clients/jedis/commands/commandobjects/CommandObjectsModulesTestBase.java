package redis.clients.jedis.commands.commandobjects;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;

/**
 * Base class for tests that need a Redis Stack server.
 */
public abstract class CommandObjectsModulesTestBase extends CommandObjectsTestBase {

  public CommandObjectsModulesTestBase(RedisProtocol protocol) {
    super(protocol, HostAndPorts.getRedisEndpoint("modules-docker"));
  }

}
