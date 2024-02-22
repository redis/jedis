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

public class SSLJedisClusterTest extends JedisClusterTestBase {

  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final ConnectionPoolConfig DEFAULT_POOL_CONFIG = new ConnectionPoolConfig();

  static final HostAndPortMapper hostAndPortMap = (HostAndPort hostAndPort) -> {
    String host = hostAndPort.getHost();
    int port = hostAndPort.getPort();
    if (host.equals("127.0.0.1")) {
      host = "localhost";
      port = port + 1000;
    } else if (host.startsWith("172")) {
      host = "localhost";
      port = mapClusterPort(hostAndPort.getHost(), hostAndPort.getPort());
    }
    return new HostAndPort(host, port);
  };

  private static int mapClusterPort(String host, int port) {
    String[] segments = host.split("\\.");
    if (segments.length == 4) {
      int lastSegment = Integer.parseInt(segments[3]);
      int delta = lastSegment - 31; // 172.20.0.31 is the first IP in the cluster
      return 6379 + delta + 2000; // stunnel serves OSS cluster nodes on 8379...
    }
    return port;
  }

  // don't map IP addresses so that we try to connect with host 127.0.0.1
  static final HostAndPortMapper portMap = (HostAndPort hostAndPort) -> {
    if ("localhost".equals(hostAndPort.getHost())) {
      return hostAndPort;
    }
    if (hostAndPort.getHost().startsWith("172")) {
      return new HostAndPort("127.0.0.1", mapClusterPort(hostAndPort.getHost(), hostAndPort.getPort()));
    }
    return new HostAndPort(hostAndPort.getHost(), hostAndPort.getPort() + 1000);
  };

  @BeforeClass
  public static void prepare() {
    SSLJedisTest.setupTrustStore(); // set up trust store for SSL tests
  }

  @Test
  public void testSSLDiscoverNodesAutomatically() {
    try (JedisCluster jc = new JedisCluster(Collections.singleton(new HostAndPort("localhost", 8379)),
        DefaultJedisClientConfig.builder().password("cluster").ssl(true)
            .hostAndPortMapper(hostAndPortMap).build(), DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
//      Map<String, JedisPool> clusterNodes = jc.getClusterNodes();
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey("172.20.0.31:6379"));
      assertTrue(clusterNodes.containsKey("172.20.0.32:6379"));
      assertTrue(clusterNodes.containsKey("172.20.0.33:6379"));

      jc.get("foo");
    }

    try (JedisCluster jc2 = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().password("cluster").ssl(true)
            .hostAndPortMapper(hostAndPortMap).build(), DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
//      Map<String, JedisPool> clusterNodes = jc2.getClusterNodes();
      Map<String, ?> clusterNodes = jc2.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey("172.20.0.31:6379"));
      assertTrue(clusterNodes.containsKey("172.20.0.32:6379"));
      assertTrue(clusterNodes.containsKey("172.20.0.33:6379"));
      jc2.get("foo");
    }
  }

  @Test
  public void testSSLWithoutPortMap() {
    try (JedisCluster jc = new JedisCluster(Collections.singleton(new HostAndPort("localhost", 8379)),
        DefaultJedisClientConfig.builder().password("cluster").ssl(true).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
//      Map<String, JedisPool> clusterNodes = jc.getClusterNodes();
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey("172.20.0.31:6379"));
      assertTrue(clusterNodes.containsKey("172.20.0.32:6379"));
      assertTrue(clusterNodes.containsKey("172.20.0.33:6379"));
    }
  }

  @Test
  public void connectByIpAddress() {
    try (JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", 7379),
        DefaultJedisClientConfig.builder().password("cluster").ssl(true)
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
        DefaultJedisClientConfig.builder().password("cluster").ssl(true)
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
        DefaultJedisClientConfig.builder().password("cluster").ssl(true)
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
        DefaultJedisClientConfig.builder().password("cluster").ssl(true)
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
        DefaultJedisClientConfig.builder().password("cluster").ssl(true)
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
        DefaultJedisClientConfig.builder().password("cluster").ssl(true)
            .hostnameVerifier(hostnameVerifier).hostAndPortMapper(portMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
//      jc2.get("foo");
//      Assert.fail("There should be no reachable node in cluster.");
////    } catch (JedisNoReachableClusterNodeException e) {
    } catch (JedisClusterOperationException e) {
      // JedisNoReachableClusterNodeException exception occurs from not being able to connect
      // since the socket factory fails the hostname verification
//      assertEquals("No reachable node in cluster.", e.getMessage());
      assertEquals("Could not initialize cluster slots cache.", e.getMessage());
    }
    
    try (JedisCluster jc3 = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().password("cluster").ssl(true)
            .hostnameVerifier(localhostVerifier).hostAndPortMapper(portMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      jc3.get("foo");
    }
  }

  @Test
  public void connectWithCustomSocketFactory() throws Exception {
    final SSLSocketFactory sslSocketFactory = SSLJedisTest.createTrustStoreSslSocketFactory();

    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().password("cluster").ssl(true)
            .sslSocketFactory(sslSocketFactory).hostAndPortMapper(portMap).build(),
        DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {
      assertEquals(3, jc.getClusterNodes().size());
    }
  }

  @Test
  public void connectWithEmptyTrustStore() throws Exception {
    final SSLSocketFactory sslSocketFactory = SSLJedisTest.createTrustNoOneSslSocketFactory();

    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379),
        DefaultJedisClientConfig.builder().password("cluster").ssl(true)
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
        DefaultJedisClientConfig.builder().password("cluster").ssl(false)
            .hostAndPortMapper(nullHostAndPortMap).build(), DEFAULT_REDIRECTIONS, DEFAULT_POOL_CONFIG)) {

//      Map<String, JedisPool> clusterNodes = jc.getClusterNodes();
      Map<String, ?> clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey("172.20.0.31:6379"));
      assertTrue(clusterNodes.containsKey("172.20.0.32:6379"));
      assertTrue(clusterNodes.containsKey("172.20.0.33:6379"));
    }
  }

  public static class LocalhostVerifier extends BasicHostnameVerifier {
    @Override
    public boolean verify(String hostname, SSLSession session) {
      if (hostname.equals("127.0.0.1") || hostname.startsWith("172.")) {
        hostname = "localhost";
      }
      return super.verify(hostname, session);
    }
  }
}
