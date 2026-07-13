package redis.clients.jedis;

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