package redis.clients.jedis.scenario;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import redis.clients.jedis.DefaultRedisCredentials;
import redis.clients.jedis.Endpoint;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisCredentials;
import redis.clients.jedis.SslOptions;
import redis.clients.jedis.SslVerifyMode;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.mcf.HealthStatus;
import redis.clients.jedis.mcf.LagAwareStrategy;
import redis.clients.jedis.util.TlsUtil;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test class demonstrating SSL configuration for LagAwareStrategy
 */
@Tags({ @Tag("scenario") })
public class LagAwareStrategySslIT {
  private static EndpointConfig crdb;
  private static Endpoint restEndpoint;
  private static Supplier<RedisCredentials> credentialsSupplier;
  private static final char[] trustStorePassword = "changeit".toCharArray();

  // truststore with certificate from REST API endpoint
  private static final String trustStorePfx = LagAwareStrategySslIT.class.getSimpleName();
  private static final Path crdbTrustStore = TlsUtil.testTruststorePath(trustStorePfx + "-crdb");
  // empty truststore to test untrusted cert
  private static final Path dummyTrustStore = TlsUtil.testTruststorePath(trustStorePfx + "-dummy");

  private SSLSocketFactory origSslSocketFactory;

  @BeforeAll
  public static void beforeClass() {

    crdb = Endpoints.getRedisEndpoint("re-active-active");
    restEndpoint = RestEndpointUtil.getRestAPIEndpoint(crdb);
    credentialsSupplier = () -> new DefaultRedisCredentials("test@redis.com", "test123");
    try {
      TlsUtil.createEmptyTruststore(dummyTrustStore, trustStorePassword);
      createTrustedTruststore(crdbTrustStore, restEndpoint.getHost(), restEndpoint.getPort(),
        trustStorePassword);
    } catch (Exception e) {
      fail("Failed to create test truststore", e);
    }
  }

  @BeforeEach
  public void enforceEmptyDefaultTrustStore() {
    TlsUtil.setCustomTrustStore(dummyTrustStore, new String(trustStorePassword));
    origSslSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
  }

  @AfterEach
  public void restoreDefaultTrustStore() {
    TlsUtil.restoreOriginalTrustStore();
    HttpsURLConnection.setDefaultSSLSocketFactory(origSslSocketFactory);
  }

  @ParameterizedTest(name = "SSL mode {0} should be HEALTHY")
  @EnumSource(value = SslVerifyMode.class, names = { "FULL", "CA", "INSECURE" })
  public void healthyWhenUsingCustomTruststore(SslVerifyMode sslVerifyMode) throws Exception {
    // Create SSL options with custom truststore
    SslOptions sslOptions = SslOptions.builder()
        .truststore(crdbTrustStore.toFile(), trustStorePassword).sslVerifyMode(sslVerifyMode)
        .build();

    // Create LagAwareStrategy config with SSL support using builder
    LagAwareStrategy.Config config = LagAwareStrategy.Config
        .builder(restEndpoint, credentialsSupplier).sslOptions(sslOptions).build();

    try (LagAwareStrategy lagAwareStrategy = new LagAwareStrategy(config)) {
      assertEquals(HealthStatus.HEALTHY, lagAwareStrategy.doHealthCheck(crdb.getHostAndPort()));
    }
  }

  @Test
  void usingDefaultTruststoreWithUntrustedCertificateInsecure() {
    // Create SSL options without specifying truststore
    SslOptions sslOptions = SslOptions.builder().sslVerifyMode(SslVerifyMode.INSECURE).build();

    // Create LagAwareStrategy config with SSL support using builder
    LagAwareStrategy.Config config = LagAwareStrategy.Config
        .builder(restEndpoint, credentialsSupplier).sslOptions(sslOptions).build();

    try (LagAwareStrategy lagAwareStrategy = new LagAwareStrategy(config)) {
      assertEquals(HealthStatus.HEALTHY, lagAwareStrategy.doHealthCheck(crdb.getHostAndPort()));
    }
  }

