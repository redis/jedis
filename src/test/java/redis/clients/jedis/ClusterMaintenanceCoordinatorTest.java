package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.TimeoutSource.TimeoutInfo;

/**
 * Unit tests for {@link ClusterMaintenanceCoordinator}: window bookkeeping against the
 * SMIGRATING/SMIGRATED broadcast (seq ids are unique per message, never shared by a pair) and the
 * dedup of slot-delta application.
 */
@ExtendWith(MockitoExtension.class)
public class ClusterMaintenanceCoordinatorTest {

  private static final HostAndPort NODE_A = new HostAndPort("127.0.0.1", 7000);
  private static final HostAndPort NODE_B = new HostAndPort("127.0.0.1", 7001);

  @Mock
  private JedisClusterInfoCache cache;
  @Mock
  private Connection conn;
  @Mock
  private Connection otherConn;

  private ClusterMaintenanceCoordinator coordinator;

  @BeforeEach
  public void setUp() {
    coordinator = new ClusterMaintenanceCoordinator(cache,
        MaintenanceNotificationsConfig.builder().build());
  }

  @Test
  public void pairWithDistinctSeqsClosesWindowAndAppliesOnce() {
    coordinator.onSMigrating(migrating(1, "0-100"), conn);
    assertTrue(coordinator.hasActiveMigration());

    // the closer is broadcast on every node connection; only its first delivery applies
    SMigratedEvent closer = migrated(2, "0-100");
    coordinator.onSMigrated(closer, conn);
    coordinator.onSMigrated(closer, otherConn);

    assertFalse(coordinator.hasActiveMigration());
    verify(cache, times(1)).applySlotMigration(closer.migrations);
  }

  @Test
  public void closerClosesOnlyTheOldestWindowBelowItsSeq() {
    coordinator.onSMigrating(migrating(1, "0-100"), conn);
    coordinator.onSMigrating(migrating(2, "200-300"), conn);

    // the first migration completes while the second is still in progress
    coordinator.onSMigrated(migrated(3, "0-100"), conn);
    assertTrue(coordinator.hasActiveMigration(), "overlapping migration must stay relaxed");

    coordinator.onSMigrated(migrated(4, "200-300"), conn);
    assertFalse(coordinator.hasActiveMigration());
    verify(cache, times(2)).applySlotMigration(anyList());
  }

  @Test
  public void duplicateCloserDoesNotCloseAnotherWindow() {
    coordinator.onSMigrating(migrating(1, "0-100"), conn);
    coordinator.onSMigrating(migrating(2, "200-300"), conn);

    SMigratedEvent closer = migrated(3, "0-100");
    coordinator.onSMigrated(closer, conn);
    coordinator.onSMigrated(closer, otherConn); // broadcast copy of the same closer

    assertTrue(coordinator.hasActiveMigration(), "second window must survive the duplicate");
    verify(cache, times(1)).applySlotMigration(anyList());
  }

  @Test
  public void lateOpenerAfterItsCloserOpensAWindowTheNextCloserRemoves() {
    coordinator.onSMigrated(migrated(2, "0-100"), conn);
    // seqs cannot pair an opener with its closer, so a reordered opener is not rejected: it opens
    // a window that lingers until the next closer takes it as the oldest, or the TTL expires
    coordinator.onSMigrating(migrating(1, "0-100"), otherConn);
    assertTrue(coordinator.hasActiveMigration());

    coordinator.onSMigrated(migrated(3, "200-300"), conn);
    assertFalse(coordinator.hasActiveMigration());
  }

  @Test
  public void redeliveredOpenerDoesNotReopenAClosedWindow() {
    SMigratingEvent opener = migrating(1, "0-100");
    coordinator.onSMigrating(opener, conn);
    coordinator.onSMigrated(migrated(2, "0-100"), conn);
    assertFalse(coordinator.hasActiveMigration());

    // the opener's broadcast copy from a slower connection lands after the window closed
    coordinator.onSMigrating(opener, otherConn);
    assertFalse(coordinator.hasActiveMigration(), "a seen opener must not relax again");
    verify(otherConn).applyCurrentTimeout();
  }

  @Test
  public void lateCloserIsAppliedAsIsAndClosesAWindow() {
    coordinator.onSMigrating(migrating(1, "0-100"), conn);
    coordinator.onSMigrated(migrated(3, "200-300"), conn);
    coordinator.onSMigrated(migrated(4, "400-500"), conn);

    // seq 2 arrives after newer closers: no ordering filter, its delta is applied as delivered
    SMigratedEvent late = migrated(2, "0-100");
    coordinator.onSMigrated(late, otherConn);
    verify(cache, times(1)).applySlotMigration(late.migrations);
    verify(cache, times(3)).applySlotMigration(anyList());
    assertFalse(coordinator.hasActiveMigration());
  }

