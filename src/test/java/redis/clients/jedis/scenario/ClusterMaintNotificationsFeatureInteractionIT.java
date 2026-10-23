package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.JedisClusterInfoCache.getNodeKey;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintNotificationsTestSupport;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvent;
import redis.clients.jedis.MaintNotificationsTestSupport.SlotMove;
import redis.clients.jedis.exceptions.JedisException;

/**
 * The delta path interacting with another client feature, each case with its own client on one
 * shared slot-shuffle / migrate database: repeated windows on one client (S3.6), periodic topology
 * refresh (S3.7). A slot-shuffle migrate on this layout can be fired indefinitely (the FI picks a
 * node owning 2+ shards each time, and no node is ever emptied), so every method fires its own and
 * the database is provisioned once. Client-side caching (S3.8) and sharded pub/sub (S9) are not
 * exercised here: on Redis Enterprise the DMC serves CLIENT TRACKING and pub/sub on dedicated
 * connections it closes when the shard leaves, so the pinned-connection observations of this
 * harness do not apply to them; the mock tests (C9, G1) keep the client-side behaviour pinned.
 * <p>
 * Shared-database rules: methods run sequentially (never enable parallel execution here); each
 * method starts with a server-side gate on the previous method's action; the base teardown joins an
 * in-flight effect before the next method starts; keys carry a per-method prefix.
 */
@Tag("scenario")
public class ClusterMaintNotificationsFeatureInteractionIT
    extends ClusterMaintNotificationsScenarioBase {

  private static final Logger log = LoggerFactory
      .getLogger(ClusterMaintNotificationsFeatureInteractionIT.class);

  private static final int REPEATED_RUNS = 3;
  private static final Duration REFRESH_PERIOD = Duration.ofSeconds(2);
  private static final Duration GATE_TIMEOUT = Duration.ofSeconds(30);

  private static Scenario sharedScenario;
  private static Database shared;
  /** The FI action of the previous method, for the gate; cleared with the database. */
  private static String lastActionId;

  @BeforeAll
  static void createSharedDatabase() {
    sharedScenario = freshScenario(SlotMigrateEffect.SLOT_SHUFFLE, "migrate", "external/ip");
    // assign before any further step so @AfterAll cleans up even on a later failure
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

  /** Runs before the base joins the effect thread, which is when the id would be cleared. */
  @AfterEach
  void rememberEffect() {
    if (lastEffectActionId != null) {
      lastActionId = lastEffectActionId;
    }
  }

  private void fire() {
    startEffect(shared, sharedScenario, Collections.emptyMap());
  }

  /**
   * S3.6 Three migrations on one client and one set of pinned connections: each run's deltas are
   * applied on top of the previous state (S3.1), the node set never changes (S3.2), every window
   * closes (no open window after each run), and the sequence ids a connection reads only ever
   * increase.
   */
  @Test
  @Timeout(600)
  void repeatedMigrationsOnOneClient() {
    openObserver(shared);
    openVerificationClient(shared);
    pinOneConnectionPerNode();
    Set<String> nodes = nodeKeys(client);

    for (int run = 1; run <= REPEATED_RUNS; run++) {
      HostAndPort[] ownerBefore = ownersBySlot(client);
      long seqFloor = maxSeqRecorded();

      fire();
      awaitSMigratingAfter(seqFloor);
      awaitEffectSettled(nodes.size());
      joinEffectThreads();
      assertEffectSucceeded();

      List<SlotMove> moves = distinctMoves(eventsAfter(seqFloor));
      HostAndPort[] ownerAfter = ownersBySlot(client);
      log.info("run {}: moves {}", run, moves);
      assertFalse(moves.isEmpty(), "run " + run + " produced no SMIGRATED delta");
      List<String> mismatches = ownerMismatches(ownerBefore, ownerAfter, moves);
      assertTrue(mismatches.isEmpty(), "run " + run + ": " + mismatches);
      assertEquals(nodes, nodeKeys(client), "run " + run + ": node set changed");
      assertEquals(0, MaintNotificationsTestSupport.openMigrationWindows(client),
        "run " + run + ": a window was left to the TTL (closer lost)");
      assertFalse(MaintNotificationsTestSupport.hasActiveMigration(client),
        "run " + run + ": migration still active");
      recorded.forEach((node, events) -> assertSeqIncreasing(node, events.all()));
    }
    verification.refreshClusterTopology();
    assertEquals(nodeKeys(verification), nodeKeys(client), "server truth after the runs");
  }

  /**
   * S3.7, mock C8 and C11. A client refreshing its topology every 2 s while deltas arrive: both
   * paths write the same slot map, so every command lands on the current owner with its single
   * attempt (no MOVED, no failed connection) and the final owners equal a fresh CLUSTER SLOTS.
   */
  @Test
  @Timeout(600)
  void deltasInterleaveWithPeriodicRefresh() {
    openObserver(shared, config -> {
    }, builder -> builder.topologyRefreshPeriod(REFRESH_PERIOD));
    openVerificationClient(shared);
    pinOneConnectionPerNode();
    Set<String> nodes = nodeKeys(client);
    List<String> keys = keysAcrossRanges("refresh", shardRanges(sharedScenario, client));

    fire();
    int commands = 0;
    List<String> failures = new ArrayList<>();
    while (!effectDone()) {
      for (String key : keys) {
        try {
          client.set(key, "v");
          assertEquals("v", client.get(key));
          commands += 2;
        } catch (JedisException e) {
          failures.add(key + ": " + e);
        }
      }
      observeAll();
      sleepQuietly(50);
    }
    awaitEffectSettled(nodes.size());
    log.info("{} commands during the effect, {} failures: {}", commands, failures.size(), failures);

    assertTrue(failures.isEmpty(), "commands that did not land with one attempt: " + failures);
    verification.refreshClusterTopology();
    HostAndPort[] serverOwners = ownersBySlot(verification);
    HostAndPort[] clientOwners = ownersBySlot(client);
    for (int slot = 0; slot < clientOwners.length; slot++) {
      assertEquals(getNodeKey(serverOwners[slot]), getNodeKey(clientOwners[slot]),
        "owner of slot " + slot + " differs from a fresh CLUSTER SLOTS");
    }
  }

  private static void assertSeqIncreasing(HostAndPort node, List<ReceivedEvent> events) {
    for (int i = 1; i < events.size(); i++) {
      assertTrue(events.get(i).seq > events.get(i - 1).seq,
        "seq not increasing on " + node + ": " + events);
    }
  }
}
