package redis.clients.jedis.tests.commands;

import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisX;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.tests.HostAndPortUtil;

public abstract class JedisCommandTestBase {

  protected static final HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

  protected JedisX jedis;

  public JedisCommandTestBase() {
    super();
  }

  @Before
  public void setUp() throws Exception {
    Connection connection = new Connection(hnp, DefaultJedisClientConfig.builder()
        .timeoutMillis(500).password("foobared").build());
    connection.executeCommand(Protocol.Command.FLUSHALL);
    jedis = new JedisX(connection);
  }

  @After
  public void tearDown() throws Exception {
    jedis.close();
  }

  protected JedisX createJedis() {
    return new JedisX(hnp, DefaultJedisClientConfig.builder().password("foobared").build());
  }
}
