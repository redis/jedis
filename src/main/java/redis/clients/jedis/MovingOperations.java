package redis.clients.jedis;

import java.net.SocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * The pool's active MOVING operations, each deduplicated (by the event's {@link MovingEventId})
 * from the per-connection MOVING events announcing it. Purely a store: admission (dedup + merge)
 * and queries; the dedup rule lives on the event, and reacting to an admitted operation is the
 * caller's job. Operations never supersede each other, they only expire; reads remove expired
 * entries one by one on detection, so the hot path costs the same after a maintenance cycle as
 * before it.
 */
final class MovingOperations {

  private final ConcurrentHashMap<MovingEventId, MovingOperation> operations = new ConcurrentHashMap<>();

  /**
   * Processes one MOVING event delivery. Returns the operation snapshot this delivery produced — a
   * fresh operation for a new identity, or the merged operation for a known one joined by a new
   * receiver peer — or null when the delivery changed nothing (known operation, known peer:
   * temporal churn immunity). Null means: do not react. Re-deliveries exit lock-free on the initial
   * get: an event is delivered once per connection, so all but the first delivery per peer take
   * that path.
   */
  MovingOperation process(MovingEvent e, SocketAddress receiverPeer) {
    MovingEventId id = e.identity();
    MovingOperation existing = operations.get(id);
    if (existing != null && existing.affected.contains(receiverPeer)) {
      return null;
    }
    long now = NanoClock.INSTANCE.getAsLong();
    MovingOperation[] applied = new MovingOperation[1];
    operations.compute(id, (k, cur) -> { // atomic per identity
      if (cur == null) {
        return applied[0] = new MovingOperation(e, receiverPeer, now);
      }
      if (cur.affected.contains(receiverPeer)) {
        return cur; // a racing delivery already merged this peer; no transition
      }
      return applied[0] = cur.merge(receiverPeer);
    });
    return applied[0];
  }

  /**
   * The single active (unexpired) operation matching {@code predicate}, or null — the predicate is
   * tested on top of the active filter, never against expired operations; expired entries found
   * along the way are removed one by one. If several match, an arbitrary one is returned: callers'
   * predicates are expected to identify at most one operation (peers are disjoint across concurrent
   * operations).
   */
  MovingOperation findActive(Predicate<MovingOperation> predicate) {
    if (operations.isEmpty()) {
      return null;
    }
    MovingOperation match = null;
    for (MovingOperation op : operations.values()) {
      if (!op.isValid()) {
        operations.remove(op.id, op);
      } else if (predicate.test(op)) {
        match = op;
      }
    }
    return match;
  }

  /** True while any MOVING operation is unexpired; the pool-wide relax gate. */
  boolean hasActive() {
    return findActive(op -> true) != null;
  }

  /**
   * One server-side MOVING operation, as aggregated by the pool from the per-connection MOVING
   * events announcing it. Defined by its first-admitted event: {@code seq}, {@code endpoint} and
   * the windows come from that delivery and never change. Immutable: {@code affected} holds the
   * receivers' resolved peers (one per affected connection that delivered the event); deliveries
   * from new peers merge by swapping in a new instance.
   */
  static final class MovingOperation {

    private final MovingEventId id;
    final long seq;
    /** Original target as sent by the server, unresolved; null = 'none' (no remap). */
    final HostAndPort endpoint;
    final Set<SocketAddress> affected;
    final long deadlineNanos;
    final long reconnectAtNanos;

    private MovingOperation(MovingEvent e, SocketAddress receiverPeer, long now) {
      this.id = e.identity();
      this.seq = e.seq;
      this.endpoint = e.target;
      this.affected = Collections.singleton(receiverPeer);
      // expires at = observed at + time_s
      this.deadlineNanos = now + TimeUnit.SECONDS.toNanos(e.ttlSeconds);
      // Reconnect instant: a real target marks immediately; 'none' marks at half the raw grace,
      this.reconnectAtNanos = e.target == null ? now + TimeUnit.SECONDS.toNanos(e.ttlSeconds) / 2
          : now;
    }

    private MovingOperation(MovingOperation source, Set<SocketAddress> affected) {
      this.id = source.id;
      this.seq = source.seq;
      this.endpoint = source.endpoint;
      this.affected = affected;
      this.deadlineNanos = source.deadlineNanos;
      this.reconnectAtNanos = source.reconnectAtNanos;
    }

    /**
     * A later delivery of the same operation only widens the peer set — its event never moves the
     * windows (a re-delivery may carry an adjusted remaining time; identity excludes it for exactly
     * this reason).
     */
    private MovingOperation merge(SocketAddress peer) {
      Set<SocketAddress> merged = new HashSet<>(affected.size() + 1, 1.0f);
      merged.addAll(affected);
      merged.add(peer);
      return new MovingOperation(this, Collections.unmodifiableSet(merged));
    }

    /** True while the operation's window is open;. */
    boolean isValid() {
      return deadlineNanos - NanoClock.INSTANCE.getAsLong() > 0;
    }
  }
}