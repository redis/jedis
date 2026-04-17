package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import redis.clients.jedis.util.SafeEncoder;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.DefaultJedisSocketFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.PushConsumer;
import redis.clients.jedis.PushConsumerChainImpl;
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
  class PushInvalidateConsumer {

    @Test
    public void pushInvalidateConsumerRegisteredWithConfigConstructor() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      CacheConnection conn = new CacheConnection(socketFactory, config, cache);

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);

      // Verify PushInvalidateConsumer is registered
      assertThat(consumers, contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER),
        instanceOf(CacheConnection.PushInvalidateConsumer.class)));
    }

    @Test
    public void pushInvalidateConsumerRegisteredWithBuilder() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      CacheConnection conn = (CacheConnection) CacheConnection.builder(cache)
          .socketFactory(socketFactory).clientConfig(config).build();

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);

      // Verify PushInvalidateConsumer is registered
      assertThat(consumers, contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER),
        instanceOf(CacheConnection.PushInvalidateConsumer.class)));
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
        String invalidateMessage = ">2\r\n"
            + "$10\r\ninvalidate\r\n"
            + "*1\r\n"
            + "$8\r\ntestkey1\r\n";

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
}