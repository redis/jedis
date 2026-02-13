package redis.clients.jedis.tls;

import io.redis.test.annotations.ConditionalOnEnv;
import io.redis.test.annotations.SinceRedisVersion;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.EnvCondition;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.util.TestEnvUtil;
import redis.clients.jedis.util.TlsUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test class is a copy of {@link SSLJedisIT}.
 * <p>
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
@SinceRedisVersion(value = "6.0.0", message = "Not running ACL test on this version of Redis")
@ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_SOURCE, enabled = false)
public class SSLACLJedisIT {

  protected static EndpointConfig endpoint;

  protected static EndpointConfig endpointWithDefaultUser;

  @RegisterExtension
  public static EnvCondition envCondition = new EnvCondition();

  @RegisterExtension
  public static RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint("standalone0-acl-tls"));

  private static final String trustStoreName = SSLACLJedisIT.class.getSimpleName();

  @BeforeAll
  public static void prepare() {
    endpoint = Endpoints.getRedisEndpoint("standalone0-acl-tls");
    endpointWithDefaultUser = Endpoints.getRedisEndpoint("standalone0-tls");
    List<Path> trustedCertLocation = Arrays.asList(endpoint.getCertificatesLocation(),
      endpointWithDefaultUser.getCertificatesLocation());
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
    try (
        Jedis jedis = new Jedis(endpoint.getURIBuilder().defaultCredentials().build().toString())) {
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
}
