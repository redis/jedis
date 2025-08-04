package redis.clients.jedis.commands.jedis;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import redis.clients.jedis.*;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

public abstract class JedisCommandsTestBase {

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(endpoint);
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(endpoint);

  protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0");

  protected final RedisProtocol protocol;

  protected Jedis jedis;

  /**
   * The RESP protocol is to be injected by the subclasses, usually via JUnit
   * parameterized tests, because most of the subclassed tests are meant to be
   * executed against multiple RESP versions. For the special cases where a single
   * RESP version is relevant, we still force the subclass to be explicit and
   * call this constructor.
   *
   * @param protocol The RESP protocol to use during the tests.
   */
  public JedisCommandsTestBase(RedisProtocol protocol) {
    this.protocol = protocol;
  }

  @BeforeEach
  public void setUp() throws Exception {
    jedis = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder()
        .protocol(protocol).timeoutMillis(500).build());
    jedis.flushAll();
  }

  @AfterEach
  public void tearDown() throws Exception {
    jedis.close();
  }

  protected Jedis createJedis() {
    return new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder()
        .protocol(protocol).build());
  }
}
