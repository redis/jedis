package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static redis.clients.jedis.JedisClusterInfoCache.getNodeKey;
import static redis.clients.jedis.util.ClusterKeyRegistry.keyInRange;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.redis.test.fi.Database;
import com.redis.test.fi.Scenario;
import com.redis.test.fi.SlotMigrateEffect;
import com.redis.test.fi.TriggerCatalog;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.function.Executable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintNotificationsTestSupport;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedClusterEvent;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvent;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvents;
import redis.clients.jedis.MaintNotificationsTestSupport.SlotMove;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.PushMessageTypes;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.SslOptions;
import redis.clients.jedis.builders.ClusterClientBuilder;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Toolkit for scenario tests that observe SMIGRATING / SMIGRATED on a {@link RedisClusterClient}
 * against an OSS-cluster database: an observer client tuned so relaxed and restored timeouts are
 * observable and every redirect surfaces as an exception, one pinned connection per node that reads
 * the pushes, a verification client reading the server-side truth, and the settle logic for an
 * effect whose deltas arrive as a cascade.
 */
abstract class ClusterMaintNotificationsScenarioBase extends FaultInjectorScenarioBase {

  private static final Logger logger = LoggerFactory
      .getLogger(ClusterMaintNotificationsScenarioBase.class);

  /** The full slot-migrate catalog, resolved once: effect -> triggers -> requirements. */
  static final TriggerCatalog CLUSTER_CATALOG = TriggerCatalog.resolve(faultInjector,
    SlotMigrateEffect.class);

  /** No new event for this long after the FI reported the effect done = the stream is drained. */
  static final long SETTLE_MILLIS = 3_000;
  /** ASM plans before the first slot moves; rladmin triggers announce within seconds. */
  static final long FIRST_PUSH_TIMEOUT_MS = 150_000;
  /** Node-set convergence after the stream drained (ASM scale effects trail the last push). */
  static final long CONVERGE_TIMEOUT_MS = 120_000;
  private static final long PUMP_INTERVAL_MS = 100;
  /** While waiting for the opener: short enough to read it before its closer is buffered too. */
  private static final long FIRST_PUSH_POLL_MS = 10;

  /** The observer client under test; closed on teardown before the database is released. */
  RedisClusterClient client;
  /** Maintenance notifications off; only reads {@code CLUSTER SLOTS} as the server-side truth. */
  RedisClusterClient verification;
  /** Further clients a test opened through {@link #openClient}; closed on teardown. */
  final List<RedisClusterClient> extraClients = new ArrayList<>();

  /** One borrowed connection per node, keyed by node; pushes are read through these. */
  final Map<HostAndPort, Connection> pinned = new LinkedHashMap<>();
  final Map<HostAndPort, ReceivedEvents> recorded = new LinkedHashMap<>();
  /** Pinned connections the server closed (expected for a departing node after SMIGRATED). */
  final Set<HostAndPort> dropped = new LinkedHashSet<>();

  // --- clients ---

  /** Notifications on, both relaxed timeouts far above the base ones. */
  static MaintenanceNotificationsConfig observerMaintenance() {
    return MaintNotificationsObserverBase.relaxedMaintenance().autoResolveEndpointType().build();
  }

  /**
   * Opens the observer: RESP3 with the low base timeouts of the standalone observer, pool of three
   * per node (one pinned + two probes), no periodic refresh, and {@code maxAttempts(1)} so every
   * redirect and failed connection reaches the test as an exception it can count instead of a
   * silent retry.
   */
  RedisClusterClient openObserver(Database database) {
    return openObserver(database, config -> {
    }, builder -> {
    });
  }

  /**
   * As {@link #openObserver(Database)}, with a test's own additions applied last: to the client
   * config (e.g. a larger socket timeout) and to the builder (e.g. a periodic refresh or a cache).
   */
  RedisClusterClient openObserver(Database database,
      Consumer<DefaultJedisClientConfig.Builder> clientConfig,
      Consumer<ClusterClientBuilder<RedisClusterClient>> builder) {
    GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(3);
    client = build(database, observerMaintenance(), config -> {
      MaintNotificationsObserverBase.observerClientConfig(config);
      clientConfig.accept(config);
    }, b -> {
      b.poolConfig(poolConfig).maxAttempts(1);
      builder.accept(b);
    });
    return client;
  }

