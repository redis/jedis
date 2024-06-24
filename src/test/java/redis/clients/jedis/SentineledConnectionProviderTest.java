package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.SentineledConnectionProvider;

/**
 * @see JedisSentinelPoolTest
 */
public class SentineledConnectionProviderTest {

  private static final String MASTER_NAME = "mymaster";

  protected static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(1);
  protected static final HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(3);

  protected Set<HostAndPort> sentinels = new HashSet<>();

  @Before
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

  @Test(expected = JedisConnectionException.class)
  public void initializeWithNotAvailableSentinelsShouldThrowException() {
    Set<HostAndPort> wrongSentinels = new HashSet<>();
    wrongSentinels.add(new HostAndPort("localhost", 65432));
    wrongSentinels.add(new HostAndPort("localhost", 65431));

    try (SentineledConnectionProvider provider = new SentineledConnectionProvider(MASTER_NAME,
        DefaultJedisClientConfig.builder().build(), wrongSentinels, DefaultJedisClientConfig.builder().build())) {
    }
  }

  @Test(expected = JedisException.class)
  public void initializeWithNotMonitoredMasterNameShouldThrowException() {
    final String wrongMasterName = "wrongMasterName";
    try (SentineledConnectionProvider provider = new SentineledConnectionProvider(wrongMasterName,
        DefaultJedisClientConfig.builder().build(), sentinels, DefaultJedisClientConfig.builder().build())) {
    }
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();

    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME,
        DefaultJedisClientConfig.builder().timeoutMillis(1000).password("foobared").database(2).build(),
        config, sentinels, DefaultJedisClientConfig.builder().build())) {
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

    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME,
        DefaultJedisClientConfig.builder().timeoutMillis(1000).password("foobared").database(2).build(),
        config, sentinels, DefaultJedisClientConfig.builder().build())) {

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

    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME, DefaultJedisClientConfig.builder()
        .timeoutMillis(2000).credentialsProvider(credentialsProvider).database(2)
        .clientName("my_shiny_client_name").build(), new ConnectionPoolConfig(),
        sentinels, DefaultJedisClientConfig.builder().build())) {

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

    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME, DefaultJedisClientConfig.builder()
        .timeoutMillis(2000).credentialsProvider(credentialsProvider).database(2)
        .clientName("my_shiny_client_name").build(), new ConnectionPoolConfig(),
        sentinels, DefaultJedisClientConfig.builder().build())) {

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
