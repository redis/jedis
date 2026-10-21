package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.util.ClusterKeyRegistry.keyMatching;

import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.redis.test.fi.Scenario;
import com.redis.test.fi.SlotMigrateEffect;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintNotificationsTestSupport;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedClusterEvent;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvents;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.PushMessageTypes;

/**
 * Relaxed timeouts proven behaviourally, and connections opened while a window is open: S2.3 (a
 * blocking command outlives its base timeout inside the window and not after), S2.4 (relaxation
 * never tightens a larger configured timeout), S4.1 (a connection created mid-window is relaxed
 * from its first command, including one to the departing node). Each case provisions its own
 * database: S2.3 and S4.1 need the long window of remove-add / migrate, S2.4 a client configured
 * differently.
 */
@Tag("scenario")
public class ClusterMaintNotificationsTimeoutIT extends ClusterMaintNotificationsScenarioBase {

  private static final Logger log = LoggerFactory
      .getLogger(ClusterMaintNotificationsTimeoutIT.class);

  /** Server-side BLPOP delay: longer than the blocking base, far shorter than the relaxed value. */
  private static final int PROBE_DELAY_SECONDS = 3;
  private static final int LARGE_SOCKET_TIMEOUT_MS = 60_000;

  /** All shards of a node move: the longest window the rladmin triggers produce. */
  static Scenario longWindow() {
    return CLUSTER_CATALOG.effect(SlotMigrateEffect.REMOVE_ADD).trigger("migrate")
        .scenario("external/ip");
  }

  static Scenario oneShard() {
    return CLUSTER_CATALOG.effect(SlotMigrateEffect.SLOT_SHUFFLE).trigger("migrate")
        .scenario("external/ip");
  }

  /**
   * S2.3. BLPOP on a missing key in a slot that is not moving returns nil only when its server-side
   * delay expires: inside the window the relaxed blocking timeout (30 s) lets it wait the full 3 s,
   * where the 2 s base would have failed it; after the window the base is back and the same probe
   * times out on the socket between 2 and 3 s. The probe's own connection may read the SMIGRATED
   * inline while blocked, which is harmless: its socket timeout was set when the command started,
   * so the probe's success is the proof and the gate state afterwards is only logged.
   */
  @Test
  @Timeout(600)
  void blockingCommandSucceedsInsideTheWindow() {
    Scenario scenario = longWindow();
    openObserver(deployDatabase(scenario));
    Set<String> before = nodeKeys(client);
    HostAndPort[] ownerBefore = ownersBySlot(client);
    pinOneConnectionPerNode();

    startEffect(scenario);
    awaitFirstSMigrating();
    assertWindowOpen("S2.3 precondition");
    String key = keyOutsideAnnouncedSlots(ownerBefore);
    warm(key);

    Probe inside = runBlockingProbe(key);
    boolean stillOpen = MaintNotificationsTestSupport.hasActiveMigration(client);
    log.info("{}: probe inside the window: {}; window still open afterwards: {}", scenario, inside,
      stillOpen);
    assertNull(inside.error, "inside the window the probe must not time out (gate open at start,"
        + " still open afterwards: " + stillOpen + "): " + inside);
    assertNull(inside.reply, "nil proves the server held the probe for its full delay: " + inside);
    assertTrue(
      inside.durationMillis >= MaintNotificationsObserverBase.CLIENT_BLOCKING_SOCKET_TIMEOUT_MS,
      "the probe returned before the blocking base would have fired: " + inside);

    awaitEffectSettled(before.size());
    assertFalse(MaintNotificationsTestSupport.hasActiveMigration(client), "window still open");
    warm(key);

    Probe after = runBlockingProbe(key);
    log.info("{}: probe after the window: {}", scenario, after);
    assertNotNull(after.error, "after the window the probe must time out at the base: " + after);
    assertTrue(timedOutOnSocket(after.error), "expected a socket timeout, got " + after);
    assertTrue(
      after.durationMillis >= MaintNotificationsObserverBase.CLIENT_BLOCKING_SOCKET_TIMEOUT_MS
          && after.durationMillis < TimeUnit.SECONDS.toMillis(PROBE_DELAY_SECONDS),
      "the blocking base must be in force again: " + after);
  }

  /**
   * S2.4, mock B7. A socket timeout above the relaxed value is never lowered: the effective timeout
   * stays at the configured 60 s before, during and after the window.
   */
  @Test
  @Timeout(600)
  void relaxationNeverTightensALargerTimeout() {
    Scenario scenario = oneShard();
    openObserver(deployDatabase(scenario),
      config -> config.socketTimeoutMillis(LARGE_SOCKET_TIMEOUT_MS), builder -> {
      });
    Set<String> before = nodeKeys(client);
    pinOneConnectionPerNode();
    assertEffectiveTimeoutEverywhere(LARGE_SOCKET_TIMEOUT_MS, "before the effect");

    startEffect(scenario);
    awaitFirstSMigrating();
    assertEffectiveTimeoutEverywhere(LARGE_SOCKET_TIMEOUT_MS, "inside the window");
    // the value above was read under an open window: no closer consumed yet, gate still open
    assertWindowOpen("S2.4");

    awaitEffectSettled(before.size());
    observeAll();
    assertEffectiveTimeoutEverywhere(LARGE_SOCKET_TIMEOUT_MS, "after the window");
  }

