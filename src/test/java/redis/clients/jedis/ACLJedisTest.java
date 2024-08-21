package redis.clients.jedis;

import static org.junit.Assert.assertEquals;

import java.net.URISyntaxException;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;
import redis.clients.jedis.util.RedisVersionUtil;

/**
 * This test class is a copy of {@link JedisTest}.
 * <p>
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
@RunWith(Parameterized.class)
public class ACLJedisTest extends JedisCommandsTestBase {

  protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-acl");

  /**
   * Use to check if the ACL test should be ran. ACL are available only in 6.0 and later
   * @throws Exception
   */
  @BeforeClass
  public static void prepare() throws Exception {
    org.junit.Assume.assumeTrue("Not running ACL test on this version of Redis",
        RedisVersionUtil.checkRedisMajorVersionNumber(6, endpoint));
  }

  public ACLJedisTest(RedisProtocol redisProtocol) {
    super(redisProtocol);
  }

  @Test
  public void useWithoutConnecting() {
    try (Jedis j = new Jedis()) {
      assertEquals("OK", j.auth(endpoint.getUsername(), endpoint.getPassword()));
      j.dbSize();
    }
  }

  @Test
  public void connectWithConfig() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), DefaultJedisClientConfig.builder().build())) {
      jedis.auth(endpoint.getUsername(), endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithConfigInterface() {
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), new JedisClientConfig() {
    })) {
      jedis.auth(endpoint.getUsername(), endpoint.getPassword());
      assertEquals("PONG", jedis.ping());
    }
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort(), new JedisClientConfig() {
      @Override
      public String getUser() {
        return endpoint.getUsername();
      }

      @Override
      public String getPassword() {
        return endpoint.getPassword();
      }
    })) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void startWithUrl() {
    try (Jedis j = new Jedis(endpoint.getHostAndPort())) {
      assertEquals("OK", j.auth(endpoint.getUsername(), endpoint.getPassword()));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
    try (Jedis j2 = new Jedis(
        endpoint.getURIBuilder().defaultCredentials().path("/2").build().toString())) {
      assertEquals("PONG", j2.ping());
      assertEquals("bar", j2.get("foo"));
    }
  }

  @Test
  public void startWithUri() throws URISyntaxException {
    try (Jedis j = new Jedis(endpoint.getHostAndPort())) {
      assertEquals("OK", j.auth(endpoint.getUsername(), endpoint.getPassword()));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
    try (Jedis j1 = new Jedis(endpoint.getURIBuilder().defaultCredentials().path("/2").build())) {
      assertEquals("PONG", j1.ping());
      assertEquals("bar", j1.get("foo"));
    }
    try (Jedis j2 = new Jedis(endpoint.getURIBuilder().defaultCredentials().path("/2").build())) {
      assertEquals("PONG", j2.ping());
      assertEquals("bar", j2.get("foo"));
    }
  }

}
