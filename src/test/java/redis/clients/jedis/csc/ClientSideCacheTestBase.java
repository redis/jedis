package redis.clients.jedis.csc;

import java.util.function.Supplier;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

abstract class ClientSideCacheTestBase {

  protected static final HostAndPort hnp = HostAndPorts.getRedisServers().get(1);

  protected static final String baseUrl = "redis://:foobared@" + hnp.toString() + "/";

  protected Jedis control;

  @Before
  public void setUp() throws Exception {
    control = new Jedis(hnp, DefaultJedisClientConfig.builder().password("foobared").build());
    control.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    control.close();
  }

  protected static final Supplier<JedisClientConfig> clientConfig
      = () -> DefaultJedisClientConfig.builder().resp3().password("foobared").build();

  protected static final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig
      = () -> {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(1);
        return poolConfig;
      };

}
