package redis.clients.jedis.modules;

import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.Before;
import redis.clients.jedis.Connection;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.RedisProtocolUtil;

public abstract class RedisModuleCommandsTestBase {

  private static final String address = System.getProperty("modulesDocker", Protocol.DEFAULT_HOST + ':' + 6479);
  protected static final HostAndPort hnp = HostAndPort.from(address);
  protected final RedisProtocol protocol = RedisProtocolUtil.getRedisProtocol();

  protected UnifiedJedis client;

  public RedisModuleCommandsTestBase() {
    super();
  }

  // BeforeClass
  public static void prepare() {
    try (Connection connection = new Connection(hnp)) {
      assumeTrue("No Redis running on 6479 port.", connection.ping());
    } catch (JedisConnectionException jce) {
      assumeTrue("Could not connect to Redis running on 6479 port.", false);
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
