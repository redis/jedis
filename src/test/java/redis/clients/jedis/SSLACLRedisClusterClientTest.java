package redis.clients.jedis;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static redis.clients.jedis.util.TlsUtil.*;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisVersion;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.util.RedisVersionUtil;
import redis.clients.jedis.util.TlsUtil;
import redis.clients.jedis.exceptions.JedisClusterOperationException;

@SinceRedisVersion(value = "7.0.0", message = "Redis 6.2.x returns non-tls port in CLUSTER SLOTS command. Enable for  6.2.x after tests are fixed.")
@Tag("integration")
public class SSLACLRedisClusterClientTest extends RedisClusterClientTestBase {

  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();

  protected static final EndpointConfig tlsEndpoint = Endpoints.getRedisEndpoint("cluster-unbound-tls");

  // legacy test env bootstrap uses stunnel causing redis server to report non-tls port instead tls one containerised
  // test env enables tls directly on Redis nodes and in this case tls_port is correctly reported
  // TODO : remove stunnel from legacy env
  // static int tlsPortOffset = 0;
  private final HostAndPortMapper hostAndPortMap = (hostAndPort) -> {
    String host = hostAndPort.getHost();
    int port = hostAndPort.getPort();

    if ("127.0.0.1".equals(host)) {
      host = "localhost";
    }
    return new HostAndPort(host, port);
  };

  // don't map IP addresses so that we try to connect with host 127.0.0.1
  private final HostAndPortMapper portMap = (hostAndPort) -> {
    if ("localhost".equals(hostAndPort.getHost())) {
      return hostAndPort;
    }
    return new HostAndPort(hostAndPort.getHost(), hostAndPort.getPort() /* + tlsPortOffset */);
  };

  private static final String trustStoreName = SSLACLRedisClusterClientTest.class.getSimpleName();

  @BeforeAll
  public static void prepare() {
    List<Path> trustedCertLocation = Collections.singletonList(tlsEndpoint.getCertificatesLocation());
    Path trustStorePath = TlsUtil.createAndSaveTestTruststore(trustStoreName, trustedCertLocation,"changeit");

    TlsUtil.setCustomTrustStore(trustStorePath, "changeit");
  }

  @AfterAll
  public static void teardownTrustStore() {
    TlsUtil.restoreOriginalTrustStore();
  }

  @Test
  public void testSSLDiscoverNodesAutomatically() {
    DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
            .user("default").password(tlsEndpoint.getPassword()).ssl(true)
            .hostAndPortMapper(hostAndPortMap).build();

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(config)
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      Map<String, ConnectionPool> clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());

