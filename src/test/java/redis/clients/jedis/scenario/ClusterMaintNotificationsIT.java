package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.redis.test.fi.Scenario;
import com.redis.test.fi.SlotMigrateEffect;
import com.redis.test.fi.Trigger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintNotificationsTestSupport;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedClusterEvent;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvent;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvents;
import redis.clients.jedis.MaintNotificationsTestSupport.SlotMove;
import redis.clients.jedis.PushMessageTypes;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.exceptions.JedisMovedDataException;

/**
 * One real slot migration per trigger, observed end to end on a
 * {@link redis.clients.jedis.RedisClusterClient}: the pushes arrive and parse (S1), timeouts relax
 * client-wide on SMIGRATING and restore after SMIGRATED (S2), the slot map and the node pools
 * follow the SMIGRATED deltas (S3), and the client closes cleanly afterwards (S10). Every
 * observation on one effect is taken in one method on one database, because provisioning dominates
 * the cost; failures are collected so one does not hide the independent ones.
 */
@Tag("scenario")
public class ClusterMaintNotificationsIT extends ClusterMaintNotificationsScenarioBase {

  private static final Logger log = LoggerFactory.getLogger(ClusterMaintNotificationsIT.class);

  /**
   * Both Case 2 directions on every trigger, plus the endpoint-stable Case 1 on the fastest one:
   * remove-add/migrate drops one pool and creates another in one delta over the longest window;
   * add/migrate and remove/failover isolate the create and the drop; slot-shuffle/failover is the
   * endpoint-stable case with the most nodes and a sub-second window; add/reshard and
   * remove/reshard are the ASM cascades in each direction. The hostname variant runs where the node
   * identity in SMIGRATED matters most (one non-ASM, one ASM row); ip elsewhere. Every row must be
   * offered by the FI: a missing trigger or variant fails the whole method instead of silently
   * shrinking the matrix.
   */
  static Stream<Scenario> effectTriggerCombinations() {
    List<Scenario> rows = new ArrayList<>();
    row(rows, SlotMigrateEffect.REMOVE_ADD, "migrate", "external/hostname");
    row(rows, SlotMigrateEffect.SLOT_SHUFFLE, "failover", "external/ip");
    row(rows, SlotMigrateEffect.ADD, "migrate", "external/ip");
    row(rows, SlotMigrateEffect.REMOVE, "failover", "external/ip");
    row(rows, SlotMigrateEffect.ADD, "reshard", "external/ip+asm");
    row(rows, SlotMigrateEffect.REMOVE, "reshard", "external/hostname+asm");
    return rows.stream();
  }

