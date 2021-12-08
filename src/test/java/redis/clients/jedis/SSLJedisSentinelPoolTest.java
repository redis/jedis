package redis.clients.jedis;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.BeforeClass;
import org.junit.Test;

public class SSLJedisSentinelPoolTest {

  private static final String MASTER_NAME = "aclmaster";

  private static Set<HostAndPort> sentinels = new HashSet<>();

  private static final HostAndPortMapper SSL_PORT_MAPPER = (HostAndPort hap)
      -> new HostAndPort(hap.getHost(), hap.getPort() + 10000);

  private static final GenericObjectPoolConfig<Jedis> POOL_CONFIG = new GenericObjectPoolConfig<>();

  @BeforeClass
  public static void prepare() {
    SSLJedisTest.setupTrustStore();

    sentinels.add(HostAndPorts.getSentinelServers().get(4));
  }

  @Test
  public void sentinelWithoutSslConnectsToRedisWithSsl() {

    DefaultJedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
        .user("acljedis").password("fizzbuzz").clientName("master-client").ssl(true)
        .hostAndPortMapper(SSL_PORT_MAPPER).build();

    DefaultJedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .user("sentinel").password("foobared").clientName("sentinel-client").ssl(false).build();

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG,
        masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

  @Test
  public void sentinelWithSslConnectsToRedisWithoutSsl() {

    DefaultJedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
        .user("acljedis").password("fizzbuzz").clientName("master-client").ssl(false).build();

    DefaultJedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .user("sentinel").password("foobared").clientName("sentinel-client")
        .ssl(true).hostAndPortMapper(SSL_PORT_MAPPER).build();

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG,
        masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

  @Test
  public void sentinelWithSslConnectsToRedisWithSsl() {

    DefaultJedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
        .user("acljedis").password("fizzbuzz").clientName("master-client").ssl(true)
        .hostAndPortMapper(SSL_PORT_MAPPER).build();

    DefaultJedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .user("sentinel").password("foobared").clientName("sentinel-client")
        .ssl(true).hostAndPortMapper(SSL_PORT_MAPPER).build();

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG,
        masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

}
