package redis.clients.jedis.commands.jedis;

import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.HostAndPorts;

public abstract class JedisCommandsTestBase {

  protected static final HostAndPort hnp = HostAndPorts.getRedisServers().get(0);

  protected Jedis jedis;

  public JedisCommandsTestBase() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    jedis = new Jedis(hnp, DefaultJedisClientConfig.builder().timeoutMillis(500).password("foobared").build());
    jedis.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    jedis.close();
  }

  protected Jedis createJedis() {
    return new Jedis(hnp, DefaultJedisClientConfig.builder().password("foobared").build());
  }
}
