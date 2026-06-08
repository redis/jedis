package redis.clients.jedis;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-pool maintenance handler: owns the shared rebind overlay, the relax-window policy, and the
 * handoff hooks fired when a MOVING is applied.
 */
final class MaintenanceEventController implements MaintenanceEvent.Handler, SocketAddressMapper {

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceEventController.class);

  private final long maxRelaxedDurationNanos; // MIGRATING/FAILING_OVER backstop window
  private LongSupplier clockNanos = System::nanoTime; // test seam
  private final AtomicReference<RebindState> rebind = new AtomicReference<>();
  /** Synchronous hooks fired once per applied MOVING handoff; see {@link #addHandoffHook}. */
  private final List<Consumer<MaintenanceHandoff>> handoffHooks = new CopyOnWriteArrayList<>();

  private MaintenanceEventController(long maxRelaxedDurationNanos) {
    this.maxRelaxedDurationNanos = maxRelaxedDurationNanos;
  }

  static MaintenanceEventController from(MaintenanceNotificationsConfig cfg) {
    return new MaintenanceEventController(
        cfg.getTimeoutOptions().getRelaxedTimeoutMaxDuration().toNanos());
  }

  /**
   * Registers a hook invoked synchronously, once, when a MOVING handoff is applied (i.e. a real new
   * target is committed under the seq guard). Hook exceptions propagate.
   */
  public void addHandoffHook(Consumer<MaintenanceHandoff> hook) {
    handoffHooks.add(hook);
  }

  public void removeHandoffHook(Consumer<MaintenanceHandoff> hook) {
    handoffHooks.remove(hook);
  }

  /** Test seam: override the monotonic clock used for rebind expiry. */
  void setClockNanos(LongSupplier clockNanos) {
    this.clockNanos = clockNanos;
  }

  /**
   * Post-DNS address mapper: remaps the resolved peer to the rebind target only when the resolved
   * peer is the active rebind's affected node and the window is still open; else returns null (no
   * remap).
   */
  @Override
  public SocketAddress getSocketAddress(SocketAddress resolved) {
    RebindState s = rebind.get();
    if (s == null || s.deadlineNanos - clockNanos.getAsLong() <= 0) {
      return null;
    }
    return resolved.equals(s.affected) ? s.target : null;
  }

  /** True iff {@code peer} matches the active rebind's affected node. */
  boolean isAffected(SocketAddress peer) {
    RebindState s = rebind.get();
    return s != null && s.deadlineNanos - clockNanos.getAsLong() > 0 && peer.equals(s.affected);
  }

  /** Relaxes a borrowed connection for the remainder of the active rebind window. */
  public void relaxIfRebinding(Connection conn) {
    RebindState s = rebind.get();
    if (s == null) {
      return;
    }
    long remainingNanos = s.deadlineNanos - clockNanos.getAsLong();
    if (remainingNanos > 0) {
      conn.relaxTimeouts(Duration.ofNanos(remainingNanos));
    }
  }

  @Override
  public void onMoving(MovingEvent e, Connection c) {
    logger.debug("Moving to {} (seq={}, ttl={}s)", e.target, e.seq, e.ttlSeconds);
    // Any MOVING affects the receiver: mark for discard and relax commands on it before return.
    c.requestRebind();
    c.relaxTimeouts(Duration.ofSeconds(e.ttlSeconds));
    SocketAddress affected = c.getRemoteSocketAddress();
    if (affected == null) {
      return; // receiver socket already closed; no peer to register
    }
    SocketAddress target = new InetSocketAddress(e.target.getHost(), e.target.getPort());
    long deadline = clockNanos.getAsLong() + TimeUnit.SECONDS.toNanos(e.ttlSeconds);
    while (true) {
      RebindState cur = rebind.get();
      if (cur != null && e.seq <= cur.seq) {
        return; // duplicate or out-of-order event
      }
      if (rebind.compareAndSet(cur, new RebindState(e.seq, affected, target, deadline))) {
        logger.debug("Rebinding {} -> {} (seq={}, ttl={}s)", affected, target, e.seq, e.ttlSeconds);
        MaintenanceHandoff handoff = new MaintenanceHandoff(e.seq, e.target,
            Duration.ofSeconds(e.ttlSeconds));
        handoffHooks.forEach(hook -> hook.accept(handoff));
        return;
      }
      // Lost the CAS to a concurrent apply; retry (may then observe a stale seq).
    }
  }

  @Override
  public void onMigrating(MigratingEvent e, Connection c) {
    logger.debug("Migrating shards {} (seq={}, ttl={}s)", e.shardIds, e.seq, e.ttlSeconds);
    c.relaxTimeouts(Duration.ofNanos(maxRelaxedDurationNanos)); // time_s = "starts within";
                                                                // backstop
  }

  @Override
  public void onFailingOver(FailingOverEvent e, Connection c) {
    logger.debug("Failing over shards {} (seq={}, ttl={}s)", e.shardIds, e.seq, e.ttlSeconds);
    c.relaxTimeouts(Duration.ofNanos(maxRelaxedDurationNanos));
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

  /** Observation payload delivered to handoff hooks: seq, new endpoint, and the handoff window. */
  public static final class MaintenanceHandoff {
    private final long seq;
    private final HostAndPort target;
    private final Duration ttl;

    MaintenanceHandoff(long seq, HostAndPort target, Duration ttl) {
      this.seq = seq;
      this.target = target;
      this.ttl = ttl;
    }

    /** Monotonically-increasing sequence number of the originating MOVING event. */
    public long getSeq() {
      return seq;
    }

    /** The new endpoint connections will be routed to during the handoff window. */
    public HostAndPort getTarget() {
      return target;
    }

    /** Window the handoff is active for, after which the configured endpoint is used again. */
    public Duration getTtl() {
      return ttl;
    }

    @Override
    public String toString() {
      return "MaintenanceHandoff[seq=" + seq + ", target=" + target + ", ttl=" + ttl + "]";
    }
  }

  /** Immutable snapshot of an active, time-bounded MOVING rebind. */
  private static final class RebindState {
    final long seq;
    final SocketAddress affected; // receiver's resolved peer that received MOVING
    final SocketAddress target; // its replacement
    final long deadlineNanos;

    RebindState(long seq, SocketAddress affected, SocketAddress target, long deadlineNanos) {
      this.seq = seq;
      this.affected = affected;
      this.target = target;
      this.deadlineNanos = deadlineNanos;
    }
  }
}
