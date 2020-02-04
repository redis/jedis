package redis.clients.jedis.tests;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisExhaustedPoolException;
import redis.clients.jedis.tests.utils.RedisVersionUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * This test class is a copy of @JedisPoolTest where all authentications are made with
 * default:foobared credentialsinformation
 *
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
public class JedisPoolWithCompleteCredentialsTest {
  private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

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
    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000);
    Jedis jedis = pool.getResource();
    jedis.auth("default","foobared");
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.destroy();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000);
    Jedis jedis = pool.getResource();
    jedis.auth("default","foobared");
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkConnectionWithDefaultPort() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort());
    Jedis jedis = pool.getResource();
    jedis.auth("default","foobared");
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.destroy();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkJedisIsReusedWhenReturned() {

    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort());
    Jedis jedis = pool.getResource();
    jedis.auth("default","foobared");
    jedis.set("foo", "0");
    jedis.close();

    jedis = pool.getResource();
    jedis.auth("foobared");
    jedis.incr("foo");
    jedis.close();
    pool.destroy();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkPoolRepairedWhenJedisIsBroken() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort());
    Jedis jedis = pool.getResource();
    jedis.auth("default","foobared");
    jedis.quit();
    jedis.close();

    jedis = pool.getResource();
    jedis.auth("default", "foobared");
    jedis.incr("foo");
    jedis.close();
    pool.destroy();
    assertTrue(pool.isClosed());
  }

  @Test(expected = JedisExhaustedPoolException.class)
  public void checkPoolOverflow() {
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort());
    Jedis jedis = pool.getResource();
    jedis.auth("default", "foobared");
    jedis.set("foo", "0");

    Jedis newJedis = pool.getResource();
    newJedis.auth("default", "foobared");
    newJedis.incr("foo");
  }

  @Test
  public void securePool() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setTestOnBorrow(true);
    JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(), 2000, "default","foobared");
    Jedis jedis = pool.getResource();
    jedis.set("foo", "bar");
    jedis.close();
    pool.destroy();
    assertTrue(pool.isClosed());
  }

  @Test
  public void securePoolNonSSL() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setTestOnBorrow(true);
    JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(), 2000, "default","foobared", false);
    Jedis jedis = pool.getResource();
    jedis.set("foo", "bar");
    jedis.close();
    pool.destroy();
    assertTrue(pool.isClosed());
  }

  @Test
  public void nonDefaultDatabase() {
    JedisPool pool0 = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000, "default",
        "foobared");
    Jedis jedis0 = pool0.getResource();
    jedis0.set("foo", "bar");
    assertEquals("bar", jedis0.get("foo"));
    jedis0.close();
    pool0.destroy();
    assertTrue(pool0.isClosed());

    JedisPool pool1 = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
        "foobared", 1);
    Jedis jedis1 = pool1.getResource();
    assertNull(jedis1.get("foo"));
    jedis1.close();
    pool1.destroy();
    assertTrue(pool1.isClosed());
  }

  @Test
  public void nonDefaultDatabaseNonSSL() {
    JedisPool pool0 = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000, "default",
            "foobared", false);
    Jedis jedis0 = pool0.getResource();
    jedis0.set("foo", "bar");
    assertEquals("bar", jedis0.get("foo"));
    jedis0.close();
    pool0.destroy();
    assertTrue(pool0.isClosed());

    JedisPool pool1 = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
            "foobared", 1, false);
    Jedis jedis1 = pool1.getResource();
    assertNull(jedis1.get("foo"));
    jedis1.close();
    pool1.destroy();
    assertTrue(pool1.isClosed());
  }

  @Test
  public void startWithUrlString() {
    Jedis j = new Jedis("localhost", 6380);
    j.auth("default", "foobared");
    j.select(2);
    j.set("foo", "bar");
    JedisPool pool = new JedisPool("redis://default:foobared@localhost:6380/2");
    Jedis jedis = pool.getResource();
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));
  }

  @Test
  public void startWithUrl() throws URISyntaxException {
    Jedis j = new Jedis("localhost", 6380);
    j.auth("default", "foobared");
    j.select(2);
    j.set("foo", "bar");
    JedisPool pool = new JedisPool(new URI("redis://default:foobared@localhost:6380/2"));
    Jedis jedis = pool.getResource();
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));
  }

  @Test(expected = InvalidURIException.class)
  public void shouldThrowInvalidURIExceptionForInvalidURI() throws URISyntaxException {
    JedisPool pool = new JedisPool(new URI("localhost:6380"));
  }

  @Test
  public void connectWithURICredentials() throws URISyntaxException {
    JedisPool pool = new JedisPool("localhost", 6380);
    Jedis j = pool.getResource();

    j.auth("default", "foobared");
    j.set("foo", "bar");

    // create new user
    j.aclSetUser("alice", "on", ">alicePassword", "~*", "+@all");

    Jedis jedis = new Jedis(new URI("redis://default:foobared@localhost:6380"));
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));

    Jedis jedis2 = new Jedis(new URI("redis://alice:alicePassword@localhost:6380"));
    assertEquals("PONG", jedis2.ping());
    assertEquals("bar", jedis2.get("foo"));

    // delete user
    j.aclDelUser("alice");
  }

  @Test
  public void allowUrlWithNoDBAndNoPassword() throws URISyntaxException {
    new JedisPool("redis://localhost:6380");
    new JedisPool(new URI("redis://localhost:6380"));
  }

  @Test
  public void selectDatabaseOnActivation() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
        "default", "foobared");

    Jedis jedis0 = pool.getResource();
    assertEquals(0, jedis0.getDB());

    jedis0.select(1);
    assertEquals(1, jedis0.getDB());

    jedis0.close();

    Jedis jedis1 = pool.getResource();
    assertTrue("Jedis instance was not reused", jedis1 == jedis0);
    assertEquals(0, jedis1.getDB());

    jedis1.close();
    pool.destroy();
    assertTrue(pool.isClosed());
  }

  @Test
  public void customClientName() {
    JedisPool pool0 = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
        "default", "foobared", 0, "my_shiny_client_name");

    Jedis jedis = pool0.getResource();

    assertEquals("my_shiny_client_name", jedis.clientGetname());

    jedis.close();
    pool0.destroy();
    assertTrue(pool0.isClosed());
  }

  @Test
  public void customClientNameNoSSL() {
    JedisPool pool0 = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
           "default", "foobared", 0, "my_shiny_client_name_no_ssl", false);

    Jedis jedis = pool0.getResource();

    assertEquals("my_shiny_client_name_no_ssl", jedis.clientGetname());

    jedis.close();
    pool0.destroy();
    assertTrue(pool0.isClosed());
  }

  @Test
  public void returnResourceDestroysResourceOnException() {

    class CrashingJedis extends Jedis {
      @Override
      public void resetState() {
        throw new RuntimeException();
      }
    }

    final AtomicInteger destroyed = new AtomicInteger(0);

    class CrashingJedisPooledObjectFactory implements PooledObjectFactory<Jedis> {

      @Override
      public PooledObject<Jedis> makeObject() throws Exception {
        return new DefaultPooledObject<Jedis>(new CrashingJedis());
      }

      @Override
      public void destroyObject(PooledObject<Jedis> p) throws Exception {
        destroyed.incrementAndGet();
      }

      @Override
      public boolean validateObject(PooledObject<Jedis> p) {
        return true;
      }

      @Override
      public void activateObject(PooledObject<Jedis> p) throws Exception {
      }

      @Override
      public void passivateObject(PooledObject<Jedis> p) throws Exception {
      }
    }

    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMaxTotal(1);
    JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(), 2000, "user", "foobared");
    pool.initPool(config, new CrashingJedisPooledObjectFactory());
    Jedis crashingJedis = pool.getResource();

    try {
      crashingJedis.close();
    } catch (Exception ignored) {
    }

    assertEquals(1, destroyed.get());
  }

  @Test
  public void returnResourceShouldResetState() {
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(), 2000, "default", "foobared");

    Jedis jedis = pool.getResource();
    try {
      jedis.set("hello", "jedis");
      Transaction t = jedis.multi();
      t.set("hello", "world");
    } finally {
      jedis.close();
    }

    Jedis jedis2 = pool.getResource();
    try {
      assertTrue(jedis == jedis2);
      assertEquals("jedis", jedis2.get("hello"));
    } finally {
      jedis2.close();
    }

    pool.destroy();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkResourceIsCloseable() {
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisPool pool = new JedisPool(config, hnp.getHost(), hnp.getPort(), 2000, "default", "foobared");

    Jedis jedis = pool.getResource();
    try {
      jedis.set("hello", "jedis");
    } finally {
      jedis.close();
    }

    Jedis jedis2 = pool.getResource();
    try {
      assertEquals(jedis, jedis2);
    } finally {
      jedis2.close();
    }
  }

  @Test
  public void getNumActiveIsNegativeWhenPoolIsClosed() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
        "user", "foobared", 0, "my_shiny_client_name");

    pool.destroy();
    assertTrue(pool.getNumActive() < 0);
  }

  @Test
  public void getNumActiveReturnsTheCorrectNumber() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000);
    Jedis jedis = pool.getResource();
    jedis.auth("default","foobared");
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));

    assertEquals(1, pool.getNumActive());

    Jedis jedis2 = pool.getResource();
    jedis.auth("default","foobared");
    jedis.set("foo", "bar");

    assertEquals(2, pool.getNumActive());

    jedis.close();
    assertEquals(1, pool.getNumActive());

    jedis2.close();

    assertEquals(0, pool.getNumActive());

    pool.destroy();
  }

  @Test
  public void testAddObject() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000);
    pool.addObjects(1);
    assertEquals(1, pool.getNumIdle());
    pool.destroy();
  }

  @Test
  public void closeResourceTwice() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000);
    Jedis j = pool.getResource();
    j.auth("default", "foobared");
    j.ping();
    j.close();
    j.close();
  }


  @Test
  public void testCloseConnectionOnMakeObject() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setTestOnBorrow(true);
    JedisPool pool = new JedisPool(new JedisPoolConfig(), hnp.getHost(), hnp.getPort(), 2000,
        "default", "wrong pass");
    Jedis jedis = new Jedis("redis://default:foobared@localhost:6379/");
    int currentClientCount = getClientCount(jedis.clientList());
    try {
      pool.getResource();
      fail("Should throw exception as password is incorrect.");
    } catch (Exception e) {
      assertEquals(currentClientCount, getClientCount(jedis.clientList()));
    }

  }

  private int getClientCount(final String clientList) {
    return clientList.split("\n").length;
  }

}
