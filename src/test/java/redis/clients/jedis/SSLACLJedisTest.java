package redis.clients.jedis;

import static org.junit.Assert.*;
import static redis.clients.jedis.util.TlsUtil.*;

import io.redis.test.annotations.SinceRedisVersion;
import java.nio.file.Path;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import redis.clients.jedis.util.RedisVersionRule;
import redis.clients.jedis.util.TlsUtil;

/**
 * This test class is a copy of {@link SSLJedisTest}.
 * <p>
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
@SinceRedisVersion(value = "6.0.0", message = "Not running ACL test on this version of Redis")
public class SSLACLJedisTest {

  protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-acl-tls");

  protected static final EndpointConfig endpointWithDefaultUser = HostAndPorts.getRedisEndpoint("standalone0-tls");

  @ClassRule
  public static RedisVersionRule versionRule = new RedisVersionRule(endpoint);

  @BeforeClass
  public static void prepare() {
    Path trusStorePath = createAndSaveEnvTruststore("redis1-2-5-8-sentinel", "changeit");
    TlsUtil.setCustomTrustStore(trusStorePath, "changeit");
  }

  @AfterClass
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
