package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

@Tag("integration")
@ResourceLock(value = Endpoints.SENTINEL_STANDALONE2_1)
@ResourceLock(value = Endpoints.SENTINEL_STANDALONE2_3)
public class JedisSentinelPoolTest {

  private static final String MASTER_NAME = "mymaster";

  protected static HostAndPort sentinel1;
  protected static HostAndPort sentinel2;

  protected final Set<String> sentinels = new HashSet<>();

  @BeforeAll
  public static void prepare() {
    sentinel1 = Endpoints.getRedisEndpoint(Endpoints.SENTINEL_STANDALONE2_1).getHostAndPort();
    sentinel2 = Endpoints.getRedisEndpoint(Endpoints.SENTINEL_STANDALONE2_3).getHostAndPort();
  }

  @BeforeEach
  public void setUp() throws Exception {
    sentinels.clear();

    sentinels.add(sentinel1.toString());
    sentinels.add(sentinel2.toString());
  }

  @Test
  public void repeatedSentinelPoolInitialization() {

    for (int i = 0; i < 20; ++i) {
      GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();

      JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
          "foobared", 2);
      pool.getResource().close();
      pool.destroy();
    }
  }

  @Test
  public void initializeWithNotAvailableSentinelsShouldThrowException() {
    Set<String> wrongSentinels = new HashSet<String>();
    wrongSentinels.add(new HostAndPort("localhost", 65432).toString());
    wrongSentinels.add(new HostAndPort("localhost", 65431).toString());

    assertThrows(JedisConnectionException.class,
        () -> new JedisSentinelPool(MASTER_NAME, wrongSentinels).close());
  }

  @Test
  public void initializeWithNotMonitoredMasterNameShouldThrowException() {
    final String wrongMasterName = "wrongMasterName";
    assertThrows(JedisException.class, ()-> new JedisSentinelPool(wrongMasterName, sentinels).close());
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "foobared", 2);
    Jedis jedis = pool.getResource();
    jedis.auth("foobared");
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void returnResourceShouldResetState() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "foobared", 2)) {

      Jedis jedis = null;
      try (Jedis jedis1 = pool.getResource()) {
        jedis = jedis1;
        jedis1.set("hello", "jedis");
        Transaction t = jedis1.multi();
        t.set("hello", "world");
      }

      try (Jedis jedis2 = pool.getResource()) {
        assertSame(jedis, jedis2);
        assertEquals("jedis", jedis2.get("hello"));
      }
    }
  }

  @Test
  public void checkResourceIsCloseable() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "foobared", 2);

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
  public void customClientName() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "foobared", 0, "my_shiny_client_name");

    Jedis jedis = pool.getResource();

    try {
      assertEquals("my_shiny_client_name", jedis.clientGetname());
    } finally {
      jedis.close();
      pool.destroy();
    }

    assertTrue(pool.isClosed());
  }
}