  /**
   * Opens a further client on the database and registers it for teardown. RESP3 (the notifications
   * need it) plus the database credentials and TLS trust; everything else is stock unless the
   * consumers add it. Used for stock traffic clients and the handshake matrix.
   */
  RedisClusterClient openClient(Database database, MaintenanceNotificationsConfig maintenance,
      Consumer<DefaultJedisClientConfig.Builder> clientConfig,
      Consumer<ClusterClientBuilder<RedisClusterClient>> builder) {
    RedisClusterClient opened = build(database, maintenance, config -> {
      config.protocol(RedisProtocol.RESP3);
      clientConfig.accept(config);
    }, builder);
    extraClients.add(opened);
    return opened;
  }

  private RedisClusterClient build(Database database, MaintenanceNotificationsConfig maintenance,
      Consumer<DefaultJedisClientConfig.Builder> clientConfig,
      Consumer<ClusterClientBuilder<RedisClusterClient>> builder) {
    DefaultJedisClientConfig.Builder config = clientConfig(database);
    clientConfig.accept(config);
    ClusterClientBuilder<RedisClusterClient> b = RedisClusterClient.builder().nodes(seed(database))
        .clientConfig(config.build()).maintenanceNotifications(maintenance);
    builder.accept(b);
    return b.build();
  }

  /** Opens the verification client: stock settings, maintenance notifications disabled. */
  RedisClusterClient openVerificationClient(Database database) {
    verification = RedisClusterClient.builder().nodes(seed(database))
        .clientConfig(clientConfig(database).build())
        .maintenanceNotifications(MaintenanceNotificationsConfig.DISABLED).build();
    return verification;
  }

  /**
   * Credentials and pinned TLS trust for {@code database}; protocol and timeouts left to the
   * caller.
   */
  DefaultJedisClientConfig.Builder clientConfig(Database database) {
    DefaultJedisClientConfig.Builder config = database.clientConfig();
    SslOptions ssl = pinnedSslOptions(database);
    if (ssl != null) {
      config.sslOptions(ssl);
    }
    return config;
  }

  private static Set<HostAndPort> seed(Database database) {
    return Collections.singleton(new HostAndPort(database.host(), database.port()));
  }

  // --- shared databases ---

  /**
   * One catalog row from a fresh discovery. The FI names and ports every discovered dbconfig
   * uniquely, so a database is never created from a dbconfig that already produced one.
   */
  static Scenario freshScenario(SlotMigrateEffect effect, String trigger, String config) {
    return faultInjector.discover(effect).trigger(trigger).scenario(config);
  }

  /**
   * Provisions a database a whole class shares (released by the class, not by the per-test
   * teardown, which only knows {@link #database}).
   */
  static Database provisionShared(Scenario scenario) {
    Database shared = faultInjector.provision(scenario);
    awaitEndpointConnectable(shared.endpoint());
    return shared;
  }

  /**
   * Gate for a method on a shared database: the previous method's effect must be over on the
   * server. A fresh client's own state cannot tell (it has read no push), so the FI is asked.
   */
  static void assertNoEffectRunning(String actionId) {
    if (actionId == null) {
      return;
    }
    String status = faultInjector.actionStatus(actionId);
    assertFalse("running".equals(status) || "pending".equals(status),
      "the previous effect (" + actionId + ") is still " + status);
  }

  // --- topology snapshots ---

  /** The node keys ({@code host:port}) the client currently has pools for. */
  static Set<String> nodeKeys(RedisClusterClient client) {
    return new LinkedHashSet<>(client.getClusterNodes().keySet());
  }

  /** The client's current owner of every slot. */
  static HostAndPort[] ownersBySlot(RedisClusterClient client) {
    HostAndPort[] owners = new HostAndPort[Protocol.CLUSTER_HASHSLOTS];
    for (int slot = 0; slot < owners.length; slot++) {
      owners[slot] = MaintNotificationsTestSupport.slotOwner(client, slot);
    }
    return owners;
  }

  /** The first slot the owner table routes to {@code node}, or -1. */
  static int slotOwnedBy(HostAndPort[] owners, String nodeKey, IntPredicate exclude) {
    for (int slot = 0; slot < owners.length; slot++) {
      if (owners[slot] != null && nodeKey.equals(getNodeKey(owners[slot])) && !exclude.test(slot)) {
        return slot;
      }
    }
    return -1;
  }

