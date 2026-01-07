package redis.clients.jedis.modules;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import redis.clients.jedis.*;
import redis.clients.jedis.commands.CommandsTestsParameters;
import redis.clients.jedis.util.EnvCondition;
import redis.clients.jedis.util.RedisVersionCondition;

@Tag("integration")
public abstract class RedisModuleCommandsTestBase {

  protected static String preferredEndpointId = "modules-docker";

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint(preferredEndpointId));

  @RegisterExtension
  public EnvCondition envCondition = new EnvCondition();

  protected static EndpointConfig endpoint;

  /**
   * Input data for parameterized tests. In principle all subclasses of this
   * class should be parameterized tests, to run with several versions of RESP.
   *
   * @see CommandsTestsParameters#respVersions()
   */
  protected final RedisProtocol protocol;

  protected Jedis jedis;
  protected UnifiedJedis client;

  /**
   * The RESP protocol is to be injected by the subclasses, usually via JUnit
   * parameterized tests, because most of the subclassed tests are meant to be
   * executed against multiple RESP versions. For the special cases where a single
   * RESP version is relevant, we still force the subclass to be explicit and
   * call this constructor.
   *
   * @param protocol The RESP protocol to use during the tests.
   */
  public RedisModuleCommandsTestBase(RedisProtocol protocol) {
    this.protocol = protocol;
  }

  // BeforeClass
  public static void prepare() {
    endpoint = Endpoints.getRedisEndpoint(preferredEndpointId);
  }

  @BeforeEach
  public void setUp() {
    if (endpoint == null) {
      return;
    }
    jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().protocol(protocol).timeoutMillis(500).build());
    jedis.flushAll();
    client = RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
        .clientConfig(endpoint.getClientConfigBuilder().protocol(protocol).build()).build();
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (endpoint == null) {
      return;
    }
    client.close();
    jedis.close();
  }

}
