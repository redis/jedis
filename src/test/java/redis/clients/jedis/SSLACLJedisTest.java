package redis.clients.jedis;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.util.RedisVersionUtil;

/**
 * This test class is a copy of {@link SSLJedisTest}.
 * <p>
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
public class SSLACLJedisTest {

  protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-acl-tls");

  protected static final EndpointConfig endpointWithDefaultUser = HostAndPorts.getRedisEndpoint("standalone0-tls");

  @BeforeClass
  public static void prepare() {
    // We need to set up certificates first before connecting to the endpoint with enabled TLS
    SSLJedisTest.setupTrustStore();
    // Use to check if the ACL test should be ran. ACL are available only in 6.0 and later
    org.junit.Assume.assumeTrue("Not running ACL test on this version of Redis",
        RedisVersionUtil.checkRedisMajorVersionNumber(6, endpoint));
  }

  @Test
  public void connectWithSsl() {
    try (Jedis jedis = new Jedis(endpoint.getHost(), endpoint.getPort(), true)) {
      jedis.auth(endpoint.getUsername(), endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithConfig() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder().ssl(true).build())) {
      jedis.auth(endpoint.getUsername(), endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithUrl() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    try (Jedis jedis = new Jedis(
        endpointWithDefaultUser.getURIBuilder().defaultCredentials().build().toString())) {
      assertEquals("PONG", jedis.ping());
    }
    try (Jedis jedis = new Jedis(
        endpoint.getURIBuilder().defaultCredentials().build().toString())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithUri() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    try (Jedis jedis = new Jedis(
        endpointWithDefaultUser.getURIBuilder().defaultCredentials().build())) {
      assertEquals("PONG", jedis.ping());
    }
    try (Jedis jedis = new Jedis(endpoint.getURIBuilder().defaultCredentials().build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  /**
   * Creates an SSLSocketFactory that trusts all certificates in truststore.jceks.
   */
  static SSLSocketFactory createTrustStoreSslSocketFactory() throws Exception {

    KeyStore trustStore = KeyStore.getInstance("jceks");
    try (InputStream inputStream = new FileInputStream("src/test/resources/truststore.jceks")) {
      trustStore.load(inputStream, null);
    }

    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX");
    trustManagerFactory.init(trustStore);
    TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, trustManagers, new SecureRandom());
    return sslContext.getSocketFactory();
  }

  /**
   * Creates an SSLSocketFactory with a trust manager that does not trust any certificates.
   */
  static SSLSocketFactory createTrustNoOneSslSocketFactory() throws Exception {
    TrustManager[] unTrustManagers = new TrustManager[] { new X509TrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }

      public void checkClientTrusted(X509Certificate[] chain, String authType) {
        throw new RuntimeException(new InvalidAlgorithmParameterException());
      }

      public void checkServerTrusted(X509Certificate[] chain, String authType) {
        throw new RuntimeException(new InvalidAlgorithmParameterException());
      }
    } };
    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(null, unTrustManagers, new SecureRandom());
    return sslContext.getSocketFactory();
  }

  /**
   * Very basic hostname verifier implementation for testing. NOT recommended for production.
   */
  static class BasicHostnameVerifier implements HostnameVerifier {

    private static final String COMMON_NAME_RDN_PREFIX = "CN=";

    @Override
    public boolean verify(String hostname, SSLSession session) {
      X509Certificate peerCertificate;
      try {
        peerCertificate = (X509Certificate) session.getPeerCertificates()[0];
      } catch (SSLPeerUnverifiedException e) {
        throw new IllegalStateException("The session does not contain a peer X.509 certificate.", e);
      }
      String peerCertificateCN = getCommonName(peerCertificate);
      return hostname.equals(peerCertificateCN);
    }

    private String getCommonName(X509Certificate peerCertificate) {
      String subjectDN = peerCertificate.getSubjectDN().getName();
      String[] dnComponents = subjectDN.split(",");
      for (String dnComponent : dnComponents) {
        dnComponent = dnComponent.trim();
        if (dnComponent.startsWith(COMMON_NAME_RDN_PREFIX)) {
          return dnComponent.substring(COMMON_NAME_RDN_PREFIX.length());
        }
      }
      throw new IllegalArgumentException("The certificate has no common name.");
    }
  }
}