  @Test
  public void everyDeliveryReappliesItsConnectionTimeoutButOnlyTheFirstAppliesTheDelta() {
    SMigratedEvent closer = migrated(2, "0-100");
    coordinator.onSMigrated(closer, conn);
    coordinator.onSMigrated(closer, otherConn);

    verify(cache, times(1)).applySlotMigration(closer.migrations);
    // another connection's copy may have closed the window: each delivery re-evaluates its own
    verify(conn, times(1)).applyCurrentTimeout();
    verify(otherConn, times(1)).applyCurrentTimeout();
  }

  @Test
  public void distinctClosersDoNotWaitForEachOther() throws Exception {
    CountDownLatch applying = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    SMigratedEvent first = migrated(1, "0-100");
    SMigratedEvent second = migrated(2, "200-300");
    doAnswer(inv -> {
      applying.countDown();
      release.await(5, TimeUnit.SECONDS); // the cache is slow to take the first delta
      return null;
    }).when(cache).applySlotMigration(first.migrations);

    Thread holder = new Thread(() -> coordinator.onSMigrated(first, conn));
    holder.start();
    assertTrue(applying.await(5, TimeUnit.SECONDS));

    // no coordinator-level lock: the second closer goes straight through to the cache
    coordinator.onSMigrated(second, otherConn);
    verify(cache, timeout(5000).times(1)).applySlotMigration(second.migrations);
    assertTrue(holder.isAlive(), "the first delivery is still inside the cache");

    release.countDown();
    holder.join(5000);
    assertFalse(holder.isAlive());
  }

  @Test
  public void concurrentCopiesOfOneCloserApplyExactlyOnce() throws Exception {
    SMigratedEvent closer = migrated(2, "0-100");
    int copies = 8;
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(copies);
    // the broadcast lands once per node connection, so every copy rides its own connection
    List<Connection> connections = new ArrayList<>();
    for (int i = 0; i < copies; i++) {
      Connection copyConn = mock(Connection.class);
      connections.add(copyConn);
      new Thread(() -> {
        try {
          start.await(5, TimeUnit.SECONDS);
          coordinator.onSMigrated(closer, copyConn);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          done.countDown();
        }
      }).start();
    }
    start.countDown();
    assertTrue(done.await(5, TimeUnit.SECONDS));

    verify(cache, times(1)).applySlotMigration(closer.migrations);
    for (Connection copyConn : connections) {
      verify(copyConn, times(1)).applyCurrentTimeout();
    }
  }

  @Test
  public void concurrentDistinctClosersEachConcludeAWindow() throws Exception {
    // two closers racing on the same oldest window: the loser must move on to the next one rather
    // than leave it open until the TTL backstop; many short rounds to actually hit the race
    int windows = 8;
    for (int round = 0; round < 50; round++) {
      long base = round * 100L;
      for (int i = 1; i <= windows; i++) {
        coordinator.onSMigrating(migrating(base + i, "0"), conn);
      }
      assertEquals(windows, coordinator.openMigrationWindows());

      CountDownLatch start = new CountDownLatch(1);
      CountDownLatch done = new CountDownLatch(windows);
      for (int i = 1; i <= windows; i++) {
        SMigratedEvent closer = migrated(base + windows + i, "0");
        new Thread(() -> {
          try {
            start.await(5, TimeUnit.SECONDS);
            coordinator.onSMigrated(closer, mock(Connection.class));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          } finally {
            done.countDown();
          }
        }).start();
      }
      start.countDown();
      assertTrue(done.await(5, TimeUnit.SECONDS));

      assertEquals(0, coordinator.openMigrationWindows(),
        "round " + round + ": every distinct closer must conclude exactly one window");
      assertFalse(coordinator.hasActiveMigration());
    }
  }

  @Test
  public void openerAloneAppliesNothing() {
    coordinator.onSMigrating(migrating(1, "0-100"), conn);
    verify(cache, never()).applySlotMigration(anyList());
    verify(conn).applyCurrentTimeout();
  }

  @Test
  public void duplicateOpenerWhileItsWindowIsOpenDoesNotOpenASecondOne() {
    SMigratingEvent opener = migrating(1, "0-100");
    coordinator.onSMigrating(opener, conn);
    coordinator.onSMigrating(opener, otherConn); // broadcast copy of the same opener
    assertTrue(coordinator.hasActiveMigration());

    // one closer is enough: a second window for the same seq would linger until the TTL
    coordinator.onSMigrated(migrated(2, "0-100"), conn);
    assertFalse(coordinator.hasActiveMigration());
  }

