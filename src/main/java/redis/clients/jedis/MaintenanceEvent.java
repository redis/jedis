package redis.clients.jedis;

/**
 * A server maintenance event. One subclass per type, each carrying the fields relevant to that
 * type. Dispatched to a {@link MaintenanceEventListener} via {@link #accept}.
 */
abstract class MaintenanceEvent {

  final long seq;

  MaintenanceEvent(long seq) {
    this.seq = seq;
  }

  abstract void accept(MaintenanceEventListener listener, Connection conn);
}

/**
 * {@code [MOVING, seq, time_s, host:port | null]} — endpoint moves to {@code target} within
 * {@code ttlSeconds}. A {@code null} target is the {@code none} endpoint type: no remap; reconnect
 * to the configured endpoint.
 */
final class MovingEvent extends MaintenanceEvent {
  final long ttlSeconds;
  /**
   * New endpoint, or {@code null} for the {@code none} type (reconnect to the configured endpoint).
   */
  final HostAndPort target;

  MovingEvent(long seq, long ttlSeconds, HostAndPort target) {
    super(seq);
    this.ttlSeconds = ttlSeconds;
    this.target = target;
  }

  @Override
  void accept(MaintenanceEventListener l, Connection c) {
    l.onMoving(this, c);
  }
}

/**
 * {@code [MIGRATING, seq, time_s, shards]} — {@code time_s} = starts-within; {@code shardIds}
 * diagnostic.
 */
final class MigratingEvent extends MaintenanceEvent {
  final long ttlSeconds;
  final String shardIds;

  MigratingEvent(long seq, long ttlSeconds, String shardIds) {
    super(seq);
    this.ttlSeconds = ttlSeconds;
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
  final long ttlSeconds;
  final String shardIds;

  FailingOverEvent(long seq, long ttlSeconds, String shardIds) {
    super(seq);
    this.ttlSeconds = ttlSeconds;
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