  @ParameterizedTest(name = "SSL mode {0} should result in {1}")
  @CsvSource({ "FULL, UNHEALTHY", "CA, UNHEALTHY" })
  void usingDefaultTruststoreWithUntrustedCertificateThrowsException(SslVerifyMode sslVerifyMode,
      HealthStatus expected) {
    // Create SSL options without specifying truststore
    SslOptions sslOptions = SslOptions.builder().sslVerifyMode(sslVerifyMode).build();

    // Create LagAwareStrategy config with SSL support using builder
    LagAwareStrategy.Config config = LagAwareStrategy.Config
        .builder(restEndpoint, credentialsSupplier).sslOptions(sslOptions).build();

    try (LagAwareStrategy lagAwareStrategy = new LagAwareStrategy(config)) {
      JedisException ex = assertThrows(JedisException.class, () -> {
        lagAwareStrategy.doHealthCheck(crdb.getHostAndPort());
      });
    }
  }

  @ParameterizedTest(name = "SSL mode {0} should result in {1}")
  @CsvSource({ "FULL, HEALTHY", "CA, HEALTHY", "INSECURE, HEALTHY" })
  void healthyWhenUsingDefaultTruststoreWithTrustedCertificate(SslVerifyMode sslVerifyMode,
      HealthStatus expected) {
    // Create SSL options without specifying truststore
    SslOptions sslOptions = SslOptions.builder().sslVerifyMode(sslVerifyMode).build();

    // Create LagAwareStrategy config with SSL support using builder
    LagAwareStrategy.Config config = LagAwareStrategy.Config
        .builder(restEndpoint, credentialsSupplier).sslOptions(sslOptions).build();

    // Verify configuration - trusted cert should pass
    // Default SSL context is used if no user defined trust store is configured
    // Configure default SSL context to trust our custom CA
    // and reinitialize default SSL context for HttpsURLConnection
    TlsUtil.setCustomTrustStore(crdbTrustStore, new String(trustStorePassword));
    SSLSocketFactory orig = HttpsURLConnection.getDefaultSSLSocketFactory();
    try (LagAwareStrategy lagAwareStrategy = new LagAwareStrategy(config)) {
      HttpsURLConnection
          .setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
      assertEquals(expected, lagAwareStrategy.doHealthCheck(crdb.getHostAndPort()));
    } finally {
      TlsUtil.restoreOriginalTrustStore();
      HttpsURLConnection.setDefaultSSLSocketFactory(orig);
    }
  }

  @Test
  public void fallbackToDefaultSslContextWhenSslOptionsAreNull() {
    // Test that SSL options can be null
    // If SSL options are null, we should fallback to default SSL context
    LagAwareStrategy.Config config = LagAwareStrategy.Config
        .builder(restEndpoint, credentialsSupplier).build();

    // Verify configuration - untrusted cert should fail
    TlsUtil.setCustomTrustStore(dummyTrustStore, new String(trustStorePassword));
    SSLSocketFactory orig = HttpsURLConnection.getDefaultSSLSocketFactory();
    try (LagAwareStrategy lagAwareStrategy = new LagAwareStrategy(config)) {
      assertThrows(JedisException.class, () -> {
        lagAwareStrategy.doHealthCheck(crdb.getHostAndPort());
      });
    } finally {
      TlsUtil.restoreOriginalTrustStore();
      HttpsURLConnection.setDefaultSSLSocketFactory(orig);
    }

    // Verify configuration - trusted cert should pass
    // Default SSL context is used if no SSL options are provided
    // Configure default SSL context to trust our custom CA
    // and reinitialize default SSL context for HttpsURLConnection
    TlsUtil.setCustomTrustStore(crdbTrustStore, new String(trustStorePassword));
    orig = HttpsURLConnection.getDefaultSSLSocketFactory();
    try (LagAwareStrategy lagAwareStrategy = new LagAwareStrategy(config)) {
      HttpsURLConnection
          .setDefaultSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
      assertEquals(HealthStatus.HEALTHY, lagAwareStrategy.doHealthCheck(crdb.getHostAndPort()));
    } finally {
      TlsUtil.restoreOriginalTrustStore();
      HttpsURLConnection.setDefaultSSLSocketFactory(orig);
    }
  }

  /**
   * Downloads the server certificate from a host and pins it into a truststore.
   * @param truststore path where the truststore will be created
   * @param host host to connect to (e.g., redis.example.com)
   * @param port TLS port (e.g., 9443)
   * @param password password for the truststore
   */
  static void createTrustedTruststore(Path truststore, String host, int port, char[] password)
      throws Exception {
    X509Certificate[] serverCerts = TlsUtil.captureServerChain(host, port);

    // Pin server certificate (first in chain)
    TlsUtil.createAndSaveTruststore(new X509Certificate[] { serverCerts[0] }, truststore,
      new String(password));
  }
}
