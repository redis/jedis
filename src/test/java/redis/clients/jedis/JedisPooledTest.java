package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public class JedisPooledTest {

  private static final HostAndPort hnp = HostAndPorts.getRedisServers().get(7);
  private static final HostAndPort pwp = HostAndPorts.getRedisServers().get(1); // password protected

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
    try (JedisPooled pool = new JedisPooled(hnp, DefaultJedisClientConfig.builder()
        .clientName("my_shiny_client_name").build());
        Connection jedis = pool.getPool().getResource()) {
      assertEquals("my_shiny_client_name", new Jedis(jedis).clientGetname());
    }
  }

  @Test
  public void invalidClientName() {
    try (JedisPooled pool = new JedisPooled(hnp, DefaultJedisClientConfig.builder()
        .clientName("invalid client name").build());
         Connection jedis = pool.getPool().getResource()) {
    } catch (Exception e) {
      if (!e.getMessage().startsWith("client info cannot contain space")) {
        Assert.fail("invalid client name test fail");
      }
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
  public void testResetValidCredentials() {
    DefaultRedisCredentialsProvider credentialsProvider = 
        new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(null, "bad password"));

    try (JedisPooled pool = new JedisPooled(pwp, DefaultJedisClientConfig.builder()
        .credentialsProvider(credentialsProvider).build())) {
      try {
        pool.get("foo");
        fail("Should not get resource from pool");
      } catch (JedisException e) { }
      assertEquals(0, pool.getPool().getNumActive());

      credentialsProvider.setCredentials(new DefaultRedisCredentials(null, "foobared"));
      assertThat(pool.get("foo"), anything());
    }
  }

  @Test
  public void testCredentialsProvider() {
    final AtomicInteger prepareCount = new AtomicInteger();
    final AtomicInteger cleanupCount = new AtomicInteger();
    final AtomicBoolean validPassword = new AtomicBoolean(false);

    RedisCredentialsProvider credentialsProvider = new RedisCredentialsProvider() {

      @Override
      public void prepare() {
        prepareCount.incrementAndGet();
      }

      @Override
      public RedisCredentials get() {
        if (!validPassword.get()) {
          return new RedisCredentials() {
            @Override
            public char[] getPassword() {
              return "invalidPass".toCharArray();
            }
          };
        }

        return new RedisCredentials() {
          @Override
          public String getUser() {
            return null;
          }

          @Override
          public char[] getPassword() {
            return "foobared".toCharArray();
          }
        };
      }

      @Override
      public void cleanUp() {
        cleanupCount.incrementAndGet();
      }
    };

    // TODO: do it without the help of pool config; from Connection constructor? (configurable) force ping?
    GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(1);
    poolConfig.setTestOnBorrow(true);
    try (JedisPooled pool = new JedisPooled(pwp, DefaultJedisClientConfig.builder()
        .credentialsProvider(credentialsProvider).build(), poolConfig)) {
      try {
        pool.get("foo");
        fail("Should not get resource from pool");
      } catch (JedisException e) {
      }
      assertEquals(0, pool.getPool().getNumActive() + pool.getPool().getNumIdle() + pool.getPool().getNumWaiters());
      assertThat(prepareCount.getAndSet(0), greaterThanOrEqualTo(1));
      assertThat(cleanupCount.getAndSet(0), greaterThanOrEqualTo(1));

      validPassword.set(true);
      assertThat(pool.get("foo"), anything());
      assertThat(prepareCount.get(), equalTo(1));
      assertThat(cleanupCount.get(), equalTo(1));
    }
  }
}
