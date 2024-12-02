package redis.clients.jedis;

import static org.junit.Assume.assumeTrue;
import static redis.clients.jedis.util.RedisVersionUtil.getRedisVersion;
import static org.junit.Assert.*;
import static redis.clients.jedis.util.TlsUtil.*;

import java.nio.file.Path;

import org.junit.AfterClass;
import io.redis.test.utils.RedisVersion;
import redis.clients.jedis.util.TlsUtil;
import org.junit.BeforeClass;
import org.junit.Test;

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
    Path trusStorePath = createAndSaveEnvTruststore("redis1-2-5-8-sentinel", "changeit");
    TlsUtil.setCustomTrustStore(trusStorePath, "changeit");
    // Use to check if the ACL test should be ran. ACL are available only in 6.0 and later
    assumeTrue("Not running ACL test on this version of Redis",
        getRedisVersion(endpoint).isGreaterThanOrEqualTo(RedisVersion.V6_0_0));
  }

  @AfterClass
  public static void teardownTrustStore() {
    TlsUtil.restoreOriginalTrustStore();
  }

  @Test
  public void connectWithSsl() {
    try (Jedis jedis = new Jedis(endpoint.getHost(), endpoint.getPort(),
            DefaultJedisClientConfig.builder()
                    .sslSocketFactory(sslSocketFactoryForEnv("redis1-2-5-8-sentinel"))
                    .ssl(true)
                    .build())) {
      jedis.auth(endpoint.getUsername(), endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithConfig() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(),
        DefaultJedisClientConfig.builder()
                .sslSocketFactory(sslSocketFactoryForEnv("redis1-2-5-8-sentinel"))
                .ssl(true).build())) {
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
            endpointWithDefaultUser.getURIBuilder()
                    .defaultCredentials().build(),
            DefaultJedisClientConfig.builder()
                    .sslSocketFactory(sslSocketFactoryForEnv("redis1-2-5-8-sentinel"))
                    .build())) {
      assertEquals("PONG", jedis.ping());
    }
    try (Jedis jedis = new Jedis(endpoint.getURIBuilder().defaultCredentials().build(),
            DefaultJedisClientConfig.builder()
                    .sslSocketFactory(sslSocketFactoryForEnv("redis1-2-5-8-sentinel"))
                    .build())) {
      assertEquals("PONG", jedis.ping());
    }
  }
}
