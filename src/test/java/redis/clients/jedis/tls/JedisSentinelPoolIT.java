package redis.clients.jedis.tls;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;

/**
 * SSL/TLS tests for {@link JedisSentinelPool} using system truststore (ssl=true flag).
 * <p>
 * Tests various combinations of SSL on master and sentinel connections:
 * <ul>
 * <li>Sentinel without SSL, Redis master with SSL</li>
 * <li>Sentinel with SSL, Redis master without SSL</li>
 * <li>Both sentinel and Redis master with SSL</li>
 * </ul>
 */
public class JedisSentinelPoolIT extends RedisSentinelTlsTestBase {

  private static final GenericObjectPoolConfig<Jedis> POOL_CONFIG = new GenericObjectPoolConfig<>();

  // Endpoints for different SSL configurations
  private static EndpointConfig aclTlsEndpoint;
  private static EndpointConfig aclEndpoint;
  private static EndpointConfig sentinelTlsEndpoint;

  @BeforeAll
  public static void setUp() {
    aclTlsEndpoint = Endpoints.getRedisEndpoint("standalone0-acl-tls");
    aclEndpoint = Endpoints.getRedisEndpoint("standalone0-acl");
    sentinelTlsEndpoint = Endpoints.getRedisEndpoint("sentinel-standalone0-tls");
  }

  /**
   * Tests sentinel without SSL connecting to Redis master with SSL.
   */
  @Test
  public void sentinelWithoutSslConnectsToRedisWithSsl() {
    DefaultJedisClientConfig masterConfig = aclTlsEndpoint.getClientConfigBuilder()
        .clientName("master-client").hostAndPortMapper(PRIMARY_SSL_PORT_MAPPER).build();

    DefaultJedisClientConfig sentinelConfig = createSentinelConfigWithoutSsl();

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, masterConfig,
        sentinelConfig)) {
      pool.getResource().close();
    }

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG,
        masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

  /**
   * Tests sentinel with SSL connecting to Redis master without SSL.
   */
  @Test
  public void sentinelWithSslConnectsToRedisWithoutSsl() {
    DefaultJedisClientConfig masterConfig = aclEndpoint.getClientConfigBuilder()
        .clientName("master-client").build();

    DefaultJedisClientConfig sentinelConfig = sentinelTlsEndpoint.getClientConfigBuilder()
        .clientName("sentinel-client").hostAndPortMapper(SENTINEL_SSL_PORT_MAPPER).build();

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, masterConfig,
        sentinelConfig)) {
      pool.getResource().close();
    }

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, POOL_CONFIG,
        masterConfig, sentinelConfig)) {
      pool.getResource().close();
    }
  }

  /**
   * Tests both sentinel and Redis master with SSL.
   */
  @Test
  public void sentinelWithSslConnectsToRedisWithSsl() {
    DefaultJedisClientConfig masterConfig = aclTlsEndpoint.getClientConfigBuilder()
        .clientName("master-client").hostAndPortMapper(PRIMARY_SSL_PORT_MAPPER).build();

    DefaultJedisClientConfig sentinelConfig = sentinelTlsEndpoint.getClientConfigBuilder()
        .clientName("sentinel-client").hostAndPortMapper(SENTINEL_SSL_PORT_MAPPER).build();

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
