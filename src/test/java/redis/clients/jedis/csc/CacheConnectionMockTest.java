package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.DefaultJedisSocketFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.PushConsumer;
import redis.clients.jedis.sch.AbstractMaintenanceEventHandlingTest;
import redis.clients.jedis.sch.MaintenanceEventTestSupport;
import redis.clients.jedis.sch.AbstractMaintenanceHandshakeTest;
import redis.clients.jedis.sch.AbstractRelaxedTimeoutBehaviorTest;
import redis.clients.jedis.util.server.RespResponse;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Unit tests for CacheConnection that don't require a real Redis server. Uses TcpMockServer to
 * simulate Redis protocol.
 * <p>
 * These tests verify CacheConnection-specific behavior (PushInvalidateConsumer registration).
 * MaintenanceEventConsumer registration is tested in ConnectionMockTest.
 * </p>
 */
public class CacheConnectionMockTest {

  private TcpMockServer mockServer;
  private Cache cache;

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new TcpMockServer();
    mockServer.start();
    cache = CacheFactory.getCache(CacheConfig.builder().build());
  }

  @AfterEach
  public void tearDown() throws IOException {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  @Nested
  class PushInvalidateConsumerTests {

    @Test
    public void pushInvalidateConsumerRegisteredWithConfigConstructor() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      CacheConnection conn = new CacheConnection(socketFactory, config, cache);

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);

      // Verify PushInvalidateConsumer is registered after the default pub/sub consumer
      assertThat(consumers, contains(MaintenanceEventTestSupport.isPubSubConsumer(),
        instanceOf(PushInvalidateConsumer.class)));
    }

    @Test
    public void pushInvalidateConsumerRegisteredWithBuilder() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      CacheConnection conn = (CacheConnection) CacheConnection.builder(cache)
          .socketFactory(socketFactory).clientConfig(config).build();

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);

      // Verify PushInvalidateConsumer is registered after the default pub/sub consumer
      assertThat(consumers, contains(MaintenanceEventTestSupport.isPubSubConsumer(),
        instanceOf(PushInvalidateConsumer.class)));
    }

    @Test
    public void arbitraryPushNotificationDoesNotBreakConnection() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      try (CacheConnection conn = new CacheConnection(socketFactory, config, cache)) {

        // Send arbitrary push notification (not "invalidate")
        mockServer.sendPushMessageToAll("ARBITRARY_PUSH", "arg1", "arg2");

        // Execute command after receiving arbitrary push notification
        // If push notification handling is broken, this will throw an exception
        assertDoesNotThrow(() -> conn.ping(),
          "PING after arbitrary push notification should not throw exception");
        assertTrue(conn.ping(), "PING should succeed");

        // Verify connection is still healthy
        assertFalse(conn.isBroken(), "Connection should not be broken");
        assertTrue(conn.isConnected(), "Connection should still be connected");
      }
    }

    @Test
    public void invalidatePushMessageInvalidatesCacheForRedisKeys() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      // Create a spy of the cache to verify method invocations
      Cache cacheSpy = spy(cache);

      try (CacheConnection conn = new CacheConnection(socketFactory, config, cacheSpy)) {

        // Send invalidate push notification with a single key
        // RESP3 format: >2\r\n$10\r\ninvalidate\r\n*1\r\n$8\r\ntestkey1\r\n
        // This represents: ["invalidate", ["testkey1"]]
        String invalidateMessage = ">2\r\n$10\r\ninvalidate\r\n*1\r\n$8\r\ntestkey1\r\n";

        mockServer.sendRawPushMessageToAll(invalidateMessage);

        // Execute a command to trigger processing of the invalidate push message
        assertDoesNotThrow(() -> conn.ping(),
          "PING after invalidate push should not throw exception");

        // Verify that cache.deleteByRedisKeys was invoked with the correct key
        verify(cacheSpy).deleteByRedisKeys(argThat(keys -> keys != null && keys.size() == 1
            && Arrays.equals(SafeEncoder.encode("testkey1"), (byte[]) keys.get(0))));
      }
    }

  }

  /**
   * Entries are evicted through their recorded Redis keys, so a command whose arguments carry no
   * recorded key (built with {@code add()} instead of {@code key()}, as the generic
   * {@code sendCommand}/{@code executeCommand} paths do) can never be invalidated by the server and
   * must not be cached.
   */
  @Nested
  class KeyTrackingTests {

    private final AtomicInteger serverGets = new AtomicInteger();

    @BeforeEach
    public void countGets() {
      mockServer.setCommandHandler((args, clientId) -> {
        if (Protocol.Command.GET.equals(args.getCommand())) {
          serverGets.incrementAndGet();
          return RespResponse.bulkString("v");
        }
        return null;
      });
    }

    @Test
    public void commandWithoutRecordedKeysIsNotCached() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(
          new HostAndPort("localhost", mockServer.getPort()), config);

      try (CacheConnection conn = new CacheConnection(socketFactory, config, cache)) {
        CommandObject<String> get = new CommandObject<>(
            new CommandArguments(Protocol.Command.GET).add("k"), BuilderFactory.STRING);

        assertEquals("v", conn.executeCommand(get));
        assertEquals("v", conn.executeCommand(get));

        assertEquals(2, serverGets.get(), "every call must reach the server");
        assertEquals(0, cache.getSize());
        assertEquals(2, cache.getStats().getNonCacheableCount());
      }
    }

    @Test
    public void commandWithRecordedKeyIsCached() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(
          new HostAndPort("localhost", mockServer.getPort()), config);

      try (CacheConnection conn = new CacheConnection(socketFactory, config, cache)) {
        CommandObject<String> get = new CommandObject<>(
            new CommandArguments(Protocol.Command.GET).key("k"), BuilderFactory.STRING);

        assertEquals("v", conn.executeCommand(get));
        assertEquals("v", conn.executeCommand(get));

        assertEquals(1, serverGets.get(), "second call must be served from the cache");
        assertEquals(1, cache.getSize());
      }
    }
  }

  /**
   * An entry keeps the protocol reply and rebuilds the value with the command's builder on every
   * read, so results need not be {@code Serializable} and callers never share an instance.
   */
  @Nested
  class ReplyRebuildTests {

    private final AtomicInteger serverGets = new AtomicInteger();

    @BeforeEach
    public void countGets() {
      mockServer.setCommandHandler((args, clientId) -> {
        if (Protocol.Command.GET.equals(args.getCommand())) {
          serverGets.incrementAndGet();
          return RespResponse.bulkString("v");
        }
        return null;
      });
    }

    @Test
    public void nonSerializableResultIsRebuiltOnEachHit() {
      try (CacheConnection conn = connect()) {
        CommandObject<Holder> get = new CommandObject<>(
            new CommandArguments(Protocol.Command.GET).key("k"), new Builder<Holder>() {
              @Override
              public Holder build(Object data) {
                return new Holder(SafeEncoder.encode((byte[]) data));
              }
            });

        Holder first = conn.executeCommand(get);
        Holder second = conn.executeCommand(get);

        assertEquals(1, serverGets.get(), "second call must be served from the cache");
        assertEquals("v", second.value);
        assertNotSame(first, second);
      }
    }

    @Test
    public void replyRejectedByBuilderIsNotCached() {
      AtomicInteger builds = new AtomicInteger();
      try (CacheConnection conn = connect()) {
        CommandObject<String> get = new CommandObject<>(
            new CommandArguments(Protocol.Command.GET).key("k"), new Builder<String>() {
              @Override
              public String build(Object data) {
                if (builds.incrementAndGet() == 1) {
                  throw new IllegalStateException("rejected");
                }
                return SafeEncoder.encode((byte[]) data);
              }
            });

        assertThrows(IllegalStateException.class, () -> conn.executeCommand(get));
        assertEquals(0, cache.getSize());

        assertEquals("v", conn.executeCommand(get));
        assertEquals(2, serverGets.get(), "a rejected reply must not be served from the cache");
      }
    }

    @Test
    public void mutatingBinaryResultDoesNotChangeCachedReply() {
      try (CacheConnection conn = connect()) {
        CommandObject<byte[]> get = new CommandObject<>(
            new CommandArguments(Protocol.Command.GET).key("k"), BuilderFactory.BINARY);

        conn.executeCommand(get)[0] = 'X'; // miss
        conn.executeCommand(get)[0] = 'Y'; // hit

        assertArrayEquals(SafeEncoder.encode("v"), conn.executeCommand(get));
        assertEquals(1, serverGets.get());
      }
    }

    private CacheConnection connect() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();
      return new CacheConnection(
          new DefaultJedisSocketFactory(new HostAndPort("localhost", mockServer.getPort()), config),
          config, cache);
    }
  }

  /**
   * Mirror of {@link redis.clients.jedis.ConnectionMockTest.MaintenanceEventHandling} for
   * {@link CacheConnection}: pooled CSC connections must register the maintenance consumer; direct
   * and builder-built CSC connections must not.
   */
  @Nested
  class MaintenanceEventHandling extends AbstractMaintenanceEventHandlingTest {

    @Override
    protected ConnectionPool createPool(HostAndPort hp, JedisClientConfig cfg,
        MaintenanceNotificationsConfig maint) {
      return new ConnectionPool(hp, cfg, cache, new ConnectionPoolConfig(), maint);
    }

    @Override
    protected Connection buildDirect(HostAndPort hostAndPort, JedisClientConfig config) {
      DefaultJedisSocketFactory sf = new DefaultJedisSocketFactory(hostAndPort, config);
      return new CacheConnection(sf, config, cache);
    }

    @Override
    protected Connection buildFromBuilder(DefaultJedisSocketFactory socketFactory,
        JedisClientConfig config) {
      return CacheConnection.builder(cache).socketFactory(socketFactory).clientConfig(config)
          .build();
    }
  }

  /**
   * Mirror of {@link redis.clients.jedis.ConnectionMockTest.RelaxedTimeoutTest} for
   * {@link CacheConnection}: same relaxation behavior across MIGRATING / FAILING_OVER / MOVING
   * events, the max-duration backstop, and blocking-command flips.
   */
  @Nested
  class RelaxedTimeoutTest extends AbstractRelaxedTimeoutBehaviorTest {

    @Override
    protected ConnectionPool createPool(HostAndPort hp, JedisClientConfig cfg,
        MaintenanceNotificationsConfig maint) {
      return new ConnectionPool(hp, cfg, cache, new ConnectionPoolConfig(), maint);
    }
  }

  /**
   * Mirror of {@link redis.clients.jedis.ConnectionMockTest.MaintenanceHandshake} for
   * {@link CacheConnection}: the RESP3-applicable paths (server rejects CLIENT MAINT_NOTIFICATIONS
   * under ENABLED vs AUTO). RESP2-specific scenarios don't apply — CacheConnection refuses RESP2.
   */
  @Nested
  class MaintenanceHandshake extends AbstractMaintenanceHandshakeTest {

    @Override
    protected Connection.Builder newConnectionBuilder() {
      return CacheConnection.builder(cache);
    }
  }

  /** Deliberately not {@code Serializable}. */
  private static final class Holder {
    final String value;

    Holder(String value) {
      this.value = value;
    }
  }
}