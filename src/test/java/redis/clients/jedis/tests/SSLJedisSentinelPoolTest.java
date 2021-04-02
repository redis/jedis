package redis.clients.jedis.tests;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPortMapper;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

public class SSLJedisSentinelPoolTest {

  private static final String MASTER_NAME = "mymaster";

  private static Set<HostAndPort> sentinels = new HashSet<>();

  private static final HostAndPortMapper SSL_PORT_MAPPER =
      (HostAndPort hap) -> new HostAndPort(hap.getHost(), hap.getPort() + 10000);

  private static final GenericObjectPoolConfig<Jedis> POOL_CONFIG = new GenericObjectPoolConfig<>();

  @BeforeClass
  public static void prepare() {
    SSLJedisTest.setupTrustStore();

    sentinels.add(HostAndPortUtil.getSentinelServers().get(1));
    sentinels.add(HostAndPortUtil.getSentinelServers().get(3));
  }

  @Test
  public void sentinelWithoutSslConnectsToRedisWithSsl() {
    DefaultJedisClientConfig masterConfig = DefaultJedisClientConfig.builder().password("foobared")
        .clientName("sentinel-master-client").ssl(true).hostAndPortMapper(SSL_PORT_MAPPER).build();
    DefaultJedisClientConfig sentinelConfig =
        DefaultJedisClientConfig.builder().clientName("sentinel-client").ssl(false).build();
    try (JedisSentinelPool pool =
        new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG, masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

  @Test
  public void sentinelWithSslConnectsToRedisWithoutSsl() {
    DefaultJedisClientConfig masterConfig = DefaultJedisClientConfig.builder().password("foobared")
        .clientName("sentinel-master-client").ssl(false).build();
    DefaultJedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .clientName("sentinel-client").ssl(true).hostAndPortMapper(SSL_PORT_MAPPER).build();
    try (JedisSentinelPool pool =
        new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG, masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

  @Test
  public void sentinelWithSslConnectsToRedisWithSsl() {
    DefaultJedisClientConfig masterConfig = DefaultJedisClientConfig.builder().password("foobared")
        .clientName("sentinel-master-client").ssl(true).hostAndPortMapper(SSL_PORT_MAPPER).build();
    DefaultJedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .clientName("sentinel-client").ssl(true).hostAndPortMapper(SSL_PORT_MAPPER).build();
    try (JedisSentinelPool pool =
        new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG, masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

}
