package redis.clients.jedis.mcf;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.DefaultRedisCredentials;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisCredentials;

@Tags({ @Tag("failover"), @Tag("scenario") })
public class RedisRestAPIIntegrationTest {
  public static class SSLBypass {
    private static SSLSocketFactory originalSSLSocketFactory;
    private static HostnameVerifier originalHostnameVerifier;

    public static void disableSSLVerification() {
      try {
        // Store original settings BEFORE changing them
        originalSSLSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
        originalHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

        // Create trust-all manager
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
          public X509Certificate[] getAcceptedIssuers() {
            return null;
          }

          public void checkClientTrusted(X509Certificate[] certs, String authType) {
          }

          public void checkServerTrusted(X509Certificate[] certs, String authType) {
          }
        } };

        // Apply bypass
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    public static void restoreSSLVerification() {
      // Restore original settings
      if (originalSSLSocketFactory != null) {
        HttpsURLConnection.setDefaultSSLSocketFactory(originalSSLSocketFactory);
      }
      if (originalHostnameVerifier != null) {
        HttpsURLConnection.setDefaultHostnameVerifier(originalHostnameVerifier);
      }
    }
  }

  private static EndpointConfig endpointConfig;
  private static Endpoint restAPIEndpoint;
  private static Supplier<RedisCredentials> credentialsSupplier;
  private static final Logger log = LoggerFactory.getLogger(RedisRestAPIIntegrationTest.class);

  @BeforeAll
  public static void beforeClass() {
    try {
      endpointConfig = HostAndPorts.getRedisEndpoint("re-active-active");
      restAPIEndpoint = getRestAPIEndpoint(endpointConfig);
      credentialsSupplier = () -> new DefaultRedisCredentials("test@redis.com", "test123");
      SSLBypass.disableSSLVerification();
    } catch (IllegalArgumentException e) {
      log.warn("Skipping test because no Redis endpoint is configured");
      assumeTrue(false);
    }
  }

  @AfterAll
  public static void teardownTrustStore() {
    SSLBypass.restoreSSLVerification();
  }

  @Test
  void testGetBdbs() throws Exception {
    RedisRestAPI api = new RedisRestAPI(restAPIEndpoint, credentialsSupplier);

    List<String> uids = api.getBdbs();
    assertEquals(3, uids.size());
    assertFalse(uids.isEmpty());
  }

  @Test
  void testCheckAvailability() throws Exception {
    RedisRestAPI api = new RedisRestAPI(restAPIEndpoint, credentialsSupplier);

    List<String> uids = api.getBdbs();
    assertFalse(uids.isEmpty());
    assertTrue(api.checkBdbAvailability(uids.get(0), false));
    assertFalse(api.checkBdbAvailability(uids.get(0), true));
  }

  private static Endpoint getRestAPIEndpoint(EndpointConfig config) {
    return new Endpoint() {
      @Override
      public String getHost() {
        // convert this to Redis FQDN by removing the node prefix
        // "dns":"redis-10232.c1.taki-active-active-test-c114170a.cto.redislabs.com"
        String host = config.getHost();
        // trim until the first dot
        String fqdn = host.substring(host.indexOf('.') + 1);
        return fqdn;
      }

      @Override
      public int getPort() {
        // default port for REST API
        return 9443;
      }
    };
  }

}
