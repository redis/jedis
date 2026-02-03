package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.redis.test.annotations.SinceRedisVersion;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.ResourceLock;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.RedisVersionCondition;

/**
 * This test class is mostly a copy of {@link JedisSentinelPoolTest}.
 * <p>
 * This tests are only executed when the server/cluster is Redis 6 or more.
 */
@SinceRedisVersion("6.0.0")
@Tag("integration")
@ResourceLock(value = Endpoints.SENTINEL_STANDALONE0)
@ResourceLock(value = Endpoints.STANDALONE0)
public class ACLJedisSentinelPoolTest {

  private static final String MASTER_NAME = "aclmaster";

  protected static HostAndPort sentinel1;

  protected Set<HostAndPort> sentinels = new HashSet<>();

  @RegisterExtension
  public static RedisVersionCondition versionCondition = new RedisVersionCondition(
      () -> Endpoints.getRedisEndpoint(Endpoints.STANDALONE0));

  @BeforeAll
  public static void prepareEndpoint() {
    sentinel1 = Endpoints.getRedisEndpoint(Endpoints.SENTINEL_STANDALONE0).getHostAndPort();
  }

  @BeforeEach
  public void setUp() throws Exception {
    sentinels.clear();
    sentinels.add(sentinel1);
  }

  private static Set<String> toStrings(Set<HostAndPort> hostAndPorts) {
    return hostAndPorts.stream().map(HostAndPort::toString).collect(Collectors.toSet());
  }

  @Test
  public void repeatedSentinelPoolInitialization() {

    for (int i = 0; i < 20; ++i) {
      GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();

      JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, toStrings(sentinels), config, 1000, 1000,
          "acljedis", "fizzbuzz", 2, null, 1000, 1000, "sentinel", "foobared", null);
      pool.getResource().close();
      pool.destroy();
    }
  }

  @Test
  public void repeatedSentinelPoolInitializationWithConfig() {

    for (int i = 0; i < 20; ++i) {

      GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();

      JedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
          .connectionTimeoutMillis(1000).socketTimeoutMillis(1000).database(2)
          .user("acljedis").password("fizzbuzz").build();

      JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
          .connectionTimeoutMillis(1000).socketTimeoutMillis(1000)
          .user("sentinel").password("foobared").build();

      JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, poolConfig, masterConfig, sentinelConfig);
      pool.getResource().close();
      pool.destroy();
    }
  }

  @Test
  public void initializeWithNotAvailableSentinelsShouldThrowException() {

    GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();

    JedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(1000).socketTimeoutMillis(1000).database(2).user("acljedis")
        .password("fizzbuzz").build();

    JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(1000).socketTimeoutMillis(1000).user("default")
        .password("wrongpassword").build();
    assertThrows(JedisConnectionException.class, () -> {
      try (JedisSentinelPool ignored = new JedisSentinelPool(MASTER_NAME, sentinels, poolConfig,
          masterConfig, sentinelConfig)) {
        // do nothing
      }
    });
  }

  @Test
  public void initializeWithNotMonitoredMasterNameShouldThrowException() {

    GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();

    JedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(1000).socketTimeoutMillis(1000).database(2).user("acljedis")
        .password("fizzbuzz").build();

    JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
        .connectionTimeoutMillis(1000).socketTimeoutMillis(1000).user("sentinel")
        .password("foobared").build();

    assertThrows(JedisException.class, () -> {
      try (JedisSentinelPool ignored = new JedisSentinelPool("wrongMasterName", sentinels, poolConfig,
          masterConfig, sentinelConfig)) {
        // do nothing
      }
    });
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, toStrings(sentinels), config,
        1000, 1000, "acljedis", "fizzbuzz", 2, null, 1000, 1000, "sentinel", "foobared", null);
    try (Jedis jedis = pool.getResource()) {
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
    }
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void returnResourceShouldResetState() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, toStrings(sentinels), config,
        1000, 1000, "acljedis", "fizzbuzz", 2, null, 1000, 1000, "sentinel", "foobared", null)) {
      Jedis jedis;
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
    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, toStrings(sentinels), config,
        1000, 1000, "acljedis", "fizzbuzz", 2, null, 1000, 1000, "sentinel", "foobared", null)) {

      Jedis jedis;
      try (Jedis jedis1 = pool.getResource()) {
        jedis = jedis1;
        jedis1.set("hello", "jedis");
      }

      try (Jedis jedis2 = pool.getResource()) {
        assertEquals(jedis, jedis2);
      }
    }
  }

  @Test
  public void customClientName() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, toStrings(sentinels), config,
        1000, 1000, "acljedis", "fizzbuzz", 0, "my_shiny_master_client",
        1000, 1000, "sentinel", "foobared", "my_shiny_sentinel_client");

    try (Jedis jedis = pool.getResource()) {
      assertEquals("my_shiny_master_client", jedis.clientGetname());
    } finally {
      pool.close();
    }

    assertTrue(pool.isClosed());
  }

}
