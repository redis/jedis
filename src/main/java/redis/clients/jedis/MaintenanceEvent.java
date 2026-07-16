package redis.clients.jedis;

/**
 * A server maintenance event. One subclass per type, each carrying the fields relevant to that
 * type. Dispatched to a {@link MaintenanceEventHandler} when provided.
 */
public abstract class MaintenanceEvent {

  public enum EventType {
    MOVING, MIGRATING, MIGRATED, FAILING_OVER, FAILED_OVER
  }

  final long seq;

  MaintenanceEvent(long seq) {
    this.seq = seq;
  }

  abstract void accept(MaintenanceEventHandler listener, Connection conn);

  public long getSeq() {
    return seq;
  }

  public abstract EventType getType();

  /**
   * {@code [MOVING, seq, time_s, host:port]} — endpoint moves to {@code target} within
   * {@code ttlSeconds}.
   */
  static final class MovingEvent extends MaintenanceEvent {
    final long ttlSeconds;
    final HostAndPort target;

    MovingEvent(long seq, long ttlSeconds, HostAndPort target) {
      super(seq);
      this.ttlSeconds = ttlSeconds;
      this.target = target;
    }

    @Override
    void accept(MaintenanceEventHandler handler, Connection c) {
      handler.onMoving(this, c);
    }

    public long getTtlSeconds() {
      return ttlSeconds;
    }

    public HostAndPort getTarget() {
      return target;
    }

    @Override
    public EventType getType() {
      return EventType.MOVING;
    }
  }

  /**
   * {@code [MIGRATING, seq, time_s, shards]} — {@code time_s} = starts-within; {@code shardIds}
   * diagnostic.
   */
  static final class MigratingEvent extends MaintenanceEvent {
    final long ttlSeconds;
    final String shardIds;

    MigratingEvent(long seq, long ttlSeconds, String shardIds) {
      super(seq);
      this.ttlSeconds = ttlSeconds;
      this.shardIds = shardIds;
    }

    @Override
    void accept(MaintenanceEventHandler handler, Connection c) {
      handler.onMigrating(this, c);
    }

    public long getTtlSeconds() {
      return ttlSeconds;
    }

    public String getShardIds() {
      return shardIds;
    }

    @Override
    public EventType getType() {
      return EventType.MIGRATING;
    }
  }

  /** {@code [MIGRATED, seq, shards]} — terminator; no time_s on the wire. */
  static final class MigratedEvent extends MaintenanceEvent {
    final String shardIds;

    MigratedEvent(long seq, String shardIds) {
      super(seq);
      this.shardIds = shardIds;
    }

    @Override
    void accept(MaintenanceEventHandler handler, Connection c) {
      handler.onMigrated(this, c);
    }

    public String getShardIds() {
      return shardIds;
    }

    @Override
    public EventType getType() {
      return EventType.MIGRATED;
    }
  }

  /**
   * {@code [FAILING_OVER, seq, time_s, shards]} — {@code time_s} = starts-within; {@code shardIds}
   * diagnostic.
   */
  static final class FailingOverEvent extends MaintenanceEvent {
    final long ttlSeconds;
    final String shardIds;

    FailingOverEvent(long seq, long ttlSeconds, String shardIds) {
      super(seq);
      this.ttlSeconds = ttlSeconds;
      this.shardIds = shardIds;
    }

    @Override
    void accept(MaintenanceEventHandler handler, Connection c) {
      handler.onFailingOver(this, c);
    }

    public long getTtlSeconds() {
      return ttlSeconds;
    }

    public String getShardIds() {
      return shardIds;
    }

    @Override
    public EventType getType() {
      return EventType.FAILING_OVER;
    }
  }

  /** {@code [FAILED_OVER, seq, shards]} — terminator. */
  static final class FailedOverEvent extends MaintenanceEvent {
    final String shardIds;

    FailedOverEvent(long seq, String shardIds) {
      super(seq);
      this.shardIds = shardIds;
    }

    @Override
    void accept(MaintenanceEventHandler handler, Connection c) {
      handler.onFailedOver(this, c);
    }

    public String getShardIds() {
      return shardIds;
    }

    @Override
    public EventType getType() {
      return EventType.FAILED_OVER;
    }
  }
}
