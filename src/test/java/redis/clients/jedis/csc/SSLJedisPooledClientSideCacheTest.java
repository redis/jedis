package redis.clients.jedis.csc;

import io.redis.test.utils.RedisVersion;
import java.nio.file.Path;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;

import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.util.RedisVersionUtil;
import redis.clients.jedis.util.TlsUtil;

public class SSLJedisPooledClientSideCacheTest extends JedisPooledClientSideCacheTestBase {

  @BeforeClass
  public static void prepare() {
    Path trusStorePath = TlsUtil.createAndSaveEnvTruststore("redis1-2-5-8-sentinel", "changeit");
    TlsUtil.setCustomTrustStore(trusStorePath, "changeit");

    endpoint = HostAndPorts.getRedisEndpoint("standalone0-tls");

    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build())) {
      Assume.assumeTrue("Jedis Client side caching is only supported with 'Redis 7.4' or later.",
              RedisVersionUtil.getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_4));
    }
  }

  @AfterClass
  public static void teardownTrustStore() {
    TlsUtil.restoreOriginalTrustStore();
  }
}
