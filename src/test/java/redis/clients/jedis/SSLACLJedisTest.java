package redis.clients.jedis;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
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

  @BeforeClass
  public static void prepare() {
    // Use to check if the ACL test should be ran. ACL are available only in 6.0 and later
    org.junit.Assume.assumeTrue("Not running ACL test on this version of Redis",
        RedisVersionUtil.checkRedisMajorVersionNumber(6));

    SSLJedisTest.setupTrustStore();
  }

  @Test
  public void connectWithSsl() {
    try (Jedis jedis = new Jedis("localhost", 6390, true)) {
      jedis.auth("acljedis", "fizzbuzz");
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithConfig() {
    try (Jedis jedis = new Jedis(new HostAndPort("localhost", 6390), DefaultJedisClientConfig
        .builder().ssl(true).build())) {
      jedis.auth("acljedis", "fizzbuzz");
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithUrl() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    try (Jedis jedis = new Jedis("rediss://localhost:6390")) {
      jedis.auth("default", "foobared");
      assertEquals("PONG", jedis.ping());
    }
    try (Jedis jedis = new Jedis("rediss://localhost:6390")) {
      jedis.auth("acljedis", "fizzbuzz");
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithCompleteCredentialsUrl() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    try (Jedis jedis = new Jedis("rediss://default:foobared@localhost:6390")) {
      assertEquals("PONG", jedis.ping());
    }
    try (Jedis jedis = new Jedis("rediss://acljedis:fizzbuzz@localhost:6390")) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithUri() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    try (Jedis jedis = new Jedis(URI.create("rediss://localhost:6390"))) {
      jedis.auth("acljedis", "fizzbuzz");
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithCompleteCredentialsUri() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    try (Jedis jedis = new Jedis(URI.create("rediss://default:foobared@localhost:6390"))) {
      assertEquals("PONG", jedis.ping());
    }
    try (Jedis jedis = new Jedis(URI.create("rediss://acljedis:fizzbuzz@localhost:6390"))) {
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
