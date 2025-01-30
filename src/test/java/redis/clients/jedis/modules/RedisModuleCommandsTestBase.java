package redis.clients.jedis.modules;

import static org.junit.Assume.assumeTrue;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runners.Parameterized.Parameters;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.commands.CommandsTestsParameters;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.RedisVersionRule;

public abstract class RedisModuleCommandsTestBase {

  @Rule
  public RedisVersionRule versionRule = new RedisVersionRule(hnp, DefaultJedisClientConfig.builder().build());

  /**
   * Input data for parameterized tests. In principle all subclasses of this
   * class should be parameterized tests, to run with several versions of RESP.
   *
   * @see CommandsTestsParameters#respVersions()
   */
  @Parameters
  public static Collection<Object[]> data() {
    return CommandsTestsParameters.respVersions();
  }

  private static final String address = System.getProperty("modulesDocker", Protocol.DEFAULT_HOST + ':' + 6479);
  protected static final HostAndPort hnp = HostAndPort.from(address);
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
    try (Connection connection = new Connection(hnp)) {
      assumeTrue("No Redis running on " + hnp.getPort() + " port.", connection.ping());
    } catch (JedisConnectionException jce) {
      assumeTrue("Could not connect to Redis running on " + hnp.getPort() + " port.", false);
    }
  }

  @Before
  public void setUp() {
    jedis = new Jedis(hnp, DefaultJedisClientConfig.builder().protocol(protocol).build());
    jedis.flushAll();
    client = new UnifiedJedis(hnp, DefaultJedisClientConfig.builder().protocol(protocol).build());
  }

  @After
  public void tearDown() throws Exception {
    client.close();
    jedis.close();
  }

}
