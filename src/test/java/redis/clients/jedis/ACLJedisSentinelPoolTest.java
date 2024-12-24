package redis.clients.jedis;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.RedisVersionUtil;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * This test class is mostly a copy of {@link JedisSentinelPoolTest}.
 * <p>
 * This tests are only executed when the server/cluster is Redis 6 or more.
 */
public class ACLJedisSentinelPoolTest {

  private static final String MASTER_NAME = "aclmaster";

  protected static HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(4);

  protected Set<HostAndPort> sentinels = new HashSet<>();

  @BeforeClass
  public static void prepare() throws Exception {
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("standalone2-primary");
    org.junit.Assume.assumeTrue("Not running ACL test on this version of Redis",
                                RedisVersionUtil.checkRedisMajorVersionNumber(6, endpoint));
  }

  private static Set<String> toStrings(Set<HostAndPort> hostAndPorts) {
    return hostAndPorts.stream().map(hap -> hap.toString()).collect(Collectors.toSet());
  }

  @Before
  public void setUp() throws Exception {
    sentinels.clear();
    sentinels.add(sentinel1);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void repeatedSentinelPoolInitialization() {

    for (int i = 0; i < 20; ++i) {
      var config = JedisPoolConfig.builder();

      JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, toStrings(sentinels), config.build(), 1000, 1000,
                                                     "acljedis", "fizzbuzz", 2, null, 1000, 1000, "sentinel", "foobared", null);
      pool.getResource().close();
      pool.destroy();
    }
  }

  @Test
  public void repeatedSentinelPoolInitializationWithConfig() {

    for (int i = 0; i < 20; ++i) {

      var poolConfig = JedisPoolConfig.builder();

      JedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
                                                               .connectionTimeoutMillis(1000).socketTimeoutMillis(1000).database(2)
                                                               .user("acljedis").password("fizzbuzz").build();

      JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
                                                                 .connectionTimeoutMillis(1000).socketTimeoutMillis(1000)
                                                                 .user("sentinel").password("foobared").build();

      JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, poolConfig.build(), masterConfig, sentinelConfig);
      pool.getResource().close();
      pool.destroy();
    }
  }

  @Test(expected = JedisConnectionException.class)
  public void initializeWithNotAvailableSentinelsShouldThrowException() {

    var poolConfig = JedisPoolConfig.builder();

    JedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
                                                             .connectionTimeoutMillis(1000).socketTimeoutMillis(1000).database(2)
                                                             .user("acljedis").password("fizzbuzz").build();

    JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
                                                               .connectionTimeoutMillis(1000).socketTimeoutMillis(1000)
                                                               .user("default").password("foobared").build();

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, poolConfig.build(), masterConfig, sentinelConfig);
    pool.getResource().close();
    pool.destroy();
  }

  @Test(expected = JedisException.class)
  public void initializeWithNotMonitoredMasterNameShouldThrowException() {

    var poolConfig = JedisPoolConfig.builder();

    JedisClientConfig masterConfig = DefaultJedisClientConfig.builder()
                                                             .connectionTimeoutMillis(1000).socketTimeoutMillis(1000).database(2)
                                                             .user("acljedis").password("fizzbuzz").build();

    JedisClientConfig sentinelConfig = DefaultJedisClientConfig.builder()
                                                               .connectionTimeoutMillis(1000).socketTimeoutMillis(1000)
                                                               .user("sentinel").password("foobared").build();

    JedisSentinelPool pool = new JedisSentinelPool("wrongMasterName", sentinels, poolConfig.build(), masterConfig, sentinelConfig);
    pool.getResource().close();
    pool.destroy();
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    var config = JedisPoolConfig.builder();

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, toStrings(sentinels), config.build(),
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
    var config = JedisPoolConfig.builder();
    config.maxPoolSize(1);
    config.waitingForObjectTimeout(Duration.ZERO);

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, toStrings(sentinels), config.build(),
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
    var config = JedisPoolConfig.builder();
    config.maxPoolSize(1);
    config.waitingForObjectTimeout(Duration.ZERO);
    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, toStrings(sentinels), config.build(),
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
    var config = JedisPoolConfig.builder();
    config.maxPoolSize(1);
    config.waitingForObjectTimeout(Duration.ZERO);
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, toStrings(sentinels), config.build(),
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
