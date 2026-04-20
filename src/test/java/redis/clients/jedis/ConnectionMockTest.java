package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.time.Duration;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Unit tests for Connection that don't require a real Redis server. Uses TcpMockServer to simulate
 * Redis protocol.
 * <p>
 * These tests verify Connection base class behavior (default push consumer registration and push
 * notification handling).
 * </p>
 */
public class ConnectionMockTest {

  private TcpMockServer mockServer;

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new TcpMockServer();
    mockServer.start();
  }

  @AfterEach
  public void tearDown() throws IOException {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  @Nested
  class PushNotificationHandling {

    @Test
    public void pubSubConsumerRegisteredWithConfigConstructor() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      Connection conn = new Connection(socketFactory, config);

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);

      // Verify only PUBSUB_CONSUMER is registered by default
      assertThat(consumers, contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER)));
    }

    @Test
    public void pubSubConsumerRegisteredWithBuilder() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      Connection conn = Connection.builder().socketFactory(socketFactory).clientConfig(config)
          .build();

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);

      // Verify only PUBSUB_CONSUMER is registered by default
      assertThat(consumers, contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER)));
    }
    /**
     * Tests that the MaintenanceEventConsumer is registered when using the constructors not
     * providing a JedisClientConfig.
     */
    @Test
    public void maintenanceConsumerRegisteredConstructorWithConfig() {
      TimeoutOptions timeoutOpts = TimeoutOptions.builder()
              .proactiveTimeoutsRelaxing(Duration.ofSeconds(10)).build();

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
              .timeoutOptions(timeoutOpts).build();

      Connection conn = new Connection(new HostAndPort("localhost", mockServer.getPort()), config);

      assertThat(conn.getPushConsumers(), contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER),
              instanceOf(Connection.MaintenanceEventConsumer.class)));
    }

    @Test
    public void maintenanceConsumerRegisteredWithConnectionBuilder() {
      TimeoutOptions timeoutOpts = TimeoutOptions.builder()
              .proactiveTimeoutsRelaxing(Duration.ofSeconds(10)).build();

      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder()
              .timeoutOptions(timeoutOpts).build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      Connection conn = Connection.builder().socketFactory(socketFactory).clientConfig(config)
              .build();

      assertThat(conn.getPushConsumers(), contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER),
              instanceOf(Connection.MaintenanceEventConsumer.class)));
    }

    @Test
    public void maintenanceConsumerNotRegisteredRelaxedTimeoutDisabled() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().build();

      Connection conn = new Connection(new HostAndPort("localhost", mockServer.getPort()), config);

      assertThat(conn.getPushConsumers(), contains(is(PushConsumerChainImpl.PUBSUB_CONSUMER)));
    }

    @Test
    public void arbitraryPushNotificationDoesNotBreakConnection() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      try (Connection conn = new Connection(socketFactory, config)) {

        // Send arbitrary push notification (not pub/sub related)
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

  }
}
