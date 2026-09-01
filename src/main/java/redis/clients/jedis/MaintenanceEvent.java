package redis.clients.jedis;

import java.util.Arrays;

/**
 * A server maintenance event. Dispatched to a {@link MaintenanceEventListener} via {@link #accept}.
 */
abstract class MaintenanceEvent {

  final long seq;

  MaintenanceEvent(long seq) {
    this.seq = seq;
  }

  abstract void accept(MaintenanceEventListener listener, Connection conn);

  /**
   * Identity of the server-side operation this event announces; equality is the dedup rule for
   * folding per-connection deliveries into one pool-wide operation.
   */
  Object identity() {
    return seq; // sufficient for every type but MOVING, which is also keyed by target
  }
}

/**
 * {@code [MOVING, seq, time_s, host:port | null]} — endpoint moves to {@code target} within
 * {@code gracePeriodSeconds}, then the old endpoint hard-disconnects. A {@code null} target is the
 * {@code none} endpoint type: no remap; reconnect to the configured endpoint.
 */
final class MovingEvent extends MaintenanceEvent {
  final long gracePeriodSeconds;
  /**
   * New endpoint, or {@code null} for the {@code none} type (reconnect to the configured endpoint).
   */
  final HostAndPort target;

  /**
   * Concurrent MOVINGs are told apart by target;
   */
  private final Object identity;

  MovingEvent(long seq, long gracePeriodSeconds, HostAndPort target) {
    super(seq);
    this.gracePeriodSeconds = gracePeriodSeconds;
    this.target = target;
    this.identity = Arrays.asList(seq, target);
  }

  @Override
  void accept(MaintenanceEventListener l, Connection c) {
    l.onMoving(this, c);
  }

  @Override
  Object identity() {
    return identity;
  }
}

/**
 * {@code [MIGRATING, seq, time_s, shards]} — {@code time_s} = starts-within; {@code shardIds}
 * diagnostic.
 */
final class MigratingEvent extends MaintenanceEvent {
  final long startsInSeconds;
  final String shardIds;

  MigratingEvent(long seq, long startsInSeconds, String shardIds) {
    super(seq);
    this.startsInSeconds = startsInSeconds;
    this.shardIds = shardIds;
  }

  @Override
  void accept(MaintenanceEventListener l, Connection c) {
    l.onMigrating(this, c);
  }
}

/**
 * {@code [FAILING_OVER, seq, time_s, shards]} — {@code time_s} = starts-within; {@code shardIds}
 * diagnostic.
 */
final class FailingOverEvent extends MaintenanceEvent {
  final long startsInSeconds;
  final String shardIds;

  FailingOverEvent(long seq, long startsInSeconds, String shardIds) {
    super(seq);
    this.startsInSeconds = startsInSeconds;
    this.shardIds = shardIds;
  }

  @Override
  void accept(MaintenanceEventListener l, Connection c) {
    l.onFailingOver(this, c);
  }
}

/** {@code [MIGRATED, seq, shards]} — terminator; no time_s on the wire. */
final class MigratedEvent extends MaintenanceEvent {
  final String shardIds;

  MigratedEvent(long seq, String shardIds) {
    super(seq);
    this.shardIds = shardIds;
  }

  @Override
  void accept(MaintenanceEventListener l, Connection c) {
    l.onMigrated(this, c);
  }
}

/** {@code [FAILED_OVER, seq, shards]} — terminator. */
final class FailedOverEvent extends MaintenanceEvent {
  final String shardIds;

  FailedOverEvent(long seq, String shardIds) {
    super(seq);
    this.shardIds = shardIds;
  }

  @Override
  void accept(MaintenanceEventListener l, Connection c) {
    l.onFailedOver(this, c);
  }
}
