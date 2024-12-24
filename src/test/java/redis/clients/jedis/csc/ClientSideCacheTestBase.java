package redis.clients.jedis.csc;

import org.junit.After;
import org.junit.Before;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import today.bonfire.oss.sop.SimpleObjectPoolConfig;

import java.util.function.Supplier;

abstract class ClientSideCacheTestBase {

  protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone1");

  protected static final HostAndPort hnp = endpoint.getHostAndPort();

  protected Jedis control;

  @Before
  public void setUp() throws Exception {
    control = new Jedis(hnp, endpoint.getClientConfigBuilder().build());
    control.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    control.close();
  }

  protected static final Supplier<JedisClientConfig> clientConfig = () -> endpoint.getClientConfigBuilder().resp3().build();

  protected static final Supplier<SimpleObjectPoolConfig> singleConnectionPoolConfig = () -> {
    var poolConfig = ConnectionPoolConfig.builder();
    poolConfig.maxPoolSize(1);
    return poolConfig.build();
  };

}
