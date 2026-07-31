package redis.clients.jedis;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.MovingOperations.MovingOperation;
import redis.clients.jedis.TimeoutSource.TimeoutInfo;

/**
 * Maintenance coordinator: reacts to maintenance events. MOVING deliveries are deduplicated into
 * pool-wide operations by {@link MovingOperations}; the controller reacts to each applied operation
 * — marking passes over the {@link ConnectionRegistry} that flag affected connections for
 * recycling, the relax-window policy, and the post-DNS remap of affected peers. A controller has
 * exactly one owner, which creates it, registers its reaction, and must {@link #close()} it. Never
 * shared.
 */
final class MaintenanceEventController
    implements MaintenanceEventListener, SocketAddressMapper, AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceEventController.class);

  private final MaintenanceNotificationsConfig config;
  private final long maxRelaxedDurationNanos; // MIGRATING/FAILING_OVER backstop window
  private final MovingOperations movingOperations = new MovingOperations();
  /** The owner's reaction to a completed marking pass; see {@link #setHandoffHook}. */
  private volatile Runnable handoffHook = () -> {
  };
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
    this.timeoutSupplier = () -> movingOperations.hasActive() ? relaxedTimeoutInfo : null;
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

  private static final AtomicInteger MAINTENANCE_THREAD_SEQ = new AtomicInteger();

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
   * Sets the owner's hook, fired once a MOVING handoff has been processed — its affected
   * connections retired. Runs on the marking thread and must not block.
   */
  void setHandoffHook(Runnable hook) {
    this.handoffHook = hook;
  }

  Runnable getHandoffHook() {
    return handoffHook;
  }

  /**
   * Post-DNS address mapper: remaps the resolved peer to its active operation's endpoint when the
   * peer is an affected source of an unexpired MOVING operation; else returns null (no remap). The
   * endpoint is resolved here, at connect time, so a DNS repoint mid-window is honored.
   */
  @Override
  public SocketAddress getSocketAddress(SocketAddress resolved) {
    MovingOperation op = movingOperations.findActive(o -> o.affected.contains(resolved));
    if (op == null || op.endpoint == null) {
      return null; // no active operation for this peer, or 'none': reconnect to configured endpoint
    }
    return new InetSocketAddress(op.endpoint.getHost(), op.endpoint.getPort());
  }

  /** True iff there is an active MOVING rebind window in the pool right now. */
  public boolean isRebindActive() {
    return movingOperations.hasActive();
  }

  @Override
  public void onMoving(MovingEvent e, Connection c) {
    logger.debug("Moving to {} (seq={}, ttl={}s)", e.target, e.seq, e.ttlSeconds);
    SocketAddress affectedPeer = c.getRemoteSocketAddress();
    if (affectedPeer == null) {
      return; // receiver socket already closed; no peer to register
    }
    MovingOperation applied = movingOperations.process(e, affectedPeer);
    if (applied == null) {
      long retireAt = getRetirementFor(affectedPeer);
      if (retireAt > NanoClock.INSTANCE.getAsLong()) {
        c.retireAt(retireAt);
      }
      return;
    }
    logger.debug("Applied MOVING {} -> {} (seq={}, ttl={}s, sources={})", affectedPeer, e.target,
      e.seq, e.ttlSeconds, applied.affected.size());
    handleRebind(applied);
  }

  private long getRetirementFor(SocketAddress peer) {
    MovingOperation op = movingOperations.findActive(o -> o.affected.contains(peer));
    return op == null ? 0 : op.reconnectAtNanos;
  }

  /**
   * Reaction to an applied MOVING operation: retire the snapshot's affected connections —
   * immediately for a real endpoint, at the reconnect instant for a null-endpoint ('none') MOVING.
   * A pending pass is never cancelled; a stale fire is a no-op once its operation expires.
   */
  private void handleRebind(MovingOperation snapshot) {
    if (snapshot.endpoint != null) {
      retireAffected(snapshot, NanoClock.INSTANCE.getAsLong()); // real target: reconnect
                                                                // immediately
      handoffHook.run();
      return;
    }
    long delayNanos = snapshot.reconnectAtNanos - NanoClock.INSTANCE.getAsLong();
    try {
      scheduler().schedule(handoffHook::run, delayNanos, TimeUnit.NANOSECONDS);
    } catch (RejectedExecutionException alreadyClosed) {
      // Controller closed concurrently;
    }
  }

  /**
   * Retires every registered connection whose peer is one of the snapshot's affected sources, then
   * runs the handoff hook. No I/O happens here — the pool destroys retired connections; retiring is
   * idempotent, so overlapping passes are harmless.
   */
  private void retireAffected(MovingOperation snapshot, long retireAtNanos) {
    registry.forEachLive(conn -> {
      if (snapshot.affected.contains(conn.getRemoteSocketAddress())) {
        conn.retireAt(retireAtNanos);
      }
    });
  }

  ConnectionRegistry registry() {
    return registry;
  }

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
    relaxConnectionTimeoutsFor(c, maxRelaxedDurationNanos + NanoClock.INSTANCE.getAsLong());
  }

  @Override
  public void onFailingOver(FailingOverEvent e, Connection c) {
    logger.debug("Failing over shards {} (seq={}, ttl={}s)", e.shardIds, e.seq, e.ttlSeconds);
    relaxConnectionTimeoutsFor(c, maxRelaxedDurationNanos + NanoClock.INSTANCE.getAsLong());
  }

  @Override
  public void onMigrated(MigratedEvent e, Connection c) {
    logger.debug("Migrated shards {} (seq={})", e.shardIds, e.seq);
    relaxConnectionTimeoutsFor(c, 0);
  }

  @Override
  public void onFailedOver(FailedOverEvent e, Connection c) {
    logger.debug("Failed over shards {} (seq={})", e.shardIds, e.seq);
    relaxConnectionTimeoutsFor(c, 0);
  }

  private void relaxConnectionTimeoutsFor(Connection c, long expirationTime) {
    ChainedTimeoutSource source = c.getTimeoutSource().seekBy(ExpiringTimeoutSource.class);
    if (source != null) {
      ((ExpiringTimeoutSource) source).setExpirationTime(expirationTime);
    }
    c.applyCurrentTimeout();
  }
}
