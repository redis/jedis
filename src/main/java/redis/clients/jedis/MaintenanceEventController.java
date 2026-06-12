package redis.clients.jedis;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maintenance handler: owns the shared rebind overlay, the relax-window policy, and the handoff
 * hooks fired when a MOVING is applied.
 */
public final class MaintenanceEventController
    implements MaintenanceEvent.Handler, SocketAddressMapper {

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceEventController.class);

  private final MaintenanceNotificationsConfig config;
  private final long maxRelaxedDurationNanos; // MIGRATING/FAILING_OVER backstop window
  private LongSupplier clockNanos = System::nanoTime; // test seam
  private final AtomicReference<RebindState> rebind = new AtomicReference<>();
  /** Synchronous hooks fired once per applied MOVING handoff; see {@link #addHandoffHook}. */
  private final List<Consumer<MaintenanceHandoff>> handoffHooks = new CopyOnWriteArrayList<>();

  private MaintenanceEventController(MaintenanceNotificationsConfig config) {
    this.config = config;
    this.maxRelaxedDurationNanos = config.getRelaxedWindowMaxDuration().toNanos();
  }

  /**
   * Construct a controller from the given config. Use this from a builder when the config
   * {@link MaintenanceNotificationsConfig#isEnabledOrAuto()} — for DISABLED, leave the controller
   * unset.
   */
  public static MaintenanceEventController from(MaintenanceNotificationsConfig cfg) {
    return new MaintenanceEventController(cfg);
  }

  /** Mode the controller was built with: {@code AUTO} or {@code ENABLED}. */
  public MaintenanceNotificationsConfig.Mode getMode() {
    return config.getMode();
  }

  /** Endpoint type to negotiate in {@code CLIENT MAINT_NOTIFICATIONS ON moving-endpoint-type}. */
  public MaintenanceNotificationsConfig.EndpointType getEndpointType() {
    return config.getEndpointType();
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
   * Post-DNS address mapper: remaps the resolved peer to the rebind target when the resolved peer
   * is one of the active rebind's affected sources and the window is still open; else returns null
   * (no remap). Lock-free read; {@link HashSet#contains} is O(1).
   */
  @Override
  public SocketAddress getSocketAddress(SocketAddress resolved) {
    RebindState s = rebind.get();
    if (s == null || s.deadlineNanos - clockNanos.getAsLong() <= 0) {
      return null;
    }
    return s.affected.contains(resolved) ? s.target : null;
  }

  /** True iff {@code peer} is one of the active rebind's affected sources. */
  boolean isAffected(SocketAddress peer) {
    RebindState s = rebind.get();
    return s != null && s.deadlineNanos - clockNanos.getAsLong() > 0 && s.affected.contains(peer);
  }

  /** True iff there is an active MOVING rebind window in the pool right now. */
  boolean isRebindActive() {
    RebindState s = rebind.get();
    if (s == null) return false;
    if (s.deadlineNanos - clockNanos.getAsLong() > 0) return true;
    rebind.compareAndSet(s, null); // lazy cleanup past deadline; loser silently skips
    return false;
  }

  /**
   * True iff {@code conn}'s next command should run with relaxed timeouts: either the connection
   * itself is in a per-receiver relaxation window (MIGRATING / FAILING_OVER / MOVING receiver) or a
   * pool-wide MOVING rebind window is currently active.
   */
  public boolean isTimeoutRelaxed(Connection conn) {
    return conn.isRelaxedTimeoutActive() || isRebindActive();
  }

  @Override
  public void onMoving(MovingEvent e, Connection c) {
    logger.debug("Moving to {} (seq={}, ttl={}s)", e.target, e.seq, e.ttlSeconds);
    // Cap the server-supplied ttl at the user-configured backstop so a generous or misbehaving
    // server can't pin the pool in relaxed mode beyond {@code relaxedTimeoutMaxDuration}.
    long ttlNanos = Math.min(TimeUnit.SECONDS.toNanos(e.ttlSeconds), maxRelaxedDurationNanos);
    // Any MOVING affects the receiver: mark for discard and relax commands on it before return.
    c.requestRebind();
    c.relaxTimeouts(Duration.ofNanos(ttlNanos));
    SocketAddress affectedPeer = c.getRemoteSocketAddress();
    if (affectedPeer == null) {
      return; // receiver socket already closed; no peer to register
    }
    SocketAddress target = new InetSocketAddress(e.target.getHost(), e.target.getPort());
    long deadline = clockNanos.getAsLong() + ttlNanos;

    while (true) {
      RebindState cur = rebind.get();
      if (cur != null && e.seq < cur.seq) {
        return; // older event
      }
      if (cur != null && e.seq == cur.seq) {
        // Same handoff event from another affected connection: merge its peer into the set.
        if (!target.equals(cur.target)) {
          logger.warn("Ignoring MOVING with conflicting target for seq {}: have {}, got {}", e.seq,
            cur.target, target);
          return;
        }
        if (cur.affected.contains(affectedPeer)) {
          return; // already merged
        }
        Set<SocketAddress> merged = new HashSet<>(cur.affected.size() + 1, 1.0f);
        merged.addAll(cur.affected);
        merged.add(affectedPeer);
        RebindState next = new RebindState(cur.seq, Collections.unmodifiableSet(merged), cur.target,
            cur.deadlineNanos);
        if (rebind.compareAndSet(cur, next)) {
          logger.debug("Merged source {} into rebind seq={} (sources={})", affectedPeer, cur.seq,
            next.affected.size());
          fireHandoffHook(e);
          return;
        }
        continue; // CAS lost; retry
      }
      // New seq (or first ever): replace state with a fresh single-source set.
      RebindState next = new RebindState(e.seq, Collections.singleton(affectedPeer), target,
          deadline);
      if (rebind.compareAndSet(cur, next)) {
        logger.debug("Rebinding {} -> {} (seq={}, ttl={}s)", affectedPeer, target, e.seq,
          e.ttlSeconds);
        fireHandoffHook(e);
        return;
      }
      // CAS lost (concurrent apply); retry — may then see same-seq merge path or older-seq exit.
    }
  }

  private void fireHandoffHook(MovingEvent e) {
    MaintenanceHandoff handoff = new MaintenanceHandoff(e.seq, e.target,
        Duration.ofSeconds(e.ttlSeconds));
    handoffHooks.forEach(hook -> hook.accept(handoff));
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

  /**
   * Immutable snapshot of an active, time-bounded MOVING rebind. {@code affected} holds the
   * receivers' resolved peers (one per affected connection that delivered a MOVING with this
   * {@code seq}); same-seq events with the same target merge by adding to this set under CAS. The
   * set is always immutable; updates swap the whole state.
   */
  private static final class RebindState {
    final long seq;
    final Set<SocketAddress> affected;
    final SocketAddress target;
    final long deadlineNanos;

    RebindState(long seq, Set<SocketAddress> affected, SocketAddress target, long deadlineNanos) {
      this.seq = seq;
      this.affected = affected;
      this.target = target;
      this.deadlineNanos = deadlineNanos;
    }
  }
}
