package redis.clients.jedis;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.util.SafeEncoder;

/**
 * A parsed server maintenance push notification. One subclass per type; each carries the fields
 * relevant to that type. Dispatched to a {@link Handler} via the visitor {@link #accept}.
 */
abstract class MaintenanceEvent {

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceEvent.class);

  final long seq;

  MaintenanceEvent(long seq) {
    this.seq = seq;
  }

  abstract void accept(Handler handler, Connection conn);

  /** Typed visitor for maintenance events. */
  interface Handler {
    void onMoving(MovingEvent e, Connection c);
    void onMigrating(MigratingEvent e, Connection c);
    void onMigrated(MigratedEvent e, Connection c);
    void onFailingOver(FailingOverEvent e, Connection c);
    void onFailedOver(FailedOverEvent e, Connection c);
  }

  /**
   * Returns {@code true} iff {@code t} is a maintenance push type. Length-dispatched (each length
   * maps to a single type), so the common non-maintenance path is a switch with no allocation and
   * at most one {@link Arrays#equals(byte[], byte[])}. Lets the consumer skip {@link #parse} for
   * unrelated pushes.
   */
  static boolean isMaintenanceType(byte[] t) {
    if (t == null) {
      return false;
    }
    switch (t.length) {
      case 6:
        return Arrays.equals(t, PushMessageTypes.MOVING_BYTES);
      case 8:
        return Arrays.equals(t, PushMessageTypes.MIGRATED_BYTES);
      case 9:
        return Arrays.equals(t, PushMessageTypes.MIGRATING_BYTES);
      case 11:
        return Arrays.equals(t, PushMessageTypes.FAILED_OVER_BYTES);
      case 12:
        return Arrays.equals(t, PushMessageTypes.FAILING_OVER_BYTES);
      default:
        return false;
    }
  }

  /**
   * Classifies a maintenance push frame into a typed event, or {@code null} when malformed (a
   * warning is logged). Call only for frames {@link #isMaintenanceType accepted} as maintenance
   * types. Required fields: an integer {@code seq}, an integer {@code time_s} where present, a
   * valid MOVING target, and the {@code shardIds} of the latency types; otherwise the frame is
   * logged and skipped. {@code shardIds} are used for logging only but must be present.
   */
  static MaintenanceEvent parse(PushMessage msg) {
    byte[] t = msg.getType();
    if (t == null) {
      return null;
    }
    List<Object> c = msg.getContent();
    if (Arrays.equals(t, PushMessageTypes.MOVING_BYTES)) {        // [MOVING, seq, time_s, host:port]
      if (!hasLongs(c, 1, 2)) {
        return malformed(msg);
      }
      HostAndPort target = parseHostPort(c, 3);                   // logs on failure
      return target != null ? new MovingEvent((Long) c.get(1), (Long) c.get(2), target) : null;
    }
    if (Arrays.equals(t, PushMessageTypes.MIGRATING_BYTES)) {     // [MIGRATING, seq, time_s, shards]
      return hasLongs(c, 1, 2) && hasBytes(c, 3)
          ? new MigratingEvent((Long) c.get(1), (Long) c.get(2), shardIds(c, 3)) : malformed(msg);
    }
    if (Arrays.equals(t, PushMessageTypes.FAILING_OVER_BYTES)) {  // [FAILING_OVER, seq, time_s, shards]
      return hasLongs(c, 1, 2) && hasBytes(c, 3)
          ? new FailingOverEvent((Long) c.get(1), (Long) c.get(2), shardIds(c, 3)) : malformed(msg);
    }
    if (Arrays.equals(t, PushMessageTypes.MIGRATED_BYTES)) {      // [MIGRATED, seq, shards]
      return hasLongs(c, 1) && hasBytes(c, 2)
          ? new MigratedEvent((Long) c.get(1), shardIds(c, 2)) : malformed(msg);
    }
    if (Arrays.equals(t, PushMessageTypes.FAILED_OVER_BYTES)) {   // [FAILED_OVER, seq, shards]
      return hasLongs(c, 1) && hasBytes(c, 2)
          ? new FailedOverEvent((Long) c.get(1), shardIds(c, 2)) : malformed(msg);
    }
    return null;
  }

  private static boolean hasLongs(List<Object> c, int... idx) {
    for (int i : idx) {
      if (i >= c.size() || !(c.get(i) instanceof Long)) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasBytes(List<Object> c, int i) {
    return i < c.size() && c.get(i) instanceof byte[];
  }

  private static MaintenanceEvent malformed(PushMessage msg) {
    logger.warn("Ignoring malformed maintenance push: {}", msg.getContent());
    return null;
  }

  /** Diagnostic shard-id list (stringified JSON array), used for logging only; required on the wire. */
  private static String shardIds(List<Object> c, int i) {
    return SafeEncoder.encode((byte[]) c.get(i));
  }

  private static HostAndPort parseHostPort(List<Object> c, int i) {
    if (i >= c.size() || !(c.get(i) instanceof byte[])) {
      logger.warn("Invalid MOVING target, expected host:port byte[] at index {}: {}", i, c);
      return null;
    }
    try {
      return HostAndPort.from(SafeEncoder.encode((byte[]) c.get(i)));
    } catch (Exception e) {
      logger.warn("Error parsing MOVING target from {}", c, e);
      return null;
    }
  }
}

/** {@code [MOVING, seq, time_s, host:port]} — endpoint moves to {@code target} within {@code ttlSeconds}. */
final class MovingEvent extends MaintenanceEvent {
  final long ttlSeconds;
  final HostAndPort target;

  MovingEvent(long seq, long ttlSeconds, HostAndPort target) {
    super(seq);
    this.ttlSeconds = ttlSeconds;
    this.target = target;
  }

  @Override
  void accept(Handler h, Connection c) {
    h.onMoving(this, c);
  }
}

/** {@code [MIGRATING, seq, time_s, shards]} — {@code time_s} = starts-within; {@code shardIds} diagnostic. */
final class MigratingEvent extends MaintenanceEvent {
  final long ttlSeconds;
  final String shardIds;

  MigratingEvent(long seq, long ttlSeconds, String shardIds) {
    super(seq);
    this.ttlSeconds = ttlSeconds;
    this.shardIds = shardIds;
  }

  @Override
  void accept(Handler h, Connection c) {
    h.onMigrating(this, c);
  }
}

/** {@code [FAILING_OVER, seq, time_s, shards]} — {@code time_s} = starts-within; {@code shardIds} diagnostic. */
final class FailingOverEvent extends MaintenanceEvent {
  final long ttlSeconds;
  final String shardIds;

  FailingOverEvent(long seq, long ttlSeconds, String shardIds) {
    super(seq);
    this.ttlSeconds = ttlSeconds;
    this.shardIds = shardIds;
  }

  @Override
  void accept(Handler h, Connection c) {
    h.onFailingOver(this, c);
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
  void accept(Handler h, Connection c) {
    h.onMigrated(this, c);
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
  void accept(Handler h, Connection c) {
    h.onFailedOver(this, c);
  }
}
