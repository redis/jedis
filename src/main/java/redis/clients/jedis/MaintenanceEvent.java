package redis.clients.jedis;

/**
 * A server maintenance event. One subclass per type, each carrying the fields relevant to that
 * type. Dispatched to a {@link MaintenanceEventListener} when provided.
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
