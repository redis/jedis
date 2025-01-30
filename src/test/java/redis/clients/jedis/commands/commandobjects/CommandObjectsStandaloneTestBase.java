package redis.clients.jedis.commands.commandobjects;

import redis.clients.jedis.util.EnabledOnCommandRule;
import redis.clients.jedis.util.RedisVersionRule;
import org.junit.Rule;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;

/**
 * Base class for tests that use the standalone client.
 */
public abstract class CommandObjectsStandaloneTestBase extends CommandObjectsTestBase {

  @Rule
  public RedisVersionRule versionRule = new RedisVersionRule(endpoint);
  @Rule
  public EnabledOnCommandRule enabledOnCommandRule = new EnabledOnCommandRule(endpoint);

  public CommandObjectsStandaloneTestBase(RedisProtocol protocol) {
    super(protocol, HostAndPorts.getRedisEndpoint("standalone0"));
  }

}
