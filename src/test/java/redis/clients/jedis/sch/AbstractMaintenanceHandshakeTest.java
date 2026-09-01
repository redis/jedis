package redis.clients.jedis.sch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.DefaultJedisSocketFactory;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.util.SafeEncoder;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Shared behavior tests for the maintenance handshake over RESP3 — the protocol both
 * {@link Connection} and {@link redis.clients.jedis.csc.CacheConnection} negotiate when CSC or
 * maintenance notifications are in use. Covers the RESP3 + server-rejects-MAINT_NOTIFICATIONS paths
 * for both {@code ENABLED} and {@code AUTO} modes.
 * <p>
 * RESP2-specific paths (ENABLED throws, AUTO skips) only apply to plain {@link Connection} —
 * {@link redis.clients.jedis.csc.CacheConnection} refuses RESP2 at init — and are kept as
 * Connection-specific tests outside this base.
 */
public abstract class AbstractMaintenanceHandshakeTest {

  protected static final String NOPROTO_MAINT_REPLY = "-ERR unknown CLIENT subcommand 'MAINT_NOTIFICATIONS'\r\n";

  protected TcpMockServer mockServer;

  @BeforeEach
  public void schSetUp() throws IOException {
    mockServer = new TcpMockServer();
    mockServer.start();
  }

  @AfterEach
  public void schTearDown() throws IOException {
    if (mockServer != null) {
      mockServer.stop();
    }
  }

  /** Variant-specific connection builder: plain {@code Connection} vs {@code CacheConnection}. */
  protected abstract Connection.Builder newConnectionBuilder();

  /**
   * Builds a connection with the maintenance handshake wired on exactly as
   * {@code ConnectionFactory} does in production (config + visitor + controller).
   */
  protected final Connection buildConnection(HostAndPort hostAndPort, JedisClientConfig config,
      MaintenanceNotificationsConfig maintConfig) {
    Connection.Builder builder = newConnectionBuilder()
        .socketFactory(new DefaultJedisSocketFactory(hostAndPort, config)).clientConfig(config);
    return ConnectionTestHelper.withMaintenanceHandshake(builder, maintConfig).build();
  }

  // ---- Tests ---------------------------------------------------------------

  /**
   * Mocks {@code CLIENT MAINT_NOTIFICATIONS} with the {@code -ERR} reply; other commands default.
   */
  private void rejectMaintNotifications() {
    mockServer.setCommandHandler((args, clientId) -> {
      if (args.size() < 2) return null;
      String cmd = SafeEncoder.encode(args.getCommand().getRaw()).toUpperCase();
      String sub = SafeEncoder.encode(args.get(1).getRaw()).toUpperCase();
      return "CLIENT".equals(cmd) && "MAINT_NOTIFICATIONS".equals(sub) ? NOPROTO_MAINT_REPLY : null;
    });
  }

  /** {@code Mode.ENABLED} with the server rejecting {@code CLIENT MAINT_NOTIFICATIONS}: throws. */
  @Test
  public void enabledMode_overResp3_serverRejectsCommand_throws() {
    rejectMaintNotifications();

    MaintenanceNotificationsConfig maint = MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.ENABLED).build();
    JedisClientConfig cfg = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3)
        .build();

    HostAndPort hp = new HostAndPort("localhost", mockServer.getPort());
    assertThrows(JedisConnectionException.class, () -> buildConnection(hp, cfg, maint));
  }

  /**
   * Captures each {@code CLIENT MAINT_NOTIFICATIONS} command as decoded tokens; replies stay on
   * default handling ({@code +OK}).
   */
  private List<List<String>> captureMaintNotificationsCommands() {
    List<List<String>> captured = new CopyOnWriteArrayList<>();
    mockServer.setCommandHandler((args, clientId) -> {
      List<String> tokens = new ArrayList<>();
      args.forEach(arg -> tokens.add(SafeEncoder.encode(arg.getRaw())));
      if (tokens.size() >= 2 && "CLIENT".equalsIgnoreCase(tokens.get(0))
          && "MAINT_NOTIFICATIONS".equalsIgnoreCase(tokens.get(1))) {
        captured.add(tokens);
      }
      return null;
    });
    return captured;
  }

  private MaintenanceNotificationsConfig.Builder enabledMaint() {
    return MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.ENABLED);
  }

  /**
   * Default endpoint-type source auto-resolves per connection: the mock server peer is loopback
   * (private) over plaintext, so the handshake must request {@code internal-ip}.
   */
  @Test
  public void handshakeRequestsAutoResolvedEndpointType() {
    List<List<String>> captured = captureMaintNotificationsCommands();

    JedisClientConfig cfg = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3)
        .build();
    HostAndPort hp = new HostAndPort("localhost", mockServer.getPort());
    try (Connection c = buildConnection(hp, cfg, enabledMaint().build())) {
      assertTrue(c.ping());
    }

    assertEquals(1, captured.size());
    assertEquals(
      Arrays.asList("CLIENT", "MAINT_NOTIFICATIONS", "ON", "moving-endpoint-type", "internal-ip"),
      captured.get(0));
  }

  /** A fixed endpoint type overrides auto-resolution regardless of connection characteristics. */
  @Test
  public void handshakeRequestsFixedEndpointType() {
    List<List<String>> captured = captureMaintNotificationsCommands();

    JedisClientConfig cfg = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3)
        .build();
    HostAndPort hp = new HostAndPort("localhost", mockServer.getPort());
    try (Connection c = buildConnection(hp, cfg, enabledMaint()
        .endpointType(MaintenanceNotificationsConfig.EndpointType.EXTERNAL_FQDN).build())) {
      assertTrue(c.ping());
    }

    assertEquals(1, captured.size());
    assertEquals(
      Arrays.asList("CLIENT", "MAINT_NOTIFICATIONS", "ON", "moving-endpoint-type", "external-fqdn"),
      captured.get(0));
  }

  /** {@code Mode.AUTO} with the server rejecting {@code CLIENT MAINT_NOTIFICATIONS}: succeeds. */
  @Test
  public void autoMode_overResp3_serverRejectsCommand_succeeds() {
    rejectMaintNotifications();

    JedisClientConfig cfg = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3)
        .build();

    HostAndPort hp = new HostAndPort("localhost", mockServer.getPort());
    try (Connection c = buildConnection(hp, cfg, MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.AUTO).build())) {
      assertTrue(c.isConnected());
      assertEquals(RedisProtocol.RESP3, c.getRedisProtocol());
      assertTrue(c.ping());
    }
  }
}
