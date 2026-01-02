package redis.clients.jedis.csc;

import io.redis.test.utils.RedisVersion;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.RedisVersionUtil;
import redis.clients.jedis.util.TlsUtil;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("integration")
public class SSLRedisClientSideCacheTest extends RedisClientSideCacheTestBase {

  private static final String trustStoreName = SSLRedisClientSideCacheTest.class.getSimpleName();

  @BeforeAll
  public static void prepare() {

    endpoint = Endpoints.getRedisEndpoint("standalone0-tls");

    List<Path> trustedCertLocation = Collections.singletonList(endpoint.getCertificatesLocation());
    Path trustStorePath = TlsUtil.createAndSaveTestTruststore(trustStoreName, trustedCertLocation,"changeit");
    TlsUtil.setCustomTrustStore(trustStorePath, "changeit");

    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().build())) {
      assumeTrue(RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_4),
          "Jedis Client side caching is only supported with 'Redis 7.4' or later.");
    }
  }

  @AfterAll
  public static void teardownTrustStore() {
    TlsUtil.restoreOriginalTrustStore();
  }
}
