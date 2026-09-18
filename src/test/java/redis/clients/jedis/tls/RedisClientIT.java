package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.exceptions.JedisConnectionException;

import javax.net.ssl.SSLParameters;

/**
 * SSL/TLS tests for {@link RedisClient} with basic authentication (password-only, no ACL).
 * <p>
 * Uses the system truststore (ssl=true flag) for SSL connections.
 */
public class RedisClientIT extends RedisClientTlsTestBase {

  @Test
  public void connectWithSsl() {
    try (RedisClient client = RedisClient.builder()
        .hostAndPort(endpoint.getHost(), endpoint.getPort())
        .clientConfig(
          DefaultJedisClientConfig.builder().ssl(true).password(endpoint.getPassword()).build())
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  @Test
  public void connectWithConfig() {
    try (RedisClient client = RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
        .clientConfig(
          DefaultJedisClientConfig.builder().ssl(true).password(endpoint.getPassword()).build())
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Tests opening a default SSL/TLS connection to redis using "rediss://" scheme url.
   */
  @Test
  public void connectWithUrl() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    // URI includes credentials via defaultCredentials()
    try (RedisClient client = RedisClient
        .create(endpoint.getURIBuilder().defaultCredentials().build().toString())) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Tests opening a default SSL/TLS connection to redis.
   */
  @Test
  public void connectWithUri() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    // URI includes credentials via defaultCredentials()
    try (RedisClient client = RedisClient
        .create(endpoint.getURIBuilder().defaultCredentials().build())) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Verifies that hostname verification is enabled by default when using ssl(true). Connection
   * should fail when hostname doesn't match the certificate CN/SAN.
   */
  @Test
  public void connectWithWrongHost() {
    // Connection with hostname mismatch should fail with default hostname verification
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().ssl(true)
        .password(wrongHostEndpoint.getPassword()).build();
    try (RedisClient client = RedisClient.builder()
        .hostAndPort(wrongHostEndpoint.getHost(), wrongHostEndpoint.getPort()).clientConfig(config)
        .build()) {
      assertThrows(JedisConnectionException.class, client::ping);
    }

    // Same test using URI
    try (RedisClient client = RedisClient
        .create(wrongHostEndpoint.getURIBuilder().defaultCredentials().build())) {
      assertThrows(JedisConnectionException.class, client::ping);
    }
  }

  /**
   * Verifies that hostname verification can be disabled by providing custom SSLParameters without
   * endpoint identification algorithm set.
   */
  @Test
  public void connectWrongHostWithSslParameters() {
    // Custom SSLParameters without endpoint identification allows connection despite hostname
    // mismatch
    JedisClientConfig config = DefaultJedisClientConfig.builder().ssl(true)
        .sslParameters(new SSLParameters()).user(wrongHostEndpoint.getUsername())
        .password(wrongHostEndpoint.getPassword()).build();
    try (RedisClient client = RedisClient.builder()
        .hostAndPort(wrongHostEndpoint.getHost(), wrongHostEndpoint.getPort()).clientConfig(config)
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }
}