  /**
   * The maximal runs of consecutive slots with one owner, as inclusive {@code [from, to]} pairs:
   * the shard layout as the client sees it. One key or channel per range reaches every shard.
   */
  static List<int[]> ownerRanges(HostAndPort[] owners) {
    List<int[]> ranges = new ArrayList<>();
    int from = 0;
    for (int slot = 1; slot <= owners.length; slot++) {
      if (slot == owners.length || !sameNode(owners[slot], owners[from])) {
        ranges.add(new int[] { from, slot - 1 });
        from = slot;
      }
    }
    return ranges;
  }

  private static boolean sameNode(HostAndPort a, HostAndPort b) {
    return a == null ? b == null : b != null && getNodeKey(a).equals(getNodeKey(b));
  }

  /**
   * One key per owner range, so a round over them touches every shard. The range's registered key
   * carries {@code name}, so callers with different names get distinct keys in the same slots.
   */
  static List<String> keysAcrossRanges(String name, HostAndPort[] owners) {
    List<String> keys = new ArrayList<>();
    for (int[] range : ownerRanges(owners)) {
      keys.add(keyInRange(range[0], range[1], name));
    }
    return keys;
  }

  // --- deltas ---

  /**
   * Every SMIGRATED entry read on any recorded connection, once each and in seq order. The same
   * push reaches every connection, so the recordings repeat each entry once per connection that
   * read it.
   */
  static List<SlotMove> distinctMoves(List<ReceivedEvent> events) {
    return events.stream().filter(event -> PushMessageTypes.SMIGRATED.equals(event.type))
        .sorted(Comparator.comparingLong(event -> event.seq))
        .flatMap(event -> ((ReceivedClusterEvent) event).moves.stream()).distinct()
        .collect(Collectors.toList());
  }

  /**
   * S3.1: every slot of every delta must route to the delta's destination, and every other slot
   * must keep its owner. Returns the violations, empty when the slot map followed the deltas.
   */
  static List<String> ownerMismatches(HostAndPort[] ownerBefore, HostAndPort[] ownerAfter,
      List<SlotMove> moves) {
    List<String> mismatches = new ArrayList<>();
    boolean[] moved = new boolean[ownerBefore.length];
    for (SlotMove move : moves) {
      move.forEachSlot(slot -> {
        moved[slot] = true;
        if (!getNodeKey(move.dest()).equals(getNodeKey(ownerAfter[slot]))) {
          mismatches.add("slot " + slot + " -> " + ownerAfter[slot] + ", expected " + move.dest());
        }
      });
    }
    for (int slot = 0; slot < ownerBefore.length; slot++) {
      if (!moved[slot] && !getNodeKey(ownerBefore[slot]).equals(getNodeKey(ownerAfter[slot]))) {
        mismatches.add("unmoved slot " + slot + " changed owner " + ownerBefore[slot] + " -> "
            + ownerAfter[slot]);
      }
    }
    return mismatches;
  }

  // --- pinned connections ---

  /**
   * Borrows (and keeps) one connection per node of the observer, each recorded, so maintenance
   * pushes arrive on connections the test controls. The handshake enables the notifications before
   * the effect fires.
   */
  void pinOneConnectionPerNode() {
    HostAndPort[] owners = ownersBySlot(client);
    for (String node : nodeKeys(client)) {
      int slot = slotOwnedBy(owners, node, s -> false);
      if (slot < 0) {
        fail("node " + node + " owns no slot; cannot pin a connection to it");
      }
      Connection connection = client.getConnectionFromSlot(slot);
      HostAndPort key = owners[slot];
      pinned.put(key, connection);
      recorded.put(key, MaintNotificationsTestSupport.record(connection));
    }
    logger.info("pinned one connection per node: {}", pinned.keySet());
  }

  /** Pinned connections still open. */
  Map<HostAndPort, Connection> livePinned() {
    Map<HostAndPort, Connection> live = new LinkedHashMap<>(pinned);
    live.keySet().removeAll(dropped);
    return live;
  }

