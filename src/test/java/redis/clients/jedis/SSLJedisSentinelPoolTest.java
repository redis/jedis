package redis.clients.jedis;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.util.TlsUtil;

public class SSLJedisSentinelPoolTest {

  private static final String MASTER_NAME = "aclmaster";

  private static Set<HostAndPort> sentinels = new HashSet<>();

  private static final HostAndPortMapper SSL_PORT_MAPPER = (HostAndPort hap)
      -> new HostAndPort(hap.getHost(), hap.getPort() + 10000);

  private static final GenericObjectPoolConfig<Jedis> POOL_CONFIG = new GenericObjectPoolConfig<>();
  private static final String trustStoreName = SSLJedisSentinelPoolTest.class.getSimpleName();

  @BeforeClass
  public static void prepare() {
    List<Path> trustedCertLocation = Collections.singletonList(Paths.get("redis9-sentinel/work/tls"));
    Path trustStorePath = TlsUtil.createAndSaveTestTruststore(trustStoreName, trustedCertLocation,"changeit");
    TlsUtil.setCustomTrustStore(trustStorePath, "changeit");

    sentinels.add(HostAndPorts.getSentinelServers().get(4));
  }

  @AfterClass
  public static void teardownTrustStore() {
    TlsUtil.restoreOriginalTrustStore();
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
