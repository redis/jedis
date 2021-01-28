package redis.clients.jedis.tests;

import org.junit.*;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisClusterMaxAttemptsException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;
import redis.clients.jedis.tests.SSLJedisTest.BasicHostnameVerifier;
import redis.clients.jedis.tests.utils.RedisVersionUtil;

import javax.net.ssl.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class SSLJedisClusterWithCompleteCredentialsTest extends JedisClusterTest {
  private static final int DEFAULT_TIMEOUT = 2000;
  private static final int DEFAULT_REDIRECTIONS = 5;
  private static final JedisPoolConfig DEFAULT_CONFIG = new JedisPoolConfig();
  
  private JedisClusterHostAndPortMap hostAndPortMap = new JedisClusterHostAndPortMap() {
    public HostAndPort getSSLHostAndPort(String host, int port) {
      host = host.equalsIgnoreCase("127.0.0.1") ? "localhost" : host;
      return new HostAndPort(host, port + 1000);
    }
  };
  
  //don't map IP addresses so that we try to connect with host 127.0.0.1
  private JedisClusterHostAndPortMap portMap = new JedisClusterHostAndPortMap() {
    public HostAndPort getSSLHostAndPort(String host, int port) {
      return new HostAndPort(host, port + 1000);
    }
  };

  @BeforeClass
  public static void prepare() {
    org.junit.Assume.assumeTrue("Not running ACL test on this version of Redis", RedisVersionUtil.checkRedisMajorVersionNumber(6));

    SSLJedisTest.setupTrustStore();
  }

  @Test
  public void testSSLDiscoverNodesAutomatically() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("localhost", 8379));
    JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_REDIRECTIONS,
    		                           "default","cluster", null, DEFAULT_CONFIG, true, null, null, null, hostAndPortMap);
    Map<String, JedisPool> clusterNodes = jc.getClusterNodes();
    assertEquals(3, clusterNodes.size());
    assertTrue(clusterNodes.containsKey("localhost:8379"));
    assertTrue(clusterNodes.containsKey("localhost:8380"));
    assertTrue(clusterNodes.containsKey("localhost:8381"));
    
    jc.get("foo");
    jc.close();
  
    JedisCluster jc2 = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, 
    		                            DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true, null, null, null, hostAndPortMap);
    clusterNodes = jc2.getClusterNodes();
    assertEquals(3, clusterNodes.size());
    assertTrue(clusterNodes.containsKey("localhost:8379"));
    assertTrue(clusterNodes.containsKey("localhost:8380"));
    assertTrue(clusterNodes.containsKey("localhost:8381"));
    jc2.get("foo");
    jc2.close();
  }
  
  @Test
  public void testSSLWithoutPortMap() {
    Set<HostAndPort> jedisClusterNode = new HashSet<HostAndPort>();
    jedisClusterNode.add(new HostAndPort("localhost", 8379));
    try(JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_REDIRECTIONS,
            "default", "cluster", null, DEFAULT_CONFIG,
            true, null, null, null, null)){

      Map<String, JedisPool> clusterNodes = jc.getClusterNodes();
      assertEquals(3, clusterNodes.size());
      assertTrue(clusterNodes.containsKey("127.0.0.1:7379"));
      assertTrue(clusterNodes.containsKey("127.0.0.1:7380"));
      assertTrue(clusterNodes.containsKey("127.0.0.1:7381"));
    }
  }
  
  @Test
  public void connectByIpAddress() {
    try(JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
        null, null, null, hostAndPortMap)){
      jc.get("foo");
    }
  }
  
  @Test
  public void connectToNodesFailsWithSSLParametersAndNoHostMapping() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
    
    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
        null, sslParameters, null, portMap)){
      jc.get("foo");
      Assert.fail("The code did not throw the expected JedisClusterMaxAttemptsException.");
    } catch (JedisNoReachableClusterNodeException e) {
      // initial connection to localhost works, but subsequent connections to nodes use 127.0.0.1
      // and fail hostname verification
    }
  }
  
  @Test
  public void connectToNodesSucceedsWithSSLParametersAndHostMapping() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
    
    try(JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
        null, sslParameters, null, hostAndPortMap)){
      jc.get("foo");
    }
  }
  
  @Test
  public void connectByIpAddressFailsWithSSLParameters() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
    
    try (JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
          DEFAULT_REDIRECTIONS, "user", "cluster", null, DEFAULT_CONFIG, true,
          null, sslParameters, null, hostAndPortMap)){
      jc.get("key");
      Assert.fail("The code did not throw the expected JedisConnectionException.");
    } catch (JedisConnectionException e) {
//      Assert.assertEquals(SSLHandshakeException.class, e.getCause().getClass());
//      Assert.assertEquals(CertificateException.class, e.getCause().getCause().getClass());
    }
  }
  
  @Test
  public void connectWithCustomHostNameVerifier() {
    HostnameVerifier hostnameVerifier = new BasicHostnameVerifier();
    HostnameVerifier localhostVerifier = new LocalhostVerifier();
    
    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
        null, null, hostnameVerifier, portMap)){
      jc.get("foo");
      Assert.fail("The code did not throw the expected JedisClusterMaxAttemptsException.");
    } catch (JedisNoReachableClusterNodeException e) {
      // initial connection made with 'localhost' but subsequent connections to nodes use 127.0.0.1
      // which causes custom hostname verification to fail
    }

    try ( JedisCluster jc2 = new JedisCluster(new HostAndPort("127.0.0.1", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
          DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
          null, null, hostnameVerifier, portMap)){
      jc2.get("key");
      Assert.fail("The code did not throw the expected NullPointerException.");
    } catch (JedisConnectionException e) {
    } 
    
    JedisCluster jc3 = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
        null, null, localhostVerifier, portMap);
    jc3.get("foo");
    jc3.close();
  }
  
  @Test
  public void connectWithCustomSocketFactory() throws Exception {
    final SSLSocketFactory sslSocketFactory = SSLJedisTest.createTrustStoreSslSocketFactory();

    try(JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
                                       DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
                                       sslSocketFactory, null, null, portMap)){
      assertEquals(3, jc.getClusterNodes().size());
    }
  }
  
  @Test
  public void connectWithEmptyTrustStore() throws Exception {
    final SSLSocketFactory sslSocketFactory = SSLJedisTest.createTrustNoOneSslSocketFactory();

    try (JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
            DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
            sslSocketFactory, null, null, null)){
      jc.get("key");
      Assert.fail("The code did not throw the expected JedisConnectionException.");
    } catch (JedisConnectionException e) {
//      Assert.assertEquals("Unexpected first inner exception.",
//          SSLException.class, e.getCause().getClass());
//      Assert.assertEquals("Unexpected third inner exception",
//          RuntimeException.class, e.getCause().getCause().getClass());
//      Assert.assertEquals("Unexpected fourth inner exception.",
//          InvalidAlgorithmParameterException.class, e.getCause().getCause().getCause().getClass());
    } 
  }
  
  @Test
  public void hostAndPortMapIgnoredIfSSLFalse() {
    JedisClusterHostAndPortMap hostAndPortMap = new JedisClusterHostAndPortMap() {
      public HostAndPort getSSLHostAndPort(String host, int port) {
        return new HostAndPort(host, port + 2000);
      }
    };
    
    try(JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 7379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
            DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, false,
            null, null, null, hostAndPortMap)){
    
      Map<String, JedisPool> nodes = jc.getClusterNodes();
      assertTrue(nodes.containsKey("127.0.0.1:7379"));
      assertFalse(nodes.containsKey("127.0.0.1:9739"));
    }
  }
  
  @Test
  public void defaultHostAndPortUsedIfMapReturnsNull() {
    JedisClusterHostAndPortMap hostAndPortMap = new JedisClusterHostAndPortMap() {
      public HostAndPort getSSLHostAndPort(String host, int port) {
        return null;
      }
    };
    
    try(JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 7379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
            DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, false,
            null, null, null, hostAndPortMap)){
    
      Map<String, JedisPool> clusterNodes = jc.getClusterNodes();
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
