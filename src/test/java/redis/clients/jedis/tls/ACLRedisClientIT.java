package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;

/**
 * SSL/TLS tests for {@link RedisClient} with ACL authentication (username + password).
 * <p>
 * This test class focuses on testing the ssl(true) flag approach (using system truststore) with ACL
 * credentials.
 */

public class ACLRedisClientIT extends RedisClientTlsTestBase {
  /**
   * Tests SSL connection with explicit ACL credentials (username + password).
   */
  @Test
  public void connectWithSsl() {
    try (
        RedisClient client = RedisClient.builder()
            .hostAndPort(aclEndpoint.getHost(), aclEndpoint.getPort())
            .clientConfig(DefaultJedisClientConfig.builder().ssl(true)
                .user(aclEndpoint.getUsername()).password(aclEndpoint.getPassword()).build())
            .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Tests SSL connection using endpoint's client config builder (credentials from endpoint).
   */
  @Test
  public void connectWithConfig() {
    try (
        RedisClient client = RedisClient.builder().hostAndPort(aclEndpoint.getHostAndPort())
            .clientConfig(DefaultJedisClientConfig.builder().ssl(true)
                .user(aclEndpoint.getUsername()).password(aclEndpoint.getPassword()).build())
            .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Tests SSL connection using URL with credentials.
   */
  @Test
  public void connectWithUrl() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    // Test with default user endpoint
    try (RedisClient client = RedisClient
        .create(endpoint.getURIBuilder().defaultCredentials().build().toString())) {
      assertEquals("PONG", client.ping());
    }
    // Test with ACL user endpoint
    try (RedisClient client = RedisClient
        .create(aclEndpoint.getURIBuilder().defaultCredentials().build().toString())) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Tests SSL connection using URI with credentials.
   */
  @Test
  public void connectWithUri() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    // Test with default user endpoint
    try (RedisClient client = RedisClient
        .create(endpoint.getURIBuilder().defaultCredentials().build())) {
      assertEquals("PONG", client.ping());
    }
    // Test with ACL user endpoint
    try (RedisClient client = RedisClient
        .create(aclEndpoint.getURIBuilder().defaultCredentials().build())) {
      assertEquals("PONG", client.ping());
    }
  }
}
