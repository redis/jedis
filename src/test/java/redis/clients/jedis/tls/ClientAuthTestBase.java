package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.redis.test.annotations.ConditionalOnEnv;
import io.redis.test.utils.RedisVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.SslOptions;
import redis.clients.jedis.SslVerifyMode;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.util.EnvCondition;
import redis.clients.jedis.util.RedisVersionUtil;
import redis.clients.jedis.util.TestEnvUtil;
import redis.clients.jedis.util.TlsUtil;

/**
 * Abstract base class for mTLS (mutual TLS) authentication tests.
 * <p>
 * This class provides common setup for tests that verify certificate-based client authentication.
 * It configures both truststore (for server verification) and keystore (for client authentication).
 * <p>
 * The mTLS setup requires: - A truststore containing the CA certificate to verify the Redis server
 * - A keystore containing the client certificate and private key for client authentication
 */
@ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_SOURCE, enabled = false)
public abstract class ClientAuthTestBase {

  private static final String TRUSTSTORE_PASSWORD = "changeit";
  private static final String KEYSTORE_PASSWORD = "changeit";

  /** Default mTLS user for testing */
  protected static final String MTLS_USER_1 = "mtls-user1";
  protected static final String MTLS_USER_2 = "mtls-user2";

  @RegisterExtension
  public static EnvCondition envCondition = new EnvCondition();

  protected static EndpointConfig standaloneEndpoint;
  protected static EndpointConfig clusterEndpoint;
  protected static Path trustStorePath;
  protected static Path keyStorePath1;
  protected static Path keyStorePath2;

  @BeforeAll
  public static void setUpMtlsStores() {
    standaloneEndpoint = Endpoints.getRedisEndpoint("standalone-mtls");
    clusterEndpoint = Endpoints.getRedisEndpoint("cluster-mtls");

    // Create truststore with CA certificate for server verification
    List<Path> trustedCertLocation = Collections
        .singletonList(standaloneEndpoint.getCertificatesLocation());
    trustStorePath = TlsUtil.createAndSaveTestTruststore(ClientAuthTestBase.class.getSimpleName(),
      trustedCertLocation, TRUSTSTORE_PASSWORD);
    TlsUtil.setCustomTrustStore(trustStorePath, TRUSTSTORE_PASSWORD);

    // Use pre-generated PKCS12 keystores from Docker container
    // The container generates .p12 files with password "changeit" for each TLS_CLIENT_CNS entry
    Path certLocation = standaloneEndpoint.getCertificatesLocation();
    keyStorePath1 = TlsUtil.clientKeystorePath(certLocation, MTLS_USER_1);
    keyStorePath2 = TlsUtil.clientKeystorePath(certLocation, MTLS_USER_2);
  }

  @AfterAll
  public static void tearDownMtlsStores() {
    TlsUtil.restoreOriginalTrustStore();
  }

  /**
   * Creates SslOptions configured for mTLS with the specified client keystore.
   * @param keystorePath path to the client keystore
   * @return SslOptions configured for mTLS
   */
  protected static SslOptions createMtlsSslOptions(Path keystorePath) {
    return SslOptions.builder().truststore(trustStorePath.toFile()).trustStoreType("jceks")
        .keystore(keystorePath.toFile(), KEYSTORE_PASSWORD.toCharArray()).keyStoreType("PKCS12")
        .sslVerifyMode(SslVerifyMode.FULL).build();
  }

  /**
   * Creates SslOptions for mtls-user1.
   */
  protected static SslOptions createMtlsSslOptionsUser1() {
    return createMtlsSslOptions(keyStorePath1);
  }

  /**
   * Creates SslOptions for mtls-user2.
   */
  protected static SslOptions createMtlsSslOptionsUser2() {
    return createMtlsSslOptions(keyStorePath2);
  }

  /**
   * Asserts the expected username based on Redis version.
   * <p>
   * Redis 8.6+ supports automatic certificate-based authentication via tls-auth-clients-user CN,
   * where the username is extracted from the client certificate's Common Name. For Redis versions
   * below 8.6, the user will be "default" since cert-based auth is not supported.
   * @param jedis the connected Jedis client to check version
   * @param actualUsername the actual username from ACL WHOAMI
   * @param expectedCertUser the expected username from client certificate CN
   */
  protected static void assertExpectedUsername(redis.clients.jedis.Jedis jedis,
      String actualUsername, String expectedCertUser) {
    RedisVersion version = RedisVersionUtil.getRedisVersion(jedis);
    assertUsernameForVersion(version, actualUsername, expectedCertUser);
  }

  /**
   * Asserts the expected username based on Redis version for UnifiedJedis clients (RedisClient,
   * RedisClusterClient, etc.).
   * @param jedis the connected UnifiedJedis client to check version
   * @param actualUsername the actual username from ACL WHOAMI
   * @param expectedCertUser the expected username from client certificate CN
   */
  protected static void assertExpectedUsername(UnifiedJedis jedis, String actualUsername,
      String expectedCertUser) {
    RedisVersion version = RedisVersionUtil.getRedisVersion(jedis);
    assertUsernameForVersion(version, actualUsername, expectedCertUser);
  }

  private static void assertUsernameForVersion(RedisVersion version, String actualUsername,
      String expectedCertUser) {
    if (version.isGreaterThanOrEqualTo(RedisVersion.V8_6_0)) {
      assertEquals(expectedCertUser, actualUsername,
        "Redis " + version + " supports cert-based auth, expected username from certificate CN");
    } else {
      List<String> allowedUsers = Arrays.asList("default", expectedCertUser);
      assertTrue(allowedUsers.contains(actualUsername),
        "Redis " + version + " does not support cert-based auth, expected 'default' or '"
            + expectedCertUser + "' but was '" + actualUsername + "'");
    }
  }
}
