package redis.clients.jedis;

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