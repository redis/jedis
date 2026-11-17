package redis.clients.jedis.sch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.ConnectionTestHelper.hasClusterMaintenanceController;
import static redis.clients.jedis.ConnectionTestHelper.hasExpiringRelaxOverlay;
import static redis.clients.jedis.ConnectionTestHelper.hasMaintenanceController;
import static redis.clients.jedis.ConnectionTestHelper.hasMaintenanceEventConsumer;
import static redis.clients.jedis.ConnectionTestHelper.getConfiguredMaintenanceMode;
import static redis.clients.jedis.ConnectionTestHelper.hasPoolRelaxOverlay;
import static redis.clients.jedis.ConnectionTestHelper.hasPoolControllerListener;
import static redis.clients.jedis.ConnectionTestHelper.isRelaxedTimeoutActive;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig.Mode;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.providers.ClusterConnectionProvider;

/**
 * Sections A (handshake and per-connection wiring) and E (builder and provider surface) of the
 * cluster maintenance test plan, against a three-node {@code TcpMockServer} cluster. The cases are
 * defined in ClusterMaintenanceIT.md, in this package directory.
 */
public class ClusterMaintenanceHandshakeMockTest extends AbstractClusterMaintenanceMockTest {

  /** Case A1, section A, in ClusterMaintenanceIT.md. */
  @Test
  public void autoOverResp3WiresEveryPooledConnection() {
    openClient(defaultClientConfig(), maintConfig(Mode.AUTO));
    client.set(keyOnA(), "v");
    client.set(keyOnB(), "v");

    assertEquals(1, handlerA.maintNotificationsHandshakes().size());
    assertEquals(1, handlerB.maintNotificationsHandshakes().size());
    assertEquals(
      Arrays.asList("CLIENT", "MAINT_NOTIFICATIONS", "ON", "moving-endpoint-type", "internal-ip"),
      handlerA.maintNotificationsHandshakes().get(0));

    assertWired(nodeA, A_FROM);
    assertWired(nodeB, B_FROM);
  }

  /** Case A2, section A, in ClusterMaintenanceIT.md. */
  @Test
  public void autoWithARejectingNodeIsQuietlyOffForThatNodeOnly() {
    handlerA.rejectMaintNotifications(true);
    openClient(defaultClientConfig(), maintConfig(Mode.AUTO));

    client.set(keyOnA(), "v"); // no exception: AUTO degrades per connection
    client.set(keyOnB(), "v");

    assertEquals(1, handlerA.maintNotificationsHandshakes().size(), "the handshake was attempted");
    assertNotWired(nodeA, A_FROM);
    assertTrue(hasMaintenanceController(poolOf(nodeA)), "the pool keeps its controller");
    assertWired(nodeB, B_FROM);
  }

  /** Cases A3 (section A) and E3 (section E) in ClusterMaintenanceIT.md. */
  @Test
  public void enabledWithARejectingNodeFailsTheCommandAndKeepsNoConnection() {
    handlerA.rejectMaintNotifications(true);
    client = clientBuilder(defaultClientConfig(), maintConfig(Mode.ENABLED)).maxAttempts(2).build();

    assertThrows(JedisClusterOperationException.class, () -> client.set(keyOnA(), "v"));

    ConnectionPool poolA = poolOf(nodeA);
    assertEquals(0, poolA.getNumIdle(), "a connection that failed its handshake is not pooled");
    assertEquals(0, poolA.getNumActive());
    assertTrue(handlerA.commands("SET").isEmpty(), "the command never reached the node");
    awaitConnections(serverA, 0);
    client.set(keyOnB(), "v"); // the other node is unaffected
    assertEquals(1, handlerB.commands("SET").size());
  }

  /** Case A4, section A, in ClusterMaintenanceIT.md. */
  @Test
  public void resp2SkipsTheHandshakeAndInstallsNothing() {
    openClient(clientConfig(RedisProtocol.RESP2, SO_TIMEOUT_MS), maintConfig(Mode.AUTO));
    client.set(keyOnA(), "v");

    assertTrue(handlerA.maintNotificationsHandshakes().isEmpty());
    assertNotWired(nodeA, A_FROM);
    assertTrue(hasMaintenanceController(poolOf(nodeA)), "AUTO: the pool controller exists");
  }

