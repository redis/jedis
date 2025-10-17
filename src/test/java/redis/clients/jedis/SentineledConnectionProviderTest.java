package redis.clients.jedis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.junit.jupiter.api.Tag;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.SentineledConnectionProvider;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @see JedisSentinelPoolTest
 */
@Tag("integration")
@RunWith(PowerMockRunner.class)
@PrepareForTest({SentineledConnectionProvider.class})
public class SentineledConnectionProviderTest {

  private static final String MASTER_NAME = "mymaster";

  protected static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(1);
  protected static final HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(3);

  protected Set<HostAndPort> sentinels = new HashSet<>();

  protected String password = "foobared";

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

    try (JedisSentineled jedis = JedisSentineled
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

    try (JedisSentineled jedis = JedisSentineled
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

    try (JedisSentineled jedis = JedisSentineled.builder().masterName(MASTER_NAME)
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

    try (JedisSentineled jedis = JedisSentineled.builder().masterName(MASTER_NAME)
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

  @Test
  public void testReadWriteSeparation() throws InterruptedException {
    DefaultRedisCredentialsProvider credentialsProvider
            = new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(null, password));

    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME, DefaultJedisClientConfig.builder()
            .timeoutMillis(2000).credentialsProvider(credentialsProvider).database(2)
            .clientName("my_shiny_client_name").build(), new ConnectionPoolConfig(),
            sentinels, DefaultJedisClientConfig.builder().build())) {

      jedis.set("foo", "bar");
      Thread.sleep(1000);
      assertEquals("bar", jedis.get("foo"));
    }
  }

  @Test
  public void testReadFromREPLICAAndNoSlave() throws InterruptedException {
    DefaultRedisCredentialsProvider credentialsProvider
            = new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(null, password));

    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME, DefaultJedisClientConfig.builder()
            .timeoutMillis(2000).credentialsProvider(credentialsProvider).database(2)
            .clientName("my_shiny_client_name").build(), new ConnectionPoolConfig(),
            sentinels, DefaultJedisClientConfig.builder().build(), ReadFrom.REPLICA)) {

      Thread.sleep(1000);
      Whitebox.setInternalState(jedis.provider, "slavePools", new ArrayList<>());
      jedis.set("foo", "bar");
      Thread.sleep(1000);
      assertThrows(JedisException.class, () -> jedis.get("foo"));
    }
  }

  @Test
  public void testFallbackTOMasterWhenNOSlave() throws InterruptedException {
    DefaultRedisCredentialsProvider credentialsProvider
            = new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(null, password));

    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME, DefaultJedisClientConfig.builder()
            .timeoutMillis(2000).credentialsProvider(credentialsProvider).database(2)
            .clientName("my_shiny_client_name").build(), new ConnectionPoolConfig(),
            sentinels, DefaultJedisClientConfig.builder().build(), ReadFrom.REPLICA_PREFERRED)) {

      Thread.sleep(1000);
      Whitebox.setInternalState(jedis.provider, "slavePools", new ArrayList<>());
      jedis.set("foo", "bar");
      Thread.sleep(1000);
      assertDoesNotThrow(() -> jedis.get("foo"));
    }
  }

  @Test
  public void testAllWriteCommandsWhenNOSlave() throws InterruptedException {
    DefaultRedisCredentialsProvider credentialsProvider
            = new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(null, password));

    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME, DefaultJedisClientConfig.builder()
            .timeoutMillis(2000).credentialsProvider(credentialsProvider).database(2)
            .clientName("my_shiny_client_name").build(), new ConnectionPoolConfig(),
            sentinels, DefaultJedisClientConfig.builder().build(), ReadFrom.REPLICA_PREFERRED, command -> false)) {

      Thread.sleep(1000);
      Whitebox.setInternalState(jedis.provider, "slavePools", new ArrayList<>());
      jedis.set("foo", "bar");
      Thread.sleep(1000);
      assertDoesNotThrow(() -> jedis.get("foo"));
    }
  }

  @Test
  public void testCreateJedisSentineledWithBuilder() throws InterruptedException {
    DefaultRedisCredentialsProvider credentialsProvider
            = new DefaultRedisCredentialsProvider(new DefaultRedisCredentials(null, password));

    JedisSentineled jedis = JedisSentineled.builder().
            masterName(MASTER_NAME).
            clientConfig(DefaultJedisClientConfig.builder()
                    .timeoutMillis(2000).credentialsProvider(credentialsProvider).database(2)
                    .clientName("my_shiny_client_name").build()).readForm(ReadFrom.REPLICA_PREFERRED).readOnlyPredicate(command -> false).build();
    Thread.sleep(1000);
    Whitebox.setInternalState(jedis.provider, "slavePools", new ArrayList<>());
    jedis.set("foo", "bar");
    Thread.sleep(1000);
    assertDoesNotThrow(() -> jedis.get("foo"));

  }
}
