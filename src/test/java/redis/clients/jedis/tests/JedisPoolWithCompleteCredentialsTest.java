package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisExhaustedPoolException;
import redis.clients.jedis.tests.utils.RedisVersionUtil;

/**
 * This test class is a copy of {@link JedisPoolTest}.
 *
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
public class JedisPoolWithCompleteCredentialsTest {
  private static final HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

  /**
   * Use to check if the ACL test should be ran. ACL are available only in 6.0 and later
   * @throws Exception
   */
  @Before
  public void setUp() throws Exception {
    Jedis jedis = new Jedis(hnp.getHost(), hnp.getPort(), 500);
    jedis.connect();
    jedis.auth("foobared");
    // run the test only if the verison support ACL (6 or later)
    boolean shouldNotRun = ((new RedisVersionUtil(jedis)).getRedisMajorVersionNumber() < 6);

    if ( shouldNotRun ) {
      org.junit.Assume.assumeFalse("Not running ACL test on this version of Redis", shouldNotRun);
    }
  }

  @Test
  public void checkConnections() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), "acljedis", "fizzbuzz");
    try (Jedis jedis = pool.getResource()) {
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    JedisPool pool = new JedisPool(hnp.getHost(), hnp.getPort(), "acljedis", "fizzbuzz");
    try (Jedis jedis = pool.getResource()) {
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkResourceIsClosableAndReusable() {
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    try (JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(),
        Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, 0 /*infinite*/, "acljedis", "fizzbuzz",
        Protocol.DEFAULT_DATABASE, "closable-resuable-pool", false, null, null, null)) {

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
  public void checkPoolRepairedWhenJedisIsBroken() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(),
        Protocol.DEFAULT_TIMEOUT, Protocol.DEFAULT_TIMEOUT, 0 /*infinite*/, "acljedis", "fizzbuzz",
        Protocol.DEFAULT_DATABASE, "repairable-pool");
    try (Jedis jedis = pool.getResource()) {
      jedis.set("foo", "0");
      jedis.quit();
    }

    try (Jedis jedis = pool.getResource()) {
      jedis.incr("foo");
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test(expected = JedisExhaustedPoolException.class)
  public void checkPoolOverflow() {
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    try (JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort());
        Jedis jedis = pool.getResource()) {
      jedis.auth("acljedis", "fizzbuzz");
      
      try (Jedis jedis2 = pool.getResource()) {
        jedis2.auth("acljedis", "fizzbuzz");
      }
    }
  }

  @Test
  public void securePool() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setTestOnBorrow(true);
    JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(), 2000, "acljedis", "fizzbuzz");
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
    JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(), 2000, "acljedis", "fizzbuzz", false);
    try (Jedis jedis = pool.getResource()) {
      jedis.set("foo", "bar");
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void nonDefaultDatabase() {
    try (JedisPool pool0 = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
        "acljedis", "fizzbuzz");
        Jedis jedis0 = pool0.getResource()) {
      jedis0.set("foo", "bar");
      assertEquals("bar", jedis0.get("foo"));
    }

    try (JedisPool pool1 = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
        "acljedis", "fizzbuzz", 1);
        Jedis jedis1 = pool1.getResource()) {
      assertNull(jedis1.get("foo"));
    }
  }

  @Test
  public void startWithUrlString() {
    try (Jedis j = new Jedis("localhost", 6379)) {
      j.auth("acljedis", "fizzbuzz");
      j.select(2);
      j.set("foo", "bar");
    }

    try (JedisPool pool = new JedisPool("redis://acljedis:fizzbuzz@localhost:6379/2");
        Jedis jedis = pool.getResource()) {
      assertEquals("bar", jedis.get("foo"));
    }
  }

  @Test
  public void startWithUrl() throws URISyntaxException {
    try (Jedis j = new Jedis("localhost", 6379)) {
      j.auth("acljedis", "fizzbuzz");
      j.select(2);
      j.set("foo", "bar");
    }

    try (JedisPool pool = new JedisPool(new URI("redis://acljedis:fizzbuzz@localhost:6379/2"));
        Jedis jedis = pool.getResource()) {
      assertEquals("bar", jedis.get("foo"));
    }

    try (JedisPool pool = new JedisPool(new URI("redis://default:foobared@localhost:6379/2"));
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
    new JedisPool("redis://localhost:6379").close();
    new JedisPool(new URI("redis://localhost:6379")).close();
  }

  @Test
  public void selectDatabaseOnActivation() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
        "acljedis", "fizzbuzz")) {

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
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
        "acljedis", "fizzbuzz", 0, "my_shiny_client_name");
        Jedis jedis = pool.getResource()) {

      assertEquals("my_shiny_client_name", jedis.clientGetname());
    }
  }

  @Test
  public void customClientNameNoSSL() {
    try (JedisPool pool0 = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
        "acljedis", "fizzbuzz", 0, "my_shiny_client_name_no_ssl", false);
        Jedis jedis = pool0.getResource()) {
      
      assertEquals("my_shiny_client_name_no_ssl", jedis.clientGetname());
    }
  }

  @Test
  public void testCloseConnectionOnMakeObject() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setTestOnBorrow(true);
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
        "acljedis", "foobared");
        Jedis jedis = new Jedis("redis://:foobared@localhost:6379/")) {
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
