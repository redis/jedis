package redis.clients.jedis.tests.commands;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.tests.HostAndPortUtil;

public abstract class JedisCommandTestBase {
  protected static final HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

  protected Jedis jedis;

  public JedisCommandTestBase() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    jedis = new Jedis(hnp.getHost(), hnp.getPort(), 500);
    jedis.connect();
    jedis.auth("foobared");
    jedis.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    jedis.disconnect();
  }

  protected Jedis createJedis() {
    Jedis j = new Jedis(hnp);
    j.connect();
    j.auth("foobared");
    return j;
  }
}
