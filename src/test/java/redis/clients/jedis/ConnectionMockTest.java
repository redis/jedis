package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.util.SafeEncoder;
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

      // Verify only the (gated) pub/sub consumer is registered by default
      assertThat(consumers, hasSize(1));
    }

    @Test
    public void pubSubConsumerRegisteredWithBuilder() {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      Connection conn = Connection.builder().socketFactory(socketFactory).clientConfig(config)
          .build();

      List<PushConsumer> consumers = ConnectionTestHelper.getPushConsumers(conn);

      // Verify only the (gated) pub/sub consumer is registered by default
      assertThat(consumers, hasSize(1));
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

    @Test
    public void pubSubPushWithoutActiveSubscriptionDoesNotCorruptReply() throws Exception {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      try (Connection conn = new Connection(socketFactory, config)) {
        assertTrue(conn.ping());

        // A pub/sub message delivered while no pub/sub loop is running, e.g. a message the
        // server sent after the unsubscribe confirmation on a reused pooled connection
        mockServer.sendPushMessageToAll("message", "channel", "payload");
        Thread.sleep(200); // let the push land in the socket receive buffer

        assertTrue(conn.ping(), "PING must not receive the stray pub/sub push as its reply");
        assertFalse(conn.isBroken(), "Connection should not be broken");
      }
    }

    @Test
    public void pubSubPushPropagatedDuringActiveSubscription() throws Exception {
      DefaultJedisClientConfig config = DefaultJedisClientConfig.builder().resp3().build();

      HostAndPort hostAndPort = new HostAndPort("localhost", mockServer.getPort());
      DefaultJedisSocketFactory socketFactory = new DefaultJedisSocketFactory(hostAndPort, config);

      try (Connection conn = new Connection(socketFactory, config)) {
        ConnectionTestHelper.setActiveSubscription(conn, true);

        mockServer.sendPushMessageToAll("message", "channel", "payload");

        // Blocking read; the pub/sub loop consumes propagated pushes this way
        Object reply = conn.getUnflushedObject();
        List<?> content = assertInstanceOf(List.class, reply);
        assertArrayEquals(SafeEncoder.encode("message"), (byte[]) content.get(0));

        ConnectionTestHelper.setActiveSubscription(conn, false);
        mockServer.sendPushMessageToAll("message", "channel", "payload");
        Thread.sleep(200);
        assertTrue(conn.ping(), "After the subscription ends, pushes must be dropped again");
      }
    }

  }
}
