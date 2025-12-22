package redis.clients.jedis;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.SentineledConnectionProvider;
import redis.clients.jedis.util.ReflectionTestUtil;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @see JedisSentinelPoolTest
 */
@Tag("integration")
public class SentineledConnectionProviderTest {

  private static final String MASTER_NAME = "mymaster";

  protected static final HostAndPort sentinel1 = HostAndPorts.getRedisEndpoint("sentinel-standalone2-1").getHostAndPort();
  protected static final HostAndPort sentinel2 = HostAndPorts.getRedisEndpoint("sentinel-standalone2-3").getHostAndPort();

  private static final EndpointConfig primary = HostAndPorts.getRedisEndpoint("standalone2-primary");

  protected Set<HostAndPort> sentinels = new HashSet<>();

  @BeforeEach
  public void setUp() throws Exception {
    sentinels.clear();

    sentinels.add(sentinel1);
    sentinels.add(sentinel2);
  }

  @Test
  public void repeatedSentinelPoolInitialization() {
    for (int i = 0; i < 20; ++i) {

      try (SentineledConnectionProvider provider = new SentineledConnectionProvider(MASTER_NAME,
          DefaultJedisClientConfig.builder().timeoutMillis(1000).password("foobared").database(2).build(),
          sentinels, DefaultJedisClientConfig.builder().build())) {

        provider.getConnection().close();
      }
    }
  }

  /**
   * Ensure that getConnectionMap() does not cause connection leak. (#4323)
   */
  @Test
  @Timeout( value = 1)
  public void getConnectionMapDoesNotCauseConnectionLeak() {

    ConnectionPoolConfig config = new ConnectionPoolConfig();
    config.setMaxTotal(1);

    try (SentineledConnectionProvider sut = new SentineledConnectionProvider(MASTER_NAME,
            primary.getClientConfigBuilder().build(), config, sentinels,
            DefaultJedisClientConfig.builder().build())) {

      HostAndPort resolvedPrimary = sut.getCurrentMaster();
      ConnectionPool pool = ReflectionTestUtil.getField(sut,"pool");
      assertThat(pool.getNumActive(), equalTo(0));

      Map<?, ?> cm = sut.getConnectionMap();

      // exactly one entry for current primary
      // and no active connections
      assertThat(cm.size(), equalTo(1));
      assertThat(cm, hasKey(resolvedPrimary));
      assertThat(pool.getNumActive(), equalTo(0));
      // primary did not change
      assertThat(ReflectionTestUtil.getField(sut,"pool"), sameInstance(pool));
    }
  }

  /**
   * Ensure that getPrimaryNodesConnectionMap() does not cause connection leak. (#4323)
   */
  @Test
  @Timeout( value = 1)
  public void getPrimaryNodesConnectionMapDoesNotCauseConnectionLeak() {

    ConnectionPoolConfig config = new ConnectionPoolConfig();
    config.setMaxTotal(1);

    try (SentineledConnectionProvider sut = new SentineledConnectionProvider(MASTER_NAME,
            primary.getClientConfigBuilder().build(), config, sentinels,
            DefaultJedisClientConfig.builder().build())) {

      HostAndPort resolvedPrimary = sut.getCurrentMaster();
      ConnectionPool pool = ReflectionTestUtil.getField(sut,"pool");
      assertThat(pool.getNumActive(), equalTo(0));


      Map<?, ?> cm = sut.getPrimaryNodesConnectionMap();

      // exactly one entry for current primary
      // and no active connections
      assertThat(cm.size(), equalTo(1));
      assertThat(cm, hasKey(resolvedPrimary));
      assertThat(pool.getNumActive(), equalTo(0));
      // primary did not change
      assertThat(ReflectionTestUtil.getField(sut,"pool"), sameInstance(pool));
    }

  }

