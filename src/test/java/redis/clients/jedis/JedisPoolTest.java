package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public class JedisPoolTest {

  private static final EndpointConfig endpointStandalone0 = HostAndPorts.getRedisEndpoint("standalone0");

  private static final EndpointConfig endpointStandalone1 = HostAndPorts.getRedisEndpoint("standalone1");

  @Test
  public void checkConnections() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000);
    try (Jedis jedis = pool.getResource()) {
      jedis.auth(endpointStandalone0.getPassword());
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkResourceWithConfig() {
    try (JedisPool pool = new JedisPool(HostAndPorts.getRedisEndpoint("standalone7-with-lfu-policy").getHostAndPort(),
        DefaultJedisClientConfig.builder().socketTimeoutMillis(5000).build())) {

      try (Jedis jedis = pool.getResource()) {
        assertEquals("PONG", jedis.ping());
        assertEquals(5000, jedis.getClient().getSoTimeout());
        jedis.close();
      }
    }
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000);
    try (Jedis jedis = pool.getResource()) {
      jedis.auth(endpointStandalone0.getPassword());
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkConnectionWithDefaultHostAndPort() {
    JedisPool pool = new JedisPool(new JedisPoolConfig());
    try (Jedis jedis = pool.getResource()) {
      jedis.auth(endpointStandalone0.getPassword());
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
    try (JedisPool pool = new JedisPool(config, endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000, endpointStandalone0.getPassword(), 0,
        "closable-reusable-pool", false, null, null, null)) {

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
    JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort());
    try (Jedis jedis = pool.getResource()) {
      jedis.auth(endpointStandalone0.getPassword());
      jedis.set("foo", "0");
      jedis.disconnect();
    }

    try (Jedis jedis = pool.getResource()) {
      jedis.auth(endpointStandalone0.getPassword());
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
    try (JedisPool pool = new JedisPool(config, endpointStandalone0.getHost(), endpointStandalone0.getPort());
        Jedis jedis = pool.getResource()) {
      jedis.auth(endpointStandalone0.getPassword());

      try (Jedis jedis2 = pool.getResource()) {
        jedis2.auth(endpointStandalone0.getPassword());
      }
    }
  }

  @Test
  public void securePool() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setTestOnBorrow(true);
    JedisPool pool = new JedisPool(config, endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000, endpointStandalone0.getPassword());
    try (Jedis jedis = pool.getResource()) {
      jedis.set("foo", "bar");
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void nonDefaultDatabase() {
    try (JedisPool pool0 = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000,
        endpointStandalone0.getPassword()); Jedis jedis0 = pool0.getResource()) {
      jedis0.set("foo", "bar");
      assertEquals("bar", jedis0.get("foo"));
    }

    try (JedisPool pool1 = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000,
        endpointStandalone0.getPassword(), 1); Jedis jedis1 = pool1.getResource()) {
      assertNull(jedis1.get("foo"));
    }
  }

  @Test
  public void startWithUrlString() {
    try (Jedis j = new Jedis(endpointStandalone1.getHostAndPort())) {
      j.auth(endpointStandalone1.getPassword());
      j.select(2);
      j.set("foo", "bar");
    }

    try (JedisPool pool = new JedisPool(
        endpointStandalone1.getURIBuilder().credentials("", endpointStandalone1.getPassword()).path("/2").build());
        Jedis jedis = pool.getResource()) {
      assertEquals("PONG", jedis.ping());
      assertEquals("bar", jedis.get("foo"));
    }
  }

  @Test
  public void startWithUrl() throws URISyntaxException {
    try (Jedis j = new Jedis(endpointStandalone1.getHostAndPort())) {
      j.auth(endpointStandalone1.getPassword());
      j.select(2);
      j.set("foo", "bar");
    }

    try (JedisPool pool = new JedisPool(
        endpointStandalone1.getURIBuilder().credentials("", endpointStandalone1.getPassword()).path("/2").build());
        Jedis jedis = pool.getResource()) {
      assertEquals("bar", jedis.get("foo"));
    }
  }

  @Test(expected = InvalidURIException.class)
  public void shouldThrowInvalidURIExceptionForInvalidURI() throws URISyntaxException {
    new JedisPool(new URI("localhost:6380")).close();
  }

  @Test
  public void allowUrlWithNoDBAndNoPassword() throws URISyntaxException {
    new JedisPool(endpointStandalone1.getURI().toString()).close();
    new JedisPool(endpointStandalone1.getURI()).close();
  }

  @Test
  public void selectDatabaseOnActivation() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000,
        endpointStandalone0.getPassword())) {

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
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000,
        endpointStandalone0.getPassword(), 0, "my_shiny_client_name"); Jedis jedis = pool.getResource()) {

      assertEquals("my_shiny_client_name", jedis.clientGetname());
    }
  }

  @Test
  public void invalidClientName() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000,
        endpointStandalone0.getPassword(), 0, "invalid client name"); Jedis jedis = pool.getResource()) {
    } catch (Exception e) {
      if (!e.getMessage().startsWith("client info cannot contain space")) {
        Assert.fail("invalid client name test fail");
      }
    }
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

    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    JedisPool pool = new JedisPool(config, new CrashingJedisPooledObjectFactory());
    Jedis crashingJedis = pool.getResource();

    try {
      crashingJedis.close();
    } catch (Exception ignored) {
    }

    assertEquals(1, destroyed.get());
  }

  @Test
  public void returnResourceShouldResetState() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisPool pool = new JedisPool(config, endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000, endpointStandalone0.getPassword());

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

    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void getNumActiveWhenPoolIsClosed() {
    JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000,
        endpointStandalone0.getPassword(), 0, "my_shiny_client_name");

    try (Jedis j = pool.getResource()) {
      j.ping();
    }

    pool.close();
    assertEquals(0, pool.getNumActive());
  }

  @Test
  public void getNumActiveReturnsTheCorrectNumber() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000)) {
      Jedis jedis = pool.getResource();
      jedis.auth(endpointStandalone0.getPassword());
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));

      assertEquals(1, pool.getNumActive());

      Jedis jedis2 = pool.getResource();
      jedis.auth(endpointStandalone0.getPassword());
      jedis.set("foo", "bar");

      assertEquals(2, pool.getNumActive());

      jedis.close();
      assertEquals(1, pool.getNumActive());

      jedis2.close();

      assertEquals(0, pool.getNumActive());
    }
  }

  @Test
  public void testAddObject() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000)) {
      pool.addObjects(1);
      assertEquals(1, pool.getNumIdle());
    }
  }

  @Test
  public void closeResourceTwice() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000)) {
      Jedis j = pool.getResource();
      j.auth(endpointStandalone0.getPassword());
      j.ping();
      j.close();
      j.close();
    }
  }

  @Test
  public void closeBrokenResourceTwice() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(), endpointStandalone0.getPort(), 2000)) {
      Jedis j = pool.getResource();
      try {
        // make connection broken
        j.getClient().getOne();
        fail();
      } catch (Exception e) {
        assertTrue(e instanceof JedisConnectionException);
      }
      assertTrue(j.isBroken());
      j.close();
      j.close();
    }
  }

  @Test
  public void testCloseConnectionOnMakeObject() {
    JedisPoolConfig config = new JedisPoolConfig();
    config.setTestOnBorrow(true);
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHost(),
        endpointStandalone0.getPort(), 2000, "wrong pass");
        Jedis jedis = new Jedis(endpointStandalone0.getURIBuilder().defaultCredentials().build())) {
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

  @Test
  public void testResetInvalidCredentials() {
    DefaultRedisCredentialsProvider credentialsProvider
        = new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(null, endpointStandalone0.getPassword()));
    JedisFactory factory = new JedisFactory(endpointStandalone0.getHostAndPort(), DefaultJedisClientConfig.builder()
        .credentialsProvider(credentialsProvider).clientName("my_shiny_client_name").build());

    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), factory)) {
      Jedis obj1_ref;
      try (Jedis obj1_1 = pool.getResource()) {
        obj1_ref = obj1_1;
        obj1_1.set("foo", "bar");
        assertEquals("bar", obj1_1.get("foo"));
        assertEquals(1, pool.getNumActive());
      }
      assertEquals(0, pool.getNumActive());
      try (Jedis obj1_2 = pool.getResource()) {
        assertSame(obj1_ref, obj1_2);
        assertEquals(1, pool.getNumActive());
        credentialsProvider.setCredentials(new DefaultRedisCredentials(null, "wrong password"));
        try (Jedis obj2 = pool.getResource()) {
          fail("Should not get resource from pool");
        } catch (JedisException e) { }
        assertEquals(1, pool.getNumActive());
      }
      assertEquals(0, pool.getNumActive());
    }
  }

  @Test
  public void testResetValidCredentials() {
    DefaultRedisCredentialsProvider credentialsProvider
        = new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(null, "bad password"));
    JedisFactory factory = new JedisFactory(endpointStandalone0.getHostAndPort(), DefaultJedisClientConfig.builder()
        .credentialsProvider(credentialsProvider).clientName("my_shiny_client_name").build());

    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), factory)) {
      try (Jedis obj1 = pool.getResource()) {
        fail("Should not get resource from pool");
      } catch (JedisException e) { }
      assertEquals(0, pool.getNumActive());

      credentialsProvider.setCredentials(new DefaultRedisCredentials(null, endpointStandalone0.getPassword()));
      try (Jedis obj2 = pool.getResource()) {
        obj2.set("foo", "bar");
        assertEquals("bar", obj2.get("foo"));
      }
    }
  }
}
