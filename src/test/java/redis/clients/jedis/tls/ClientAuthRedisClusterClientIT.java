package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.redis.test.annotations.ConditionalOnEnv;
import io.redis.test.utils.RedisVersion;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import redis.clients.jedis.*;
import redis.clients.jedis.util.EnvCondition;
import redis.clients.jedis.util.RedisVersionUtil;
import redis.clients.jedis.util.TestEnvUtil;
import redis.clients.jedis.util.TlsUtil;

/**
 * Integration tests for mTLS (mutual TLS) certificate-based authentication with Redis Cluster.
 * <p>
 * These tests verify that: - Client certificate authentication works correctly with Redis cluster -
 * The authenticated user matches the certificate CN - Cluster operations work correctly with mTLS
 */
@ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_SOURCE, enabled = false)
public class ClientAuthRedisClusterClientIT {

  private static final String TRUSTSTORE_PASSWORD = "changeit";
  private static final String KEYSTORE_PASSWORD = "changeit";
  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();

  protected static final String MTLS_USER_1 = "mtls-user1";
  protected static final String MTLS_USER_2 = "mtls-user2";

  @RegisterExtension
  public static EnvCondition envCondition = new EnvCondition();

  protected static EndpointConfig clusterEndpoint;
  protected static Path trustStorePath;
  protected static Path keyStorePath1;
  protected static Path keyStorePath2;

  @BeforeAll
  public static void setUpMtlsStores() {
    clusterEndpoint = Endpoints.getRedisEndpoint("cluster-mtls");

    // Create truststore with CA certificate for server verification
    List<Path> trustedCertLocation = Collections
        .singletonList(clusterEndpoint.getCertificatesLocation());
    trustStorePath = TlsUtil.createAndSaveTestTruststore(
      ClientAuthRedisClusterClientIT.class.getSimpleName(), trustedCertLocation,
      TRUSTSTORE_PASSWORD);
    TlsUtil.setCustomTrustStore(trustStorePath, TRUSTSTORE_PASSWORD);

    // Use pre-generated PKCS12 keystores from Docker container
    // The container generates .p12 files with password "changeit" for each TLS_CLIENT_CNS entry
    Path certLocation = clusterEndpoint.getCertificatesLocation();
    keyStorePath1 = TlsUtil.clientKeystorePath(certLocation, MTLS_USER_1);
    keyStorePath2 = TlsUtil.clientKeystorePath(certLocation, MTLS_USER_2);
  }

  @AfterAll
  public static void tearDownMtlsStores() {
    TlsUtil.restoreOriginalTrustStore();
  }

  private static SslOptions createMtlsSslOptions(Path keystorePath) {
    return SslOptions.builder().truststore(trustStorePath.toFile()).trustStoreType("jceks")
        .keystore(keystorePath.toFile(), KEYSTORE_PASSWORD.toCharArray()).keyStoreType("PKCS12")
        .sslVerifyMode(SslVerifyMode.FULL).build();
  }

  /**
   * Executes ACL WHOAMI command and returns the username.
   */
  private String aclWhoAmI(RedisClusterClient cluster) {
    return cluster.executeCommand(new CommandObject<>(
        new ClusterCommandArguments(Protocol.Command.ACL).add("WHOAMI"), BuilderFactory.STRING));
  }

  /**
   * Asserts the expected username based on Redis version.
   */
  private void assertExpectedUsername(UnifiedJedis jedis, String actualUsername,
      String expectedCertUser) {
    RedisVersion version = RedisVersionUtil.getRedisVersion(jedis);
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

  /**
   * Tests mTLS connection to cluster with mtls-user1 certificate. Verifies cluster node discovery
   * and ACL WHOAMI based on Redis version.
   */
  @Test
  public void connectWithMtlsUser1() {
    SslOptions sslOptions = createMtlsSslOptions(keyStorePath1);

    try (RedisClusterClient cluster = RedisClusterClient.builder()
        .nodes(new HashSet<>(clusterEndpoint.getHostsAndPorts()))
        .clientConfig(DefaultJedisClientConfig.builder().sslOptions(sslOptions).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      assertEquals("PONG", cluster.ping());
      assertExpectedUsername(cluster, aclWhoAmI(cluster), MTLS_USER_1);
    }
  }

  /**
   * Tests mTLS connection to cluster with mtls-user2 certificate.
   */
  @Test
  public void connectWithMtlsUser2() {
    SslOptions sslOptions = createMtlsSslOptions(keyStorePath2);

    try (RedisClusterClient cluster = RedisClusterClient.builder()
        .nodes(new HashSet<>(clusterEndpoint.getHostsAndPorts()))
        .clientConfig(DefaultJedisClientConfig.builder().sslOptions(sslOptions).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      assertEquals("PONG", cluster.ping());
      assertExpectedUsername(cluster, aclWhoAmI(cluster), MTLS_USER_2);
    }
  }

  /**
   * Tests that cluster node discovery works with mTLS.
   */
  @Test
  public void discoverClusterNodesWithMtls() {
    SslOptions sslOptions = createMtlsSslOptions(keyStorePath1);

    try (RedisClusterClient cluster = RedisClusterClient.builder()
        .nodes(Collections.singleton(clusterEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().sslOptions(sslOptions).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      Map<String, ?> clusterNodes = cluster.getClusterNodes();
      // Should discover all 3 cluster nodes
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey(clusterEndpoint.getHostAndPort(0).toString()));
      assertTrue(clusterNodes.containsKey(clusterEndpoint.getHostAndPort(1).toString()));
      assertTrue(clusterNodes.containsKey(clusterEndpoint.getHostAndPort(2).toString()));
    }
  }

  /**
   * Tests basic cluster operations with mTLS authentication.
   */
  @Test
  public void performClusterOperationsWithMtls() {
    SslOptions sslOptions = createMtlsSslOptions(keyStorePath1);

    try (RedisClusterClient cluster = RedisClusterClient.builder()
        .nodes(new HashSet<>(clusterEndpoint.getHostsAndPorts()))
        .clientConfig(DefaultJedisClientConfig.builder().sslOptions(sslOptions).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {

      // Test basic operations across cluster
      String key1 = "mtls-cluster-key1";
      String key2 = "mtls-cluster-key2";
      String value = "mtls-cluster-value";

      assertEquals("OK", cluster.set(key1, value));
      assertEquals("OK", cluster.set(key2, value));
      assertEquals(value, cluster.get(key1));
      assertEquals(value, cluster.get(key2));
      assertEquals(1, cluster.del(key1));
      assertEquals(1, cluster.del(key2));
    }
  }
}
