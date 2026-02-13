package redis.clients.jedis.tls;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import io.redis.test.annotations.ConditionalOnEnv;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPortMapper;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.SslOptions;
import redis.clients.jedis.util.*;

/**
 * Abstract base class for SSL/TLS Redis cluster tests.
 * <p>
 * This class provides common setup and teardown for TLS-enabled Redis cluster tests, including
 * truststore initialization and cluster client configuration.
 * <p>
 * Uses the {@code cluster-stable-tls} endpoint for stable integration tests.
 * <p>
 * Note: The {@link RedisVersionCondition} and {@link EnabledOnCommandCondition} extensions use the
 * non-TLS {@code cluster-stable} endpoint for version/command checks because JUnit 5 extensions run
 * before {@code @BeforeAll} methods where the truststore is configured.
 */
@ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_SOURCE, enabled = false)
public abstract class TLSRedisClusterTestBase {

  private static final String ENDPOINT_NAME = "cluster-stable-tls";
  /**
   * Non-TLS endpoint used for version and command checks. Extensions run before @BeforeAll, so we
   * can't use TLS endpoints for these checks since the truststore isn't configured yet.
   */
  private static final String VERSION_CHECK_ENDPOINT_NAME = "cluster-stable";
  private static final String TRUSTSTORE_PASSWORD = "changeit";

  @RegisterExtension
  public static RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint(VERSION_CHECK_ENDPOINT_NAME));

  @RegisterExtension
  public static EnvCondition envCondition = new EnvCondition();

  @RegisterExtension
  public EnabledOnCommandCondition enabledOnCommandCondition = new EnabledOnCommandCondition(
      () -> Endpoints.getRedisEndpoint(VERSION_CHECK_ENDPOINT_NAME));

  protected static EndpointConfig tlsEndpoint;
  protected static Path trustStorePath;

  protected RedisClusterClient cluster;

  /**
   * HostAndPortMapper that maps IP addresses (127.0.0.1) to localhost for hostname verification.
   */
  protected final HostAndPortMapper hostAndPortMap = (HostAndPort hostAndPort) -> {
    String host = hostAndPort.getHost();
    int port = hostAndPort.getPort();
    if ("127.0.0.1".equals(host)) {
      host = "localhost";
    }
    return new HostAndPort(host, port);
  };

  /**
   * HostAndPortMapper that only maps localhost, leaving IP addresses unchanged. Useful for testing
   * hostname verification failures.
   */
  protected final HostAndPortMapper portMap = (HostAndPort hostAndPort) -> {
    if ("localhost".equals(hostAndPort.getHost())) {
      return hostAndPort;
    }
    return new HostAndPort(hostAndPort.getHost(), hostAndPort.getPort());
  };

  @BeforeAll
  public static void prepareEndpointAndTrustStore() {
    tlsEndpoint = Endpoints.getRedisEndpoint(ENDPOINT_NAME);
    List<Path> trustedCertLocation = Collections
        .singletonList(tlsEndpoint.getCertificatesLocation());
    trustStorePath = TlsUtil.createAndSaveTestTruststore(
      TLSRedisClusterTestBase.class.getSimpleName(), trustedCertLocation, TRUSTSTORE_PASSWORD);
    TlsUtil.setCustomTrustStore(trustStorePath, TRUSTSTORE_PASSWORD);
  }

  @AfterAll
  public static void teardownTrustStore() {
    TlsUtil.restoreOriginalTrustStore();
  }

  @BeforeEach
  public void setUp() {
    SslOptions sslOptions = SslOptions.builder().truststore(trustStorePath.toFile())
        .trustStoreType("jceks").build();

    cluster = RedisClusterClient.builder().nodes(new HashSet<>(tlsEndpoint.getHostsAndPorts()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(sslOptions).hostAndPortMapper(hostAndPortMap).build())
        .build();
    cluster.flushAll();
  }

  @AfterEach
  public void tearDown() {
    if (cluster != null) {
      cluster.flushAll();
      cluster.close();
    }
  }

  /**
   * Creates SslOptions configured for mutual TLS with the cluster. Includes both truststore (to
   * trust server) and keystore (for client cert).
   * @return SslOptions configured for mTLS
   */
  protected static SslOptions createSslOptions() {
    return SslOptions.builder().truststore(trustStorePath.toFile()).trustStoreType("jceks").build();
  }
}
