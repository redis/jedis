package redis.clients.jedis;

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