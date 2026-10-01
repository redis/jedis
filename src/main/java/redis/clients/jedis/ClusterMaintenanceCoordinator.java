package redis.clients.jedis;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.TimeoutSource.TimeoutInfo;

/**
 * Per-client dedup and apply point for the SMIGRATING/SMIGRATED cluster maintenance broadcast. Seq
 * ids are ordered and unique per message: an SMIGRATED never shares its SMIGRATING's seq. Every
 * first delivery of an SMIGRATED is applied as-is, in arrival order; a stale slot left by a
 * reordered closer self-heals through MOVED redirects and the topology refresh.
 */
final class ClusterMaintenanceCoordinator {

  private static final Logger logger = LoggerFactory.getLogger(ClusterMaintenanceCoordinator.class);

  private static final int MAX_HISTORY_OF_EVENTS = 128;

  private final JedisClusterInfoCache cache;
  private final long maxRelaxedDurationNanos;
  private final Supplier<TimeoutInfo> timeoutSupplier;

  private final ConcurrentSkipListMap<Long, MigratingWindow> migratingWindows = new ConcurrentSkipListMap<>();

  /** Openers already seen, by seq: a re-delivered SMIGRATING must not reopen a closed window. */
  private final ConcurrentSkipListSet<Long> seenSMigrating = new ConcurrentSkipListSet<>();

  /** Closers already applied, by seq; the broadcast copies from other connections dedup here. */
  private final ConcurrentSkipListMap<Long, SMigratedEvent> seenSMigrated = new ConcurrentSkipListMap<>();

  private final MaintenanceNotificationsConfig config;

  ClusterMaintenanceCoordinator(JedisClusterInfoCache cache,
      MaintenanceNotificationsConfig config) {
    this.cache = cache;
    this.config = config;
    this.maxRelaxedDurationNanos = config.getRelaxedWindowMaxDuration().toNanos();
    TimeoutInfo relaxedTimeoutInfo = new TimeoutInfo(config.getRelaxedTimeout(),
        config.getRelaxedBlockingTimeout());
    this.timeoutSupplier = () -> hasActiveMigration() || cache.hasPendingSlotDeltas()
        ? relaxedTimeoutInfo
        : null;
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

  void onSMigrating(SMigratingEvent e, Connection c) {
    if (seenSMigrating.add(e.seq)) {
      long deadline = NanoClock.INSTANCE.getAsLong() + maxRelaxedDurationNanos;
      migratingWindows.put(e.seq, new MigratingWindow(e.seq, deadline));
      if (logger.isDebugEnabled()) {
        logger.debug("Slot migration starting: {} (seq={}) conn={}", e.slots, e.seq,
          c.toIdentityString());
      }
      while (seenSMigrating.size() > MAX_HISTORY_OF_EVENTS) {
        seenSMigrating.pollFirst();
      }
    }
    c.applyCurrentTimeout();
  }

  /**
   * The winning {@code putIfAbsent} is the only delivery of a seq that closes a window and applies
   * the delta; every delivery re-evaluates its connection's timeout, since another connection's
   * copy may have closed the window meanwhile.
   */
  void onSMigrated(SMigratedEvent e, Connection c) {
    if (seenSMigrated.putIfAbsent(e.seq, e) == null) {
      logger.debug("Slot migration done (seq={}, entries={})", e.seq, e.migrations.size());
      closeMigrationWindow(e.seq);
      cache.applySlotMigration(e.migrations);
      while (seenSMigrated.size() > MAX_HISTORY_OF_EVENTS) {
        seenSMigrated.pollFirstEntry();
      }
    }
    c.applyCurrentTimeout();
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

}
