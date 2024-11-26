package redis.clients.jedis.csc;

import org.junit.AfterClass;

import redis.clients.jedis.Jedis;
import io.redis.test.utils.RedisVersion;
import redis.clients.jedis.util.TlsUtil;
import org.junit.BeforeClass;
import redis.clients.jedis.HostAndPorts;

import java.nio.file.Path;

import static org.junit.Assume.assumeTrue;
import static redis.clients.jedis.util.RedisVersionUtil.getRedisVersion;

public class SSLJedisPooledClientSideCacheTest extends JedisPooledClientSideCacheTestBase {

  @BeforeClass
  public static void prepare() {
    Path trusStorePath = TlsUtil.createAndSaveEnvTruststore("redis1-2-5-10-sentinel", "changeit");
    TlsUtil.setCustomTrustStore(trusStorePath, "changeit");

    endpoint = HostAndPorts.getRedisEndpoint("standalone0-tls");

    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build())) {
      assumeTrue("Jedis Client side caching is only supported with 'Redis 7.4' or later.",
              getRedisVersion(jedis).isGreaterThanOrEqualTo(RedisVersion.V7_4));
    }
  }

  @AfterClass
  public static void teardownTrustStore() {
    TlsUtil.restoreOriginalTrustStore();
  }
}
