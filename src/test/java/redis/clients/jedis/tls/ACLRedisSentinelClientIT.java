package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.redis.test.annotations.ConditionalOnEnv;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisSentinelClient;
import redis.clients.jedis.util.EnvCondition;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.util.TestEnvUtil;

/**
 * SSL/TLS tests for {@link RedisSentinelClient} with ACL authentication (username + password).
 * <p>
 * This test class focuses on testing the ssl(true) flag approach (using system truststore) rather
 * than explicit SslOptions configuration.
 */
@ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_SOURCE, enabled = false)
public class ACLRedisSentinelClientIT extends RedisSentinelTlsTestBase {

  // Endpoint for master with ACL authentication
  private static EndpointConfig aclEndpoint;

  @RegisterExtension
  public static EnvCondition envCondition = new EnvCondition();

  @RegisterExtension
  public static RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint("standalone0-acl-tls"));

  @BeforeAll
  public static void setUp() {
    aclEndpoint = Endpoints.getRedisEndpoint("standalone0-acl-tls");
  }

  /**
   * Tests SSL connection with explicit ACL credentials (username + password).
   */
  @Test
  public void connectWithSsl() {
    DefaultJedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
        .clientName("master-client").ssl(true).user(aclEndpoint.getUsername())
        .password(aclEndpoint.getPassword()).hostAndPortMapper(PRIMARY_SSL_PORT_MAPPER).build();

    DefaultJedisClientConfig sentinelConfig = Endpoints.getRedisEndpoint("sentinel-standalone0-tls")
        .getClientConfigBuilder().clientName("sentinel-client").ssl(true)
        .hostAndPortMapper(SENTINEL_SSL_PORT_MAPPER).build();

    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels).clientConfig(masterConfig).sentinelClientConfig(sentinelConfig)
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Tests SSL connection using endpoint's client config builder (credentials from endpoint).
   */
  @Test
  public void connectWithConfig() {
    DefaultJedisClientConfig masterConfig = aclEndpoint.getClientConfigBuilder()
        .clientName("master-client").ssl(true).hostAndPortMapper(PRIMARY_SSL_PORT_MAPPER).build();

    DefaultJedisClientConfig sentinelConfig = Endpoints.getRedisEndpoint("sentinel-standalone0-tls")
        .getClientConfigBuilder().clientName("sentinel-client").ssl(true)
        .hostAndPortMapper(SENTINEL_SSL_PORT_MAPPER).build();

    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels).clientConfig(masterConfig).sentinelClientConfig(sentinelConfig)
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Tests SSL connection using SslOptions with truststore configuration.
   */
  @Test
  public void connectWithSslOptions() {
    DefaultJedisClientConfig masterConfig = aclEndpoint.getClientConfigBuilder()
        .clientName("master-client").sslOptions(sslOptions)
        .hostAndPortMapper(PRIMARY_SSL_PORT_MAPPER).build();

    DefaultJedisClientConfig sentinelConfig = createSentinelConfigWithSsl(sslOptions);

    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels).clientConfig(masterConfig).sentinelClientConfig(sentinelConfig)
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }
}
