package redis.clients.jedis;

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