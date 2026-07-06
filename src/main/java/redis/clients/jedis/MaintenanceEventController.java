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
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.TimeoutSupplierChain.TimeoutInfo;

/**
 * Maintenance handler: owns the shared rebind overlay, the relax-window policy, and the handoff
 * hooks fired when a MOVING is applied.
 */
final class MaintenanceEventController implements MaintenanceEventListener, SocketAddressMapper {

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceEventController.class);

  private final MaintenanceNotificationsConfig config;
  private final long maxRelaxedDurationNanos; // MIGRATING/FAILING_OVER backstop window
  private static final AtomicReferenceFieldUpdater<MaintenanceEventController, RebindState> REBIND = AtomicReferenceFieldUpdater
      .newUpdater(MaintenanceEventController.class, RebindState.class, "rebind");
  private volatile RebindState rebind = RebindState.EXPIRED_STATE;
  /** Synchronous hooks fired once per applied MOVING handoff; see {@link #addHandoffHook}. */
  private final List<Consumer<MaintenanceHandoff>> handoffHooks = new CopyOnWriteArrayList<>();
  private final Supplier<TimeoutInfo> timeoutSupplier;

  private MaintenanceEventController(MaintenanceNotificationsConfig config) {
    this.config = config;
    this.maxRelaxedDurationNanos = config.getRelaxedWindowMaxDuration().toNanos();

    TimeoutInfo relaxedTimeoutInfo = new TimeoutInfo(config.getRelaxedTimeout(),
        config.getRelaxedBlockingTimeout());
    this.timeoutSupplier = () -> rebind.isValid() ? relaxedTimeoutInfo : null;
  }

  /**
   * Construct a controller from the given config. Use this from a builder when the config
   * {@link MaintenanceNotificationsConfig#isEnabledOrAuto()} — for DISABLED, leave the controller
   * unset.
   */
  public static MaintenanceEventController from(MaintenanceNotificationsConfig cfg) {
    return new MaintenanceEventController(cfg);
  }

  /**
   * The config this controller was built from; drives the connection's MAINT_NOTIFICATIONS
   * handshake.
   */
  MaintenanceNotificationsConfig getConfig() {
    return config;
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
    // Cap the server-supplied ttl at the user-configured backstop so a generous or misbehaving
    // server can't pin the pool in relaxed mode beyond {@code relaxedTimeoutMaxDuration}.
    long ttlNanos = Math.min(TimeUnit.SECONDS.toNanos(e.ttlSeconds), maxRelaxedDurationNanos);
    // Any MOVING affects the receiver: mark for discard and relax commands on it before return.
    // QUESTION : are we relying on each in-use connection that connected to a rebinding endpoint
    // will receive a MOVING and then eventually will be marked for discard? If so, we
    // may want to consider a more aggressive approach to mark connections that are connected to
    // the rebinding endpoint for discard.

    // UPDATE: we have two different spots that causes connection to be dropped. One is setting
    // expireAt for connections that received MOVING event, and the other is when a connection is
    // returned to the pool, we check if it is connected to a rebinding endpoint(isAffected) and if
    // so, we drop it. So we are covering for both cases.

    // IMPORTANT: in case we like to delay the expiration since NONE as new target may be sent,
    // dropping the connections immediately just by relying isAffected would be an issue, and cause
    // immediate drops while it is not supposed to be.

    long deadline = NanoClock.INSTANCE.getAsLong() + ttlNanos;
    // HERE! we can control if an immediate gracefull drop needed,, or we need to delay in case
    // MOVING msg suggests a future renewal.
    c.expireAt(deadline);
    SocketAddress affectedPeer = c.getRemoteSocketAddress();
    if (affectedPeer == null) {
      return; // receiver socket already closed; no peer to register
    }
    SocketAddress target = new InetSocketAddress(e.target.getHost(), e.target.getPort());

    while (true) {
      RebindState cur = rebind;
      if (e.seq < cur.seq) {
        return; // older event
      }
      if (e.seq == cur.seq) {
        // Same handoff event from another affected connection: merge its peer into the set.
        if (!target.equals(cur.target)) {
          logger.warn("Ignoring MOVING with conflicting target for seq {}: have {}, got {}", e.seq,
            cur.target, target);
          return;
        }
        if (cur.affected.contains(affectedPeer)) {
          return; // already merged
        }
        RebindState next = cur.merge(affectedPeer);
        if (REBIND.compareAndSet(this, cur, next)) {
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

      if (REBIND.compareAndSet(this, cur, next)) {
        logger.debug("Rebinding {} -> {} (seq={}, ttl={}s)", affectedPeer, target, e.seq,
          e.ttlSeconds);
        fireHandoffHook(e);
        return;
      }
      // CAS lost (concurrent apply); retry — may then see same-seq merge path or older-seq exit.
    }
  }

  public Supplier<TimeoutInfo> getTimeoutSupplier() {
    return timeoutSupplier;
  }

  private void fireHandoffHook(MovingEvent e) {
    MaintenanceHandoff handoff = new MaintenanceHandoff(e.seq, e.target,
        Duration.ofSeconds(e.ttlSeconds));
    handoffHooks.forEach(hook -> hook.accept(handoff));
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

    static final RebindState EXPIRED_STATE = new RebindState(-1, Collections.emptySet(), null, 0L);
    final long seq;
    final Set<SocketAddress> affected;
    final SocketAddress target;
    final long deadlineNanos;
    volatile boolean expired = false;

    private RebindState(long seq, Set<SocketAddress> affected, SocketAddress target,
        long deadlineNanos) {
      this.seq = seq;
      this.affected = affected;
      this.target = target;
      this.deadlineNanos = deadlineNanos;
    }

    private RebindState merge(SocketAddress peer) {
      Set<SocketAddress> merged = new HashSet<>(affected.size() + 1, 1.0f);
      merged.addAll(affected);
      merged.add(peer);
      return new RebindState(seq, Collections.unmodifiableSet(merged), target, deadlineNanos);
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
