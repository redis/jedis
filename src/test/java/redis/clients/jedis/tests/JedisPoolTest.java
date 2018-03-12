package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Test;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisExhaustedPoolException;

public class JedisPoolTest {
  public static final String PASSWORD = "foobared";
  public static final int TIMEOUT = 2000;
  private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

  @Test
  public void checkConnections() {
    ClientOptions clientOptions = ClientOptions.builder().withHostAndPort(hnp).withTimeout(TIMEOUT).build();
    JedisPool pool = new JedisPool(clientOptions);
    Jedis jedis = pool.getResource();
    jedis.auth(PASSWORD);
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.destroy();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    ClientOptions clientOptions = ClientOptions.builder().withHostAndPort(hnp).withTimeout(TIMEOUT).build();
    JedisPool pool = new JedisPool(clientOptions);
    Jedis jedis = pool.getResource();
    jedis.auth(PASSWORD);
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkConnectionWithDefaultPort() {
    JedisPool pool = new JedisPool(ClientOptions.builder().withHostAndPort(hnp).build());
    Jedis jedis = pool.getResource();
    jedis.auth(PASSWORD);
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.destroy();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkJedisIsReusedWhenReturned() {

    JedisPool pool = new JedisPool(ClientOptions.builder().withHostAndPort(hnp).build());
    Jedis jedis = pool.getResource();
    jedis.auth(PASSWORD);
    jedis.set("foo", "0");
    jedis.close();

    jedis = pool.getResource();
    jedis.auth(PASSWORD);
    jedis.incr("foo");
    jedis.close();
    pool.destroy();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkPoolRepairedWhenJedisIsBroken() {
    JedisPool pool = new JedisPool(ClientOptions.builder().withHostAndPort(hnp).build());
    Jedis jedis = pool.getResource();
    jedis.auth(PASSWORD);
    jedis.quit();
    jedis.close();

    jedis = pool.getResource();
    jedis.auth(PASSWORD);
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
    JedisPool pool = new JedisPool(config, ClientOptions.builder().withHostAndPort(hnp).build());
    Jedis jedis = pool.getResource();
    jedis.auth(PASSWORD);
    jedis.set("foo", "0");

    Jedis newJedis = pool.getResource();
    newJedis.auth(PASSWORD);
    newJedis.incr("foo");
  }

  @Test
  public void securePool() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setTestOnBorrow(true);
    JedisPool pool = new JedisPool(config, ClientOptions.builder().withHostAndPort(hnp).withTimeout(TIMEOUT).withPassword(PASSWORD).build());
    Jedis jedis = pool.getResource();
    jedis.set("foo", "bar");
    jedis.close();
    pool.destroy();
    assertTrue(pool.isClosed());
  }

  @Test
  public void nonDefaultDatabase() {
    JedisPool pool0 = new JedisPool(ClientOptions.builder().withHostAndPort(hnp).withTimeout(TIMEOUT).withPassword(PASSWORD).build());
    Jedis jedis0 = pool0.getResource();
    jedis0.set("foo", "bar");
    assertEquals("bar", jedis0.get("foo"));
    jedis0.close();
    pool0.destroy();
    assertTrue(pool0.isClosed());

    JedisPool pool1 = new JedisPool(ClientOptions.builder().withHostAndPort(hnp).withTimeout(TIMEOUT).withPassword(PASSWORD).withDatabase(1).build());
    Jedis jedis1 = pool1.getResource();
    assertNull(jedis1.get("foo"));
    jedis1.close();
    pool1.destroy();
    assertTrue(pool1.isClosed());
  }

  @Test
  public void startWithUrl() {
    Jedis j = new Jedis(ClientOptions.builder().withHost("localhost").withPort(6380).build());
    j.auth(PASSWORD);
    j.select(2);
    j.set("foo", "bar");
    JedisPool pool = new JedisPool(ClientOptions.builder().withURI(URI.create("redis://:foobared@localhost:6380/2")).build());
    Jedis jedis = pool.getResource();
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));
  }

  @Test(expected = InvalidURIException.class)
  public void shouldThrowInvalidURIExceptionForInvalidURI() throws URISyntaxException {
    ClientOptions.builder().withURI(URI.create("localhost:6380")).build();
  }

  @Test
  public void allowUrlWithNoDBAndNoPassword() throws URISyntaxException {
    new JedisPool(ClientOptions.builder().withURI(URI.create("redis://localhost:6380")).build());
  }

  @Test
  public void selectDatabaseOnActivation() {
    JedisPool pool = new JedisPool(ClientOptions.builder().withHostAndPort(hnp).withTimeout(200).withPassword(PASSWORD).build());

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
    String clientName = "my_shiny_client_name";

    ClientOptions options = ClientOptions.builder().withHostAndPort(hnp).withTimeout(TIMEOUT).withPassword(PASSWORD).withDatabase(0).withClientName(clientName).build();
    JedisPool pool0 = new JedisPool(options);

    Jedis jedis = pool0.getResource();

    assertEquals(clientName, jedis.clientGetname());

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
    JedisPool pool = new JedisPool(config, ClientOptions.builder().withHostAndPort(hnp).withTimeout(TIMEOUT).withPassword(PASSWORD).build());
    pool.initPool(config, new CrashingJedisPooledObjectFactory());
    Jedis crashingJedis = pool.getResource();

    try {
      crashingJedis.close();
    } catch (Exception ignored) {
    }

    assertEquals(destroyed.get(), 1);
  }

  @Test
  public void returnResourceShouldResetState() {
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisPool pool = new JedisPool(config, ClientOptions.builder().withHostAndPort(hnp).withTimeout(TIMEOUT).withPassword(PASSWORD).build());

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
    JedisPool pool = new JedisPool(config, ClientOptions.builder().withHostAndPort(hnp).withTimeout(TIMEOUT).withPassword(PASSWORD).build());

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
    JedisPool pool = new JedisPool(ClientOptions.builder().withHostAndPort(hnp).withTimeout(TIMEOUT).withPassword(PASSWORD).build());

    pool.destroy();
    assertTrue(pool.getNumActive() < 0);
  }

  @Test
  public void getNumActiveReturnsTheCorrectNumber() {
    JedisPool pool = new JedisPool(ClientOptions.builder().withHostAndPort(hnp).withPassword(PASSWORD).withTimeout(TIMEOUT).build());
    Jedis jedis = pool.getResource();
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));

    assertEquals(1, pool.getNumActive());

    Jedis jedis2 = pool.getResource();
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
    JedisPool pool = new JedisPool(ClientOptions.builder().withHostAndPort(hnp).withTimeout(TIMEOUT).build());
    pool.addObjects(1);
    assertEquals(pool.getNumIdle(), 1);
    pool.destroy();

  }

  @Test
  public void testCloseConnectionOnMakeObject() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setTestOnBorrow(true);
    JedisPool pool = new JedisPool(ClientOptions.builder().withHostAndPort(hnp).withTimeout(TIMEOUT).withPassword("wrong pass").build());
    Jedis jedis = new Jedis(ClientOptions.builder().withURI(URI.create("redis://:foobared@localhost:6379/")).build());
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
