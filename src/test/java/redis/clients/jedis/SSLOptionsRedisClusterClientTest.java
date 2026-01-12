package redis.clients.jedis;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisVersion;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.util.RedisVersionUtil;
import redis.clients.jedis.util.TlsUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SinceRedisVersion(value = "7.0.0", message = "Redis 6.2.x returns non-tls port in CLUSTER SLOTS command. Enable for  6.2.x after test is fixed.")
@Tag("integration")
public class SSLOptionsRedisClusterClientTest extends RedisClusterClientTestBase {

  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();

  protected static EndpointConfig tlsEndpoint;

  private final HostAndPortMapper hostAndPortMap = (HostAndPort hostAndPort) -> {
    String host = hostAndPort.getHost();
    int port = hostAndPort.getPort();
    if (host.equals("127.0.0.1")) {
      host = "localhost";
    }
    return new HostAndPort(host, port);
  };

  // don't map IP addresses so that we try to connect with host 127.0.0.1
  private final HostAndPortMapper portMap = (HostAndPort hostAndPort) -> {
    if ("localhost".equals(hostAndPort.getHost())) {
      return hostAndPort;
    }
    return new HostAndPort(hostAndPort.getHost(), hostAndPort.getPort());
  };

  private static final String trustStoreName = SSLOptionsRedisClusterClientTest.class.getSimpleName();
  private static Path trustStorePath;

  @BeforeAll
  public static void prepare() {
    tlsEndpoint = Endpoints.getRedisEndpoint("cluster-unbound-tls");
    List<Path> trustedCertLocation = Collections.singletonList(tlsEndpoint.getCertificatesLocation());
    trustStorePath = TlsUtil.createAndSaveTestTruststore(trustStoreName, trustedCertLocation,"changeit");
  }

  @Test
  public void testSSLDiscoverNodesAutomatically() {
    try (RedisClusterClient jc2 = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(SslOptions.builder()
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks").build())
            .hostAndPortMapper(hostAndPortMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      Map<String, ?> clusterNodes = jc2.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      /*
       * In versions prior to Redis 7.x, Redis does not natively support automatic port switching between TLS and non-TLS ports for CLUSTER SLOTS.
       * When using Redis 6.2.16 in a cluster mode with TLS, CLUSTER command returns the regular (non-TLS) port rather than the TLS port.
       */
      if (RedisVersionUtil.getRedisVersion(jc2.getConnectionFromSlot(0)).isLessThanOrEqualTo(RedisVersion.V7_0_0)) {
        assertTrue(clusterNodes.containsKey(nodeInfo1.toString()));
        assertTrue(clusterNodes.containsKey(nodeInfo2.toString()));
        assertTrue(clusterNodes.containsKey(nodeInfo3.toString()));
      } else {
        assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(0).toString()));
        assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(1).toString()));
        assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(2).toString()));
      }
      jc2.get("foo");
    }
  }

  @Test
  public void testSSLWithoutPortMap() {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(SslOptions.builder()
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks")
                .sslVerifyMode(SslVerifyMode.CA).build())
            .build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      /**
       * In versions prior to Redis 7.x, Redis does not natively support automatic port switching between TLS and non-TLS ports for CLUSTER SLOTS.
       * When using Redis 6.2.16 in a cluster mode with TLS, CLUSTER command returns the regular (non-TLS) port rather than the TLS port.
       */
      if (RedisVersionUtil.getRedisVersion(jc.getConnectionFromSlot(0)).isLessThanOrEqualTo(RedisVersion.V7_0_0)) {
        assertTrue(clusterNodes.containsKey(nodeInfo1.toString()));
        assertTrue(clusterNodes.containsKey(nodeInfo2.toString()));
        assertTrue(clusterNodes.containsKey(nodeInfo3.toString()));
      } else {
        assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(0).toString()));
        assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(1).toString()));
        assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(2).toString()));
      }
    }
  }

  @Test
  public void connectByIpAddress() {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(SslOptions.builder()
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks").build())
            .hostAndPortMapper(hostAndPortMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      jc.get("foo");
    }
  }

  @Test
  public void connectToNodesFailsWithSSLParametersAndNoHostMapping() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", tlsEndpoint.getPort())))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(SslOptions.builder()
                .sslParameters(sslParameters)
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks").build())
            .hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      jc.get("foo");
      fail("It should fail after all cluster attempts.");
    } catch (JedisClusterOperationException e) {
      // initial connection to localhost works, but subsequent connections to nodes use 127.0.0.1
      // and fail hostname verification
      assertEquals("No more cluster attempts left.", e.getMessage());
    }
  }

  @Test
  public void connectToNodesSucceedsWithSSLParametersAndHostMapping() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(SslOptions.builder()
                .sslParameters(sslParameters)
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks").build())
            .hostAndPortMapper(hostAndPortMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      jc.get("foo");
    }
  }

  @Test
  public void connectByIpAddressFailsWithSSLParameters() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword())
            .sslOptions(SslOptions.builder()
                .sslParameters(sslParameters)
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks").build())
            .hostAndPortMapper(hostAndPortMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
    } catch (JedisClusterOperationException e) {
      assertEquals("Could not initialize cluster slots cache.", e.getMessage());
    }
  }

  @Test
  public void connectWithCustomHostNameVerifier() {
    HostnameVerifier hostnameVerifier = new TlsUtil.BasicHostnameVerifier();
    HostnameVerifier localhostVerifier = new TlsUtil.LocalhostVerifier();

    SslOptions sslOptions = SslOptions.builder()
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks")
                .sslVerifyMode(SslVerifyMode.CA).build();

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", tlsEndpoint.getPort())))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword()).sslOptions(sslOptions)
            .hostnameVerifier(hostnameVerifier).hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      jc.get("foo");
      fail("It should fail after all cluster attempts.");
    } catch (JedisClusterOperationException e) {
      // initial connection made with 'localhost' but subsequent connections to nodes use 127.0.0.1
      // which causes custom hostname verification to fail
      assertEquals("No more cluster attempts left.", e.getMessage());
    }

    try (RedisClusterClient jc2 = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("127.0.0.1", tlsEndpoint.getPort())))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword()).sslOptions(sslOptions)
            .hostnameVerifier(hostnameVerifier).hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
    } catch (JedisClusterOperationException e) {
      assertEquals("Could not initialize cluster slots cache.", e.getMessage());
    }

    try (RedisClusterClient jc3 = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().password(tlsEndpoint.getPassword()).sslOptions(sslOptions)
            .hostnameVerifier(localhostVerifier).hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      jc3.get("foo");
    }
  }

}