  /** Adds the row; fails when the FI does not offer the trigger or the variant. */
  static void row(List<Scenario> rows, SlotMigrateEffect effect, String trigger, String config) {
    Optional<Trigger> offered = CLUSTER_CATALOG.effect(effect).triggers().stream()
        .filter(t -> t.name().equals(trigger)).findFirst();
    if (!offered.isPresent() || !offered.get().offers(config)) {
      fail("FI does not offer " + effect + "/" + trigger + "[" + config + "]; offered: "
          + CLUSTER_CATALOG.effect(effect).triggers());
    }
    rows.add(offered.get().scenario(config));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("effectTriggerCombinations")
  @Timeout(900)
  void migrationIsHandled(Scenario scenario) {
    assumeFalse(scenario.requirement().asm() && Boolean.getBoolean("sch.skipAsm"),
      "ASM rows skipped by -Dsch.skipAsm=true");
    SlotMigrateEffect effect = (SlotMigrateEffect) scenario.effect();
    Observations obs = new Observations();

    // 1. before: snapshots, and the client's view must match the server's
    openObserver(deployDatabase(scenario));
    openVerificationClient(database);
    Set<String> before = nodeKeys(client);
    // the pools themselves, not just their keys: a pool the delta drops from the node map must also
    // be destroyed, and after settle it is no longer reachable through the client
    Map<String, ConnectionPool> poolsBefore = new LinkedHashMap<>(client.getClusterNodes());
    HostAndPort[] ownerBefore = ownersBySlot(client);
    verification.refreshClusterTopology();
    assertEquals(nodeKeys(verification), before, "observer and server disagree before the effect");
    pinOneConnectionPerNode();
    log.info("{}: nodes before = {}", scenario, before);

    // 2. fire
    startEffect(scenario);

    // 3. opening (S2.1): the first SMIGRATING read on any one connection opens the client-wide
    // gate. Checked before any other connection reads anything: a connection whose own push is
    // still unread can only be relaxed by the gate.
    awaitFirstSMigrating();
    Set<HostAndPort> ownPushUnread = pinnedWithoutSMigrating();
    if (anyRecorded(ReceivedEvents::sMigratedReceived)) {
      // SMIGRATED was buffered behind the same reply as the SMIGRATING: the window closed as it
      // opened (sub-second triggers), so there is no open gate to observe
      log.warn("{}: S2.1 not observable, SMIGRATED read in the same pass as the first SMIGRATING",
        scenario);
    } else {
      log.info("{}: S2.1 first SMIGRATING read; own push still unread on {}", scenario,
        ownPushUnread);
      checkGateOpen(obs);
      checkAllPinnedRelaxed(obs, ownPushUnread);
    }

    // 5. closing: FI done, stream drained, node set converged
    awaitEffectSettled(before.size() + effect.endpointDelta());
    Set<String> after = nodeKeys(client);
    Map<String, ConnectionPool> poolsAfter = new LinkedHashMap<>(client.getClusterNodes());
    HostAndPort[] ownerAfter = ownersBySlot(client);
    List<ReceivedEvent> events = allEvents();
    List<SlotMove> distinctMoves = distinctMoves(events);
    logReception(scenario);
    log.info("{}: nodes after = {}, moves = {}", scenario, after, distinctMoves);

    // S1.1 reception: at least one connection saw each message; duplicates are the client's job
    obs.check("S1.1 SMIGRATING received",
      () -> assertTrue(anyRecorded(ReceivedEvents::sMigratingReceived),
        "no SMIGRATING on any connection"));
    obs.check("S1.1 SMIGRATED received",
      () -> assertTrue(anyRecorded(ReceivedEvents::sMigratedReceived),
        "no SMIGRATED on any connection"));
    // S1.2 payload: every event parsed into a usable payload
    obs.check("S1.2 payloads parsed", () -> {
      for (ReceivedEvent event : events) {
        assertTrue(event instanceof ReceivedClusterEvent, "non-cluster event recorded: " + event);
        for (SlotMove move : ((ReceivedClusterEvent) event).moves) {
          assertNotEquals(nodeKey(move.src()), nodeKey(move.dest()), "src == dest in " + move);
        }
      }
    });
    // S1.3 identity: SMIGRATED names nodes in the same form the client keys its pools by
    obs.check("S1.3 src/dest host form matches node keys", () -> {
      for (SlotMove move : distinctMoves) {
        assertTrue(before.contains(nodeKey(move.src())),
          "SMIGRATED src " + move.src() + " is not a node key the client knew before: " + before);
        assertTrue(after.contains(nodeKey(move.dest())),
          "SMIGRATED dest " + move.dest() + " is not a node key the client has after: " + after);
      }
    });

    // S3.1 owners: every moved slot routes to dest, every other slot is unchanged
    obs.check("S3.1 slot owners follow the deltas", () -> {
      List<String> mismatches = ownerMismatches(ownerBefore, ownerAfter, distinctMoves);
      assertTrue(mismatches.isEmpty(), mismatches.size() + " owner mismatches, first: "
          + mismatches.subList(0, Math.min(5, mismatches.size())));
    });

    // S3.2 / S3.3 / S3.4 node set and pools per effect
    Set<String> removed = new LinkedHashSet<>(before);
    removed.removeAll(after);
    Set<String> added = new LinkedHashSet<>(after);
    added.removeAll(before);
    Set<String> sources = new LinkedHashSet<>();
    Set<String> destinations = new LinkedHashSet<>();
    distinctMoves.forEach(move -> {
      sources.add(nodeKey(move.src()));
      destinations.add(nodeKey(move.dest()));
    });
    switch (effect) {
      case SLOT_SHUFFLE:
        obs.check("S3.2 node set unchanged", () -> assertEquals(before, after));
        obs.check("S3.2 no pool closed", () -> poolsAfter
            .forEach((node, pool) -> assertFalse(pool.isClosed(), "pool closed: " + node)));
        break;
      case ADD:
        obs.check("S3.3 one node added", () -> assertEquals(1, added.size(), "added: " + added));
        checkAddedNode(obs, added, destinations, poolsAfter);
        break;
      case REMOVE:
        obs.check("S3.4 one node removed",
          () -> assertEquals(1, removed.size(), "removed: " + removed));
        checkRemovedNode(obs, removed, sources, ownerAfter, poolsBefore);
        break;
      case REMOVE_ADD:
        obs.check("S3.3/S3.4 one node swapped", () -> {
          assertEquals(1, removed.size(), "removed: " + removed);
          assertEquals(1, added.size(), "added: " + added);
        });
        checkAddedNode(obs, added, destinations, poolsAfter);
        checkRemovedNode(obs, removed, sources, ownerAfter, poolsBefore);
        break;
      default:
        throw new IllegalStateException(effect.toString());
    }

    // S2.2 unrelaxed: no window left, surviving connections back at the base timeout
    obs.check("S2.2 no active migration", () -> {
      assertFalse(MaintNotificationsTestSupport.hasActiveMigration(client), "active migration");
      assertEquals(0, MaintNotificationsTestSupport.openMigrationWindows(client), "open windows");
    });
    for (Map.Entry<HostAndPort, Connection> entry : livePinned().entrySet()) {
      if (removed.contains(nodeKey(entry.getKey()))) {
        continue;
      }
      Connection conn = entry.getValue();
      obs.check("S2.2 base timeout restored on " + entry.getKey(), () -> {
        conn.executeCommand(redis.clients.jedis.Protocol.Command.PING);
        assertFalse(ConnectionTestHelper.isRelaxedTimeoutActive(conn), "still relaxed");
        assertEquals(MaintNotificationsObserverBase.CLIENT_SOCKET_TIMEOUT_MS,
          MaintNotificationsTestSupport.effectiveTimeoutMillis(conn), "effective timeout");
      });
    }

    // 6. verify (S3.5): what the application sees after settle, and the server-side truth
    CommandOutcomes outcomes = probeCommands(distinctMoves, ownerAfter, after);
    log.info("{}: S3.5 outcomes: {}", scenario, outcomes);
    obs.check("S3.5 no MOVED after settle",
      () -> assertEquals(0, outcomes.moved, "MOVED redirects: " + outcomes));
    obs.check("S3.5 no failed connection after settle",
      () -> assertEquals(0, outcomes.connectionFailed, "failed connections: " + outcomes));
    obs.check("S3.5 no unexpected exception",
      () -> assertEquals(0, outcomes.other, "unexpected exceptions: " + outcomes.firstOther));
    verification.refreshClusterTopology();
    obs.check("server truth matches the client's node set",
      () -> assertEquals(nodeKeys(verification), after));

    // 7. lifecycle (S10): the client closes with every pool, nothing left behind
    releasePinned();
    client.close();
    // every pool the client ever had: the ones it holds now and the ones the deltas dropped
    Map<String, ConnectionPool> everyPool = new LinkedHashMap<>(poolsBefore);
    everyPool.putAll(poolsAfter);
    obs.check("S10 every pool closed on client.close()", () -> everyPool
        .forEach((node, pool) -> assertTrue(pool.isClosed(), "pool still open: " + node)));
    client = null;

    obs.assertAllPassed();
  }

  /** S2.1, client side: the coordinator holds an open window after the first SMIGRATING. */
  private void checkGateOpen(Observations obs) {
    obs.check("S2.1 gate open after the first SMIGRATING", () -> {
      assertTrue(MaintNotificationsTestSupport.hasActiveMigration(client), "no active migration");
      assertTrue(MaintNotificationsTestSupport.openMigrationWindows(client) > 0, "no open window");
    });
  }

  /**
   * S2.1, connection side: every live pinned connection is relaxed, read without a round-trip so
   * the check itself consumes no push. A connection in {@code ownPushUnread} has not read its own
   * SMIGRATING, so only the client-wide gate can have relaxed it; its observation says so.
   */
  private void checkAllPinnedRelaxed(Observations obs, Set<HostAndPort> ownPushUnread) {
    for (Map.Entry<HostAndPort, Connection> entry : livePinned().entrySet()) {
      Connection conn = entry.getValue();
      String how = ownPushUnread.contains(entry.getKey()) ? " (own SMIGRATING unread)" : "";
      obs.check("S2.1 relaxed on " + entry.getKey() + how, () -> {
        assertTrue(ConnectionTestHelper.isRelaxedTimeoutActive(conn), "relaxation not active");
        assertEquals(MaintNotificationsObserverBase.RELAXED_TIMEOUT_MS,
          MaintNotificationsTestSupport.effectiveTimeoutMillis(conn), "effective timeout");
      });
    }
  }

  /** Mock C4: the destination's pool exists, carries the cluster controller, and serves. */
  private void checkAddedNode(Observations obs, Set<String> added, Set<String> destinations,
      Map<String, ConnectionPool> poolsAfter) {
    obs.check("S3.3 added node is a SMIGRATED dest",
      () -> assertTrue(destinations.containsAll(added),
        "added " + added + " not among dests " + destinations));
    for (String node : added) {
      ConnectionPool pool = poolsAfter.get(node);
      obs.check("S3.3 new pool " + node + " has the cluster controller", () -> {
        assertNotNull(pool, "no pool");
        assertTrue(ConnectionTestHelper.hasClusterMaintenanceController(pool), "no controller");
        try (Connection conn = pool.getResource()) {
          conn.executeCommand(redis.clients.jedis.Protocol.Command.PING);
        }
      });
    }
  }

  /**
   * Mock C2 / F2: the source left without slots is gone, its pool destroyed, its pinned connection
   * returns cleanly.
   */
  private void checkRemovedNode(Observations obs, Set<String> removed, Set<String> sources,
      HostAndPort[] ownerAfter, Map<String, ConnectionPool> poolsBefore) {
    obs.check("S3.4 removed node is a SMIGRATED src owning no slot", () -> {
      assertTrue(sources.containsAll(removed), "removed " + removed + " not among srcs " + sources);
      for (HostAndPort owner : ownerAfter) {
        assertFalse(removed.contains(nodeKey(owner)), "removed node still owns a slot: " + owner);
      }
    });
    for (String node : removed) {
      ConnectionPool pool = poolsBefore.get(node);
      // destroyed, not merely forgotten: the pool the client handed out before the effect is
      // closed and refuses a borrow, so no connection to the departed node can be obtained again
      obs.check("S3.4 pool of removed " + node + " is destroyed", () -> {
        assertNotNull(pool, "no pool existed before the effect");
        assertTrue(pool.isClosed(), "pool of removed node still open");
        assertThrows(JedisException.class, pool::getResource,
          "destroyed pool handed out a connection");
      });
    }
    for (Map.Entry<HostAndPort, Connection> entry : pinned.entrySet()) {
      if (!removed.contains(nodeKey(entry.getKey()))) {
        continue;
      }
      Connection conn = entry.getValue();
      obs.check("S3.4 pinned connection to removed " + entry.getKey() + " returns cleanly",
        conn::close);
    }
  }

  private void logReception(Scenario scenario) {
    recorded.forEach((node, events) -> {
      long migrating = events.all().stream().filter(e -> PushMessageTypes.SMIGRATING.equals(e.type))
          .count();
      long migrated = events.all().stream().filter(e -> PushMessageTypes.SMIGRATED.equals(e.type))
          .count();
      log.info("{}: {} received SMIGRATING x{} SMIGRATED x{}{}: {}", scenario, node, migrating,
        migrated, dropped.contains(node) ? " (dropped)" : "", events.all());
    });
  }

  /**
   * One SET + GET per moved range and one per surviving node's unmoved range, through the observer
   * ({@code maxAttempts(1)}), so every outcome is what the application would have seen. After the
   * settle rule a MOVED means the slot map was not applied (S3.1) and a failed connection means a
   * pool is unusable (S3.3); the DMC's post-SMIGRATED forwarding grace can hide such a defect but
   * never cause one, so nothing here depends on it.
   */
  private CommandOutcomes probeCommands(Iterable<SlotMove> moves, HostAndPort[] ownerAfter,
      Set<String> nodesAfter) {
    CommandOutcomes outcomes = new CommandOutcomes();
    boolean[] moved = new boolean[ownerAfter.length];
    int i = 0;
    for (SlotMove move : moves) {
      move.forEachSlot(slot -> moved[slot] = true);
      probe(outcomes, keyInSlots("moved" + i++, move::contains));
    }
    for (String node : nodesAfter) {
      int slot = slotOwnedBy(ownerAfter, node, s -> moved[s]);
      if (slot >= 0) {
        int unmovedSlot = slot;
        probe(outcomes, keyInSlots("unmoved", s -> s == unmovedSlot));
      }
    }
    return outcomes;
  }

  private void probe(CommandOutcomes outcomes, String key) {
    try {
      client.set(key, "v");
      assertEquals("v", client.get(key));
      outcomes.ok++;
    } catch (JedisClusterOperationException e) {
      if (e.getCause() instanceof JedisMovedDataException) {
        outcomes.moved++;
      } else if (e.getCause() instanceof JedisConnectionException) {
        outcomes.connectionFailed++;
      } else {
        outcomes.other++;
        outcomes.firstOther = outcomes.firstOther == null ? e : outcomes.firstOther;
      }
    } catch (JedisConnectionException e) {
      outcomes.connectionFailed++;
    } catch (JedisException e) {
      outcomes.other++;
      outcomes.firstOther = outcomes.firstOther == null ? e : outcomes.firstOther;
    }
  }

  private static final class CommandOutcomes {
    int ok;
    int moved;
    int connectionFailed;
    int other;
    Throwable firstOther;

    @Override
    public String toString() {
      return "ok=" + ok + ", moved=" + moved + ", connectionFailed=" + connectionFailed + ", other="
          + other + (firstOther == null ? "" : " (" + firstOther + ")");
    }
  }
}
