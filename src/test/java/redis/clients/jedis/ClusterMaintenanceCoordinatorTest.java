package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ClusterMaintenanceCoordinator}: window bookkeeping against the
 * SMIGRATING/SMIGRATED broadcast (seq ids are unique per message, never shared by a pair) and the
 * dedup of slot-delta application.
 */
@Tag("unit")
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
    coordinator.onSMigrated(closer, conn);

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
  public void staleOpenerArrivingAfterItsCloserIsIgnored() {
    coordinator.onSMigrated(migrated(2, "0-100"), conn);
    coordinator.onSMigrating(migrating(1, "0-100"), otherConn); // reordered opener

    assertFalse(coordinator.hasActiveMigration());
  }

  @Test
  public void lateAdjacentCloserIsMergedAndAppliedCombined() {
    coordinator.onSMigrating(migrating(1, "0-100"), conn);
    coordinator.onSMigrating(migrating(2, "200-300"), conn);
    coordinator.onSMigrated(migrated(4, "200-300"), conn);
    // seq 3 arrives late; nothing completed in between, so it merges into seq 4
    coordinator.onSMigrated(migrated(3, "0-100"), otherConn);

    assertFalse(coordinator.hasActiveMigration());
    verify(cache, times(2)).applySlotMigration(anyList());
  }

  @Test
  public void lateCloserWithIntermediateIsDroppedButStillClosesAWindow() {
    coordinator.onSMigrating(migrating(1, "0-100"), conn);
    coordinator.onSMigrated(migrated(3, "200-300"), conn);
    coordinator.onSMigrated(migrated(4, "400-500"), conn);
    verify(cache, times(2)).applySlotMigration(anyList());

    // seq 2 arrives late with seq 3 in between: ambiguous, so its delta is dropped
    SMigratedEvent late = migrated(2, "0-100");
    coordinator.onSMigrated(late, otherConn);
    verify(cache, never()).applySlotMigration(late.migrations);
    verify(cache, times(2)).applySlotMigration(anyList());
    // but it is still the first delivery of a closer and concludes the pending window
    assertFalse(coordinator.hasActiveMigration());
  }

  @Test
  public void overlappingClosersAreProcessedOneAtATimeInSeqOrder() throws Exception {
    CountDownLatch applying = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    doAnswer(inv -> {
      applying.countDown();
      release.await(5, TimeUnit.SECONDS); // holds the coordinator lock mid-processing
      return null;
    }).when(cache).applySlotMigration(anyList());

    SMigratedEvent first = migrated(1, "0-100");
    SMigratedEvent second = migrated(2, "200-300");
    SMigratedEvent third = migrated(3, "400-500");

    Thread holder = new Thread(() -> coordinator.onSMigrated(first, conn));
    holder.start();
    assertTrue(applying.await(5, TimeUnit.SECONDS));
    // arrivals overlap the holder and each other, higher seq first
    Thread late = new Thread(() -> coordinator.onSMigrated(third, otherConn));
    late.start();
    await().atMost(5, TimeUnit.SECONDS).until(() -> late.getState() == Thread.State.WAITING);
    Thread earlier = new Thread(() -> coordinator.onSMigrated(second, otherConn));
    earlier.start();
    await().atMost(5, TimeUnit.SECONDS).until(() -> earlier.getState() == Thread.State.WAITING);

    release.countDown();
    holder.join(5000);
    late.join(5000);
    earlier.join(5000);
    assertFalse(holder.isAlive() || late.isAlive() || earlier.isAlive());

    // drained lowest seq first: both are the newest at their turn, so neither takes the late path
    InOrder order = inOrder(cache);
    order.verify(cache).applySlotMigration(first.migrations);
    order.verify(cache).applySlotMigration(second.migrations);
    order.verify(cache).applySlotMigration(third.migrations);
    verify(cache, times(3)).applySlotMigration(anyList());
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
