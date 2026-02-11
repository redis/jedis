package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

@Tag("integration")
public class RedisClientTest {

  private static EndpointConfig endpointStandalone7;
  private static EndpointConfig endpointStandalone1; // password protected

  @BeforeAll
  public static void prepareEndpoints() {
    endpointStandalone7 = Endpoints.getRedisEndpoint("standalone7-with-lfu-policy");
    endpointStandalone1 = Endpoints.getRedisEndpoint("standalone1");
  }

  @Test
  public void checkCloseableConnections() {
    RedisClient pool = RedisClient.builder()
        .hostAndPort(endpointStandalone7.getHost(), endpointStandalone7.getPort())
        .clientConfig(DefaultJedisClientConfig.builder().timeoutMillis(2000).build()).build();
    pool.set("foo", "bar");
    assertEquals("bar", pool.get("foo"));
    pool.close();
    assertTrue(pool.getPool().isClosed());
  }

  @Test
  public void checkResourceWithConfig() {
    try (RedisClient pool = RedisClient.builder().hostAndPort(endpointStandalone7.getHostAndPort())
        .clientConfig(DefaultJedisClientConfig.builder().socketTimeoutMillis(5000).build())
        .build()) {

      try (Connection jedis = pool.getPool().getResource()) {
        assertTrue(jedis.ping());
        assertEquals(5000, jedis.getSoTimeout());
      }
    }
  }

  @Test
  public void checkPoolOverflow() {
    GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    try (
        RedisClient pool = RedisClient.builder().hostAndPort(endpointStandalone7.getHostAndPort())
            .poolConfig(config).build();
        Connection jedis = pool.getPool().getResource()) {
      assertThrows(JedisException.class, () -> pool.getPool().getResource());
    }
  }

  @Test
  @SuppressWarnings("deprecation")
  public void startWithUrlString() {
    try (Jedis j = new Jedis(endpointStandalone1.getHostAndPort())) {
      j.auth(endpointStandalone1.getPassword());
      j.select(2);
      j.set("foo", "bar");
    }

    try (RedisClient pool = RedisClient.builder()
        .fromURI(endpointStandalone1.getURIBuilder()
            .credentials("", endpointStandalone1.getPassword()).path("/2").build().toString())
        .build()) {
      assertEquals("bar", pool.get("foo"));
    }
  }

  @Test
  @SuppressWarnings("deprecation")
  public void startWithUrl() {
    try (Jedis j = new Jedis(endpointStandalone1.getHostAndPort())) {
      j.auth(endpointStandalone1.getPassword());
      j.select(2);
      j.set("foo", "bar");
    }

    try (RedisClient pool = RedisClient.builder().fromURI(endpointStandalone1.getURIBuilder()
        .credentials("", endpointStandalone1.getPassword()).path("/2").build()).build()) {
      assertEquals("bar", pool.get("foo"));
    }
  }

  @Test
  @SuppressWarnings("deprecation")
  public void shouldThrowExceptionForInvalidURI() {
    assertThrows(Exception.class,
      () -> RedisClient.builder().fromURI(new URI("localhost:6380")).build());
  }

  @Test
  @SuppressWarnings("deprecation")
  public void allowUrlWithNoDBAndNoPassword() {
    RedisClient.builder().fromURI(endpointStandalone1.getURI().toString()).build().close();
    RedisClient.builder().fromURI(endpointStandalone1.getURI()).build().close();
  }

  @Test
  public void customClientName() {
    try (
        RedisClient pool = RedisClient.builder().hostAndPort(endpointStandalone7.getHostAndPort())
            .clientConfig(
              DefaultJedisClientConfig.builder().clientName("my_shiny_client_name").build())
            .build();
        Connection jedis = pool.getPool().getResource()) {
      assertEquals("my_shiny_client_name", new Jedis(jedis).clientGetname());
    }
  }

  @Test
  public void invalidClientName() {
    try (
        RedisClient pool = RedisClient.builder().hostAndPort(endpointStandalone7.getHostAndPort())
            .clientConfig(
              DefaultJedisClientConfig.builder().clientName("invalid client name").build())
            .build();
        Connection jedis = pool.getPool().getResource()) {
    } catch (Exception e) {
      if (!e.getMessage().startsWith("client info cannot contain space")) {
        fail("invalid client name test fail");
      }
    }
  }

  @Test
  public void getNumActiveWhenPoolIsClosed() {
    RedisClient pool = RedisClient.builder().hostAndPort(endpointStandalone7.getHostAndPort())
        .build();

    try (Connection j = pool.getPool().getResource()) {
      j.ping();
    }

    pool.close();
    assertEquals(0, pool.getPool().getNumActive());
  }

  @Test
  public void getNumActiveReturnsTheCorrectNumber() {
    try (RedisClient pool = RedisClient.builder()
        .hostAndPort(endpointStandalone7.getHost(), endpointStandalone7.getPort())
        .clientConfig(DefaultJedisClientConfig.builder().timeoutMillis(2000).build())
        .poolConfig(new ConnectionPoolConfig()).build()) {

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
    try (RedisClient pool = RedisClient.builder()
        .hostAndPort(endpointStandalone7.getHost(), endpointStandalone7.getPort())
        .clientConfig(DefaultJedisClientConfig.builder().timeoutMillis(2000).build())
        .poolConfig(new ConnectionPoolConfig()).build()) {
      Connection j = pool.getPool().getResource();
      j.ping();
      j.close();
      j.close();
    }
  }

  @Test
  public void closeBrokenResourceTwice() {
    try (RedisClient pool = RedisClient.builder()
        .hostAndPort(endpointStandalone7.getHost(), endpointStandalone7.getPort())
        .clientConfig(DefaultJedisClientConfig.builder().timeoutMillis(2000).build())
        .poolConfig(new ConnectionPoolConfig()).build()) {
      Connection j = pool.getPool().getResource();
      try {
        // make connection broken
        j.getOne();
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
  public void testResetValidCredentials() {
    DefaultRedisCredentialsProvider credentialsProvider =
        new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(null, "bad password"));

    try (RedisClient pool = RedisClient.builder().hostAndPort(endpointStandalone1.getHostAndPort())
        .clientConfig(
          DefaultJedisClientConfig.builder().credentialsProvider(credentialsProvider).build())
        .build()) {
      try {
        pool.get("foo");
        fail("Should not get resource from pool");
      } catch (JedisException e) { }
      assertEquals(0, pool.getPool().getNumActive());

      credentialsProvider.setCredentials(new DefaultRedisCredentials(null, endpointStandalone1.getPassword()));
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
            return endpointStandalone1.getPassword().toCharArray();
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
    try (RedisClient pool = RedisClient.builder().hostAndPort(endpointStandalone1.getHostAndPort())
        .clientConfig(
          DefaultJedisClientConfig.builder().credentialsProvider(credentialsProvider).build())
        .poolConfig(poolConfig).build()) {
      try {
        pool.get("foo");
        fail("Should not get resource from pool");
      } catch (JedisException e) {
        //ignore
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
