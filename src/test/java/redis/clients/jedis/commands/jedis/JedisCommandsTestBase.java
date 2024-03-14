package redis.clients.jedis.commands.jedis;

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.runners.Parameterized.Parameters;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.CommandsTestsParameters;

public abstract class JedisCommandsTestBase {

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

  protected static final HostAndPort hnp = HostAndPorts.getRedisServers().get(0);

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

  @Before
  public void setUp() throws Exception {
//    jedis = new Jedis(hnp, DefaultJedisClientConfig.builder().timeoutMillis(500).password("foobared").build());
    jedis = new Jedis(hnp, DefaultJedisClientConfig.builder()
        .protocol(protocol).timeoutMillis(500).password("foobared").build());
    jedis.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    jedis.close();
  }

  protected Jedis createJedis() {
//    return new Jedis(hnp, DefaultJedisClientConfig.builder().password("foobared").build());
    return new Jedis(hnp, DefaultJedisClientConfig.builder()
        .protocol(protocol).password("foobared").build());
  }
}
