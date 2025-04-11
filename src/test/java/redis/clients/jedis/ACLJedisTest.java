package redis.clients.jedis;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static redis.clients.jedis.util.RedisVersionUtil.getRedisVersion;

import io.redis.test.utils.RedisVersion;
import java.net.URISyntaxException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;


import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;
/**
 * This test class is a copy of {@link JedisTest}.
 * <p>
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
public class ACLJedisTest extends JedisCommandsTestBase {

  protected static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-acl");

  /**
   * Use to check if the ACL test should be ran. ACL are available only in 6.0 and later
   */
  @BeforeAll
  public static void prepare() {
    assumeTrue(getRedisVersion(endpoint).isGreaterThanOrEqualTo(RedisVersion.of("6.0.0")),
        "Not running ACL test on this version of Redis");
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

  @Test
  public void startWithHostPortStringAndSsl() {
    try (Jedis j = new Jedis(endpoint.getHost(), endpoint.getPort(), false)) {
      assertEquals("OK", j.auth(endpoint.getUsername(), endpoint.getPassword()));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
    try (Jedis j1 = new Jedis(endpoint.getURIBuilder().defaultCredentials().path("/2").build())) {
      assertEquals("PONG", j1.ping());
      assertEquals("bar", j1.get("foo"));
    }
  }

  @Test
  public void startWithHostPortStringAndTimeout() {
    try (Jedis j = new Jedis(endpoint.getHost(), endpoint.getPort(), Protocol.DEFAULT_TIMEOUT)) {
      assertEquals("OK", j.auth(endpoint.getUsername(), endpoint.getPassword()));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
    try (Jedis j1 = new Jedis(endpoint.getURIBuilder().defaultCredentials().path("/2").build())) {
      assertEquals("PONG", j1.ping());
      assertEquals("bar", j1.get("foo"));
    }
  }

  @Test
  public void startWithHostPortStringAndTimeoutAndSsl() {
    try (Jedis j = new Jedis(endpoint.getHost(), endpoint.getPort(), Protocol.DEFAULT_TIMEOUT, false)) {
      assertEquals("OK", j.auth(endpoint.getUsername(), endpoint.getPassword()));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
    try (Jedis j1 = new Jedis(endpoint.getURIBuilder().defaultCredentials().path("/2").build())) {
      assertEquals("PONG", j1.ping());
      assertEquals("bar", j1.get("foo"));
    }
  }

  @Test
  public void startWithHostPortStringAndTimeouts() {
    try (Jedis j = new Jedis(endpoint.getHost(), endpoint.getPort(), Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT)) {
      assertEquals("OK", j.auth(endpoint.getUsername(), endpoint.getPassword()));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
    try (Jedis j1 = new Jedis(endpoint.getURIBuilder().defaultCredentials().path("/2").build())) {
      assertEquals("PONG", j1.ping());
      assertEquals("bar", j1.get("foo"));
    }
  }

  @Test
  public void startWithURI() throws URISyntaxException {
    try (Jedis j = new Jedis(endpoint.getURIBuilder().defaultCredentials().build())) {
      assertEquals("OK", j.auth(endpoint.getUsername(), endpoint.getPassword()));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
    try (Jedis j1 = new Jedis(endpoint.getURIBuilder().defaultCredentials().path("/2").build())) {
      assertEquals("PONG", j1.ping());
      assertEquals("bar", j1.get("foo"));
    }
  }

  @Test
  public void startWithURIAndTimeout() throws URISyntaxException {
    try (Jedis j = new Jedis(endpoint.getURIBuilder().defaultCredentials().build(), Protocol.DEFAULT_TIMEOUT)) {
      assertEquals("OK", j.auth(endpoint.getUsername(), endpoint.getPassword()));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
    try (Jedis j1 = new Jedis(endpoint.getURIBuilder().defaultCredentials().path("/2").build())) {
      assertEquals("PONG", j1.ping());
      assertEquals("bar", j1.get("foo"));
    }
  }

  @Test
  public void startWithFactory() {
    JedisSocketFactory socketFactory = new DefaultJedisSocketFactory(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build());

    try(Jedis j = new Jedis(socketFactory)) {
      assertEquals("OK", j.auth(endpoint.getUsername(), endpoint.getPassword()));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
  }

  @Test
  public void startWithFactoryAndConfig() {
    JedisSocketFactory socketFactory = new DefaultJedisSocketFactory(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build());

    try(Jedis j = new Jedis(socketFactory, DefaultJedisClientConfig.builder().build())) {
      assertEquals("OK", j.auth(endpoint.getUsername(), endpoint.getPassword()));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
  }

  @Test
  public void startWithConnection() {
    JedisSocketFactory socketFactory = new DefaultJedisSocketFactory(endpoint.getHostAndPort(), endpoint.getClientConfigBuilder().build());
    Connection connection = new Connection(socketFactory);
    try(Jedis j = new Jedis(connection)) {
      assertEquals("OK", j.auth(endpoint.getUsername(), endpoint.getPassword()));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
  }
}
