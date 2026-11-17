package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import com.redis.test.fi.Database;
import com.redis.test.fi.Scenario;
import com.redis.test.fi.SlotMigrateEffect;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintNotificationsTestSupport;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvents;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig.Mode;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.scenario.MaintNotificationsConfigIT.MaintNotificationsInfo;

/**
 * S8: the handshake on an OSS-cluster database for every mode and protocol. ENABLED and AUTO over
 * RESP3 switch the notifications on for every node (CLIENT MAINT_NOTIFICATIONS_INFO) and give every
 * pool the cluster controller; DISABLED does neither; AUTO over RESP2 stays quiet and works;
 * ENABLED over RESP2 fails loudly. One shared database, no effect except in S8.3.
 */
@Tag("scenario")
public class ClusterMaintNotificationsConfigIT extends ClusterMaintNotificationsScenarioBase {

  private static final Logger log = LoggerFactory
      .getLogger(ClusterMaintNotificationsConfigIT.class);

  private static Scenario sharedScenario;
  private static Database shared;
  private static String lastActionId;

  @BeforeAll
  static void createSharedDatabase() {
    sharedScenario = freshScenario(SlotMigrateEffect.SLOT_SHUFFLE, "migrate", "external/ip");
    shared = provisionShared(sharedScenario);
  }

  @AfterAll
  static void deleteSharedDatabase() {
    if (shared != null) {
      faultInjector.release(shared);
      shared = null;
    }
    lastActionId = null;
  }

  @BeforeEach
  void gateOnPreviousEffect() {
    assertNoEffectRunning(lastActionId);
  }

  @AfterEach
  void rememberEffect() {
    if (lastEffectActionId != null) {
      lastActionId = lastEffectActionId;
    }
  }

  /** S8.1. */
  @Test
  @Timeout(120)
  void enabledAndAutoActivateNotificationsOnEveryNode() {
    for (Mode mode : new Mode[] { Mode.ENABLED, Mode.AUTO }) {
      RedisClusterClient c = openClient(shared,
        MaintenanceNotificationsConfig.builder().mode(mode).build(), config -> {
        }, builder -> {
        });
      Map<String, MaintNotificationsInfo> infos = infoPerNode(c);
      log.info("{}: {}", mode, infos);
      infos.forEach((node, info) -> assertEquals("on", info.status(), mode + " on " + node));
      c.getClusterNodes().forEach(
        (node, pool) -> assertTrue(ConnectionTestHelper.hasClusterMaintenanceController(pool),
          mode + ": pool of " + node + " has no cluster controller"));
      c.getClusterNodes().forEach((node, pool) -> assertEquals(mode,
        ConnectionTestHelper.getConfiguredMaintenanceMode(pool), "configured mode on " + node));
    }
  }

  /** S8.2. */
  @Test
  @Timeout(120)
  void disabledDoesNotActivateNotificationsAnywhere() {
    RedisClusterClient c = openClient(shared, MaintenanceNotificationsConfig.DISABLED, config -> {
    }, builder -> {
    });
    Map<String, MaintNotificationsInfo> infos = infoPerNode(c);
    log.info("DISABLED: {}", infos);
    infos.forEach((node, info) -> assertEquals("off", info.status(), "DISABLED on " + node));
    c.getClusterNodes().forEach(
      (node, pool) -> assertFalse(ConnectionTestHelper.hasClusterMaintenanceController(pool),
        "DISABLED: pool of " + node + " has a cluster controller"));
  }

  /**
   * S8.3. A DISABLED client across a real migration: no push is dispatched to its connections, its
   * timeouts never change, and its commands still succeed because the stock MOVED handling and
   * topology refresh take over.
   */
  @Test
  @Timeout(300)
  void disabledClientIsUnaffectedByARealMigration() {
    RedisClusterClient c = openClient(shared, MaintenanceNotificationsConfig.DISABLED,
      config -> MaintNotificationsObserverBase.observerClientConfig(config), builder -> {
      });
    HostAndPort[] owners = ownersBySlot(c);
    Map<String, Connection> held = new LinkedHashMap<>();
    Map<String, ReceivedEvents> events = new LinkedHashMap<>();
    for (String node : nodeKeys(c)) {
      Connection connection = c.getConnectionFromSlot(slotOwnedBy(owners, node, s -> false));
      held.put(node, connection);
      events.put(node, MaintNotificationsTestSupport.record(connection));
    }
    try {
      startEffect(shared, sharedScenario, Collections.emptyMap());
      joinEffectThreads();
      assertEffectSucceeded();

      held.forEach((node, connection) -> {
        connection.executeCommand(Protocol.Command.PING);
        assertTrue(events.get(node).all().isEmpty(),
          "DISABLED client received pushes on " + node + ": " + events.get(node).all());
        assertFalse(ConnectionTestHelper.isRelaxedTimeoutActive(connection),
          "DISABLED client relaxed on " + node);
        assertEquals(MaintNotificationsObserverBase.CLIENT_SOCKET_TIMEOUT_MS,
          MaintNotificationsTestSupport.effectiveTimeoutMillis(connection), "timeout on " + node);
      });
      for (String key : keysAcrossRanges("disabled", shardRanges(sharedScenario, c))) {
        c.set(key, "v");
        assertEquals("v", c.get(key), "stock MOVED handling must still serve " + key);
      }
    } finally {
      held.values().forEach(Connection::close);
    }
  }

  /** S8.4. */
  @Test
  @Timeout(120)
  void autoOverResp2SendsNoHandshakeAndWorks() {
    RedisClusterClient c = openClient(shared,
      MaintenanceNotificationsConfig.builder().mode(Mode.AUTO).build(),
      config -> config.protocol(RedisProtocol.RESP2), builder -> {
      });
    assertEquals("PONG", c.ping());
    for (Map.Entry<String, ConnectionPool> entry : c.getClusterNodes().entrySet()) {
      try (Connection connection = entry.getValue().getResource()) {
        assertFalse(ConnectionTestHelper.hasMaintenanceEventConsumer(connection),
          "AUTO over RESP2 subscribed to pushes on " + entry.getKey());
      }
      // the pool keeps its controller, configured AUTO; only the per-connection wiring is absent
      assertEquals(Mode.AUTO, ConnectionTestHelper.getConfiguredMaintenanceMode(entry.getValue()),
        "configured mode on " + entry.getKey());
    }
  }

  /** S8.5. */
  @Test
  @Timeout(120)
  void enabledOverResp2Fails() {
    JedisException e = assertThrows(JedisException.class, () -> {
      RedisClusterClient c = openClient(shared,
        MaintenanceNotificationsConfig.builder().mode(Mode.ENABLED).build(),
        config -> config.protocol(RedisProtocol.RESP2), builder -> {
        });
      c.ping();
    });
    log.info("ENABLED over RESP2 failed as expected: {}", e.toString());
  }

  /** CLIENT MAINT_NOTIFICATIONS_INFO on one connection of every node's pool. */
  private static Map<String, MaintNotificationsInfo> infoPerNode(RedisClusterClient c) {
    Map<String, MaintNotificationsInfo> infos = new LinkedHashMap<>();
    for (Map.Entry<String, ConnectionPool> entry : c.getClusterNodes().entrySet()) {
      try (Connection connection = entry.getValue().getResource()) {
        infos.put(entry.getKey(),
          connection.executeCommand(MaintNotificationsConfigIT.maintNotificationsInfo()));
      }
    }
    return infos;
  }
}