      /**
       * In versions prior to Redis 7.x, Redis does not natively support automatic port switching between TLS and
       * non-TLS ports for CLUSTER SLOTS. When using Redis 6.2.16 in a cluster mode with TLS, CLUSTER command returns
       * the regular (non-TLS) port rather than the TLS port.
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
      jc.get("foo");
    }

    try (RedisClusterClient jc2 = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(config)
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      Map clusterNodes = jc2.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(0).toString()));
      assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(1).toString()));
      assertTrue(clusterNodes.containsKey(tlsEndpoint.getHostAndPort(2).toString()));
      jc2.get("foo");
    }
  }

  @Test
  public void testSSLWithoutPortMap() {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().user("default").password(tlsEndpoint.getPassword()).ssl(true).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      /**
       * In versions prior to Redis 7.x, Redis does not natively support automatic port switching between TLS and
       * non-TLS ports for CLUSTER SLOTS. When using Redis 6.2.16 in a cluster mode with TLS, CLUSTER command returns
       * the regular (non-TLS) port rather than the TLS port.
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
        .clientConfig(DefaultJedisClientConfig.builder().user("default").password(tlsEndpoint.getPassword()).ssl(true)
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
        .clientConfig(DefaultJedisClientConfig.builder().user("default").password(tlsEndpoint.getPassword()).ssl(true)
            .sslParameters(sslParameters).hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      jc.get("foo");
      fail("It should fail after all cluster attempts.");
//    } catch (JedisClusterMaxAttemptsException e) {
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
        .clientConfig(DefaultJedisClientConfig.builder().user("default").password(tlsEndpoint.getPassword()).ssl(true)
            .sslParameters(sslParameters).hostAndPortMapper(hostAndPortMap).build())
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
        .clientConfig(DefaultJedisClientConfig.builder().user("default").password(tlsEndpoint.getPassword()).ssl(true)
            .sslParameters(sslParameters).hostAndPortMapper(hostAndPortMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
//      jc.get("key");
//      Assert.fail("There should be no reachable node in cluster.");
////    } catch (JedisNoReachableClusterNodeException e) {
    } catch (JedisClusterOperationException e) {
//      assertEquals("No reachable node in cluster.", e.getMessage());
      assertEquals("Could not initialize cluster slots cache.", e.getMessage());
    }
  }

  @Test
  public void connectWithCustomHostNameVerifier() {
    HostnameVerifier hostnameVerifier = new TlsUtil.BasicHostnameVerifier();
    HostnameVerifier localhostVerifier = new TlsUtil.LocalhostVerifier();

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", tlsEndpoint.getPort())))
        .clientConfig(DefaultJedisClientConfig.builder().user("default").password(tlsEndpoint.getPassword()).ssl(true)
            .hostnameVerifier(hostnameVerifier).hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      jc.get("foo");
      fail("It should fail after all cluster attempts.");
//    } catch (JedisClusterMaxAttemptsException e) {
    } catch (JedisClusterOperationException e) {
      // initial connection made with 'localhost' but subsequent connections to nodes use 127.0.0.1
      // which causes custom hostname verification to fail
      assertEquals("No more cluster attempts left.", e.getMessage());
    }

    try (RedisClusterClient jc2 = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("127.0.0.1", tlsEndpoint.getPort())))
        .clientConfig(DefaultJedisClientConfig.builder().user("default").password(tlsEndpoint.getPassword()).ssl(true)
            .hostnameVerifier(hostnameVerifier).hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
//      jc2.get("key");
//      Assert.fail("There should be no reachable node in cluster.");
////    } catch (JedisNoReachableClusterNodeException e) {
    } catch (JedisClusterOperationException e) {
      // JedisNoReachableClusterNodeException exception occurs from not being able to connect since
      // the socket factory fails the hostname verification
//      assertEquals("No reachable node in cluster.", e.getMessage());
      assertEquals("Could not initialize cluster slots cache.", e.getMessage());
    }

    try (RedisClusterClient jc3 = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().user("default").password(tlsEndpoint.getPassword()).ssl(true)
            .hostnameVerifier(localhostVerifier).hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      jc3.get("foo");
    }
  }

  @Test
  public void connectWithCustomSocketFactory() {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().user("default").password(tlsEndpoint.getPassword()).ssl(true)
            .sslSocketFactory(sslSocketFactoryForEnv(tlsEndpoint.getCertificatesLocation()))
            .hostAndPortMapper(portMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      assertEquals(3, jc.getClusterNodes().size());
    }
  }

  @Test
  public void connectWithEmptyTrustStore() throws Exception {
    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(tlsEndpoint.getHostAndPort()))
        .clientConfig(DefaultJedisClientConfig.builder().user("default").password(tlsEndpoint.getPassword()).ssl(true)
            .sslSocketFactory(createTrustNoOneSslSocketFactory()).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
//      jc.get("key");
//      Assert.fail("There should be no reachable node in cluster.");
////    } catch (JedisNoReachableClusterNodeException e) {
    } catch (JedisClusterOperationException e) {
//      assertEquals("No reachable node in cluster.", e.getMessage());
      assertEquals("Could not initialize cluster slots cache.", e.getMessage());
    }
  }

  @Test
  public void defaultHostAndPortUsedIfMapReturnsNull() {
    HostAndPortMapper nullHostAndPortMap = (HostAndPort hostAndPort) -> null;

    try (RedisClusterClient jc = RedisClusterClient.builder()
        .nodes(Collections.singleton(new HostAndPort("localhost", nodeInfo1.getPort())))
        .clientConfig(DefaultJedisClientConfig.builder().user("default").password(endpoint.getPassword()).ssl(false)
            .hostAndPortMapper(nullHostAndPortMap).build())
        .maxAttempts(DEFAULT_REDIRECTIONS)
        .poolConfig(DEFAULT_POOL_CONFIG)
        .build()) {
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey(nodeInfo1.toString()));
      assertTrue(clusterNodes.containsKey(nodeInfo2.toString()));
      assertTrue(clusterNodes.containsKey(nodeInfo3.toString()));
    }
  }
}
