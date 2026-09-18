package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisSentinelClient;
import redis.clients.jedis.SslOptions;

/**
 * SSL/TLS tests for RedisSentinelClient using SslOptions builder pattern.
 * <p>
 * Tests various SSL/TLS connection configurations including:
 * <ul>
 * <li>Basic SSL connection with truststore</li>
 * <li>Insecure SSL mode (no certificate verification)</li>
 * <li>Custom SSL protocol</li>
 * <li>ACL authentication over SSL</li>
 * </ul>
 * <p>
 * Both master and sentinel connections use SSL in these tests.
 */
public class SSLOptionsRedisSentinelClientIT extends RedisSentinelTlsTestBase {

  // Endpoint for master with ACL authentication
  private static EndpointConfig aclEndpoint;

  @BeforeAll
  public static void setUp() {
    aclEndpoint = Endpoints.getRedisEndpoint("standalone0-acl-tls");
  }

  /**
   * Tests connecting to Redis master and sentinel with various SSL configurations. Both master and
   * sentinel connections use SSL.
   */
  @ParameterizedTest(name = "connectWithSsl_{0}")
  @MethodSource("sslOptionsProvider")
  void connectWithSsl(String testName, SslOptions ssl) {
    DefaultJedisClientConfig masterConfig = aclEndpoint.getClientConfigBuilder()
        .clientName("master-client").sslOptions(ssl).hostAndPortMapper(PRIMARY_SSL_PORT_MAPPER)
        .build();

    DefaultJedisClientConfig sentinelConfig = createSentinelConfigWithSsl(ssl);

    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels).clientConfig(masterConfig).sentinelClientConfig(sentinelConfig)
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Tests ACL authentication over SSL (same as truststore test but explicitly named).
   */
  @Test
  public void connectWithAcl() {
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

  /**
   * Tests that sentinel without SSL can connect to Redis master with SSL.
   */
  @Test
  public void sentinelWithoutSslConnectsToRedisWithSsl() {
    DefaultJedisClientConfig masterConfig = aclEndpoint.getClientConfigBuilder()
        .clientName("master-client").sslOptions(sslOptions)
        .hostAndPortMapper(PRIMARY_SSL_PORT_MAPPER).build();

    DefaultJedisClientConfig sentinelConfig = createSentinelConfigWithoutSsl();

    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels).clientConfig(masterConfig).sentinelClientConfig(sentinelConfig)
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

}
