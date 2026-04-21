package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.cert.CertificateException;

import javax.net.ssl.SSLHandshakeException;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * SSL connection tests using badssl.com to verify proper SSL/TLS error handling.
 * <p>
 * These tests verify that Jedis correctly handles various SSL certificate issues when using the
 * simple {@code ssl(true)} configuration:
 * <ul>
 * <li>wrong.host.badssl.com - Certificate is valid but issued for a different hostname</li>
 * <li>expired.badssl.com - Certificate has expired</li>
 * </ul>
 * <p>
 * Note: badssl.com is an HTTPS server, not a Redis server. The tests verify that appropriate
 * SSL/TLS errors are thrown during the SSL handshake, before any Redis protocol communication
 * occurs.
 * <p>
 * <b>Security Note:</b> By default, Java's SSLSocket does NOT perform hostname verification. To
 * enable it, SSLParameters.setEndpointIdentificationAlgorithm("HTTPS") must be set. See:
 * https://docs.oracle.com/en/java/javase/17/security/java-secure-socket-extension-jsse-reference-guide.html
 */
public class SSLConnectionIT {

  /**
   * Tests connecting to a host with a certificate issued for a different hostname using the simple
   * {@code ssl(true)} configuration.
   * <p>
   * The certificate at wrong.host.badssl.com is valid and not expired, but it's issued for
   * *.badssl.com which doesn't match the subdomain "wrong.host" (the wildcard only covers one level
   * of subdomain).
   * <p>
   * <b>Expected behavior:</b> The SSL handshake should fail with an SSLHandshakeException
   * containing a CertificateException indicating hostname mismatch.
   */
  @Test
  public void connectToWrongHost() {
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().ssl(true).build();

    try (RedisClient client = RedisClient.builder().clientConfig(config)
        .hostAndPort("wrong.host.badssl.com", 443).build()) {

      JedisConnectionException exception = assertThrows(JedisConnectionException.class,
        () -> client.ping());

      // Verify SSL handshake fails due to hostname mismatch
      Throwable cause = exception.getCause();
      while (cause != null && !(cause instanceof SSLHandshakeException)) {
        cause = cause.getCause();
      }
      assertTrue(cause instanceof SSLHandshakeException,
        "Expected SSLHandshakeException for hostname mismatch, but got: " + exception);

      // Verify the cause chain contains CertificateException for hostname verification failure
      Throwable certCause = cause;
      while (certCause != null && !(certCause instanceof CertificateException)) {
        certCause = certCause.getCause();
      }
      assertTrue(certCause instanceof CertificateException,
        "Expected CertificateException in cause chain for hostname mismatch, but got: " + cause);
    }
  }

  /**
   * Tests connecting to a host with an expired certificate using the simple {@code ssl(true)}
   * configuration.
   * <p>
   * The certificate at expired.badssl.com is intentionally expired for testing purposes.
   * <p>
   * <b>Expected behavior:</b> The SSL handshake should fail with an SSLHandshakeException due to
   * certificate expiry.
   */
  @Test
  public void connectToExpiredCertificate() {
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().ssl(true).build();

    try (RedisClient client = RedisClient.builder().clientConfig(config)
        .hostAndPort("expired.badssl.com", 443).build()) {

      JedisConnectionException exception = assertThrows(JedisConnectionException.class,
        () -> client.ping());

      // Verify the root cause is an SSL handshake failure due to expired certificate
      Throwable cause = exception.getCause();
      while (cause != null && !(cause instanceof SSLHandshakeException)) {
        cause = cause.getCause();
      }
      assertTrue(cause instanceof SSLHandshakeException,
        "Expected SSLHandshakeException in cause chain, but got: " + exception);
    }
  }
}
