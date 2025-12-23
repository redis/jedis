package redis.clients.jedis;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import io.redis.test.annotations.SinceRedisVersion;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.RedisVersionCondition;

/**
 * This test class is a copy of {@link JedisPoolTest}.
 * <p>
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
@SinceRedisVersion("6.0.0")
@Tag("integration")
public class ACLJedisPoolTest {
  private static final EndpointConfig endpoint = Endpoints.getRedisEndpoint("standalone0-acl");

  private static final EndpointConfig endpointWithDefaultUser = Endpoints.getRedisEndpoint("standalone0");

  @RegisterExtension
  public static RedisVersionCondition versionCondition = new RedisVersionCondition(endpoint);

  @Test
  public void checkConnections() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), endpoint.getHost(), endpoint.getPort(),
        endpoint.getUsername(), endpoint.getPassword());
    try (Jedis jedis = pool.getResource()) {
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    JedisPool pool = new JedisPool(endpoint.getHost(), endpoint.getPort(), endpoint.getUsername(),
        endpoint.getPassword());
    try (Jedis jedis = pool.getResource()) {
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkResourceIsClosableAndReusable() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    try (JedisPool pool = new JedisPool(config, endpoint.getHost(), endpoint.getPort(),
        Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, 0 /* infinite */, endpoint.getUsername(),
        endpoint.getPassword(), Protocol.DEFAULT_DATABASE, "closable-reusable-pool", false, null, null, null)) {

      Jedis jedis = pool.getResource();
      jedis.set("hello", "jedis");
      jedis.close();

      Jedis jedis2 = pool.getResource();
      assertEquals(jedis, jedis2);
      assertEquals("jedis", jedis2.get("hello"));
      jedis2.close();
    }
  }

  @Test
  public void checkResourceWithConfigIsClosableAndReusable() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    try (JedisPool pool = new JedisPool(config, endpoint.getHostAndPort(),
        endpoint.getClientConfigBuilder().clientName("closable-reusable-pool")
        .build())) {

      Jedis jedis = pool.getResource();
      jedis.set("hello", "jedis");
      jedis.close();

      Jedis jedis2 = pool.getResource();
      assertEquals(jedis, jedis2);
      assertEquals("jedis", jedis2.get("hello"));
      assertEquals("closable-reusable-pool", jedis2.clientGetname());
      jedis2.close();
    }
  }

  @Test
  public void checkPoolRepairedWhenJedisIsBroken() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), endpoint.getHost(), endpoint.getPort(),
        Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, 0 /* infinite */, endpoint.getUsername(),
        endpoint.getPassword(), Protocol.DEFAULT_DATABASE, "repairable-pool");
    try (Jedis jedis = pool.getResource()) {
      jedis.set("foo", "0");
      jedis.disconnect();
    }

    try (Jedis jedis = pool.getResource()) {
      jedis.incr("foo");
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkPoolOverflow() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    try (JedisPool pool = new JedisPool(config, endpoint.getHost(), endpoint.getPort());
        Jedis jedis = pool.getResource()) {
      jedis.auth(endpoint.getUsername(), endpoint.getPassword());

      assertThrows(JedisException.class, () -> {
        try (Jedis jedis2 = pool.getResource()) {
          jedis2.auth(endpoint.getUsername(), endpoint.getPassword());
        }
      });
    }
  }

  @Test
  public void securePool() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setTestOnBorrow(true);
    JedisPool pool = new JedisPool(config, endpoint.getHost(), endpoint.getPort(), 2000, endpoint.getUsername(),
        endpoint.getPassword());
    try (Jedis jedis = pool.getResource()) {
      jedis.set("foo", "bar");
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void securePoolNonSSL() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setTestOnBorrow(true);
    JedisPool pool = new JedisPool(config, endpoint.getHost(), endpoint.getPort(), 2000, endpoint.getUsername(),
        endpoint.getPassword(), false);
    try (Jedis jedis = pool.getResource()) {
      jedis.set("foo", "bar");
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void nonDefaultDatabase() {
    try (JedisPool pool0 = new JedisPool(new JedisPoolConfig(), endpoint.getHost(), endpoint.getPort(), 2000,
        endpoint.getUsername(), endpoint.getPassword()); Jedis jedis0 = pool0.getResource()) {
      jedis0.set("foo", "bar");
      assertEquals("bar", jedis0.get("foo"));
    }

    try (JedisPool pool1 = new JedisPool(new JedisPoolConfig(), endpoint.getHost(), endpoint.getPort(), 2000,
        endpoint.getUsername(), endpoint.getPassword(), 1); Jedis jedis1 = pool1.getResource()) {
      assertNull(jedis1.get("foo"));
    }
  }

  @Test
  public void startWithUrlString() {
    try (Jedis j = new Jedis(endpoint.getHost(), endpoint.getPort())) {
      j.auth(endpoint.getUsername(), endpoint.getPassword());
      j.select(2);
      j.set("foo", "bar");
    }

    try (JedisPool pool = new JedisPool(
        endpoint.getURIBuilder().defaultCredentials().path("/2").build());
        Jedis jedis = pool.getResource()) {
      assertEquals("bar", jedis.get("foo"));
    }
  }

  @Test
  public void startWithUrl() throws URISyntaxException {
    try (Jedis j = new Jedis(endpoint.getHost(), endpoint.getPort())) {
      j.auth(endpoint.getUsername(), endpoint.getPassword());
      j.select(2);
      j.set("foo", "bar");
    }

    try (JedisPool pool = new JedisPool(
        endpoint.getURIBuilder().defaultCredentials().path("/2").build());
        Jedis jedis = pool.getResource()) {
      assertEquals("bar", jedis.get("foo"));
    }

    try (JedisPool pool = new JedisPool(
        endpointWithDefaultUser.getURIBuilder().defaultCredentials().path("/2").build());
        Jedis jedis = pool.getResource()) {
      assertEquals("bar", jedis.get("foo"));
    }
  }

  @Test
  public void shouldThrowInvalidURIExceptionForInvalidURI() {
    assertThrows(InvalidURIException.class, () -> {
      new JedisPool(new URI("redis://localhost:")).close();
    });
  }

  @Test
  public void allowUrlWithNoDBAndNoPassword() throws URISyntaxException {
    new JedisPool(endpoint.getURI().toString()).close();
    new JedisPool(endpoint.getURI()).close();
  }

  @Test
  public void selectDatabaseOnActivation() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpoint.getHost(), endpoint.getPort(), 2000,
        endpoint.getUsername(), endpoint.getPassword())) {

      Jedis jedis0 = pool.getResource();
      assertEquals(0, jedis0.getDB());

      jedis0.select(1);
      assertEquals(1, jedis0.getDB());

      jedis0.close();

      Jedis jedis1 = pool.getResource();
      assertSame(jedis1, jedis0);
      assertEquals(0, jedis1.getDB());

      jedis1.close();
    }
  }

  @Test
  public void customClientName() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpoint.getHost(), endpoint.getPort(), 2000,
        endpoint.getUsername(), endpoint.getPassword(), 0, "my_shiny_client_name"); Jedis jedis = pool.getResource()) {

      assertEquals("my_shiny_client_name", jedis.clientGetname());
    }
  }

  @Test
  public void customClientNameNoSSL() {
    try (JedisPool pool0 = new JedisPool(new JedisPoolConfig(), endpoint.getHost(), endpoint.getPort(), 2000,
        endpoint.getUsername(), endpoint.getPassword(), 0, "my_shiny_client_name_no_ssl", false);
        Jedis jedis = pool0.getResource()) {

      assertEquals("my_shiny_client_name_no_ssl", jedis.clientGetname());
    }
  }

  @Test
  public void testCloseConnectionOnMakeObject() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setTestOnBorrow(true);
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpoint.getHost(),
        endpoint.getPort(), 2000, endpoint.getUsername(), "wrongpassword");
        Jedis jedis = new Jedis(endpointWithDefaultUser.getURIBuilder()
            .credentials("", endpointWithDefaultUser.getPassword()).build())) {
      int currentClientCount = getClientCount(jedis.clientList());
      assertThrows(JedisAccessControlException.class, pool::getResource);
      // wait for the redis server to close the connection
      await().pollDelay(Duration.ofMillis(10)).atMost(500, MILLISECONDS)
          .until(() -> getClientCount(jedis.clientList()) == currentClientCount);
      assertEquals(currentClientCount, getClientCount(jedis.clientList()));
    }
  }

  private int getClientCount(final String clientList) {
    return clientList.split("\n").length;
  }
}
