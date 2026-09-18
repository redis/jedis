package redis.clients.jedis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.TimeoutSource.TimeoutInfo;

/**
 * Cluster maintenance coordinator — one per cluster client, the single dedup/apply point for the
 * per-node SMIGRATING/SMIGRATED broadcast. Seq ids are ordered, and the coordinator keeps them in
 * two lock-free structures: open SMIGRATING windows (they drive the shared relax gate — timeouts
 * stay relaxed while ANY window is open or any slot delta is still pending, and a connection
 * created mid-event relaxes from the moment its overlay is installed) and the last
 * {@value #MAX_COMPLETED_MIGRATIONS} completed SMIGRATED operations, retained to absorb broadcast
 * duplicates. An SMIGRATING older than the last processed seq is ignored; an SMIGRATED older than
 * the newest completed operation folds its delta into that operation and applies the combined
 * result, equivalent to every contribution applied separately in seq order. Deltas go through
 * {@link JedisClusterInfoCache#applySlotMigration}, which queues and applies atomically against the
 * refresh lifecycle — never blocking or spinning a read thread on a running refresh.
 */
final class ClusterMaintenanceCoordinator implements MaintenanceEventListener {

  private static final Logger logger = LoggerFactory.getLogger(ClusterMaintenanceCoordinator.class);

  /** Completed SMIGRATED operations retained for duplicate absorption; oldest dropped first. */
  private static final int MAX_COMPLETED_MIGRATIONS = 20;

  private final JedisClusterInfoCache cache;
  /** Backstop for an SMIGRATING whose SMIGRATED is lost. */
  private final long maxRelaxedDurationNanos;
  private final Supplier<TimeoutInfo> timeoutSupplier;

  /** Highest seq processed so far (either event type); gates stale SMIGRATING deliveries. */
  private final AtomicLong lastProcessedSeq = new AtomicLong(Long.MIN_VALUE);

  /**
   * Open SMIGRATING windows by seq; they hold the relax gate until closed, discarded or expired.
   */
  private final ConcurrentHashMap<Long, MigratingWindow> migratingWindows = new ConcurrentHashMap<>();

  /**
   * Completed SMIGRATED operations by seq, ascending; the newest entry is the merge target for late
   * lower-seq closers.
   */
  private final ConcurrentSkipListMap<Long, CompletedMigration> completedMigrations = new ConcurrentSkipListMap<>();

  ClusterMaintenanceCoordinator(JedisClusterInfoCache cache,
      MaintenanceNotificationsConfig config) {
    this.cache = cache;
    this.maxRelaxedDurationNanos = config.getRelaxedWindowMaxDuration().toNanos();
    TimeoutInfo relaxedTimeoutInfo = new TimeoutInfo(config.getRelaxedTimeout(),
        config.getRelaxedBlockingTimeout());
    // relaxed while any migration window is open OR any slot delta is still enqueued/mid-apply:
    // once an SMIGRATING is received, timeouts stay relaxed until the topology has settled
    this.timeoutSupplier = () -> hasActiveMigration() || cache.hasPendingSlotDeltas()
        ? relaxedTimeoutInfo
        : null;
  }

  /** The client-wide relax gate consulted by every cluster connection's timeout overlay. */
  Supplier<TimeoutInfo> getTimeoutSupplier() {
    return timeoutSupplier;
  }

  /**
   * True while any migration window is open (SMIGRATING seen, SMIGRATED not yet, TTL unexpired).
   */
  boolean hasActiveMigration() {
    if (migratingWindows.isEmpty()) {
      return false;
    }
    for (MigratingWindow window : migratingWindows.values()) {
      if (window.isExpired()) {
        migratingWindows.remove(window.seq, window);
      } else {
        return true;
      }
    }
    return false;
  }

