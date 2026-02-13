package redis.clients.jedis.tls;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.util.TlsUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * SSL/TLS tests for {@link RedisClient} with basic authentication (password-only, no ACL).
 * <p>
 * This test class follows the pattern of {@link SSLJedisIT} but adapted for {@link RedisClient}.
 * </p>
 */
public class RedisClientIT {

  protected static EndpointConfig endpoint;

  private static final String trustStoreName = RedisClientIT.class.getSimpleName();

  @BeforeAll
  public static void prepare() {
    endpoint = Endpoints.getRedisEndpoint("standalone0-tls");
    List<Path> trustedCertLocation = Collections.singletonList(endpoint.getCertificatesLocation());
    Path trustStorePath = TlsUtil.createAndSaveTestTruststore(trustStoreName, trustedCertLocation,
      "changeit");

    TlsUtil.setCustomTrustStore(trustStorePath, "changeit");
  }

  @AfterAll
  public static void teardownTrustStore() {
    TlsUtil.restoreOriginalTrustStore();
  }

  @Test
  public void connectWithSsl() {
    try (RedisClient client = RedisClient.builder()
        .hostAndPort(endpoint.getHost(), endpoint.getPort())
        .clientConfig(
          DefaultJedisClientConfig.builder().ssl(true).password(endpoint.getPassword()).build())
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  @Test
  public void connectWithConfig() {
    try (RedisClient client = RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
        .clientConfig(
          DefaultJedisClientConfig.builder().ssl(true).password(endpoint.getPassword()).build())
        .build()) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Tests opening a default SSL/TLS connection to redis using "rediss://" scheme url.
   */
  @Test
  public void connectWithUrl() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    // URI includes credentials via defaultCredentials()
    try (RedisClient client = RedisClient
        .create(endpoint.getURIBuilder().defaultCredentials().build().toString())) {
      assertEquals("PONG", client.ping());
    }
  }

  /**
   * Tests opening a default SSL/TLS connection to redis.
   */
  @Test
  public void connectWithUri() {
    // The "rediss" scheme instructs jedis to open a SSL/TLS connection.
    // URI includes credentials via defaultCredentials()
    try (RedisClient client = RedisClient
        .create(endpoint.getURIBuilder().defaultCredentials().build())) {
      assertEquals("PONG", client.ping());
    }
  }

}
