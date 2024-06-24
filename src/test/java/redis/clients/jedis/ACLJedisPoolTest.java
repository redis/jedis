package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.RedisVersionUtil;

/**
 * This test class is a copy of {@link JedisPoolTest}.
 * <p>
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
public class ACLJedisPoolTest {
  private static final EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone0-acl");

  private static final EndpointConfig endpointWithDefaultUser = HostAndPorts.getRedisEndpoint("standalone0");

  @BeforeClass
  public static void prepare() throws Exception {
    // Use to check if the ACL test should be ran. ACL are available only in 6.0 and later
    org.junit.Assume.assumeTrue("Not running ACL test on this version of Redis",
        RedisVersionUtil.checkRedisMajorVersionNumber(6, endpoint));
  }

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

  @Test(expected = JedisException.class)
  public void checkPoolOverflow() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    try (JedisPool pool = new JedisPool(config, endpoint.getHost(), endpoint.getPort());
        Jedis jedis = pool.getResource()) {
      jedis.auth(endpoint.getUsername(), endpoint.getPassword());

      try (Jedis jedis2 = pool.getResource()) {
        jedis2.auth(endpoint.getUsername(), endpoint.getPassword());
      }
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

  @Test(expected = InvalidURIException.class)
  public void shouldThrowInvalidURIExceptionForInvalidURI() throws URISyntaxException {
    new JedisPool(new URI("localhost:6379")).close();
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
      assertTrue("Jedis instance was not reused", jedis1 == jedis0);
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
      try {
        pool.getResource();
        fail("Should throw exception as password is incorrect.");
      } catch (Exception e) {
        assertEquals(currentClientCount, getClientCount(jedis.clientList()));
      }
    }
  }

  private int getClientCount(final String clientList) {
    return clientList.split("\n").length;
  }
}
