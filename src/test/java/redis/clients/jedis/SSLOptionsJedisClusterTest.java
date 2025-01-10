package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;

import io.redis.test.annotations.SinceRedisVersion;
import io.redis.test.utils.RedisVersion;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.util.RedisVersionUtil;
import redis.clients.jedis.util.TlsUtil;

@SinceRedisVersion(value = "7.0.0", message = "Redis 6.2.x returns non-tls port in CLUSTER SLOTS command. Enable for  6.2.x after test is fixed.")
public class SSLOptionsJedisClusterTest extends JedisClusterTestBase {

  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();

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

  private static final String trustStoreName = SSLOptionsJedisClusterTest.class.getSimpleName();
  private static Path trustStorePath;

  @BeforeClass
  public static void prepare() {
    List<Path> trustedCertLocation = Collections.singletonList(Paths.get("cluster-unbound/work/tls"));
    trustStorePath = TlsUtil.createAndSaveTestTruststore(trustStoreName, trustedCertLocation,"changeit");
  }

  @Test
  public void testSSLDiscoverNodesAutomatically() {
    try (JedisCluster jc2 = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().password("cluster")
            .sslOptions(SslOptions.builder()
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks").build())
            .hostAndPortMapper(hostAndPortMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      Map<String, ?> clusterNodes = jc2.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      /*
       * In versions prior to Redis 7.x, Redis does not natively support automatic port switching between TLS and non-TLS ports for CLUSTER SLOTS.
       * When using Redis 6.2.16 in a cluster mode with TLS, CLUSTER command returns the regular (non-TLS) port rather than the TLS port.
       */
      if (RedisVersionUtil.getRedisVersion(jc2.getConnectionFromSlot(0)).isLessThanOrEqualTo(RedisVersion.V7_0_0)) {
        assertTrue(clusterNodes.containsKey("127.0.0.1:7379"));
        assertTrue(clusterNodes.containsKey("127.0.0.1:7380"));
        assertTrue(clusterNodes.containsKey("127.0.0.1:7381"));
      } else {
        assertTrue(clusterNodes.containsKey("127.0.0.1:8379"));
        assertTrue(clusterNodes.containsKey("127.0.0.1:8380"));
        assertTrue(clusterNodes.containsKey("127.0.0.1:8381"));
      }
      jc2.get("foo");
    }
  }

  @Test
  public void testSSLWithoutPortMap() {
    try (JedisCluster jc = new JedisCluster(Collections.singleton(new HostAndPort("localhost", 8379)),
        DefaultJedisClientConfig.builder().password("cluster")
            .sslOptions(SslOptions.builder()
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks")
                .sslVerifyMode(SslVerifyMode.CA).build())
            .build(), DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      /**
       * In versions prior to Redis 7.x, Redis does not natively support automatic port switching between TLS and non-TLS ports for CLUSTER SLOTS.
       * When using Redis 6.2.16 in a cluster mode with TLS, CLUSTER command returns the regular (non-TLS) port rather than the TLS port.
       */
      if (RedisVersionUtil.getRedisVersion(jc.getConnectionFromSlot(0)).isLessThanOrEqualTo(RedisVersion.V7_0_0)) {
        assertTrue(clusterNodes.containsKey("127.0.0.1:7379"));
        assertTrue(clusterNodes.containsKey("127.0.0.1:7380"));
        assertTrue(clusterNodes.containsKey("127.0.0.1:7381"));
      } else {
        assertTrue(clusterNodes.containsKey("127.0.0.1:8379"));
        assertTrue(clusterNodes.containsKey("127.0.0.1:8380"));
        assertTrue(clusterNodes.containsKey("127.0.0.1:8381"));
      }
    }
  }

  @Test
  public void connectByIpAddress() {
    try (JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", 8379),
        DefaultJedisClientConfig.builder().password("cluster")
            .sslOptions(SslOptions.builder()
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks").build())
            .hostAndPortMapper(hostAndPortMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      jc.get("foo");
    }
  }

  @Test
  public void connectToNodesFailsWithSSLParametersAndNoHostMapping() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().password("cluster")
            .sslOptions(SslOptions.builder()
                .sslParameters(sslParameters)
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks").build())
            .hostAndPortMapper(portMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      jc.get("foo");
      Assert.fail("It should fail after all cluster attempts.");
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

    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().password("cluster")
            .sslOptions(SslOptions.builder()
                .sslParameters(sslParameters)
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks").build())
            .hostAndPortMapper(hostAndPortMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      jc.get("foo");
    }
  }

  @Test
  public void connectByIpAddressFailsWithSSLParameters() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

    try (JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", 8379),
        DefaultJedisClientConfig.builder().password("cluster")
            .sslOptions(SslOptions.builder()
                .sslParameters(sslParameters)
                .truststore(trustStorePath.toFile())
                .trustStoreType("jceks").build())
            .hostAndPortMapper(hostAndPortMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
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

    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().password("cluster").sslOptions(sslOptions)
            .hostnameVerifier(hostnameVerifier).hostAndPortMapper(portMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      jc.get("foo");
      Assert.fail("It should fail after all cluster attempts.");
    } catch (JedisClusterOperationException e) {
      // initial connection made with 'localhost' but subsequent connections to nodes use 127.0.0.1
      // which causes custom hostname verification to fail
      assertEquals("No more cluster attempts left.", e.getMessage());
    }

    try (JedisCluster jc2 = new JedisCluster(new HostAndPort("127.0.0.1", 8379),
        DefaultJedisClientConfig.builder().password("cluster").sslOptions(sslOptions)
            .hostnameVerifier(hostnameVerifier).hostAndPortMapper(portMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
    } catch (JedisClusterOperationException e) {
      assertEquals("Could not initialize cluster slots cache.", e.getMessage());
    }
    
    try (JedisCluster jc3 = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().password("cluster").sslOptions(sslOptions)
            .hostnameVerifier(localhostVerifier).hostAndPortMapper(portMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      jc3.get("foo");
    }
  }

}
