package redis.clients.jedis;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.commands.jedis.JedisCommandsTestBase;
import redis.clients.jedis.util.RedisVersionUtil;

/**
 * This test class is a copy of {@link JedisTest}.
 * <p>
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
public class ACLJedisTest extends JedisCommandsTestBase {

  /**
   * Use to check if the ACL test should be ran. ACL are available only in 6.0 and later
   * @throws Exception
   */
  @BeforeClass
  public static void prepare() throws Exception {
    org.junit.Assume.assumeTrue("Not running ACL test on this version of Redis",
        RedisVersionUtil.checkRedisMajorVersionNumber(6));
  }

  @Test
  public void useWithoutConnecting() {
    try (Jedis j = new Jedis()) {
      assertEquals("OK", j.auth("acljedis", "fizzbuzz"));
      j.dbSize();
    }
  }

  @Test
  public void connectWithConfig() {
    try (Jedis jedis = new Jedis(hnp, DefaultJedisClientConfig.builder().build())) {
      jedis.auth("acljedis", "fizzbuzz");
      assertEquals("PONG", jedis.ping());
    }
    try (Jedis jedis = new Jedis(hnp, DefaultJedisClientConfig.builder().user("acljedis")
        .password("fizzbuzz").build())) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void connectWithConfigInterface() {
    try (Jedis jedis = new Jedis(hnp, new JedisClientConfig() {
    })) {
      jedis.auth("acljedis", "fizzbuzz");
      assertEquals("PONG", jedis.ping());
    }
    try (Jedis jedis = new Jedis(hnp, new JedisClientConfig() {
      @Override
      public String getUser() {
        return "acljedis";
      }

      @Override
      public String getPassword() {
        return "fizzbuzz";
      }
    })) {
      assertEquals("PONG", jedis.ping());
    }
  }

  @Test
  public void startWithUrl() {
    try (Jedis j = new Jedis("localhost", 6379)) {
      assertEquals("OK", j.auth("acljedis", "fizzbuzz"));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
    try (Jedis j2 = new Jedis("redis://acljedis:fizzbuzz@localhost:6379/2")) {
      assertEquals("PONG", j2.ping());
      assertEquals("bar", j2.get("foo"));
    }
  }

  @Test
  public void startWithUri() throws URISyntaxException {
    try (Jedis j = new Jedis("localhost", 6379)) {
      assertEquals("OK", j.auth("acljedis", "fizzbuzz"));
      assertEquals("OK", j.select(2));
      j.set("foo", "bar");
    }
    try (Jedis j2 = new Jedis(new URI("redis://acljedis:fizzbuzz@localhost:6379/2"))) {
      assertEquals("PONG", j2.ping());
      assertEquals("bar", j2.get("foo"));
    }
  }

  @Test
  public void connectWithURICredentials() throws URISyntaxException {
    jedis.set("foo", "bar");

    try (Jedis j1 = new Jedis(new URI("redis://default:foobared@localhost:6379"))) {
      assertEquals("PONG", j1.ping());
      assertEquals("bar", j1.get("foo"));
    }

    try (Jedis j2 = new Jedis(new URI("redis://acljedis:fizzbuzz@localhost:6379"))) {
      assertEquals("PONG", j2.ping());
      assertEquals("bar", j2.get("foo"));
    }
  }

  @Test
  public void allowUrlWithNoDBAndNoPassword() {
    try (Jedis j1 = new Jedis("redis://localhost:6379")) {
      assertEquals("OK", j1.auth("acljedis", "fizzbuzz"));
//      assertEquals("localhost", j1.getClient().getHost());
//      assertEquals(6379, j1.getClient().getPort());
      assertEquals(0, j1.getDB());
    }

    try (Jedis j2 = new Jedis("redis://localhost:6379/")) {
      assertEquals("OK", j2.auth("acljedis", "fizzbuzz"));
//      assertEquals("localhost", j2.getClient().getHost());
//      assertEquals(6379, j2.getClient().getPort());
      assertEquals(0, j2.getDB());
    }
  }

}
