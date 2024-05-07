package redis.clients.jedis.commands.commandobjects;

import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;

/**
 * Base class for tests that use the standalone client.
 */
public abstract class CommandObjectsStandaloneTestBase extends CommandObjectsTestBase {

  public CommandObjectsStandaloneTestBase(RedisProtocol protocol) {
    super(protocol, HostAndPorts.getRedisEndpoint("standalone0"));
  }

}
