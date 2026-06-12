package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static redis.clients.jedis.JedisClientConfig.UNSET_TIMEOUT_MS;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.sch.AbstractMaintenanceEventHandlingTest;
import redis.clients.jedis.sch.AbstractMaintenanceHandshakeTest;
import redis.clients.jedis.sch.AbstractRelaxedTimeoutBehaviorTest;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.server.CommandHandler;
import redis.clients.jedis.util.server.MaintenanceEventMessages;
import redis.clients.jedis.util.server.RespResponse;
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
  private final CommandHandler mockHandler = Mockito.mock(CommandHandler.class);

  @BeforeEach
  public void setUp() throws IOException {
    mockServer = new TcpMockServer();
    mockServer.setCommandHandler(mockHandler);
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

  /** Test helper: build a controller iff the config wants maintenance, else {@code null}. */
  private static MaintenanceEventController controllerFor(MaintenanceNotificationsConfig maint) {
    return maint != null && maint.isEnabledOrAuto() ? MaintenanceEventController.from(maint) : null;
  }

  @Nested
  class MaintenanceEventHandling extends AbstractMaintenanceEventHandlingTest {

    @Override
    protected ConnectionPool createPool(HostAndPort hp, JedisClientConfig cfg,
        MaintenanceNotificationsConfig maint) {
      ConnectionFactory factory = ConnectionFactory.builder().hostAndPort(hp).clientConfig(cfg)
          .maintenanceController(controllerFor(maint)).build();
      return new ConnectionPool(factory);
    }

    @Override
    protected Connection buildDirect(HostAndPort hostAndPort, JedisClientConfig config) {
      return new Connection(hostAndPort, config);
    }

    @Override
    protected Connection buildFromBuilder(DefaultJedisSocketFactory socketFactory,
        JedisClientConfig config) {
      return Connection.builder().socketFactory(socketFactory).clientConfig(config).build();
    }
  }

  /**
   * RESP2-specific maintenance-handshake behavior on plain {@link Connection}. The RESP3 paths
   * applicable to both connection types live in {@link AbstractMaintenanceHandshakeTest}.
   */
  @Nested
  class MaintenanceHandshake extends AbstractMaintenanceHandshakeTest {

    @Override
    protected Connection buildConnection(HostAndPort hp, JedisClientConfig cfg,
        MaintenanceNotificationsConfig maint) {
      return Connection.builder().socketFactory(new DefaultJedisSocketFactory(hp, cfg))
          .clientConfig(cfg).maintenanceController(controllerFor(maint)).build();
    }

    @Test
    public void enabledMode_overResp2_throwsConnectionException() {
      MaintenanceNotificationsConfig maint = MaintenanceNotificationsConfig.builder()
          .mode(MaintenanceNotificationsConfig.Mode.ENABLED).build();
      JedisClientConfig cfg = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP2)
          .build();

      HostAndPort hp = new HostAndPort("localhost", mockServer.getPort());
      JedisConnectionException ex = assertThrows(JedisConnectionException.class,
        () -> buildConnection(hp, cfg, maint));
      assertTrue(ex.getMessage().toUpperCase().contains("RESP3"),
        "exception message should mention RESP3 requirement, got: " + ex.getMessage());
    }

    @Test
    public void autoMode_overResp2_succeedsAndDoesNotSendMaintCommand() {
      CommandHandler localHandler = Mockito.mock(CommandHandler.class);
      mockServer.setCommandHandler(localHandler);

      JedisClientConfig cfg = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP2)
          .build();

      HostAndPort hp = new HostAndPort("localhost", mockServer.getPort());
      try (Connection c = buildConnection(hp, cfg, MaintenanceNotificationsConfig.DEFAULT)) {
        assertTrue(c.isConnected());
        assertEquals(RedisProtocol.RESP2, c.getRedisProtocol());
      }
      // CLIENT MAINT_NOTIFICATIONS must not be sent when the protocol is RESP2.
      verify(localHandler, never()).handleCommand(argThat(args -> {
        if (args.size() < 2) return false;
        String cmd = SafeEncoder.encode(args.getCommand().getRaw());
        String sub = SafeEncoder.encode(args.get(1).getRaw());
        return "CLIENT".equalsIgnoreCase(cmd) && "MAINT_NOTIFICATIONS".equalsIgnoreCase(sub);
      }), anyString());
    }
  }

  @Nested
  public class RelaxedTimeoutTest extends AbstractRelaxedTimeoutBehaviorTest {

    @Override
    protected ConnectionPool createPool(HostAndPort hp, JedisClientConfig cfg,
        MaintenanceNotificationsConfig maint) {
      return new ConnectionPool(ConnectionFactory.builder().hostAndPort(hp).clientConfig(cfg)
          .maintenanceController(controllerFor(maint)).build());
    }

    @Override
    protected Connection buildDirect(HostAndPort hp, JedisClientConfig cfg,
        MaintenanceNotificationsConfig maint) {
      return Connection.builder().socketFactory(new DefaultJedisSocketFactory(hp, cfg))
          .clientConfig(cfg).maintenanceController(controllerFor(maint)).build();
    }
  }
}
