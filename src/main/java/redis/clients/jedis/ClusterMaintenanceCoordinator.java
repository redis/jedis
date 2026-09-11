package redis.clients.jedis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.TimeoutSource.TimeoutInfo;

/**
 * Cluster maintenance coordinator — one per cluster client, the single dedup/apply point for the
 * per-node SMIGRATING/SMIGRATED broadcast. Owns the seq-keyed operations table that (a) folds the
 * N-connection broadcast into one client-wide operation and (b) drives the shared relax gate:
 * timeouts stay relaxed while ANY operation is open, so overlapping migrations unrelax only when
 * the last one closes, and a connection created mid-event relaxes from the moment its overlay is
 * installed. On the first SMIGRATED delivery it hands the slot delta to
 * {@link JedisClusterInfoCache#applySlotMigration}, which queues and applies it atomically against
 * the refresh lifecycle — never blocking or spinning a read thread on a running refresh.
 */
final class ClusterMaintenanceCoordinator {

  private static final Logger logger = LoggerFactory.getLogger(ClusterMaintenanceCoordinator.class);

  private final JedisClusterInfoCache cache;
  /** Backstop for an SMIGRATING whose SMIGRATED is lost, and retention of concluded entries. */
  private final long maxRelaxedDurationNanos;
  private final Supplier<TimeoutInfo> timeoutSupplier;

  /**
   * Seq-keyed operations: migrating (SMIGRATING) entries gate the relax; non-migrating (SMIGRATED)
   * entries absorb the remaining broadcast duplicates until their TTL.
   */
  private final ConcurrentHashMap<Object, MigrationOperation> operations = new ConcurrentHashMap<>();

  private final MaintenanceNotificationsConfig config;

  ClusterMaintenanceCoordinator(JedisClusterInfoCache cache,
      MaintenanceNotificationsConfig config) {
    this.cache = cache;
    this.config = config;
    this.maxRelaxedDurationNanos = config.getRelaxedWindowMaxDuration().toNanos();
    TimeoutInfo relaxedTimeoutInfo = new TimeoutInfo(config.getRelaxedTimeout(),
        config.getRelaxedBlockingTimeout());
    this.timeoutSupplier = () -> hasActiveMigration() ? relaxedTimeoutInfo : null;
  }

  /**
   * The config this coordinator was built from; drives the cluster connections' MAINT_NOTIFICATIONS
   * handshake.
   */
  MaintenanceNotificationsConfig getConfig() {
    return config;
  }

  /** The client-wide relax gate consulted by every cluster connection's timeout overlay. */
  Supplier<TimeoutInfo> getTimeoutSupplier() {
    return timeoutSupplier;
  }

  /**
   * True while any migration window is open (SMIGRATING seen, SMIGRATED not yet, TTL unexpired).
   */
  boolean hasActiveMigration() {
    if (operations.isEmpty()) {
      return false;
    }
    for (MigrationOperation op : operations.values()) {
      if (op.isExpired()) {
        operations.remove(op.id, op);
      } else if (op.isMigrating()) {
        return true;
      }
    }
    return false;
  }

  /** Dispatched by each pool's {@link ClusterMaintenanceController} on the read thread. */
  void onSMigrating(SMigratingEvent e, Connection c) {
    if (logger.isDebugEnabled()) {
      logger.debug("Slot migration starting: {} (seq={}) conn={}", e.slots, e.seq,
        c.toIdentityString());
    }
    long deadline = NanoClock.INSTANCE.getAsLong() + maxRelaxedDurationNanos;
    operations.computeIfAbsent(e.identity(), k -> new MigrationOperation(k, e.seq, deadline, true));
    c.applyCurrentTimeout(); // the gate just opened; push the relax to the receiving socket now
  }

  /** Dispatched by each pool's {@link ClusterMaintenanceController} on the read thread. */
  void onSMigrated(SMigratedEvent e, Connection c) {
    long deadline = NanoClock.INSTANCE.getAsLong() + maxRelaxedDurationNanos;
    boolean[] firstDelivery = { false };
    operations.compute(e.identity(), (k, cur) -> { // atomic per identity
      if (cur == null) {
        // SMIGRATED without a preceding SMIGRATING (e.g. connected mid-event): still applied
        firstDelivery[0] = true;
        return new MigrationOperation(k, e.seq, deadline, false);
      }
      return cur; // duplicate broadcast delivery; retained to absorb the rest
    });
    discardOutdatedMigrationWindows(e.seq);
    c.applyCurrentTimeout(); // unrelax the receiving socket if the gate just shut
    if (!firstDelivery[0]) {
      return;
    }
    logger.debug("Slot migration done (seq={}, entries={})", e.seq, e.migrations.size());
    // the cache queues and applies atomically against its refresh lifecycle; a delta racing a
    // refresh may be applied later, on the refreshing/draining thread
    cache.applySlotMigration(e.migrations);
  }

  /**
   * Seq ids are ordered, so a closing SMIGRATED also concludes any operation opened under a lower
   * seq whose own SMIGRATED never arrived (lost or reordered) — the stale opener must not keep the
   * relax gate held. Discarded entries are removed outright; a late lower-seq SMIGRATED would then
   * re-apply as a fresh delta (the MOVED fallback self-heals the topology if it was stale).
   */
  private void discardOutdatedMigrationWindows(long closingSeq) {
    for (MigrationOperation op : operations.values()) {
      if (op.seq < closingSeq) {
        operations.remove(op.id, op); // best-effort: a racing delivery may have already replaced it
        if (logger.isDebugEnabled()) {
          logger.debug("Discarding stale migration window (seq={}) on closing seq={}", op.seq,
            closingSeq);
        }
      }
    }
  }

  /** One client-wide migration operation, folded from the per-node broadcast; keyed by seq. */
  private static final class MigrationOperation {
    final Object id;
    final long seq;
    final long deadlineNanos;
    final boolean isMigrating;

    private MigrationOperation(Object id, long seq, long deadlineNanos, boolean isMigrating) {
      this.id = id;
      this.seq = seq;
      this.deadlineNanos = deadlineNanos;
      this.isMigrating = isMigrating;
    }

    boolean isExpired() {
      return deadlineNanos - NanoClock.INSTANCE.getAsLong() <= 0;
    }

    boolean isMigrating() {
      return isMigrating;
    }
  }

}
