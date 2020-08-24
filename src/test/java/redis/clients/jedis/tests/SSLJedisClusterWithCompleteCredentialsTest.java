package redis.clients.jedis.tests;


import org.junit.*;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisClusterMaxAttemptsException;
import redis.clients.jedis.exceptions.JedisConnectionException;
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

  @Before
  public void setUp() throws InterruptedException {
    super.setUp();
    SSLJedisTest.setupTrustStore(); // set up trust store for SSL tests

    HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);
    Jedis jedis = new Jedis(hnp.getHost(), hnp.getPort(), 500);
    jedis.connect();
    jedis.auth("foobared");
    // run the test only if the verison support ACL (6 or later)
    boolean shouldNotRun = ((new RedisVersionUtil(jedis)).getRedisMajorVersionNumber() < 6);
    jedis.close();
    if ( shouldNotRun ) {
      org.junit.Assume.assumeFalse("Not running ACL test on this version of Redis", shouldNotRun);
    }

  }

  @AfterClass
  public static void cleanUp() {
    JedisClusterTest.cleanUp();
  }

  @After
  public void tearDown() throws InterruptedException {
    cleanUp();
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
    JedisCluster jc = new JedisCluster(jedisClusterNode, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, DEFAULT_REDIRECTIONS,
            "default", "cluster", null, DEFAULT_CONFIG,
            true, null, null, null, null);

    Map<String, JedisPool> clusterNodes = jc.getClusterNodes();
    assertEquals(3, clusterNodes.size());
    assertTrue(clusterNodes.containsKey("127.0.0.1:7379"));
    assertTrue(clusterNodes.containsKey("127.0.0.1:7380"));
    assertTrue(clusterNodes.containsKey("127.0.0.1:7381"));
    jc.close();
  }
  
  @Test
  public void connectByIpAddress() {
    JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
        null, null, null, hostAndPortMap);
    jc.get("foo");
    jc.close();
  }
  
  @Test
  public void connectToNodesFailsWithSSLParametersAndNoHostMapping() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
    
    JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
        null, sslParameters, null, portMap);
    
    try {
      jc.get("foo");
      Assert.fail("The code did not throw the expected JedisClusterMaxAttemptsException.");
    } catch (JedisClusterMaxAttemptsException e) {
      // initial connection to localhost works, but subsequent connections to nodes use 127.0.0.1
      // and fail hostname verification
    }
    jc.close();
  }
  
  @Test
  public void connectToNodesSucceedsWithSSLParametersAndHostMapping() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
    
    JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
        null, sslParameters, null, hostAndPortMap);
    jc.get("foo");
    jc.close();
  }
  
  @Test
  public void connectByIpAddressFailsWithSSLParameters() {
    final SSLParameters sslParameters = new SSLParameters();
    sslParameters.setEndpointIdentificationAlgorithm("HTTPS");
    
    JedisCluster jc = null;
    try {
      jc = new JedisCluster(new HostAndPort("127.0.0.1", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
          DEFAULT_REDIRECTIONS, "user", "cluster", null, DEFAULT_CONFIG, true,
          null, sslParameters, null, hostAndPortMap);
      Assert.fail("The code did not throw the expected JedisConnectionException.");
    } catch (JedisConnectionException e) {
      Assert.assertEquals(SSLException.class, e.getCause().getClass());
      Assert.assertEquals(SSLHandshakeException.class, e.getCause().getCause().getClass());
      Assert.assertEquals(CertificateException.class, e.getCause().getCause().getCause().getClass());
    } finally {
      if (jc != null) {
        jc.close();
      }
    }
  }
  
  @Test
  public void connectWithCustomHostNameVerifier() {
    HostnameVerifier hostnameVerifier = new BasicHostnameVerifier();
    HostnameVerifier localhostVerifier = new LocalhostVerifier();
    
    JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
        null, null, hostnameVerifier, portMap);;
    try {
      jc.get("foo");
      Assert.fail("The code did not throw the expected JedisClusterMaxAttemptsException.");
    } catch (JedisClusterMaxAttemptsException e) {
      // initial connection made with 'localhost' but subsequent connections to nodes use 127.0.0.1
      // which causes custom hostname verification to fail
    }
    jc.close();

    JedisCluster jc2 = null;
    try {
      jc2 = new JedisCluster(new HostAndPort("127.0.0.1", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
          DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
          null, null, hostnameVerifier, portMap);
      Assert.fail("The code did not throw the expected NullPointerException.");
    } catch (NullPointerException e) {
      // Null pointer exception occurs from closing Jedis object that did not connect due to custom
      // hostname validation. When closing this Jedis object, the RedisOutputStream in the underlying 
      // Connection object is null and causes this exception
    } finally {
      if (jc2 != null) {
        jc2.close();
      }
    }
    
    JedisCluster jc3 = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
        DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
        null, null, localhostVerifier, portMap);;
    jc3.get("foo");
    jc3.close();
  }
  
  @Test
  public void connectWithCustomSocketFactory() throws Exception {
    final SSLSocketFactory sslSocketFactory = SSLJedisTest.createTrustStoreSslSocketFactory();

    JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
                                       DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
                                       sslSocketFactory, null, null, portMap);
    assertEquals(3, jc.getClusterNodes().size());
    jc.close();
  }
  
  @Test
  public void connectWithEmptyTrustStore() throws Exception {
    final SSLSocketFactory sslSocketFactory = SSLJedisTest.createTrustNoOneSslSocketFactory();

    JedisCluster jc = null;
    try {
      jc = new JedisCluster(new HostAndPort("localhost", 8379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
            DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, true,
            sslSocketFactory, null, null, null);
      Assert.fail("The code did not throw the expected JedisConnectionException.");
    } catch (JedisConnectionException e) {
      Assert.assertEquals("Unexpected first inner exception.",
          SSLException.class, e.getCause().getClass());
      Assert.assertEquals("Unexpected second inner exception.",
          SSLException.class, e.getCause().getCause().getClass());
      Assert.assertEquals("Unexpected third inner exception",
          RuntimeException.class, e.getCause().getCause().getCause().getClass());
      Assert.assertEquals("Unexpected fourth inner exception.",
          InvalidAlgorithmParameterException.class, e.getCause().getCause().getCause().getCause().getClass());
    } finally {
      if (jc != null) {
        jc.close();
      }
    }
  }
  
  @Test
  public void hostAndPortMapIgnoredIfSSLFalse() {
    JedisClusterHostAndPortMap hostAndPortMap = new JedisClusterHostAndPortMap() {
      public HostAndPort getSSLHostAndPort(String host, int port) {
        return new HostAndPort(host, port + 2000);
      }
    };
    
    JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 7379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
            DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, false,
            null, null, null, hostAndPortMap);
    
    Map<String, JedisPool> nodes = jc.getClusterNodes();
    assertTrue(nodes.containsKey("127.0.0.1:7379"));
    assertFalse(nodes.containsKey("127.0.0.1:9739"));
    jc.close();
  }
  
  @Test
  public void defaultHostAndPortUsedIfMapReturnsNull() {
    JedisClusterHostAndPortMap hostAndPortMap = new JedisClusterHostAndPortMap() {
      public HostAndPort getSSLHostAndPort(String host, int port) {
        return null;
      }
    };
    
    JedisCluster jc = new JedisCluster(new HostAndPort("localhost", 7379), DEFAULT_TIMEOUT, DEFAULT_TIMEOUT,
            DEFAULT_REDIRECTIONS, "default", "cluster", null, DEFAULT_CONFIG, false,
            null, null, null, hostAndPortMap);
    
    Map<String, JedisPool> clusterNodes = jc.getClusterNodes();
    assertEquals(3, clusterNodes.size());
    assertTrue(clusterNodes.containsKey("127.0.0.1:7379"));
    assertTrue(clusterNodes.containsKey("127.0.0.1:7380"));
    assertTrue(clusterNodes.containsKey("127.0.0.1:7381"));
    jc.close();
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