  /** Cases A5 (section A) and E2 (section E) in ClusterMaintenanceIT.md. */
  @Test
  public void disabledSendsNoHandshakeAndWiresNoController() {
    openClient(defaultClientConfig(), MaintenanceNotificationsConfig.DISABLED);
    client.set(keyOnA(), "v");
    client.set(keyOnB(), "v");

    assertTrue(handlerA.maintNotificationsHandshakes().isEmpty());
    assertTrue(handlerB.maintNotificationsHandshakes().isEmpty());
    for (ConnectionPool pool : client.getClusterNodes().values()) {
      assertFalse(hasMaintenanceController(pool));
    }
    assertNotWired(nodeA, A_FROM);
  }

  /** Case A6, section A, in ClusterMaintenanceIT.md. */
  @Test
  public void connectionOpenedDuringAnOpenWindowRelaxesItsHandshake() {
    int soTimeoutMs = 250;
    handlerB.helloDelayMillis(900); // slower than the socket timeout, faster than the relaxed one
    client = clientBuilder(clientConfig(RedisProtocol.RESP3, soTimeoutMs), maintConfig(Mode.AUTO))
        .maxAttempts(1).build();

    // control: no window, the slow HELLO exceeds the configured socket timeout
    assertThrows(JedisClusterOperationException.class, () -> client.set(keyOnB(), "v"));
    assertTrue(handlerB.commands("SET").isEmpty());

    // a migration window opened on A relaxes the handshake of a fresh connection to B
    deliver(serverA, A_FROM, migrating(1L, "0-100"));
    client.set(keyOnB(), "v");

    assertEquals(1, handlerB.commands("SET").size());
  }

  /** Case E1, section E, in ClusterMaintenanceIT.md. */
  @Test
  public void builderDefaultsToAutoHandshake() {
    client = RedisClusterClient.builder().nodes(Collections.singleton(nodeA))
        .clientConfig(defaultClientConfig()).build();
    client.set(keyOnA(), "v");

    assertEquals(1, handlerA.maintNotificationsHandshakes().size());
    assertTrue(hasClusterMaintenanceController(poolOf(nodeA)));
    // the handshake and the controller alone would look the same under ENABLED
    assertEquals(Mode.AUTO, getConfiguredMaintenanceMode(poolOf(nodeA)));
  }

  /** Case E4, section E, in ClusterMaintenanceIT.md. */
  @Test
  public void providerConstructorWiresTheClusterControllerOnlyWhenConfigured() {
    try (
        ClusterConnectionProvider with = new ClusterConnectionProvider(Collections.singleton(nodeA),
            defaultClientConfig(), null, new GenericObjectPoolConfig<>(), null,
            maintConfig(Mode.AUTO));
        ClusterConnectionProvider without = new ClusterConnectionProvider(
            Collections.singleton(nodeA), defaultClientConfig(), null,
            new GenericObjectPoolConfig<>(), null)) {
      assertEquals(2, with.getNodes().size());
      for (ConnectionPool pool : with.getNodes().values()) {
        assertTrue(hasClusterMaintenanceController(pool));
      }
      for (ConnectionPool pool : without.getNodes().values()) {
        assertFalse(hasMaintenanceController(pool));
      }
    }
  }

  private void assertWired(HostAndPort node, int slot) {
    ConnectionPool pool = poolOf(node);
    assertTrue(hasClusterMaintenanceController(pool));
    try (Connection connection = client.getConnectionFromSlot(slot)) {
      assertTrue(hasPoolControllerListener(pool, connection));
      assertTrue(hasPoolRelaxOverlay(pool, connection));
      assertFalse(hasExpiringRelaxOverlay(connection),
        "no per-connection window in the cluster family");
      assertTrue(hasMaintenanceEventConsumer(connection));
      assertFalse(isRelaxedTimeoutActive(connection), "nothing open yet");
    }
  }

  private void assertNotWired(HostAndPort node, int slot) {
    ConnectionPool pool = poolOf(node);
    try (Connection connection = client.getConnectionFromSlot(slot)) {
      assertTrue(connection.ping(), "the connection is usable");
      assertFalse(hasPoolControllerListener(pool, connection));
      assertFalse(hasPoolRelaxOverlay(pool, connection));
      assertFalse(hasExpiringRelaxOverlay(connection));
      assertFalse(hasMaintenanceEventConsumer(connection));
    }
  }
}
