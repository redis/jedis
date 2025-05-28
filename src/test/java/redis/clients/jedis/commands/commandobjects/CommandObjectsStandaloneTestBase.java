package redis.clients.jedis.commands.commandobjects;

import org.junit.jupiter.api.extension.RegisterExtension;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;

/**
 * Base class for tests that use the standalone client.
 */
public abstract class CommandObjectsStandaloneTestBase extends CommandObjectsTestBase {

  @RegisterExtension
  RedisVersionCondition redisVersionCondition = new RedisVersionCondition(endpoint);
  @RegisterExtension
  EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(endpoint);

  public CommandObjectsStandaloneTestBase(RedisProtocol protocol) {
    super(protocol, HostAndPorts.getRedisEndpoint("standalone0"));
  }

}
