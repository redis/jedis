package redis.clients.jedis;

import java.util.Objects;

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
  MaintenanceEventId identity() {
    return new MaintenanceEventId(getClass(), seq);
  }
}

class MaintenanceEventId {

  private final Class<? extends MaintenanceEvent> type;
  private final long seq;

  MaintenanceEventId(Class<? extends MaintenanceEvent> type, long seq) {
    this.type = type;
    this.seq = seq;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MaintenanceEventId other = (MaintenanceEventId) o;
    return seq == other.seq && type == other.type;
  }

  @Override
  public int hashCode() {
    return 31 * Long.hashCode(seq) + type.hashCode();
  }
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

  @Override
  MovingEventId identity() {
    return new MovingEventId(seq, target);
  }
}

/** MOVING identity: seq + the original (unresolved) target endpoint, {@code null} = 'none'. */
final class MovingEventId extends MaintenanceEventId {

  /** Keying on the endpoint as sent keeps identity independent of DNS timing. */
  final HostAndPort endpoint;

  MovingEventId(long seq, HostAndPort endpoint) {
    super(MovingEvent.class, seq);
    this.endpoint = endpoint;
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o) && Objects.equals(endpoint, ((MovingEventId) o).endpoint);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hashCode(endpoint);
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
