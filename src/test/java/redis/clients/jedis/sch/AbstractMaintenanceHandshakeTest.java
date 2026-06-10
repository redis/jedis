package redis.clients.jedis.sch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisDataException;
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

  /** Build the connection under test (variant-specific: plain Connection or CacheConnection). */
  protected abstract Connection buildConnection(HostAndPort hostAndPort, JedisClientConfig config);

  // ---- Tests ---------------------------------------------------------------

  /** {@code Mode.ENABLED} with the server rejecting {@code CLIENT MAINT_NOTIFICATIONS}: throws. */
  @Test
  public void enabledMode_overResp3_serverRejectsCommand_throws() {
    mockServer.respondWith("CLIENT", "MAINT_NOTIFICATIONS", NOPROTO_MAINT_REPLY);

    MaintenanceNotificationsConfig maint = MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.ENABLED).build();
    JedisClientConfig cfg = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3)
        .maintNotificationsConfig(maint).build();

    HostAndPort hp = new HostAndPort("localhost", mockServer.getPort());
    assertThrows(JedisDataException.class, () -> buildConnection(hp, cfg));
  }

  /** {@code Mode.AUTO} with the server rejecting {@code CLIENT MAINT_NOTIFICATIONS}: succeeds. */
  @Test
  public void autoMode_overResp3_serverRejectsCommand_succeeds() {
    mockServer.respondWith("CLIENT", "MAINT_NOTIFICATIONS", NOPROTO_MAINT_REPLY);

    JedisClientConfig cfg = DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3)
        .maintNotificationsConfig(MaintenanceNotificationsConfig.DEFAULT) // AUTO
        .build();

    HostAndPort hp = new HostAndPort("localhost", mockServer.getPort());
    try (Connection c = buildConnection(hp, cfg)) {
      assertTrue(c.isConnected());
      assertEquals(RedisProtocol.RESP3, c.getRedisProtocol());
      assertTrue(c.ping());
    }
  }
}
