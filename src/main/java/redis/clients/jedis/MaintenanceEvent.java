package redis.clients.jedis;

/**
 * A server maintenance event. One subclass per type, each carrying the fields relevant to that
 * type. Dispatched to a {@link MaintenanceEventListener} via {@link #accept}.
 */
public abstract class MaintenanceEvent {

  final long seq;

  MaintenanceEvent(long seq) {
    this.seq = seq;
  }

  abstract void accept(MaintenanceEventListener listener, Connection conn);

  public long getSeq() {
    return seq;
  }
}

/**
 * {@code [MOVING, seq, time_s, host:port]} — endpoint moves to {@code target} within
 * {@code ttlSeconds}.
 */
public final class MovingEvent extends MaintenanceEvent {
  final long ttlSeconds;
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

  public long getTtlSeconds() {
    return ttlSeconds;
  }

  public HostAndPort getTarget() {
    return target;
  }
}

/**
 * {@code [MIGRATING, seq, time_s, shards]} — {@code time_s} = starts-within; {@code shardIds}
 * diagnostic.
 */
public final class MigratingEvent extends MaintenanceEvent {
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

  public long getTtlSeconds() {
    return ttlSeconds;
  }

  public String getShardIds() {
    return shardIds;
  }
}

/**
 * {@code [FAILING_OVER, seq, time_s, shards]} — {@code time_s} = starts-within; {@code shardIds}
 * diagnostic.
 */
public final class FailingOverEvent extends MaintenanceEvent {
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

  public long getTtlSeconds() {
    return ttlSeconds;
  }

  public String getShardIds() {
    return shardIds;
  }
}

/** {@code [MIGRATED, seq, shards]} — terminator; no time_s on the wire. */
public final class MigratedEvent extends MaintenanceEvent {
  final String shardIds;

  MigratedEvent(long seq, String shardIds) {
    super(seq);
    this.shardIds = shardIds;
  }

  @Override
  void accept(MaintenanceEventListener l, Connection c) {
    l.onMigrated(this, c);
  }

  public String getShardIds() {
    return shardIds;
  }
}

/** {@code [FAILED_OVER, seq, shards]} — terminator. */
public final class FailedOverEvent extends MaintenanceEvent {
  final String shardIds;

  FailedOverEvent(long seq, String shardIds) {
    super(seq);
    this.shardIds = shardIds;
  }

  @Override
  void accept(MaintenanceEventListener l, Connection c) {
    l.onFailedOver(this, c);
  }

  public String getShardIds() {
    return shardIds;
  }
}
