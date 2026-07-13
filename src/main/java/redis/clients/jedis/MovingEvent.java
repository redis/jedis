package redis.clients.jedis;

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