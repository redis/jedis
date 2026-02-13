package redis.clients.jedis.tls;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.util.TlsUtil;

public class JedisSentinelPoolIT {

  private static EndpointConfig sentinel;

  private static final String MASTER_NAME = "aclmaster";

  private static Set<HostAndPort> sentinels = new HashSet<>();

  private static final HostAndPortMapper SSL_PORT_MAPPER = (HostAndPort hap) -> new HostAndPort(
      hap.getHost(), hap.getPort() + 10000);

  private static final HostAndPortMapper SSL_PORT_MAPPER_PRIMARY = (
      HostAndPort hap) -> new HostAndPort(hap.getHost(), hap.getPort() + 11);

  private static final GenericObjectPoolConfig<Jedis> POOL_CONFIG = new GenericObjectPoolConfig<>();
  private static final String trustStoreName = JedisSentinelPoolIT.class.getSimpleName();

  @BeforeAll
  public static void prepare() {
    sentinel = Endpoints.getRedisEndpoint("sentinel-standalone0");
    List<Path> trustedCertLocation = Collections
        .singletonList(Paths.get("redis1-2-5-8-sentinel/work/tls"));
    Path trustStorePath = TlsUtil.createAndSaveTestTruststore(trustStoreName, trustedCertLocation,
      "changeit");
    TlsUtil.setCustomTrustStore(trustStorePath, "changeit");

    sentinels.add(sentinel.getHostAndPort());
  }

  @AfterAll
  public static void teardownTrustStore() {
    TlsUtil.restoreOriginalTrustStore();
  }

  @Test
  public void sentinelWithoutSslConnectsToRedisWithSsl() {

    DefaultJedisClientConfig masterConfig = Endpoints.getRedisEndpoint("standalone0-acl-tls")
        .getClientConfigBuilder().clientName("master-client")
        .hostAndPortMapper(SSL_PORT_MAPPER_PRIMARY).build();

    DefaultJedisClientConfig sentinelConfig = sentinel.getClientConfigBuilder()
        .clientName("sentinel-client").build();

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, masterConfig,
        sentinelConfig)) {
      pool.getResource().close();
    }

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG,
        masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

  @Test
  public void sentinelWithSslConnectsToRedisWithoutSsl() {

    DefaultJedisClientConfig masterConfig = Endpoints.getRedisEndpoint("standalone0-acl")
        .getClientConfigBuilder().clientName("master-client").build();

    DefaultJedisClientConfig sentinelConfig = Endpoints.getRedisEndpoint("sentinel-standalone0-tls")
        .getClientConfigBuilder().clientName("sentinel-client").hostAndPortMapper(SSL_PORT_MAPPER)
        .build();

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, masterConfig,
        sentinelConfig)) {
      pool.getResource().close();
    }

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG,
        masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

  @Test
  public void sentinelWithSslConnectsToRedisWithSsl() {

    DefaultJedisClientConfig masterConfig = Endpoints.getRedisEndpoint("standalone0-acl-tls")
        .getClientConfigBuilder().clientName("master-client")
        .hostAndPortMapper(SSL_PORT_MAPPER_PRIMARY).build();

    DefaultJedisClientConfig sentinelConfig = Endpoints.getRedisEndpoint("sentinel-standalone0-tls")
        .getClientConfigBuilder().clientName("sentinel-client").hostAndPortMapper(SSL_PORT_MAPPER)
        .build();

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, masterConfig,
        sentinelConfig)) {
      pool.getResource().close();
    }

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG,
        masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

}
