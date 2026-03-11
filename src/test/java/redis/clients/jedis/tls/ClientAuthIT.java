package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.SslOptions;
import redis.clients.jedis.UnifiedJedis;

/**
 * Abstract integration test class for mTLS (mutual TLS) certificate-based authentication.
 * <p>
 * Defines common test methods that verify client certificate authentication works correctly across
 * different Redis deployment types (standalone, cluster).
 * <p>
 * Subclasses must implement factory methods to create environment-specific clients and execute
 * commands.
 */
public abstract class ClientAuthIT extends ClientAuthTestBase {

  /**
   * Creates a client with the specified SSL options.
   * <p>
   * Subclasses provide environment-specific implementations (e.g., RedisClient for standalone,
   * RedisClusterClient for cluster).
   * @param sslOptions SSL configuration for mTLS
   * @return UnifiedJedis client configured with mTLS
   */
  protected abstract UnifiedJedis createClient(SslOptions sslOptions);

  /**
   * Executes ACL WHOAMI command and returns the authenticated username.
   * <p>
   * Subclasses provide environment-specific implementations to handle different command argument
   * types (CommandArguments vs ClusterCommandArguments).
   * @param client the connected client
   * @return the authenticated username
   */
  protected abstract String executeAclWhoAmI(UnifiedJedis client);

  /**
   * Tests mTLS connection with mtls-user1 certificate.
   * <p>
   * Verifies that ACL WHOAMI returns the expected username based on Redis version.
   */
  @Test
  public void connectWithMtlsUser1() {
    SslOptions sslOptions = createMtlsSslOptionsUser1();

    try (UnifiedJedis client = createClient(sslOptions)) {
      assertEquals("PONG", client.ping());
      assertExpectedUsername(client, executeAclWhoAmI(client), MTLS_USER_1);
    }
  }

  /**
   * Tests mTLS connection with mtls-user2 certificate.
   * <p>
   * Verifies that a different certificate authenticates as a different user.
   */
  @Test
  public void connectWithMtlsUser2() {
    SslOptions sslOptions = createMtlsSslOptionsUser2();

    try (UnifiedJedis client = createClient(sslOptions)) {
      assertEquals("PONG", client.ping());
      assertExpectedUsername(client, executeAclWhoAmI(client), MTLS_USER_2);
    }
  }

  /**
   * Tests mTLS connection with mtls-user-without-acl certificate.
   * <p>
   * Verifies that when using a certificate for a user without a corresponding ACL user configured
   * in Redis, the connection succeeds and ACL WHOAMI returns "default".
   */
  @Test
  public void connectWithMtlsUserWithoutAcl() {
    SslOptions sslOptions = createMtlsSslOptionsUserWithoutAcl();

    try (UnifiedJedis client = createClient(sslOptions)) {
      assertEquals("PONG", client.ping());
      assertEquals("default", executeAclWhoAmI(client));
    }
  }

  /**
   * Tests that mTLS authenticated users can perform basic Redis operations.
   */
  @Test
  public void performBasicOperationsWithMtls() {
    SslOptions sslOptions = createMtlsSslOptionsUser1();

    try (UnifiedJedis client = createClient(sslOptions)) {
      String key = "mtls-test-key";
      String value = "mtls-test-value";

      assertEquals("OK", client.set(key, value));
      assertEquals(value, client.get(key));
      assertEquals(1, client.del(key));
    }
  }
}