  /**
   * S4.1, mock A6 and F3. While the window is open, one more connection per node is borrowed, and
   * that borrow necessarily creates a connection: before it, the only pooled connection ever
   * returned idle is the seed node's discovery connection (every other pool is created lazily and
   * {@code minIdle} is 0), and the pinned borrow took it. Its handshake therefore runs under the
   * open gate: it is relaxed from its first command. Whether the server also sends it the
   * SMIGRATING is logged, not asserted. After the effect the one to the departing node returns to
   * its destroyed pool without throwing and the others are back at the base.
   */
  @Test
  @Timeout(600)
  void connectionsOpenedDuringTheWindow() {
    Scenario scenario = longWindow();
    openObserver(deployDatabase(scenario));
    Set<String> before = nodeKeys(client);
    Map<String, ConnectionPool> poolsBefore = new LinkedHashMap<>(client.getClusterNodes());
    HostAndPort[] ownerBefore = ownersBySlot(client);
    pinOneConnectionPerNode();

    startEffect(scenario);
    awaitFirstSMigrating();
    assertWindowOpen("S4.1 precondition");

    Map<String, Connection> fresh = new LinkedHashMap<>();
    Map<String, ReceivedEvents> freshEvents = new LinkedHashMap<>();
    for (String node : before) {
      Connection connection = client
          .getConnectionFromSlot(slotOwnedBy(ownerBefore, node, s -> false));
      fresh.put(node, connection);
      freshEvents.put(node, MaintNotificationsTestSupport.record(connection));
    }
    Observations obs = new Observations();
    fresh.forEach(
      (node, connection) -> obs.check("S4.1 fresh connection to " + node + " is relaxed", () -> {
        connection.executeCommand(Protocol.Command.PING);
        assertTrue(ConnectionTestHelper.isRelaxedTimeoutActive(connection), "not relaxed");
        assertEquals(MaintNotificationsObserverBase.RELAXED_TIMEOUT_MS,
          MaintNotificationsTestSupport.effectiveTimeoutMillis(connection), "effective timeout");
        log.info("{}: fresh connection to {} received SMIGRATING itself: {}", scenario, node,
          freshEvents.get(node).sMigratingReceived());
      }));

    awaitEffectSettled(before.size());
    Set<String> after = nodeKeys(client);
    Set<String> removed = before.stream().filter(n -> !after.contains(n))
        .collect(Collectors.toSet());
    fresh.forEach((node, connection) -> {
      if (removed.contains(node)) {
        obs.check("S4.1 fresh connection to departed " + node + " returns to its destroyed pool",
          () -> {
            connection.close();
            assertTrue(poolsBefore.get(node).isClosed(), "pool of departed node still open");
          });
      } else {
        obs.check("S4.1 fresh connection to " + node + " back at the base", () -> {
          connection.executeCommand(Protocol.Command.PING);
          assertFalse(ConnectionTestHelper.isRelaxedTimeoutActive(connection), "still relaxed");
          assertEquals(MaintNotificationsObserverBase.CLIENT_SOCKET_TIMEOUT_MS,
            MaintNotificationsTestSupport.effectiveTimeoutMillis(connection), "effective timeout");
          connection.close();
        });
      }
    });
    obs.assertAllPassed();
  }

  // --- helpers ---

  /** A key in a slot no SMIGRATING announced, so its commands never meet the move itself. */
  private String keyOutsideAnnouncedSlots(HostAndPort[] ownerBefore) {
    List<ReceivedClusterEvent> openings = allEvents().stream()
        .filter(e -> PushMessageTypes.SMIGRATING.equals(e.type)).map(e -> (ReceivedClusterEvent) e)
        .collect(Collectors.toList());
    return keyMatching(slot -> ownerBefore[slot] != null
        && openings.stream().noneMatch(opening -> opening.contains(slot)),
      "probe");
  }

  private void assertEffectiveTimeoutEverywhere(int expected, String when) {
    for (Map.Entry<HostAndPort, Connection> entry : livePinned().entrySet()) {
      assertEquals(expected, MaintNotificationsTestSupport.effectiveTimeoutMillis(entry.getValue()),
        when + ": effective timeout on " + entry.getKey());
    }
  }

  /**
   * One non-blocking command on the probe's slot, so a pool connection that has to be created (or
   * replaced after a timeout broke the previous one) is created here and not inside the timing.
   */
  private void warm(String key) {
    client.get(key);
  }

  /** BLPOP through the observer, flagged blocking so the blocking timeouts apply. */
  private Probe runBlockingProbe(String key) {
    CommandObject<List<String>> blpop = new CommandObject<>(
        new CommandArguments(Protocol.Command.BLPOP).key(key).add(PROBE_DELAY_SECONDS).blocking(),
        BuilderFactory.STRING_LIST);
    long start = System.nanoTime();
    try {
      return new Probe(client.executeCommand(blpop), null, elapsedMillis(start));
    } catch (RuntimeException e) {
      return new Probe(null, e, elapsedMillis(start));
    }
  }

  /**
   * The cluster executor wraps a connection failure; the socket timeout is somewhere in the chain.
   */
  private static boolean timedOutOnSocket(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause()) {
      if (cause instanceof SocketTimeoutException) {
        return true;
      }
    }
    return false;
  }

  private static final class Probe {
    final List<String> reply;
    final Throwable error;
    final long durationMillis;

    Probe(List<String> reply, Throwable error, long durationMillis) {
      this.reply = reply;
      this.error = error;
      this.durationMillis = durationMillis;
    }

    @Override
    public String toString() {
      return (error != null ? "failure(" + error + ")"
          : reply == null ? "nil reply" : "reply " + reply) + " after " + durationMillis + " ms";
    }
  }
}
