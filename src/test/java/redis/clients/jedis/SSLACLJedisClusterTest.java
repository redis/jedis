package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.SSLJedisTest.BasicHostnameVerifier;
import redis.clients.jedis.util.RedisVersionUtil;

public class SSLACLJedisClusterTest extends JedisClusterTestBase {

  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();

  private final HostAndPortMapper hostAndPortMap = (HostAndPort hostAndPort) -> {
    String host = hostAndPort.getHost();
    int port = hostAndPort.getPort();
    if (host.equals("127.0.0.1")) {
      host = "localhost";
      port = port + 1000;
    }
    return new HostAndPort(host, port);
  };

  // don't map IP addresses so that we try to connect with host 127.0.0.1
  private final HostAndPortMapper portMap = (HostAndPort hostAndPort) -> {
    if ("localhost".equals(hostAndPort.getHost())) {
      return hostAndPort;
    }
    return new HostAndPort(hostAndPort.getHost(), hostAndPort.getPort() + 1000);
  };

  @BeforeClass
  public static void prepare() {
    // We need to set up certificates first before connecting to the endpoint with enabled TLS
    SSLJedisTest.setupTrustStore();

    // TODO(imalinovskyi): Remove hardcoded connection settings
    //  once this test is refactored to support RE
    org.junit.Assume.assumeTrue("Not running ACL test on this version of Redis",
            RedisVersionUtil.checkRedisMajorVersionNumber(6,
                    new EndpointConfig(new HostAndPort("localhost", 8379),
                            "default", "cluster", true)));
  }

  @Test
  public void testSSLDiscoverNodesAutomatically() {
    try (JedisCluster jc = new JedisCluster(Collections.singleton(new HostAndPort("localhost", 8379)),
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(true)
            .hostAndPortMapper(hostAndPortMap).build(), DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      Map clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey("127.0.0.1:7379"));
      assertTrue(clusterNodes.containsKey("127.0.0.1:7380"));
      assertTrue(clusterNodes.containsKey("127.0.0.1:7381"));
      jc.get("foo");
    }

    try (JedisCluster jc2 = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(true)
            .hostAndPortMapper(hostAndPortMap).build(), DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      Map clusterNodes = jc2.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey("127.0.0.1:7379"));
      assertTrue(clusterNodes.containsKey("127.0.0.1:7380"));
      assertTrue(clusterNodes.containsKey("127.0.0.1:7381"));
      jc2.get("foo");
    }
  }

  @Test
  public void testSSLWithoutPortMap() {
    try (JedisCluster jc = new JedisCluster(Collections.singleton(new HostAndPort("localhost", 8379)),
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(true).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
//      Map<String, JedisPool> clusterNodes = jc.getClusterNodes();
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey("127.0.0.1:7379"));
      assertTrue(clusterNodes.containsKey("127.0.0.1:7380"));
      assertTrue(clusterNodes.containsKey("127.0.0.1:7381"));
    }
  }

  @Test
  public void connectByIpAddress() {
    try (JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", 7379),
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(true)
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
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(true)
            .sslParameters(sslParameters).hostAndPortMapper(portMap).build(), DEFAULT_REDIRECTIONS,
        DEFAULT_POOL_CONFIG)) {
      jc.get("foo");
      Assert.fail("It should fail after all cluster attempts.");
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

    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(true)
            .sslParameters(sslParameters).hostAndPortMapper(hostAndPortMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      jc.get("foo");
    }
  }

  @Test
  public void connectByIpAddressFailsWithSSLParameters() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");

    try (JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", 8379),
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(true)
            .sslParameters(sslParameters).hostAndPortMapper(hostAndPortMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
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
    HostnameVerifier hostnameVerifier = new BasicHostnameVerifier();
    HostnameVerifier localhostVerifier = new LocalhostVerifier();

    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(true)
            .hostnameVerifier(hostnameVerifier).hostAndPortMapper(portMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      jc.get("foo");
      Assert.fail("It should fail after all cluster attempts.");
//    } catch (JedisClusterMaxAttemptsException e) {
    } catch (JedisClusterOperationException e) {
      // initial connection made with 'localhost' but subsequent connections to nodes use 127.0.0.1
      // which causes custom hostname verification to fail
      assertEquals("No more cluster attempts left.", e.getMessage());
    }

    try (JedisCluster jc2 = new JedisCluster(new HostAndPort("127.0.0.1", 8379),
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(true)
            .hostnameVerifier(hostnameVerifier).hostAndPortMapper(portMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
//      jc2.get("key");
//      Assert.fail("There should be no reachable node in cluster.");
////    } catch (JedisNoReachableClusterNodeException e) {
    } catch (JedisClusterOperationException e) {
      // JedisNoReachableClusterNodeException exception occurs from not being able to connect since
      // the socket factory fails the hostname verification
//      assertEquals("No reachable node in cluster.", e.getMessage());
      assertEquals("Could not initialize cluster slots cache.", e.getMessage());
    }

    try (JedisCluster jc3 = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(true)
            .hostnameVerifier(localhostVerifier).hostAndPortMapper(portMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      jc3.get("foo");
    }
  }

  @Test
  public void connectWithCustomSocketFactory() throws Exception {
    final SSLSocketFactory sslSocketFactory = SSLJedisTest.createTrustStoreSslSocketFactory();

    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(true)
            .sslSocketFactory(sslSocketFactory).hostAndPortMapper(portMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      assertEquals(3, jc.getClusterNodes().size());
    }
  }

  @Test
  public void connectWithEmptyTrustStore() throws Exception {
    final SSLSocketFactory sslSocketFactory = SSLJedisTest.createTrustNoOneSslSocketFactory();

    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(true)
            .sslSocketFactory(sslSocketFactory).build(), DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
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

    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 7379),
        DefaultJedisClientConfig.builder().user("default").password("cluster").ssl(false)
            .hostAndPortMapper(nullHostAndPortMap).build(), DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      Map clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey("127.0.0.1:7379"));
      assertTrue(clusterNodes.containsKey("127.0.0.1:7380"));
      assertTrue(clusterNodes.containsKey("127.0.0.1:7381"));
    }
  }

  public class LocalhostVerifier extends BasicHostnameVerifier {
    @Override
    public boolean verify(String hostname, SSLSession session) {
      if (hostname.equals("127.0.0.1")) {
        hostname = "localhost";
      }
      return super.verify(hostname, session);
    }
  }
}
