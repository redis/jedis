package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public class JedisPooledTest {

  private static final HostAndPort hnp = HostAndPorts.getRedisServers().get(7);

  @Test
  public void checkCloseableConnections() {
    JedisPooled pool = new JedisPooled(new ConnectionPoolConfig(), hnp.getHost(), hnp.getPort(), 2000);
    pool.set("foo", "bar");
    assertEquals("bar", pool.get("foo"));
    pool.close();
    assertTrue(pool.getPool().isClosed());
  }

  @Test
  public void checkResourceWithConfig() {
    try (JedisPooled pool = new JedisPooled(hnp,
        DefaultJedisClientConfig.builder().socketTimeoutMillis(5000).build())) {

      try (Connection jedis = pool.getPool().getResource()) {
        assertTrue(jedis.ping());
        assertEquals(5000, jedis.getSoTimeout());
      }
    }
  }

  @Test(expected = JedisException.class)
  public void checkPoolOverflow() {
    GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    try (JedisPooled pool = new JedisPooled(hnp, config);
        Connection jedis = pool.getPool().getResource()) {

      try (Connection jedis2 = pool.getPool().getResource()) {
      }
    }
  }

  @Test
  public void startWithUrlString() {
    try (Jedis j = new Jedis("localhost", 6380)) {
      j.auth("foobared");
      j.select(2);
      j.set("foo", "bar");
    }

    try (JedisPooled pool = new JedisPooled("redis://:foobared@localhost:6380/2")) {
      assertEquals("bar", pool.get("foo"));
    }
  }

  @Test
  public void startWithUrl() throws URISyntaxException {
    try (Jedis j = new Jedis("localhost", 6380)) {
      j.auth("foobared");
      j.select(2);
      j.set("foo", "bar");
    }

    try (JedisPooled pool = new JedisPooled(new URI("redis://:foobared@localhost:6380/2"))) {
      assertEquals("bar", pool.get("foo"));
    }
  }

  @Test(expected = Exception.class)
  public void shouldThrowExceptionForInvalidURI() throws URISyntaxException {
    new JedisPooled(new URI("localhost:6380")).close();
  }

  @Test
  public void allowUrlWithNoDBAndNoPassword() throws URISyntaxException {
    new JedisPooled("redis://localhost:6380").close();
    new JedisPooled(new URI("redis://localhost:6380")).close();
  }

  @Test
  public void customClientName() {
    try (JedisPooled pool = new JedisPooled(hnp, DefaultJedisClientConfig.builder().clientName("my_shiny_client_name").build());
        Connection jedis = pool.getPool().getResource()) {
      assertEquals("my_shiny_client_name", new Jedis(jedis).clientGetname());
    }
  }

  @Test
  public void getNumActiveWhenPoolIsClosed() {
    JedisPooled pool = new JedisPooled(hnp);

    try (Connection j = pool.getPool().getResource()) {
      j.ping();
    }

    pool.close();
    assertEquals(0, pool.getPool().getNumActive());
  }

  @Test
  public void getNumActiveReturnsTheCorrectNumber() {
    try (JedisPooled pool = new JedisPooled(new ConnectionPoolConfig(), hnp.getHost(), hnp.getPort(), 2000)) {

      Connection jedis = pool.getPool().getResource();
      assertEquals(1, pool.getPool().getNumActive());

      Connection jedis2 = pool.getPool().getResource();
      assertEquals(2, pool.getPool().getNumActive());

      jedis.close();
      assertEquals(1, pool.getPool().getNumActive());

      jedis2.close();
      assertEquals(0, pool.getPool().getNumActive());
    }
  }

  @Test
  public void closeResourceTwice() {
    try (JedisPooled pool = new JedisPooled(new ConnectionPoolConfig(), hnp.getHost(), hnp.getPort(), 2000)) {
      Connection j = pool.getPool().getResource();
      j.ping();
      j.close();
      j.close();
    }
  }

  @Test
  public void closeBrokenResourceTwice() {
    try (JedisPooled pool = new JedisPooled(new ConnectionPoolConfig(), hnp.getHost(), hnp.getPort(), 2000)) {
      Connection j = pool.getPool().getResource();
      try {
        // make connection broken
        j.getOne();
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
  public void testResetValidPassword() {
    ConnectionFactory factory = new ConnectionFactory(HostAndPorts.getRedisServers().get(0),
        DefaultJedisClientConfig.builder().password("bad password")
            .clientName("my_shiny_client_name").build());

    try (JedisPooled pool = new JedisPooled(new ConnectionPoolConfig(), factory)) {
      try {
        pool.get("foo");
        fail("Should not get resource from pool");
      } catch (JedisException e) { }
      assertEquals(0, pool.getPool().getNumActive());

      factory.setPassword("foobared");
      pool.set("foo", "bar");
      assertEquals("bar", pool.get("foo"));
    }
  }
}