  /** Observes every live pinned connection once; see {@link #observe}. */
  void observeAll() {
    for (Map.Entry<HostAndPort, Connection> entry : livePinned().entrySet()) {
      observe(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Observes the live pinned connections one at a time and stops right after the first one that
   * recorded a SMIGRATING, so the others are left unread since their previous observation. Returns
   * whether one did.
   */
  private boolean observeUntilSMigrating(long seqFloor) {
    for (Map.Entry<HostAndPort, Connection> entry : livePinned().entrySet()) {
      observe(entry.getKey(), entry.getValue());
      if (recordedSMigratingAfter(recorded.get(entry.getKey()), seqFloor)) {
        return true;
      }
    }
    return false;
  }

  private static boolean recordedSMigratingAfter(ReceivedEvents events, long seqFloor) {
    return events.all().stream()
        .anyMatch(e -> PushMessageTypes.SMIGRATING.equals(e.type) && e.seq > seqFloor);
  }

  /** The highest seq recorded on any pinned connection, or -1 before the first push. */
  long maxSeqRecorded() {
    return allEvents().stream().mapToLong(e -> e.seq).max().orElse(-1);
  }

  /**
   * The recorded events with a seq above {@code seqFloor}: one firing's worth on a reused client.
   */
  List<ReceivedEvent> eventsAfter(long seqFloor) {
    return allEvents().stream().filter(e -> e.seq > seqFloor).collect(Collectors.toList());
  }

  /**
   * Observes one pinned connection via a PING round-trip: buffered pushes are consumed inline
   * during the reply read, dispatching maintenance events on this thread. A connection the server
   * closed is recorded in {@link #dropped}; that is expected only for a departing node after the
   * SMIGRATED that removed it was seen somewhere — a drop before any SMIGRATED fails the test.
   */
  private void observe(HostAndPort node, Connection connection) {
    try {
      connection.executeCommand(Protocol.Command.PING);
    } catch (JedisConnectionException e) {
      dropped.add(node);
      if (!anyRecorded(ReceivedEvents::sMigratedReceived)) {
        fail("pinned connection to " + node + " dropped before any SMIGRATED; events: "
            + allEvents() + ", cause: " + e);
      }
      logger.info("pinned connection to {} dropped after SMIGRATED (expected for a departing node)",
        node);
    }
  }

  /** Pinned connections that have not read a SMIGRATING themselves. */
  Set<HostAndPort> pinnedWithoutSMigrating() {
    Set<HostAndPort> unread = new LinkedHashSet<>();
    recorded.forEach((node, events) -> {
      if (!events.sMigratingReceived()) {
        unread.add(node);
      }
    });
    return unread;
  }

  boolean anyRecorded(Predicate<ReceivedEvents> condition) {
    return recorded.values().stream().anyMatch(condition);
  }

  /** Every recorded event of every pinned connection, in per-connection dispatch order. */
  List<ReceivedEvent> allEvents() {
    List<ReceivedEvent> all = new ArrayList<>();
    recorded.values().forEach(events -> all.addAll(events.all()));
    return all;
  }

  /** Nanos of the most recent event on any pinned connection, or -1. */
  long lastEventAtNanos() {
    long last = -1;
    for (ReceivedEvents events : recorded.values()) {
      last = Math.max(last, events.lastEventAtNanos());
    }
    return last;
  }

  /**
   * Pumps the pinned connections one at a time until one of them recorded a SMIGRATING and stops
   * there, so the other connections have not read their own copy of the push yet: the state the
   * client-wide gate check (S2.1) needs. Fails when the effect completes without one.
   */
  void awaitFirstSMigrating() {
    awaitSMigratingAfter(-1);
  }

  /**
   * As {@link #awaitFirstSMigrating()} for a later firing on the same client: a push newer than
   * {@code seqFloor}.
   */
  void awaitSMigratingAfter(long seqFloor) {
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(FIRST_PUSH_TIMEOUT_MS);
    while (System.nanoTime() < deadline) {
      if (observeUntilSMigrating(seqFloor)) {
        return;
      }
      if (effectDone()) {
        assertEffectSucceeded();
        // the effect finished without a push being read yet: give the stream one more pass
        if (observeUntilSMigrating(seqFloor)) {
          return;
        }
        fail("effect completed but no SMIGRATING was received on any pinned connection: "
            + allEvents());
      }
      sleepQuietly(FIRST_PUSH_POLL_MS);
    }
    fail("no SMIGRATING within " + FIRST_PUSH_TIMEOUT_MS + " ms; effect failures: " + effectFailures
        + ", events: " + allEvents());
  }

  /**
   * The window the first SMIGRATING opened must still be open, on the record (no closer read yet)
   * and on the gate (what actually relaxes the connections). A closed window at this point is a
   * failure, never a reason to skip: every row this is used on produces a window of seconds or
   * more, so missing it means the FI, the server or the client's push handling misbehaved.
   */
  void assertWindowOpen(String what) {
    assertTrue(anyRecorded(ReceivedEvents::sMigratingReceived),
      what + ": no SMIGRATING recorded; events: " + allEvents());
    assertFalse(anyRecorded(ReceivedEvents::sMigratedReceived), what
        + ": a SMIGRATED was already read, the window closed before it could be observed; events: "
        + allEvents());
    assertTrue(MaintNotificationsTestSupport.hasActiveMigration(client),
      what + ": the client-wide gate is not open; events: " + allEvents());
  }

  /**
   * Waits for the effect to be over from the client's point of view, in three steps that must stay
   * in this order: the FI action completed (authoritative end of the cascade), then the pinned
   * connections were pumped until no event arrived for {@link #SETTLE_MILLIS}, then the client's
   * node count reached {@code expectedNodes}. The node count alone is not a stop condition: during
   * an ASM scale-out the new node gets its first shard early and more deltas follow, so a
   * count-only poll can return before the last SMIGRATED and race the unrelax assertions. Expected
   * SMIGRATED counts are never asserted.
   */
  void awaitEffectSettled(int expectedNodes) {
    while (!effectDone()) {
      observeAll();
      sleepQuietly(PUMP_INTERVAL_MS);
    }
    assertEffectSucceeded();

    long settleStart = System.nanoTime();
    while (true) {
      observeAll();
      long last = lastEventAtNanos();
      long quietSince = Math.max(last, settleStart);
      if (elapsedMillis(quietSince) >= SETTLE_MILLIS) {
        break;
      }
      sleepQuietly(PUMP_INTERVAL_MS);
    }

    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CONVERGE_TIMEOUT_MS);
    while (client.getClusterNodes().size() != expectedNodes) {
      if (System.nanoTime() >= deadline) {
        fail("node set did not converge to " + expectedNodes + " nodes within "
            + CONVERGE_TIMEOUT_MS + " ms; nodes: " + nodeKeys(client) + ", events: " + allEvents());
      }
      observeAll();
      sleepQuietly(PUMP_INTERVAL_MS);
    }
  }

  /**
   * Returns every pinned connection to its pool. Returning is never an error, also for a pool the
   * delta destroyed: the connection is discarded, no replacement is created.
   */
  void releasePinned() {
    for (Connection connection : pinned.values()) {
      try {
        connection.close();
      } catch (RuntimeException e) {
        logger.warn("releasing a pinned connection threw", e);
      }
    }
    pinned.clear();
    recorded.clear();
    dropped.clear();
  }

  @AfterEach
  final void tearDownClusterClients() {
    try {
      releasePinned();
    } finally {
      try {
        if (client != null) {
          client.close();
          client = null;
        }
      } finally {
        try {
          if (verification != null) {
            verification.close();
            verification = null;
          }
        } finally {
          for (RedisClusterClient extra : extraClients) {
            try {
              extra.close();
            } catch (RuntimeException e) {
              logger.warn("closing an extra client threw", e);
            }
          }
          extraClients.clear();
        }
      }
    }
  }

  // --- soft assertions ---

  /**
   * Collects assertion failures instead of throwing, so one failing observation does not hide the
   * independent ones taken on the same effect (gate, delta application, unrelax, traffic). Gates
   * that make the rest meaningless still fail fast through plain assertions.
   */
  static final class Observations {

    private final List<Throwable> failures = new ArrayList<>();
    private final Set<String> names = new HashSet<>();

    void check(String what, Executable assertion) {
      names.add(what);
      try {
        assertion.execute();
      } catch (Throwable t) {
        logger.warn("observation failed: {}: {}", what, t.getMessage());
        failures.add(new AssertionError(what + ": " + t.getMessage(), t));
      }
    }

    /** Throws one failure carrying every collected one; a no-op when all observations passed. */
    void assertAllPassed() {
      if (failures.isEmpty()) {
        logger.info("all {} observations passed: {}", names.size(), names);
        return;
      }
      List<Executable> rethrows = new ArrayList<>();
      for (Throwable failure : failures) {
        rethrows.add(() -> {
          throw failure;
        });
      }
      assertAll(failures.size() + " of " + names.size() + " observations failed", rethrows);
    }
  }
}