  @Test
  public void duplicateOpenerArrivingBeforeTheWindowIsPublishedWaitsForIt() throws Exception {
    // park the winning copy between seenSMigrating.add and migratingWindows.put: the clock is read
    // exactly there, so a duplicate copy arriving now must not evaluate its timeout yet
    CountDownLatch inGap = new CountDownLatch(1);
    CountDownLatch releaseWinner = new CountDownLatch(1);
    NanoClock.INSTANCE = () -> {
      inGap.countDown();
      try {
        releaseWinner.await(5, TimeUnit.SECONDS);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
      return 0L;
    };
    try {
      SMigratingEvent opener = migrating(1, "0-100");
      AtomicReference<TimeoutInfo> seenByDuplicate = new AtomicReference<>();
      CountDownLatch duplicateDone = new CountDownLatch(1);
      doAnswer(inv -> {
        seenByDuplicate.set(coordinator.getTimeoutSupplier().get());
        return null;
      }).when(otherConn).applyCurrentTimeout();

      Thread winner = new Thread(() -> coordinator.onSMigrating(opener, conn));
      winner.start();
      assertTrue(inGap.await(5, TimeUnit.SECONDS));

      Thread duplicate = new Thread(() -> {
        coordinator.onSMigrating(opener, otherConn); // broadcast copy of the parked opener
        duplicateDone.countDown();
      });
      duplicate.start();
      assertFalse(duplicateDone.await(200, TimeUnit.MILLISECONDS),
        "the duplicate must wait until the winner has published its window");
      assertNull(seenByDuplicate.get());

      releaseWinner.countDown();
      assertTrue(duplicateDone.await(5, TimeUnit.SECONDS));
      winner.join(5000);
      duplicate.join(5000);

      assertNotNull(seenByDuplicate.get(),
        "the duplicate evaluates its timeout against the window");
      assertEquals(1, coordinator.openMigrationWindows());
      assertTrue(coordinator.hasActiveMigration());
    } finally {
      NanoClock.INSTANCE = System::nanoTime;
    }
  }

  @Test
  public void closerBelowEveryOpenWindowClosesNothing() {
    coordinator.onSMigrating(migrating(5, "0-100"), conn);
    // an opener always precedes its closer, so this closer cannot belong to the seq-5 migration
    SMigratedEvent closer = migrated(3, "200-300");
    coordinator.onSMigrated(closer, conn);

    assertTrue(coordinator.hasActiveMigration(), "the seq-5 window must survive");
    verify(cache, times(1)).applySlotMigration(closer.migrations); // the delta still applies
  }

  @Test
  public void timeoutSupplierRelaxesWhileAWindowIsOpenOrADeltaIsPending() {
    when(cache.hasPendingSlotDeltas()).thenReturn(false);
    assertNull(coordinator.getTimeoutSupplier().get(), "nothing in flight: no relaxation");

    coordinator.onSMigrating(migrating(1, "0-100"), conn);
    assertNotNull(coordinator.getTimeoutSupplier().get(), "open window relaxes");

    coordinator.onSMigrated(migrated(2, "0-100"), conn);
    assertNull(coordinator.getTimeoutSupplier().get(),
      "closed window and drained delta: back to normal");

    // a delta queued behind a running refresh keeps the client relaxed until it lands
    when(cache.hasPendingSlotDeltas()).thenReturn(true);
    assertNotNull(coordinator.getTimeoutSupplier().get(), "pending delta relaxes");
  }

  @Test
  public void timeoutSupplierCarriesTheConfiguredRelaxedTimeouts() {
    coordinator = new ClusterMaintenanceCoordinator(cache, MaintenanceNotificationsConfig.builder()
        .relaxedTimeout(1234).relaxedBlockingTimeout(5678).build());
    coordinator.onSMigrating(migrating(1, "0-100"), conn);

    TimeoutInfo relaxed = coordinator.getTimeoutSupplier().get();
    assertNotNull(relaxed);
    assertEquals(1234, relaxed.timeout);
    assertEquals(5678, relaxed.blockingTimeout);
  }

  @Test
  public void windowWithALostCloserExpiresOnTheTtlBackstop() {
    AtomicLong now = new AtomicLong(0);
    NanoClock.INSTANCE = now::get;
    try {
      coordinator = new ClusterMaintenanceCoordinator(cache, MaintenanceNotificationsConfig
          .builder().relaxedWindowMaxDuration(Duration.ofSeconds(10)).build());
      coordinator.onSMigrating(migrating(1, "0-100"), conn);
      assertTrue(coordinator.hasActiveMigration());

      now.addAndGet(TimeUnit.SECONDS.toNanos(9));
      assertTrue(coordinator.hasActiveMigration(), "still inside the TTL");

      now.addAndGet(TimeUnit.SECONDS.toNanos(2));
      assertFalse(coordinator.hasActiveMigration(), "the closer never came: TTL unrelaxes");
      assertNull(coordinator.getTimeoutSupplier().get());
      verify(cache, never()).applySlotMigration(anyList());
    } finally {
      NanoClock.INSTANCE = System::nanoTime;
    }
  }

  @Test
  public void expiredWindowIsNotClosedInPlaceOfALiveOne() {
    AtomicLong now = new AtomicLong(0);
    NanoClock.INSTANCE = now::get;
    try {
      coordinator = new ClusterMaintenanceCoordinator(cache, MaintenanceNotificationsConfig
          .builder().relaxedWindowMaxDuration(Duration.ofSeconds(10)).build());
      coordinator.onSMigrating(migrating(1, "0-100"), conn);
      now.addAndGet(TimeUnit.SECONDS.toNanos(11));
      assertFalse(coordinator.hasActiveMigration()); // seq 1 is swept here

      coordinator.onSMigrating(migrating(2, "200-300"), conn);
      coordinator.onSMigrated(migrated(3, "200-300"), conn);
      assertFalse(coordinator.hasActiveMigration(), "the closer must conclude the live window");
    } finally {
      NanoClock.INSTANCE = System::nanoTime;
    }
  }

  @Test
  public void emptyCloserEndsTheWindowWithoutTouchingTheCache() {
    coordinator.onSMigrating(migrating(1L, "0-100"), conn);
    assertTrue(coordinator.hasActiveMigration());

    coordinator.onSMigrated(new SMigratedEvent(2L, Collections.emptyList()), conn);

    assertFalse(coordinator.hasActiveMigration(), "the window is closed...");
    verify(cache, never()).applySlotMigration(anyList()); // ...and nothing is rerouted or dropped
    verify(conn, times(2)).applyCurrentTimeout();
    // a re-delivered empty closer is deduplicated like any other
    coordinator.onSMigrated(new SMigratedEvent(2L, Collections.emptyList()), conn);
    verify(cache, never()).applySlotMigration(anyList());
  }

  @Test
  public void liveWindowsAreNotEvictedByTheHistoryCap() {
    // more openers than the dedup history holds: every one of them is still a live migration
    int openers = 130;
    for (long seq = 1; seq <= openers; seq++) {
      coordinator.onSMigrating(migrating(seq, "0"), conn);
    }
    assertEquals(openers, coordinator.openMigrationWindows(),
      "live windows are bounded by TTL only");
    assertTrue(coordinator.hasActiveMigration());

    // closers for the oldest 129 migrations, in order: each concludes its own window ...
    for (long seq = 1; seq < openers; seq++) {
      coordinator.onSMigrated(migrated(1000 + seq, "0"), conn);
    }
    // ... and the youngest migration, whose closer has not arrived, keeps the relax open
    assertEquals(1, coordinator.openMigrationWindows());
    assertTrue(coordinator.hasActiveMigration(), "the 130th migration is still running");

    coordinator.onSMigrated(migrated(2000, "0"), conn);
    assertEquals(0, coordinator.openMigrationWindows());
    assertFalse(coordinator.hasActiveMigration());
  }

  @Test
  public void standaloneEventsAreIgnored() {
    // standalone/enterprise events reach the coordinator only through the per-pool controller,
    // which must drop them without touching the migration state or the connection timeout
    ClusterMaintenanceController controller = new ClusterMaintenanceController(coordinator);
    when(conn.toIdentityString()).thenReturn("conn");

    controller.onMoving(new MovingEvent(1L, 10, NODE_B), conn);
    controller.onMigrating(new MigratingEvent(2L, 5, "1"), conn);
    controller.onMigrated(new MigratedEvent(3L, "1"), conn);
    controller.onFailingOver(new FailingOverEvent(4L, 5, "1"), conn);
    controller.onFailedOver(new FailedOverEvent(5L, "1"), conn);

    assertFalse(coordinator.hasActiveMigration());
    verifyNoInteractions(cache);
    verify(conn, never()).applyCurrentTimeout();
  }

  private static SMigratingEvent migrating(long seq, String slots) {
    return new SMigratingEvent(seq, HashSlotRanges.parse(slots));
  }

  private static SMigratedEvent migrated(long seq, String slots) {
    List<SlotMigration> migrations = Collections
        .singletonList(new SlotMigration(NODE_A, NODE_B, HashSlotRanges.parse(slots)));
    return new SMigratedEvent(seq, migrations);
  }
}
