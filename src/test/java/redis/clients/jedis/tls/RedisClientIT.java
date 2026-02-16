package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;

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
}
