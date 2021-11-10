package redis.clients.jedis.tests.commands;

import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.tests.HostAndPortUtil;

public abstract class JedisCommandsTestBase {

  protected static final HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

  protected static final JedisClientConfig config
      = DefaultJedisClientConfig.builder().timeoutMillis(500).password("foobared").build();

  protected Jedis jedis;

  public JedisCommandsTestBase() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    jedis = new Jedis(hnp, config);
    jedis.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    jedis.close();
  }

  protected Jedis createJedis() {
    return new Jedis(hnp, config);
  }

  protected Jedis createJedisDefaultTimeout() {
    return new Jedis(hnp, DefaultJedisClientConfig.builder().password("foobared").build());
  }
}
