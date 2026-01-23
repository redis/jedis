package redis.clients.jedis;

import io.redis.test.annotations.ConditionalOnEnv;
import io.redis.test.annotations.SinceRedisVersion;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;
import redis.clients.jedis.util.EnvCondition;
import redis.clients.jedis.util.RedisVersionCondition;
import redis.clients.jedis.util.TestEnvUtil;
import redis.clients.jedis.util.TlsUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This test class is a copy of {@link SSLJedisTest}.
 * <p>
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
@SinceRedisVersion(value = "6.0.0", message = "Not running ACL test on this version of Redis")
@Tag("integration")
@ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_SOURCE, enabled = false)
@ResourceLocks({
    @ResourceLock(value = Endpoints.STANDALONE0_ACL_TLS),
    @ResourceLock(value = Endpoints.STANDALONE0_TLS)
})
public class SSLACLJedisTest {

  protected static EndpointConfig endpoint;

  protected static EndpointConfig endpointWithDefaultUser;

  @RegisterExtension
  public static EnvCondition envCondition = new EnvCondition();

  @RegisterExtension
  public static RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint(Endpoints.STANDALONE0_ACL_TLS));

  private static final String trustStoreName = SSLACLJedisTest.class.getSimpleName();

  @BeforeAll
  public static void prepare() {
    endpoint = Endpoints.getRedisEndpoint(Endpoints.STANDALONE0_ACL_TLS);
    endpointWithDefaultUser = Endpoints.getRedisEndpoint(Endpoints.STANDALONE0_TLS);
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
}
