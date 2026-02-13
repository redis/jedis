package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;

/**
 * SSL/TLS tests for {@link Jedis} with ACL authentication (username + password).
 * <p>
 * This test class focuses on testing the ssl(true) flag approach (using system truststore) with ACL
 * credentials.
 */

public class ACLJedisIT extends JedisTlsTestBase {
  /**
   * Tests SSL connection with explicit ACL credentials (username + password).
   */
  @Test
  public void connectWithSsl() {
    try (Jedis jedis = new Jedis(aclEndpoint.getHost(), aclEndpoint.getPort(), true)) {
      jedis.auth(aclEndpoint.getUsername(), aclEndpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  /**
   * Tests SSL connection using DefaultJedisClientConfig with ACL credentials.
   */
  @Test
  public void connectWithConfig() {
    try (Jedis jedis = new Jedis(aclEndpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder().ssl(true).build())) {
      jedis.auth(aclEndpoint.getUsername(), aclEndpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  /**
   * Tests SSL connection using URL with credentials.
   */
  @Test
  public void connectWithUrl() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    // Test with default user endpoint
    try (
        Jedis jedis = new Jedis(endpoint.getURIBuilder().defaultCredentials().build().toString())) {
      assertEquals("PONG", jedis.ping());
    }
    // Test with ACL user endpoint
    try (Jedis jedis = new Jedis(
        aclEndpoint.getURIBuilder().defaultCredentials().build().toString())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  /**
   * Tests SSL connection using URI with credentials.
   */
  @Test
  public void connectWithUri() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    // Test with default user endpoint
    try (Jedis jedis = new Jedis(endpoint.getURIBuilder().defaultCredentials().build())) {
      assertEquals("PONG", jedis.ping());
    }
    // Test with ACL user endpoint
    try (Jedis jedis = new Jedis(aclEndpoint.getURIBuilder().defaultCredentials().build())) {
      assertEquals("PONG", jedis.ping());
    }
  }
}
