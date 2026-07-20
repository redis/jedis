package redis.clients.jedis;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.TimeoutSource.TimeoutInfo;

/**
 * Maintenance handler: owns the shared rebind overlay, the relax-window policy, the registry of
 * pool-managed connections, and the marking passes that flag affected connections for recycling.
 */
final class MaintenanceEventController
    implements MaintenanceEventListener, SocketAddressMapper, AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceEventController.class);

  private final MaintenanceNotificationsConfig config;
  private final long maxRelaxedDurationNanos; // MIGRATING/FAILING_OVER backstop window
  private static final AtomicReferenceFieldUpdater<MaintenanceEventController, RebindState> REBIND = AtomicReferenceFieldUpdater
      .newUpdater(MaintenanceEventController.class, RebindState.class, "rebind");
  private volatile RebindState rebind = RebindState.EXPIRED_STATE;
  /** Hooks fired once per completed marking pass; see {@link #addHandoffHook}. */
  private final List<Runnable> handoffHooks = new CopyOnWriteArrayList<>();
  private final Supplier<TimeoutInfo> timeoutSupplier;

  private final ConnectionRegistry registry = new ConnectionRegistry();

  /**
   * Marking scheduler for delayed ('none') passes; created lazily on the first null-target rebind
   */
  private volatile ScheduledExecutorService scheduler;
  private boolean closed; // guarded by schedulerLock
  private final Object schedulerLock = new Object();

  private MaintenanceEventController(MaintenanceNotificationsConfig config,
      ScheduledExecutorService scheduler) {
    this.config = config;
    this.maxRelaxedDurationNanos = config.getRelaxedWindowMaxDuration().toNanos();
    this.scheduler = scheduler;

    TimeoutInfo relaxedTimeoutInfo = new TimeoutInfo(config.getRelaxedTimeout(),
        config.getRelaxedBlockingTimeout());
    this.timeoutSupplier = () -> rebind.isValid() ? relaxedTimeoutInfo : null;
  }

  /**
   * Construct a controller from the given config. The creator owns the controller and must
   * {@link #close()} it.
   */
  public static MaintenanceEventController from(MaintenanceNotificationsConfig cfg) {
    return new MaintenanceEventController(cfg, null);
  }

  /**
   * Test seam: an explicit marking scheduler (pre-populates the lazy field). The controller owns it
   * and shuts it down on {@link #close()}.
   */
  static MaintenanceEventController from(MaintenanceNotificationsConfig cfg,
      ScheduledExecutorService scheduler) {
    return new MaintenanceEventController(cfg, scheduler);
  }

  /**
   * The marking scheduler, created on first use.
   */
  private ScheduledExecutorService scheduler() {
    ScheduledExecutorService s = scheduler;
    if (s == null) {
      synchronized (schedulerLock) {
        if (closed) {
          throw new RejectedExecutionException("controller closed");
        }
        s = scheduler;
        if (s == null) {
          scheduler = s = newMaintenanceScheduler();
        }
      }
    }
    return s;
  }

  private static final java.util.concurrent.atomic.AtomicInteger MAINTENANCE_THREAD_SEQ = new java.util.concurrent.atomic.AtomicInteger();

  private static ScheduledExecutorService newMaintenanceScheduler() {
    String name = "jedis-maintenance-" + MAINTENANCE_THREAD_SEQ.incrementAndGet();
    return Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, name);
      t.setDaemon(true);
      return t;
    });
  }

  /**
   * The config this controller was built from; drives the connection's MAINT_NOTIFICATIONS
   * handshake.
   */
  MaintenanceNotificationsConfig getConfig() {
    return config;
  }

  /**
   * Registers a hook fired once a MOVING handoff has been processed — its tracked (affected)
   * connections marked for reconnect. Runs on the marking thread and must not block; exceptions
   * propagate. Hooks live as long as the controller.
   */
  void addHandoffHook(Runnable hook) {
    handoffHooks.add(hook);
  }

  /**
   * Post-DNS address mapper: remaps the resolved peer to the rebind target when the resolved peer
   * is one of the active rebind's affected sources and the window is still open; else returns null
   * (no remap). Lock-free read; {@link HashSet#contains} is O(1).
   */
  @Override
  public SocketAddress getSocketAddress(SocketAddress resolved) {
    return (rebind.isValid() && rebind.affected.contains(resolved)) ? rebind.target : null;
  }

  public boolean isAffected(Connection c) {
    // map is expected to be empty most of the time, considering the lifecyle of controller and
    // rebinding operations.
    return rebind.isValid() && rebind.affected.contains(c.getRemoteSocketAddress());
  }

  /** True iff there is an active MOVING rebind window in the pool right now. */
  public boolean isRebindActive() {
    return rebind.isValid();
  }

  @Override
  public void onMoving(MovingEvent e, Connection c) {
    logger.debug("Moving to {} (seq={}, ttl={}s)", e.target, e.seq, e.ttlSeconds);
    long now = NanoClock.INSTANCE.getAsLong();
    // expires at = observed at + time_s
    long deadline = now + TimeUnit.SECONDS.toNanos(e.ttlSeconds);
    // Reconnect instant: a real target marks immediately; 'none' marks at half the raw grace,
    // giving DNS time to repoint before affected connections reconnect to the configured endpoint.
    long reconnectAt = e.target == null ? now + TimeUnit.SECONDS.toNanos(e.ttlSeconds) / 2 : now;

    SocketAddress affectedPeer = c.getRemoteSocketAddress();
    if (affectedPeer == null) {
      return; // receiver socket already closed; no peer to register
    }
    SocketAddress target = e.target == null ? null
        : new InetSocketAddress(e.target.getHost(), e.target.getPort());

    while (true) {
      RebindState cur = rebind;
      if (e.seq < cur.seq) {
        return; // stale replay of a superseded event
      }
      if (e.seq == cur.seq) {
        if (!Objects.equals(target, cur.target)) {
          logger.warn("Ignoring MOVING with conflicting target for seq {}: have {}, got {}", e.seq,
            cur.target, target);
          return;
        }
        if (cur.affected.contains(affectedPeer)) {
          // Re-delivery from an already-known source — e.g. the moving node notifying a
          // connection created after the reconnect instant. No state change, no pass: such
          // connections
          // stay unmarked (temporal churn immunity).
          return;
        }
        RebindState next = cur.merge(affectedPeer);
        if (REBIND.compareAndSet(this, cur, next)) {
          logger.debug("Merged source {} into rebind seq={} (sources={})", affectedPeer, cur.seq,
            next.affected.size());
          handleRebind(next);
          return;
        }
        continue; // CAS lost; retry
      }
      // New seq (or first ever): replace state with a fresh single-source set.
      RebindState next = new RebindState(e.seq, Collections.singleton(affectedPeer), target,
          deadline, reconnectAt);
      if (REBIND.compareAndSet(this, cur, next)) {
        logger.debug("Rebinding {} -> {} (seq={}, ttl={}s)", affectedPeer, target, e.seq,
          e.ttlSeconds);
        handleRebind(next);
        return;
      }
      // CAS lost (concurrent apply); retry — may then see same-seq merge path or older-seq exit.
    }
  }

  /**
   * Reaction to an applied (seq-deduplicated) MOVING rebind: mark the snapshot's affected
   * connections — inline if the reconnect instant is already due, otherwise at that instant. Marked
   * connections are recycled on return to the pool; the handoff hooks evict marked idles. A marking
   * pass pending from a superseded epoch is never cancelled — its stale fire is a no-op via the seq
   * guard.
   */
  private void handleRebind(RebindState snapshot) {
    if (snapshot.target != null) {
      markAffected(snapshot); // real target: reconnect immediately
      return;
    }
    // 'none': mark at the reconnect instant (a late merge yields a non-positive delay: runs now)
    long delayNanos = snapshot.reconnectAtNanos - NanoClock.INSTANCE.getAsLong();
    try {
      scheduler().schedule(() -> markAffected(snapshot), delayNanos, TimeUnit.NANOSECONDS);
    } catch (RejectedExecutionException alreadyClosed) {
      // Controller closed concurrently;
    }
  }

  /**
   * The marking pass: mark every registered connection whose peer is one of the snapshot's sources,
   * then run the handoff hooks (the pool evicts marked idles). Mark-only — never any I/O; only the
   * pool destroys. Every state transition (new seq or merged source) marks against the exact
   * snapshot it produced, so every source is covered by the transition that admitted it;
   * overlapping passes are harmless (marking is an idempotent one-way write). A stale fire
   * (superseded epoch) is a no-op via the seq guard.
   */
  private void markAffected(RebindState snapshot) {
    if (rebind.seq != snapshot.seq) {
      return; // superseded; the new epoch's transitions run their own marking passes
    }
    registry.forEachLive(conn -> {
      if (snapshot.affected.contains(conn.getRemoteSocketAddress())) {
        conn.markForReconnect();
      }
    });
    handoffHooks.forEach(Runnable::run);
  }

  /** Registry the owning pool's factory registers every created connection into. */
  ConnectionRegistry registry() {
    return registry;
  }

  /** Idempotent: stops the maintenance scheduler, dropping any queued marking pass. */
  @Override
  public void close() {
    synchronized (schedulerLock) {
      closed = true;
      if (scheduler != null) {
        scheduler.shutdownNow();
      }
    }
  }

  public Supplier<TimeoutInfo> getTimeoutSupplier() {
    return timeoutSupplier;
  }

  @Override
  public void onMigrating(MigratingEvent e, Connection c) {
    logger.debug("Migrating shards {} (seq={}, ttl={}s)", e.shardIds, e.seq, e.ttlSeconds);
    c.relaxTimeouts(maxRelaxedDurationNanos + NanoClock.INSTANCE.getAsLong()); // time_s = "starts
                                                                               // within";
    // backstop
  }

  @Override
  public void onFailingOver(FailingOverEvent e, Connection c) {
    logger.debug("Failing over shards {} (seq={}, ttl={}s)", e.shardIds, e.seq, e.ttlSeconds);
    c.relaxTimeouts(maxRelaxedDurationNanos + NanoClock.INSTANCE.getAsLong()); // time_s = "starts
                                                                               // within"; backstop
  }

  @Override
  public void onMigrated(MigratedEvent e, Connection c) {
    logger.debug("Migrated shards {} (seq={})", e.shardIds, e.seq);
    c.resetRelaxedTimeouts();
  }

  @Override
  public void onFailedOver(FailedOverEvent e, Connection c) {
    logger.debug("Failed over shards {} (seq={})", e.shardIds, e.seq);
    c.resetRelaxedTimeouts();
  }

  /**
   * Immutable snapshot of an active, time-bounded MOVING rebind. {@code affected} holds the
   * receivers' resolved peers (one per affected connection that delivered a MOVING with this
   * {@code seq}); same-seq events with the same target merge by adding to this set under CAS. The
   * set is always immutable; updates swap the whole state.
   */
  private static final class RebindState {

    static final RebindState EXPIRED_STATE = new RebindState(-1, Collections.emptySet(), null, 0L,
        0L);
    final long seq;
    final Set<SocketAddress> affected;
    final SocketAddress target;
    final long deadlineNanos; // relax-window end; gates isValid()/remap
    final long reconnectAtNanos; // marking due: commit now (target) | commit now + ttl/2 (none)
    volatile boolean expired = false;

    private RebindState(long seq, Set<SocketAddress> affected, SocketAddress target,
        long deadlineNanos, long reconnectAtNanos) {
      this.seq = seq;
      this.affected = affected;
      this.target = target;
      this.deadlineNanos = deadlineNanos;
      this.reconnectAtNanos = reconnectAtNanos;
    }

    private RebindState merge(SocketAddress peer) {
      Set<SocketAddress> merged = new HashSet<>(affected.size() + 1, 1.0f);
      merged.addAll(affected);
      merged.add(peer);
      return new RebindState(seq, Collections.unmodifiableSet(merged), target, deadlineNanos,
          reconnectAtNanos);
    }

    private boolean isValid() {
      if (expired) {
        return false;
      }
      if (deadlineNanos - NanoClock.INSTANCE.getAsLong() > 0) {
        return true;
      }
      expired = true;
      return false;
    }
  }
}
