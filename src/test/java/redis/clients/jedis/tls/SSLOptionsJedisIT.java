package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.SslOptions;

/**
 * SSL/TLS tests for {@link Jedis} using SslOptions builder pattern.
 * <p>
 * Tests various SSL/TLS connection configurations including:
 * <ul>
 * <li>Basic SSL connection with truststore</li>
 * <li>Insecure SSL mode (no certificate verification)</li>
 * <li>Custom SSL protocol</li>
 * <li>ACL authentication over SSL</li>
 * </ul>
 */
public class SSLOptionsJedisIT extends JedisTlsTestBase {

  /**
   * Tests connecting to Redis with various SSL configurations using DefaultJedisClientConfig.
   */
  @ParameterizedTest(name = "connectWithSsl_{0}")
  @MethodSource("sslOptionsProvider")
  void connectWithSsl(String testName, SslOptions ssl) {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder().sslOptions(ssl).build())) {
      jedis.auth(endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  /**
   * Tests connecting to Redis with various SSL configurations using endpoint's client config.
   */
  @ParameterizedTest(name = "connectWithClientConfig_{0}")
  @MethodSource("sslOptionsProvider")
  void connectWithClientConfig(String testName, SslOptions ssl) {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().sslOptions(ssl).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  /**
   * Tests ACL authentication over SSL.
   */
  @Test
  public void connectWithAcl() {
    try (Jedis jedis = new Jedis(aclEndpoint.getHostAndPort(),
        aclEndpoint.getClientConfigBuilder().sslOptions(sslOptions).build())) {
      assertEquals("PONG", jedis.ping());
    }
  }
}
