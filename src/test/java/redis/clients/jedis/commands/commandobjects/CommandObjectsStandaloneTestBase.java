package redis.clients.jedis.commands.commandobjects;

import io.redis.test.utils.EnabledOnCommandRule;
import io.redis.test.utils.RedisVersionRule;
import org.junit.Rule;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;

/**
 * Base class for tests that use the standalone client.
 */
public abstract class CommandObjectsStandaloneTestBase extends CommandObjectsTestBase {

  @Rule
  public RedisVersionRule versionRule = new RedisVersionRule(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build());
  @Rule
  public EnabledOnCommandRule enabledOnCommandRule = new EnabledOnCommandRule(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build());

  public CommandObjectsStandaloneTestBase(RedisProtocol protocol) {
    super(protocol, HostAndPorts.getRedisEndpoint("standalone0"));
  }

}
