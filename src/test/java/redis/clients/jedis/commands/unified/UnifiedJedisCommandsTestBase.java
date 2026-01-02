package redis.clients.jedis.commands.unified;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeAll;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.CommandsTestsParameters;
import redis.clients.jedis.util.EnabledOnCommandCondition;
import redis.clients.jedis.util.RedisVersionCondition;

@Tag("integration")
public abstract class UnifiedJedisCommandsTestBase {

  /**
   * Input data for parameterized tests. In principle all subclasses of this
   * class should be parameterized tests, to run with several versions of RESP.
   *
   * @see CommandsTestsParameters#respVersions()
   */
  protected final RedisProtocol protocol;

  protected UnifiedJedis jedis;

  protected static EndpointConfig endpoint;

  @RegisterExtension
  public RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint("standalone0"));
  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      () -> Endpoints.getRedisEndpoint("standalone0"));
  @RegisterExtension
  public EnvCondition envCondition = new EnvCondition();

  @BeforeAll
  public static void prepareEndpoint() {
    endpoint = Endpoints.getRedisEndpoint("standalone0");
  }

  /**
   * The RESP protocol is to be injected by the subclasses, usually via JUnit
   * parameterized tests, because most of the subclassed tests are meant to be
   * executed against multiple RESP versions. For the special cases where a single
   * RESP version is relevant, we still force the subclass to be explicit and
   * call this constructor.
   *
   * @param protocol The RESP protocol to use during the tests.
   */
  public UnifiedJedisCommandsTestBase(RedisProtocol protocol) {
    this.protocol = protocol;
  }

  /**
   * Subclasses provide specific UnifiedJedis setup.
   */
  protected abstract UnifiedJedis createTestClient();

  protected void clearData() {
    if (jedis != null) {
      jedis.flushAll();
    }
  }

  @BeforeEach
  void setUpBase() {
    jedis = createTestClient();
    clearData();
  }

  @AfterEach
  void tearDownBase() {
    if (jedis != null) {
      jedis.close();
    }
  }
}
