package redis.clients.jedis.commands.commandobjects;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;

/**
 * Base class for tests that need a Redis Stack server.
 */
public abstract class CommandObjectsModulesTestBase extends CommandObjectsTestBase {

  private static final String address =
      System.getProperty("modulesDocker", Protocol.DEFAULT_HOST + ':' + 6479);

  public CommandObjectsModulesTestBase(RedisProtocol protocol) {
    super(protocol, HostAndPort.from(address), null);
  }

}
