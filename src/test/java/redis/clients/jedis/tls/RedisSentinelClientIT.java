package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.net.ssl.SSLParameters;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.RedisSentinelClient;
import redis.clients.jedis.SslOptions;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * SSL/TLS tests for {@link RedisSentinelClient} with basic authentication (password-only, no ACL).
 * <p>
 * Tests various SSL/TLS connection configurations including:
 * <ul>
 * <li>Basic SSL connection with truststore</li>
 * <li>Insecure SSL mode (no certificate verification)</li>
 * <li>Custom SSL protocol</li>
 * </ul>
 * <p>
 * This test class uses the default user with password authentication instead of ACL user. The
 * sentinel connection does not use SSL, only the master connection uses SSL.
 */
public class RedisSentinelClientIT extends RedisSentinelTlsTestBase {

  // Endpoint for master with default user (password-only, no ACL)
  private static EndpointConfig masterEndpoint;

  @BeforeAll
  public static void setUp() {
    masterEndpoint = Endpoints.getRedisEndpoint("standalone0-tls");
  }

  @ParameterizedTest(name = "connectWithSsl_{0}")
  @MethodSource("sslOptionsProvider")
  void connectWithSsl(String testName, SslOptions ssl) {
    DefaultJedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
        .clientName("master-client").sslOptions(ssl).password(masterEndpoint.getPassword())
        .hostAndPortMapper(PRIMARY_SSL_PORT_MAPPER).build();

    // Sentinel requires authentication but does not use SSL
    DefaultJedisClientConfig sentinelConfig = createSentinelConfigWithoutSsl();

    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinels).clientConfig(masterConfig).sentinelClientConfig(sentinelConfig)
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Verifies that hostname verification is enabled by default for sentinel connections. Connection
   * should fail when hostname doesn't match the certificate CN/SAN.
   */
  @Test
  public void connectWithWrongHost() {
    // Sentinel config with hostname mismatch should fail with default hostname verification
    DefaultJedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .user(sentinelWrongHost.getUsername()).password(sentinelWrongHost.getPassword()).ssl(true)
        .build();
    assertThrows(JedisConnectionException.class, () -> {
      try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
          .sentinels(sentinelsWrongHost).sentinelClientConfig(sentinelConfig).build()) {
        client.ping();
      }
    });
  }

  /**
   * Verifies that hostname verification can be disabled for sentinel connections by providing
   * custom SSLParameters without endpoint identification algorithm.
   */
  @Test
  public void connectWrongHostWithSslParameters() {
    // Custom SSLParameters without endpoint identification allows connection despite hostname
    // mismatch
    JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .user(sentinelWrongHost.getUsername()).password(sentinelWrongHost.getPassword()).ssl(true)
        .sslParameters(new SSLParameters()).build();

    // Master uses correct hostname and SSL
    DefaultJedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
        .clientName("master-client").ssl(true).password(masterEndpoint.getPassword())
        .hostAndPortMapper(PRIMARY_SSL_PORT_MAPPER).build();

    try (RedisSentinelClient client = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .sentinels(sentinelsWrongHost).clientConfig(masterConfig)
        .sentinelClientConfig(sentinelConfig).build()) {
      assertEquals("PONG", client.ping());
    }
  }

}
