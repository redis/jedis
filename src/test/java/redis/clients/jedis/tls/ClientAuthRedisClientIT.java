package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.*;

/**
 * Integration tests for mTLS (mutual TLS) certificate-based authentication with standalone Redis.
 * <p>
 * These tests verify that: - Client certificate authentication works correctly - The authenticated
 * user matches the certificate CN (Redis 8.6+) or is "default" (older versions) - Different client
 * certificates authenticate as different users
 */
public class ClientAuthRedisClientIT extends ClientAuthTestBase {

  /**
   * Executes ACL WHOAMI command and returns the username.
   */
  private String aclWhoAmI(RedisClient client) {
    return client.executeCommand(new CommandObject<>(
        new CommandArguments(Protocol.Command.ACL).add("WHOAMI"), BuilderFactory.STRING));
  }

  /**
   * Tests mTLS connection with mtls-user1 certificate. Verifies that ACL WHOAMI returns the
   * expected username based on Redis version.
   */
  @Test
  public void connectWithMtlsUser1() {
    SslOptions sslOptions = createMtlsSslOptionsUser1();

    try (RedisClient client = RedisClient.builder()
        .hostAndPort(standaloneEndpoint.getHost(), standaloneEndpoint.getPort())
        .clientConfig(DefaultJedisClientConfig.builder().sslOptions(sslOptions).build()).build()) {
      assertEquals("PONG", client.ping());
      // Verify username based on Redis version
      assertExpectedUsername(client, aclWhoAmI(client), MTLS_USER_1);
    }
  }

  /**
   * Tests mTLS connection with mtls-user2 certificate. Verifies that a different certificate
   * authenticates as a different user.
   */
  @Test
  public void connectWithMtlsUser2() {
    SslOptions sslOptions = createMtlsSslOptionsUser2();

    try (RedisClient client = RedisClient.builder()
        .hostAndPort(standaloneEndpoint.getHost(), standaloneEndpoint.getPort())
        .clientConfig(DefaultJedisClientConfig.builder().sslOptions(sslOptions).build()).build()) {
      assertEquals("PONG", client.ping());
      // Verify username based on Redis version
      assertExpectedUsername(client, aclWhoAmI(client), MTLS_USER_2);
    }
  }

  /**
   * Tests mTLS connection using HostAndPort object.
   */
  @Test
  public void connectWithHostAndPort() {
    SslOptions sslOptions = createMtlsSslOptionsUser1();

    try (RedisClient client = RedisClient.builder().hostAndPort(standaloneEndpoint.getHostAndPort())
        .clientConfig(DefaultJedisClientConfig.builder().sslOptions(sslOptions).build()).build()) {
      assertEquals("PONG", client.ping());
      assertExpectedUsername(client, aclWhoAmI(client), MTLS_USER_1);
    }
  }

  /**
   * Tests that mTLS authenticated users can perform basic Redis operations.
   */
  @Test
  public void performBasicOperationsWithMtls() {
    SslOptions sslOptions = createMtlsSslOptionsUser1();

    try (RedisClient client = RedisClient.builder().hostAndPort(standaloneEndpoint.getHostAndPort())
        .clientConfig(DefaultJedisClientConfig.builder().sslOptions(sslOptions).build()).build()) {
      // Test basic operations
      String key = "mtls-test-key";
      String value = "mtls-test-value";

      assertEquals("OK", client.set(key, value));
      assertEquals(value, client.get(key));
      assertEquals(1, client.del(key));
    }
  }
}
