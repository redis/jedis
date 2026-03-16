package redis.clients.jedis.tls;

import static org.junit.jupiter.api.Assertions.*;
import static redis.clients.jedis.util.TlsUtil.*;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.*;
import redis.clients.jedis.util.TlsUtil;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

public class RedisClusterClientIT extends RedisClusterTestBase {

  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();

  /**
   * Provides different SslOptions configurations for parametrized tests.
   */
  protected static Stream<Arguments> sslOptionsProvider() {
    return Stream.of(Arguments.of("truststore", createSslOptions()),
      Arguments.of("insecure", SslOptions.builder().sslVerifyMode(SslVerifyMode.INSECURE).build()),
      Arguments.of("ssl-protocol",
        SslOptions.builder().sslProtocol("SSL").truststore(trustStorePath.toFile())
            .trustStoreType("jceks").sslVerifyMode(SslVerifyMode.CA).build()));
  }

  /**
   * Tests SSL discover nodes with various SSL configurations.
   */
  @ParameterizedTest(name = "testSSLDiscoverNodesAutomatically_{0}")
  @MethodSource("sslOptionsProvider")
  void testSSLDiscoverNodesAutomatically(String testName, SslOptions ssl) {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(ssl).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(0).toString()));
      assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(1).toString()));
      assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(2).toString()));
      assertEquals("PONG", jc.ping());
    }
  }

  /**
   * Tests that connecting with ssl=true flag (system truststore) works.
   */
  @Test
  void connectWithSslFlag() {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(
          DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword()).ssl(true).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      assertEquals("PONG", jc.ping());
    }
  }

  /**
   * Tests that connecting to nodes succeeds with SSL parameters and hostname verification.
   */
  @ParameterizedTest(name = "connectToNodesSucceedsWithSSLParametersAndHostMapping_{0}")
  @MethodSource("sslOptionsProvider")
  void connectToNodesSucceedsWithSSLParametersAndHostMapping(String testName, SslOptions ssl) {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(ssl).sslParameters(sslParameters).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      assertEquals("PONG", jc.ping());
    }
  }

  /**
   * Tests connecting with custom hostname verifier and SslOptions (from
   * SSLOptionsRedisClusterClientIT).
   */
  @Test
  public void connectWithCustomHostNameVerifierAndSslOptions() {
    HostnameVerifier hostnameVerifier = new TlsUtil.BasicHostnameVerifier();

    SslOptions sslOptions = SslOptions.builder().truststore(trustStorePath.toFile())
        .trustStoreType("jceks").sslVerifyMode(SslVerifyMode.CA).build();

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(sslOptions).hostnameVerifier(hostnameVerifier).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      assertEquals("PONG", jc.ping());
    }
  }

  /**
   * Tests connecting with custom SSL socket factory.
   */
  @Test
  void connectWithCustomSocketFactory() {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(
          DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword()).ssl(true)
              .sslSocketFactory(sslSocketFactoryForEnv(tlsEndpoint.getCertificatesLocation()))
              .build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      assertEquals("PONG", jc.ping());
    }
  }

  /**
   * Tests that connecting with an empty trust store fails.
   */
  @Test
  void connectWithEmptyTrustStore() throws Exception {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .ssl(true).sslSocketFactory(createTrustNoOneSslSocketFactory()).build())
        .maxAttempts(DEFAULT_REDIRECTIONS).poolConfig(DEFAULT_POOL_CONFIG).build()) {
      jc.get("foo");
      fail("Should have thrown an exception");
    } catch (JedisClusterOperationException e) {
      assertEquals("Could not initialize cluster slots cache.", e.getMessage());
    }
  }

}
