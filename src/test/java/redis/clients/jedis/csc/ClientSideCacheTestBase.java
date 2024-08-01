package redis.clients.jedis.csc;

import java.util.function.Supplier;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.Before;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;

public abstract class ClientSideCacheTestBase {

  private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone1");

  protected static final HostAndPort hnp = endpoint.getHostAndPort();

  protected static final String baseUrl = "redis://:foobared@" + hnp.toString() + "/"; // TODO: use EndpointConfig

  protected Jedis control;

  @Before
  public void setUp() throws Exception {
    control = new Jedis(hnp, DefaultJedisClientConfig.builder().password("foobared").build()); // TODO: use EndpointConfig
    control.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    control.close();
  }

  protected static final Supplier<JedisClientConfig> clientConfig
      = () -> DefaultJedisClientConfig.builder().resp3().password("foobared").build(); // TODO: use EndpointConfig

  protected static final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig
      = () -> {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(1);
        return poolConfig;
      };

}
