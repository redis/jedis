package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.SslOptions;

/**
 * Integration tests for mTLS (mutual TLS) certificate-based authentication with Jedis.
 * <p>
 * These tests verify that: - Client certificate authentication works correctly with Jedis - The
 * authenticated user matches the certificate CN (Redis 8.6+) or is "default" (older versions) -
 * Different client certificates authenticate as different users
 */
public class ClientAuthJedisIT extends ClientAuthTestBase {

  /**
   * Tests mTLS connection with mtls-user1 certificate using Jedis. Verifies that ACL WHOAMI returns
   * the expected username based on Redis version.
   */
  @Test
  public void connectWithMtlsUser1() {
    SslOptions sslOptions = createMtlsSslOptionsUser1();

    try (Jedis jedis = new Jedis(standaloneEndpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder().sslOptions(sslOptions).build())) {
      assertEquals("PONG", jedis.ping());
      // Verify username based on Redis version
      assertExpectedUsername(jedis, jedis.aclWhoAmI(), MTLS_USER_1);
    }
  }

  /**
   * Tests mTLS connection with mtls-user2 certificate using Jedis. Verifies that a different
   * certificate authenticates as a different user.
   */
  @Test
  public void connectWithMtlsUser2() {
    SslOptions sslOptions = createMtlsSslOptionsUser2();

    try (Jedis jedis = new Jedis(standaloneEndpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder().sslOptions(sslOptions).build())) {
      assertEquals("PONG", jedis.ping());
      // Verify username based on Redis version
      assertExpectedUsername(jedis, jedis.aclWhoAmI(), MTLS_USER_2);
    }
  }

  /**
   * Tests mTLS connection using host and port separately.
   */
  @Test
  public void connectWithHostAndPort() {
    SslOptions sslOptions = createMtlsSslOptionsUser1();

    try (Jedis jedis = new Jedis(standaloneEndpoint.getHost(), standaloneEndpoint.getPort(),
        DefaultJedisClientConfig.builder().sslOptions(sslOptions).build())) {
      assertEquals("PONG", jedis.ping());
      assertExpectedUsername(jedis, jedis.aclWhoAmI(), MTLS_USER_1);
    }
  }

  /**
   * Tests that mTLS authenticated users can perform basic Redis operations with Jedis.
   */
  @Test
  public void performBasicOperationsWithMtls() {
    SslOptions sslOptions = createMtlsSslOptionsUser1();

    try (Jedis jedis = new Jedis(standaloneEndpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder().sslOptions(sslOptions).build())) {
      // Test basic operations
      String key = "mtls-jedis-test-key";
      String value = "mtls-jedis-test-value";

      assertEquals("OK", jedis.set(key, value));
      assertEquals(value, jedis.get(key));
      assertEquals(1, jedis.del(key));
    }
  }
}
