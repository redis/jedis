package redis.clients.jedis.modules;

import static org.junit.Assume.assumeTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import redis.clients.jedis.Connection;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.providers.PooledConnectionProvider;

public abstract class RedisModuleCommandsTestBase {

  protected static final HostAndPort hnp = new HostAndPort(Protocol.DEFAULT_HOST, 6479);

  private static final PooledConnectionProvider provider = new PooledConnectionProvider(hnp);
  protected UnifiedJedis client;

  public RedisModuleCommandsTestBase() {
    super();
  }

  public static void prepare() {
    try (Connection connection = new Connection(hnp)) {
      assumeTrue("No Redis running on 6479 port. Ignoring modules tests.", connection.ping());
    } catch (JedisConnectionException jce) {
      assumeTrue(false);
    }
  }

  @Before
  public void setUp() {
    try (Jedis jedis = createJedis()) {
      jedis.flushAll();
    }
    client = new UnifiedJedis(provider);
  }
//
//  @After
//  public void tearDown() throws Exception {
//    client.close();
//  }
//
//  public static void tearDown() {
//    client.close();
//  }

  protected static Jedis createJedis() {
    return new Jedis(hnp);
  }
}