  @Override
  public void onSMigrating(SMigratingEvent e, Connection c) {
    if (e.seq < lastProcessedSeq.get()) { // lock-free stale gate
      if (logger.isDebugEnabled()) {
        logger.debug("Ignoring stale SMIGRATING (seq={} < last processed seq={})", e.seq,
          lastProcessedSeq.get());
      }
      return;
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Slot migration starting: {} (seq={}) conn={}", e.slots, e.seq,
        c.toIdentityString());
    }
    long deadline = NanoClock.INSTANCE.getAsLong() + maxRelaxedDurationNanos;
    migratingWindows.computeIfAbsent(e.seq, k -> new MigratingWindow(e.seq, deadline));
    lastProcessedSeq.accumulateAndGet(e.seq, Math::max);
    if (e.seq < lastProcessedSeq.get()) {
      // a newer event was processed between the gate and the insert; this window is stale
      migratingWindows.remove(e.seq);
      return;
    }
    c.applyCurrentTimeout(); // the gate just opened; push the relax to the receiving socket now
  }

  @Override
  public void onSMigrated(SMigratedEvent e, Connection c) {
    long seq = e.seq;
    // every processed seq is recorded here, merged ones included: the known-check is one lookup
    if (completedMigrations.containsKey(seq) || completedMigrations.putIfAbsent(seq,
      new CompletedMigration(seq, e.migrations)) != null) {
      onDuplicateSMigrated(seq, c); // known/processed seq: absorbed, delta never re-applied
      return;
    }
    lastProcessedSeq.accumulateAndGet(seq, Math::max);

    List<SlotMigration> toApply;
    // non-null: our own entry was just recorded, and trimming never drains the map below its cap
    Map.Entry<Long, CompletedMigration> newest = completedMigrations.lastEntry();
    if (seq >= newest.getKey()) {
      toApply = e.migrations;
      logger.debug("Slot migration done (seq={}, entries={})", seq, e.migrations.size());
    } else {
      // late lower-seq closer: fold its delta into the newest operation and apply the combined
      // result — equivalent to every contribution applied separately in ascending seq order
      toApply = newest.getValue().merge(seq, e.migrations);
      if (toApply == null) {
        // merged once already, then its map entry was trimmed; recorded again above, never
        // re-applied
        onDuplicateSMigrated(seq, c);
        return;
      }
      logger.debug("Late slot migration (seq={}) merged into seq={}; applying combined delta", seq,
        newest.getKey());
    }
    discardOutdatedMigrationWindows(seq);
    // the cache queues and applies atomically against its refresh lifecycle; a delta racing a
    // refresh may be applied later, on the refreshing/draining thread
    cache.applySlotMigration(toApply);
    // unrelax only takes effect once the gate is really shut: with the delta still enqueued or
    // queued behind a refresh, hasPendingSlotDeltas() keeps the receiving socket relaxed
    c.applyCurrentTimeout();
    trimCompletedMigrations();
  }

  /** A duplicate SMIGRATED delivery: same reactions as always, but no delta application. */
  private void onDuplicateSMigrated(long seq, Connection c) {
    discardOutdatedMigrationWindows(seq);
    c.applyCurrentTimeout();
  }

  /** Drops the oldest completed operations beyond the retention cap; lock-free. */
  private void trimCompletedMigrations() {
    while (completedMigrations.size() > MAX_COMPLETED_MIGRATIONS) {
      completedMigrations.pollFirstEntry();
    }
  }

  /**
   * Seq ids are ordered, so a closing SMIGRATED also concludes any window opened under a lower seq
   * whose own SMIGRATED never arrived (lost or reordered) — the stale opener must not keep the
   * relax gate held.
   */
  private void discardOutdatedMigrationWindows(long closingSeq) {
    for (MigratingWindow window : migratingWindows.values()) {
      if (window.seq < closingSeq) {
        migratingWindows.remove(window.seq, window);
        if (logger.isDebugEnabled()) {
          logger.debug("Discarding stale migration window (seq={}) on closing seq={}", window.seq,
            closingSeq);
        }
      }
    }
  }

  /** One open SMIGRATING window, folded from the per-node broadcast; keyed by seq. */
  private static final class MigratingWindow {
    final long seq;
    final long deadlineNanos;

    MigratingWindow(long seq, long deadlineNanos) {
      this.seq = seq;
      this.deadlineNanos = deadlineNanos;
    }

    boolean isExpired() {
      return deadlineNanos - NanoClock.INSTANCE.getAsLong() <= 0;
    }
  }

  /**
   * One completed SMIGRATED operation carrying its slot delta, plus the deltas of late lower-seq
   * closers merged into it — each contribution kept isolated under its own seq.
   */
  private static final class CompletedMigration {
    /**
     * Per-seq contributions, ascending; iteration order is the as-if-sequential application order.
     */
    private final ConcurrentSkipListMap<Long, List<SlotMigration>> deltas = new ConcurrentSkipListMap<>();

    CompletedMigration(long seq, List<SlotMigration> delta) {
      deltas.put(seq, delta);
    }

    /**
     * Folds a lower-seq delta in and returns the combined delta, or {@code null} when that seq was
     * already merged. The combined delta is freshly built: contributions are laid down in ascending
     * seq order, so a higher seq overrides overlapping slots and the result equals applying each
     * contribution separately in seq order.
     */
    List<SlotMigration> merge(long seq, List<SlotMigration> delta) {
      if (deltas.putIfAbsent(seq, delta) != null) {
        return null;
      }
      return combinedDelta();
    }

    private List<SlotMigration> combinedDelta() {
      SlotMigration[] ownerBySlot = new SlotMigration[Protocol.CLUSTER_HASHSLOTS];
      for (List<SlotMigration> contribution : deltas.values()) { // ascending seq: later overrides
        for (SlotMigration migration : contribution) {
          migration.slots.forEachSlot(slot -> ownerBySlot[slot] = migration);
        }
      }

      Map<SlotMigration, StringBuilder> rangesByEntry = new LinkedHashMap<>();
      for (int slot = 0; slot < ownerBySlot.length; slot++) {
        SlotMigration owner = ownerBySlot[slot];
        if (owner == null) {
          continue;
        }
        int from = slot;
        while (slot + 1 < ownerBySlot.length && ownerBySlot[slot + 1] == owner) {
          slot++;
        }
        StringBuilder ranges = rangesByEntry.computeIfAbsent(owner, k -> new StringBuilder());
        if (ranges.length() > 0) {
          ranges.append(',');
        }
        ranges.append(from);
        if (slot > from) {
          ranges.append('-').append(slot);
        }
      }

      List<SlotMigration> combined = new ArrayList<>(rangesByEntry.size());
      for (Map.Entry<SlotMigration, StringBuilder> entry : rangesByEntry.entrySet()) {
        SlotMigration source = entry.getKey();
        combined.add(new SlotMigration(source.src, source.dest,
            HashSlotRanges.parse(entry.getValue().toString())));
      }
      return combined;
    }
  }

  @Override
  public void onMoving(MovingEvent e, Connection c) {
    logger.warn("Standalone maintenance events are not supported by this controller: {} conn={}", e,
      c);
  }

  @Override
  public void onMigrating(MigratingEvent e, Connection c) {
    logger.warn("Standalone maintenance events are not supported by this controller: {} conn={}", e,
      c);
  }

  @Override
  public void onMigrated(MigratedEvent e, Connection c) {
    logger.warn("Standalone maintenance events are not supported by this controller: {} conn={}", e,
      c);
  }

  @Override
  public void onFailingOver(FailingOverEvent e, Connection c) {
    logger.warn("Standalone maintenance events are not supported by this controller: {} conn={}", e,
      c);
  }

  @Override
  public void onFailedOver(FailedOverEvent e, Connection c) {
    logger.warn("Standalone maintenance events are not supported by this controller: {} conn={}", e,
      c);
  }
}
