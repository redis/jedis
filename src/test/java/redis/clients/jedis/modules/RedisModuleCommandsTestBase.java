package redis.clients.jedis.modules;

import static org.junit.Assume.assumeTrue;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
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

public abstract class RedisModuleCommandsTestBase {

  /**
   * Input data for parameterized tests. In principle all subclasses of this
   * class should be parameterized tests, to run with several version of RESP.
   */
  @Parameters
  public static Collection<Object[]> data() {
    return CommandsTestsParameters.respVersions();
  }

  private static final String address = System.getProperty("modulesDocker", Protocol.DEFAULT_HOST + ':' + 52567);
  protected static final HostAndPort hnp = HostAndPort.from(address);
  protected final RedisProtocol protocol;

  protected UnifiedJedis client;

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
    try (Jedis jedis = new Jedis(hnp)) {
      jedis.flushAll();
    }
    client = new UnifiedJedis(hnp, DefaultJedisClientConfig.builder().protocol(protocol).build());
  }

  @After
  public void tearDown() throws Exception {
    client.close();
  }
//
//  public static void tearDown() {
//    client.close();
//  }
//
//  protected static Connection createConnection() {
//    return new Connection(hnp);
//  }
}