  @Test
  public void initializeWithNotAvailableSentinelsShouldThrowException() {
    Set<HostAndPort> wrongSentinels = new HashSet<>();
    wrongSentinels.add(new HostAndPort("localhost", 65432));
    wrongSentinels.add(new HostAndPort("localhost", 65431));
    assertThrows(JedisConnectionException.class, () -> {
      try (SentineledConnectionProvider provider = new SentineledConnectionProvider(MASTER_NAME,
          DefaultJedisClientConfig.builder().build(), wrongSentinels, DefaultJedisClientConfig.builder().build())) {
      }
    });
  }

  @Test
  public void initializeWithNotMonitoredMasterNameShouldThrowException() {
    final String wrongMasterName = "wrongMasterName";
    assertThrows(JedisException.class, () -> {
      try (SentineledConnectionProvider provider = new SentineledConnectionProvider(wrongMasterName,
          DefaultJedisClientConfig.builder().build(), sentinels, DefaultJedisClientConfig.builder().build())) {
      }
    });
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();

    try (RedisSentinelClient jedis = RedisSentinelClient
        .builder().masterName(MASTER_NAME).clientConfig(DefaultJedisClientConfig.builder()
            .timeoutMillis(1000).password("foobared").database(2).build())
        .poolConfig(config).sentinels(sentinels).build()) {
      assertSame(SentineledConnectionProvider.class, jedis.provider.getClass());
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
    }
  }

  @Test
  public void checkResourceIsCloseable() {
    GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);

    try (RedisSentinelClient jedis = RedisSentinelClient
        .builder().masterName(MASTER_NAME).clientConfig(DefaultJedisClientConfig.builder()
            .timeoutMillis(1000).password("foobared").database(2).build())
        .poolConfig(config).sentinels(sentinels).build()) {

      Connection conn = jedis.provider.getConnection();
      try {
        conn.ping();
      } finally {
        conn.close();
      }

      Connection conn2 = jedis.provider.getConnection();
      try {
        assertEquals(conn, conn2);
      } finally {
        conn2.close();
      }
    }
  }

  @Test
  public void testResetInvalidPassword() {
    DefaultRedisCredentialsProvider credentialsProvider
        = new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(null, "foobared"));

    try (RedisSentinelClient jedis = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .clientConfig(DefaultJedisClientConfig.builder().timeoutMillis(2000)
            .credentialsProvider(credentialsProvider).database(2).clientName("my_shiny_client_name")
            .build())
        .sentinels(sentinels).build()) {

      jedis.set("foo", "bar");

      Connection conn1_ref;
      try (Connection conn1_1 = jedis.provider.getConnection()) {
        conn1_ref = conn1_1;
        assertEquals("bar", new Jedis(conn1_1).get("foo"));
      }

      credentialsProvider.setCredentials(new DefaultRedisCredentials(null, "wrong password"));

      try (Connection conn1_2 = jedis.provider.getConnection()) {
        assertSame(conn1_ref, conn1_2);

        try (Connection conn2 = jedis.provider.getConnection()) {
          fail("Should not get resource from pool");
        } catch (JedisException e) { }
      }
    }
  }

  @Test
  public void testResetValidPassword() {
    DefaultRedisCredentialsProvider credentialsProvider
        = new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(null, "wrong password"));

    try (RedisSentinelClient jedis = RedisSentinelClient.builder().masterName(MASTER_NAME)
        .clientConfig(DefaultJedisClientConfig.builder().timeoutMillis(2000)
            .credentialsProvider(credentialsProvider).database(2).clientName("my_shiny_client_name")
            .build())
        .sentinels(sentinels).build()) {

      try (Connection conn1 = jedis.provider.getConnection()) {
        fail("Should not get resource from pool");
      } catch (JedisException e) { }

      credentialsProvider.setCredentials(new DefaultRedisCredentials(null, "foobared"));

      try (Connection conn2 = jedis.provider.getConnection()) {
        new Jedis(conn2).set("foo", "bar");
        assertEquals("bar", jedis.get("foo"));
      }
    }
  }
}
