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
import redis.clients.jedis.Endpoint;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisCredentials;
import redis.clients.jedis.scenario.RestEndpointUtil;

@Tags({ @Tag("failover"), @Tag("scenario") })
public class RedisRestAPIIT {
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
        log.error("Failed to disable SSL verification", e);
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
  private static final Logger log = LoggerFactory.getLogger(RedisRestAPIIT.class);

  @BeforeAll
  public static void beforeClass() {
    try {
      endpointConfig = HostAndPorts.getRedisEndpoint("re-active-active");
      restAPIEndpoint = RestEndpointUtil.getRestAPIEndpoint(endpointConfig);
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

    List<RedisRestAPI.BdbInfo> bdbs = api.getBdbs();
    assertEquals(3, bdbs.size());
    assertFalse(bdbs.isEmpty());

    // Verify that each BDB has a UID and endpoints
    for (RedisRestAPI.BdbInfo bdb : bdbs) {
      assertNotNull(bdb.getUid());
      assertNotNull(bdb.getEndpoints());
    }
  }

  @Test
  void testCheckAvailability() throws Exception {
    RedisRestAPI api = new RedisRestAPI(restAPIEndpoint, credentialsSupplier);

    List<RedisRestAPI.BdbInfo> bdbs = api.getBdbs();
    assertFalse(bdbs.isEmpty());
    String firstBdbUid = bdbs.get(0).getUid();
    assertTrue(api.checkBdbAvailability(firstBdbUid, false));
    assertFalse(api.checkBdbAvailability(firstBdbUid, true));
  }

}
