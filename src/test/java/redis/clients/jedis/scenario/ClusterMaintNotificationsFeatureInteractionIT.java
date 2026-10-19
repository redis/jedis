package redis.clients.jedis.scenario;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisShardedPubSub;
import redis.clients.jedis.MaintNotificationsTestSupport;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvent;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvents;
import redis.clients.jedis.MaintNotificationsTestSupport.SlotMove;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisClusterCRC16;

/**
 * The delta path interacting with another client feature, each case with its own client on one
 * shared slot-shuffle / migrate database: repeated windows on one client (S3.6), periodic topology
 * refresh (S3.7), client-side caching (S3.8), a sharded subscription (S9). A slot-shuffle migrate
 * on this layout can be fired indefinitely (the FI picks a node owning 2+ shards each time, and no
 * node is ever emptied), so every method fires its own and the database is provisioned once.
 * <p>
 * Shared-database rules: methods run sequentially (never enable parallel execution here); each
 * method starts with a server-side gate on the previous method's action; the base teardown joins an
 * in-flight effect before the next method starts; keys and channels carry a per-method prefix.
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
   * S3.6 (go-redis ClusterMultipleActions). Three migrations on one client and one set of pinned
   * connections: each run's deltas are applied on top of the previous state (S3.1), the node set
   * never changes (S3.2), every window closes (no open window after each run), and the sequence ids
   * a connection reads only ever increase.
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
        "run " + run + ": windows left open");
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
    List<String> keys = keysAcrossRanges("refresh", ownersBySlot(client));

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
      assertEquals(nodeKey(serverOwners[slot]), nodeKey(clientOwners[slot]),
        "owner of slot " + slot + " differs from a fresh CLUSTER SLOTS");
    }
  }

  /**
   * S3.8, mock C9. A value cached before the move is not trusted across the delta: the cache is
   * flushed when the delta is applied, so the next read goes to the server and misses in the cache.
   */
  @Test
  @Timeout(600)
  void clientSideCacheIsFlushedByADelta() {
    openObserver(shared, config -> {
    }, builder -> builder.cacheConfig(CacheConfig.builder().maxSize(1_000).build()));
    pinOneConnectionPerNode();
    Set<String> nodes = nodeKeys(client);
    Cache cache = client.getCache();
    String key = "csc:0";

    client.set(key, "v");
    assertEquals("v", client.get(key));
    assertEquals("v", client.get(key));
    assertTrue(cache.getSize() > 0, "GET was not cached");
    assertTrue(cache.getStats().getHitCount() >= 1, "second GET was not a cache hit");
    long flushesBefore = cache.getStats().getFlushCount();

    fire();
    awaitFirstSMigrating();
    awaitEffectSettled(nodes.size());

    assertTrue(cache.getStats().getFlushCount() > flushesBefore,
      "the delta did not flush the cache");
    assertEquals(0, cache.getSize(), "entries survived the delta");
    long missesBefore = cache.getStats().getMissCount();
    assertEquals("v", client.get(key));
    assertEquals(missesBefore + 1, cache.getStats().getMissCount(),
      "the read after the delta was not served by the server");
  }

  /**
   * S9.1 and S9.2, mock G1. One sharded subscription per owner range, each on its own connection
   * borrowed from the observer, so the maintenance pushes a subscribed connection consumes inline
   * are recorded. Messages published before and after the move must both arrive, in order, on every
   * channel whose slot did not move; the gate opens from a subscribed connection's push (no pinned
   * connection is pumped here); and no window is left once the closers were consumed. The one
   * channel in the moved range (S9.2) has its messages logged, not asserted.
   */
  @Test
  @Timeout(600)
  void shardedPubSubSurvivesAMigration() throws InterruptedException {
    openObserver(shared);
    openVerificationClient(shared);
    Set<String> nodes = nodeKeys(client);
    List<Subscriber> subscribers = new ArrayList<>();
    for (int[] range : ownerRanges(ownersBySlot(client))) {
      subscribers.add(new Subscriber(
          keyInSlots("chan:" + subscribers.size(), s -> s >= range[0] && s <= range[1])));
    }
    try {
      for (Subscriber subscriber : subscribers) {
        subscriber.start();
      }
      publishAndAwait(subscribers, "before");

      fire();
      awaitGateOpenedByASubscriber();
      awaitEffectSettled(nodes.size());
      await().atMost(GATE_TIMEOUT).pollInterval(Duration.ofMillis(100))
          .until(() -> !MaintNotificationsTestSupport.hasActiveMigration(client)
              && MaintNotificationsTestSupport.openMigrationWindows(client) == 0);

      publishAndAwait(subscribers, "after");

      List<ReceivedEvent> events = new ArrayList<>();
      subscribers.forEach(s -> events.addAll(s.events.all()));
      List<SlotMove> moves = distinctMoves(events);
      assertFalse(moves.isEmpty(), "no subscribed connection recorded a SMIGRATED");
      assertTrue(subscribers.stream().anyMatch(s -> s.events.sMigratingReceived()),
        "no subscribed connection recorded a SMIGRATING");
      for (Subscriber subscriber : subscribers) {
        boolean moved = moves.stream().anyMatch(m -> m.contains(subscriber.slot));
        log.info("channel {} (slot {}{}): messages {}, pushes {}", subscriber.channel,
          subscriber.slot, moved ? ", moved" : "", subscriber.messages, subscriber.events.all());
        if (!moved) {
          assertEquals(Arrays.asList("before", "after"), new ArrayList<>(subscriber.messages),
            "S9.1 channel " + subscriber.channel);
        }
      }
    } finally {
      for (Subscriber subscriber : subscribers) {
        subscriber.stop();
      }
    }
  }

  private void awaitGateOpenedByASubscriber() {
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(FIRST_PUSH_TIMEOUT_MS);
    while (System.nanoTime() < deadline) {
      if (MaintNotificationsTestSupport.hasActiveMigration(client)) {
        log.info("gate opened from a subscribed connection's SMIGRATING");
        return;
      }
      if (effectDone()) {
        // the window opened and closed between two polls (sub-second move): nothing to see here
        log.info("effect finished before the open gate could be observed");
        return;
      }
      sleepQuietly(50);
    }
  }

  private void publishAndAwait(List<Subscriber> subscribers, String message) {
    for (Subscriber subscriber : subscribers) {
      verification.spublish(subscriber.channel, message);
    }
    for (Subscriber subscriber : subscribers) {
      try {
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(50))
            .until(() -> subscriber.messages.contains(message));
      } catch (RuntimeException e) {
        log.warn("channel {} did not deliver '{}' within 10 s; received {}", subscriber.channel,
          message, subscriber.messages);
      }
    }
  }

  private static void assertSeqIncreasing(HostAndPort node, List<ReceivedEvent> events) {
    for (int i = 1; i < events.size(); i++) {
      assertTrue(events.get(i).seq > events.get(i - 1).seq,
        "seq not increasing on " + node + ": " + events);
    }
  }

  /** One sharded subscription on its own recorded connection, reading on its own thread. */
  private final class Subscriber {
    final String channel;
    final int slot;
    final List<String> messages = Collections.synchronizedList(new ArrayList<>());
    final CountDownLatch subscribed = new CountDownLatch(1);
    final AtomicReference<Throwable> failure = new AtomicReference<>();
    Connection connection;
    ReceivedEvents events;
    Thread thread;
    final JedisShardedPubSub pubSub = new JedisShardedPubSub() {
      @Override
      public void onSMessage(String channel, String message) {
        messages.add(message);
      }

      @Override
      public void onSSubscribe(String channel, int subscribedChannels) {
        subscribed.countDown();
      }
    };

    Subscriber(String channel) {
      this.channel = channel;
      this.slot = JedisClusterCRC16.getSlot(channel);
    }

    void start() throws InterruptedException {
      connection = client.getConnectionFromSlot(slot);
      events = MaintNotificationsTestSupport.record(connection);
      thread = new Thread(() -> {
        try {
          pubSub.proceed(connection, channel);
        } catch (Throwable t) {
          failure.set(t);
        }
      }, "ssubscribe-" + channel);
      thread.start();
      assertTrue(subscribed.await(10, TimeUnit.SECONDS), "not subscribed to " + channel);
    }

    void stop() throws InterruptedException {
      if (pubSub.isSubscribed()) {
        pubSub.sunsubscribe();
      }
      if (thread != null) {
        thread.join(TimeUnit.SECONDS.toMillis(10));
      }
      if (connection != null) {
        connection.close();
      }
      if (failure.get() != null) {
        log.warn("subscriber {} ended with {}", channel, failure.get().toString());
      }
    }
  }
}
