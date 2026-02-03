package redis.clients.jedis;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.BeforeAll;
import redis.clients.jedis.exceptions.InvalidURIException;
import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.ResourceLocks;

@Tag("integration")
@ResourceLocks({
    @ResourceLock(value = Endpoints.STANDALONE0),
    @ResourceLock(value = Endpoints.STANDALONE1),
    @ResourceLock(value = Endpoints.STANDALONE7_WITH_LFU_POLICY)
})
public class JedisPoolTest {

  private static EndpointConfig endpointStandalone0;

  private static EndpointConfig endpointStandalone1;

  private String testKey;
  private String testValue;

  @BeforeAll
  public static void prepareEndpoints() {
    endpointStandalone0 = Endpoints.getRedisEndpoint(Endpoints.STANDALONE0);
    endpointStandalone1 = Endpoints.getRedisEndpoint(Endpoints.STANDALONE1);
  }

  @BeforeEach
  public void setUpTestKey(TestInfo testInfo) {
    testKey = testInfo.getDisplayName() + "-key";
    testValue = testInfo.getDisplayName() + "-value";
  }

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
    try (JedisPool pool = new JedisPool(Endpoints.getRedisEndpoint(Endpoints.STANDALONE7_WITH_LFU_POLICY).getHostAndPort(),
        DefaultJedisClientConfig.builder().socketTimeoutMillis(5000).build())) {

      try (Jedis jedis = pool.getResource()) {
        assertEquals("PONG", jedis.ping());
        assertEquals(5000, jedis.getClient().getSoTimeout());
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
      assertSame(jedis, jedis2);
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

  @Test
  public void checkPoolOverflow() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    try (JedisPool pool = new JedisPool(config, endpointStandalone0.getHost(), endpointStandalone0.getPort());
        Jedis jedis = pool.getResource()) {
      jedis.auth(endpointStandalone0.getPassword());

      assertThrows(JedisException.class, pool::getResource);
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

  @Test
  public void shouldThrowInvalidURIExceptionForInvalidURI() throws URISyntaxException {
    assertThrows(InvalidURIException.class, ()->new JedisPool(new URI("localhost:6380")).close());
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
      assertSame(jedis1, jedis0);
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
       fail("invalid client name test fail");
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

    try (Jedis jedis2 = pool.getResource()) {
      assertSame(jedis, jedis2);
      assertEquals("jedis", jedis2.get("hello"));
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
        assertInstanceOf(JedisConnectionException.class, e);
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
        } catch (JedisException e) {
          //ignore
        }
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
      } catch (JedisException e) {
        //ignore
      }
      assertEquals(0, pool.getNumActive());

      credentialsProvider.setCredentials(new DefaultRedisCredentials(null, endpointStandalone0.getPassword()));
      try (Jedis obj2 = pool.getResource()) {
        obj2.set("foo", "bar");
        assertEquals("bar", obj2.get("foo"));
      }
    }
  }

  @Test
  public void testWithResource() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHostAndPort(),
        endpointStandalone0.getClientConfigBuilder().build())) {

      pool.withResource(jedis -> {
        jedis.set(testKey, testValue);
      });

      pool.withResource(jedis -> {
        assertEquals(testValue, jedis.get(testKey));
      });
    }
  }

  @Test
  public void testWithResourceReturnsConnectionToPool() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHostAndPort(),
        endpointStandalone0.getClientConfigBuilder().build())) {

      pool.withResource(jedis -> {
        assertThat(pool.getNumActive(), equalTo(1));
        jedis.set("foo", "bar");
      });

      assertThat(pool.getNumActive(), equalTo(0));
    }
  }

  @Test
  public void testWithResourceGet() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHostAndPort(),
        endpointStandalone0.getClientConfigBuilder().build())) {

      String result = pool.withResourceGet(jedis -> {
        jedis.set(testKey, testValue);
        return jedis.get(testKey);
      });

      assertEquals(testValue, result);
    }
  }

  @Test
  public void testWithResourceGetReturnsConnectionToPool() {
    try (JedisPool pool = new JedisPool(new JedisPoolConfig(), endpointStandalone0.getHostAndPort(),
        endpointStandalone0.getClientConfigBuilder().build())) {

      String result = pool.withResourceGet(jedis -> {
        assertThat(pool.getNumActive(), equalTo(1));
        jedis.set("foo", "bar");
        return jedis.get("foo");
      });

      assertThat(result, equalTo("bar"));
      assertThat(pool.getNumActive(), equalTo(0));
    }
  }

}
