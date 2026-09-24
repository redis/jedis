package redis.clients.jedis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.TimeoutSource.TimeoutInfo;

/**
 * Per-client dedup and apply point for the SMIGRATING/SMIGRATED cluster maintenance broadcast. Seq
 * ids are ordered and unique per message: an SMIGRATED never shares its SMIGRATING's seq. See
 * late-smigrated-handling-decision.md for the late-closer rule.
 */
final class ClusterMaintenanceCoordinator implements MaintenanceEventListener {

  private static final Logger logger = LoggerFactory.getLogger(ClusterMaintenanceCoordinator.class);

  private static final int MAX_HISTORY_OF_EVENTS = 128;

  private final JedisClusterInfoCache cache;
  private final long maxRelaxedDurationNanos;
  private final Supplier<TimeoutInfo> timeoutSupplier;

  private final ConcurrentSkipListMap<Long, MigratingWindow> migratingWindows = new ConcurrentSkipListMap<>();

  private final ConcurrentSkipListMap<Long, SMigratedEvent> pendingSMigrated = new ConcurrentSkipListMap<>();

  private final ReentrantLock smigratedLock = new ReentrantLock();

  private final ConcurrentSkipListMap<Long, CompletedMigration> completedMigrations = new ConcurrentSkipListMap<>();

  ClusterMaintenanceCoordinator(JedisClusterInfoCache cache,
      MaintenanceNotificationsConfig config) {
    this.cache = cache;
    this.maxRelaxedDurationNanos = config.getRelaxedWindowMaxDuration().toNanos();
    TimeoutInfo relaxedTimeoutInfo = new TimeoutInfo(config.getRelaxedTimeout(),
        config.getRelaxedBlockingTimeout());
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
    while (migratingWindows.size() > MAX_HISTORY_OF_EVENTS) {
      migratingWindows.pollFirstEntry();
    }
    return false;
  }

  @Override
  public void onSMigrating(SMigratingEvent e, Connection c) {
    long deadline = NanoClock.INSTANCE.getAsLong() + maxRelaxedDurationNanos;
    MigratingWindow mw = new MigratingWindow(e.seq, deadline);
    migratingWindows.putIfAbsent(e.seq, mw);
    if (logger.isDebugEnabled() && migratingWindows.get(e.seq) == mw) {
      logger.debug("Slot migration starting: {} (seq={}) conn={}", e.slots, e.seq,
        c.toIdentityString());
    }
    c.applyCurrentTimeout();
  }

  @Override
  public void onSMigrated(SMigratedEvent e, Connection c) {
    pendingSMigrated.putIfAbsent(e.seq, e);
    smigratedLock.lock();
    try {
      boolean applied = false;
      cache.setSlotMigrationInProgress(true);
      try {
        Map.Entry<Long, SMigratedEvent> next;
        while ((next = pendingSMigrated.pollFirstEntry()) != null) {
          applied |= processSMigrated(next.getValue());
        }
      } finally {
        cache.setSlotMigrationInProgress(false);
      }
      if (applied) {
        cache.cleanupSlotlessNodes();
      }
    } finally {
      smigratedLock.unlock();
    }
    c.applyCurrentTimeout();
  }

  /**
   * Runs under {@link #smigratedLock}: one closer at a time, so record → merge → apply is atomic.
   * @return whether a slot delta was handed to the cache
   */
  private boolean processSMigrated(SMigratedEvent e) {
    if (completedMigrations.putIfAbsent(e.seq,
      new CompletedMigration(e.seq, e.migrations)) != null) {
      return false;
    }
    List<SlotMigration> toApply;
    Map.Entry<Long, CompletedMigration> lastMigration = completedMigrations.lastEntry();
    long newestSeq = lastMigration.getKey();
    if (e.seq >= newestSeq) {
      toApply = e.migrations;
      logger.debug("Slot migration done (seq={}, entries={})", e.seq, e.migrations.size());
    } else if (isSafeToMergeInto(newestSeq, e.seq)) {
      toApply = lastMigration.getValue().merge(e.seq, e.migrations);
      logger.debug("Late slot migration (seq={}) merged into seq={}; applying combined delta",
        e.seq, newestSeq);
    } else {
      logger.info(
        "Dropping late slot migration (seq={}): ordering context is ambiguous to merge to (seq={})",
        e.seq, newestSeq);
      closeMigrationWindow(e.seq);
      return false;
    }
    closeMigrationWindow(e.seq);
    cache.applySlotMigration(toApply);
    trimCompletedMigrations();
    return true;
  }

  /**
   * A late delta may only be merged when its ordering context is unambiguous: no other completed
   * operation lies between it and the newest. Anything else is dropped — see
   * late-smigrated-handling-decision.md.
   */
  private boolean isSafeToMergeInto(long newestSeq, long lateSeq) {
    return completedMigrations.subMap(lateSeq, false, newestSeq, false).isEmpty();
  }

  /** Drops the oldest completed operations beyond the retention cap; lock-free. */
  private void trimCompletedMigrations() {
    while (completedMigrations.size() > MAX_HISTORY_OF_EVENTS) {
      completedMigrations.pollFirstEntry();
    }
  }

  /**
   * Concludes the one window this closer belongs to. Seq ids are unique, so the pair cannot be
   * matched by seq; since an opener always precedes its closer, the closer concludes the oldest
   * open window below its own seq. Exactly one window per first delivery: closing every lower
   * window would unrelax an overlapping migration still in progress, and the relax gate is
   * client-wide anyway, so which window goes does not matter — only how many stay open. A window
   * whose closer is lost therefore lingers until the next closer or the TTL backstop.
   */
  private void closeMigrationWindow(long closingSeq) {
    Map.Entry<Long, MigratingWindow> oldest = migratingWindows.firstEntry();
    if (oldest != null && oldest.getKey() < closingSeq) {
      if (migratingWindows.remove(oldest.getKey(), oldest.getValue())) {
        if (logger.isDebugEnabled()) {
          logger.debug("Closing migration window (seq={}) on closing seq={}", oldest.getKey(),
            closingSeq);
        }
      }
    }
  }

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

  private static final class CompletedMigration {
    private final ConcurrentSkipListMap<Long, List<SlotMigration>> deltas = new ConcurrentSkipListMap<>();

    CompletedMigration(long seq, List<SlotMigration> delta) {
      deltas.put(seq, delta);
    }

    /**
     * Folds a lower-seq delta in and returns the combined delta, freshly built: contributions are
     * laid down in ascending seq order, so a higher seq overrides overlapping slots and the result
     * equals applying each contribution separately in seq order.
     */
    List<SlotMigration> merge(long seq, List<SlotMigration> delta) {
      deltas.putIfAbsent(seq, delta);
      return combinedDelta();
    }

    private List<SlotMigration> combinedDelta() {
      SlotMigration[] ownerBySlot = new SlotMigration[Protocol.CLUSTER_HASHSLOTS];
      for (List<SlotMigration> contribution : deltas.values()) {
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
