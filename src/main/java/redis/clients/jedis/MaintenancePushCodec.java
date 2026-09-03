package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Decodes RESP3 maintenance push frames into {@link MaintenanceEvent}s or
 * {@link ClusterMaintenanceEvent}s — the push transport for maintenance notifications. Token
 * classification ({@link PushType#resolve}) and per-type field extraction ({@link #build},
 * {@link #buildCluster}) both live here.
 */
final class MaintenancePushCodec {

  /** A maintenance push wire type: its token and how to decode its frame content into an event. */
  enum PushType {
    MOVING(PushMessageTypes.MOVING_BYTES, MaintenancePushCodec::moving),
    MIGRATING(PushMessageTypes.MIGRATING_BYTES, MaintenancePushCodec::migrating),
    FAILING_OVER(PushMessageTypes.FAILING_OVER_BYTES, MaintenancePushCodec::failingOver),
    MIGRATED(PushMessageTypes.MIGRATED_BYTES, MaintenancePushCodec::migrated),
    FAILED_OVER(PushMessageTypes.FAILED_OVER_BYTES, MaintenancePushCodec::failedOver),
    SMIGRATING(PushMessageTypes.SMIGRATING_BYTES, MaintenancePushCodec::sMigrating),
    SMIGRATED(PushMessageTypes.SMIGRATED_BYTES, MaintenancePushCodec::sMigrated);

    private final byte[] token;
    private final Function<List<Object>, MaintenanceEvent> decoder;

    PushType(byte[] token, Function<List<Object>, MaintenanceEvent> decoder) {
      this.token = token;
      this.decoder = decoder;
    }

    /**
     * Resolves a push type token to its maintenance type, or {@code null} when it is not a
     * maintenance push. Length-switch fast path: rejects unrelated pushes with at most two
     * comparisons (MIGRATING and SMIGRATED share length 9).
     */
    static PushType resolve(byte[] type) {
      if (type == null) {
        return null;
      }
      switch (type.length) {
        case 6:
          return Arrays.equals(type, MOVING.token) ? MOVING : null;
        case 8:
          return Arrays.equals(type, MIGRATED.token) ? MIGRATED : null;
        case 9:
          if (Arrays.equals(type, MIGRATING.token)) {
            return MIGRATING;
          }
          return Arrays.equals(type, SMIGRATED.token) ? SMIGRATED : null;
        case 10:
          return Arrays.equals(type, SMIGRATING.token) ? SMIGRATING : null;
        case 11:
          return Arrays.equals(type, FAILED_OVER.token) ? FAILED_OVER : null;
        case 12:
          return Arrays.equals(type, FAILING_OVER.token) ? FAILING_OVER : null;
        default:
          return null;
      }
    }
  }

  /**
   * Builds the domain event for an already-resolved push type.
   * @throws MalformedMaintenanceEventException if the frame's fields are malformed (missing or
   *           wrong-typed seq/time/shards, or a MOVING with a missing or unparseable target — a
   *           null target is valid and denotes the {@code none} endpoint type)
   */
  static MaintenanceEvent build(PushType type, PushMessage msg) {
    return type.decoder.apply(msg.getContent());
  }

  private static MaintenanceEvent moving(List<Object> c) { // [MOVING, seq, time_s, host:port |
                                                           // null]
    if (c.size() < 4 || !(c.get(1) instanceof Long) || !(c.get(2) instanceof Long)) {
      throw malformed("MOVING", c);
    }
    // Explicit RESP3 null target => 'none' endpoint type (no remap). A byte[] target is parsed;
    // anything else (wrong type, unparseable) is malformed.
    HostAndPort target = c.get(3) == null ? null : parseHostPort(c, 3);
    return new MovingEvent((Long) c.get(1), (Long) c.get(2), target);
  }

  private static MaintenanceEvent migrating(List<Object> c) { // [MIGRATING, seq, time_s, shards]
    if (c.size() < 4 || !(c.get(1) instanceof Long) || !(c.get(2) instanceof Long)
        || !(c.get(3) instanceof byte[])) {
      throw malformed("MIGRATING", c);
    }
    return new MigratingEvent((Long) c.get(1), (Long) c.get(2), shardIds(c, 3));
  }

  private static MaintenanceEvent failingOver(List<Object> c) { // [FAILING_OVER, seq, time_s,
                                                                // shards]
    if (c.size() < 4 || !(c.get(1) instanceof Long) || !(c.get(2) instanceof Long)
        || !(c.get(3) instanceof byte[])) {
      throw malformed("FAILING_OVER", c);
    }
    return new FailingOverEvent((Long) c.get(1), (Long) c.get(2), shardIds(c, 3));
  }

  private static MaintenanceEvent migrated(List<Object> c) { // [MIGRATED, seq, shards]
    if (c.size() < 3 || !(c.get(1) instanceof Long) || !(c.get(2) instanceof byte[])) {
      throw malformed("MIGRATED", c);
    }
    return new MigratedEvent((Long) c.get(1), shardIds(c, 2));
  }

  private static MaintenanceEvent failedOver(List<Object> c) { // [FAILED_OVER, seq, shards]
    if (c.size() < 3 || !(c.get(1) instanceof Long) || !(c.get(2) instanceof byte[])) {
      throw malformed("FAILED_OVER", c);
    }
    return new FailedOverEvent((Long) c.get(1), shardIds(c, 2));
  }

  private static ClusterMaintenanceEvent sMigrating(List<Object> c) { // [SMIGRATING, seq, slots]
    if (c.size() < 3 || !(c.get(1) instanceof Long) || !(c.get(2) instanceof byte[])) {
      throw malformed("SMIGRATING", c);
    }
    return new SMigratingEvent((Long) c.get(1), slotRanges("SMIGRATING", c, (byte[]) c.get(2)));
  }

  private static ClusterMaintenanceEvent sMigrated(List<Object> c) { // [SMIGRATED, seq,
                                                                     // [[src, dest, slots]...]]
    if (c.size() < 3 || !(c.get(1) instanceof Long) || !(c.get(2) instanceof List)) {
      throw malformed("SMIGRATED", c);
    }
    List<?> entries = (List<?>) c.get(2);
    List<SlotMigration> migrations = new ArrayList<>(entries.size());
    for (Object entryObj : entries) {
      if (!(entryObj instanceof List)) {
        throw malformed("SMIGRATED", c);
      }
      List<?> e = (List<?>) entryObj;
      if (e.size() < 3 || !(e.get(0) instanceof byte[]) || !(e.get(1) instanceof byte[])
          || !(e.get(2) instanceof byte[])) {
        throw malformed("SMIGRATED", c);
      }
      migrations.add(new SlotMigration(nodeAddress("SMIGRATED", c, (byte[]) e.get(0)),
          nodeAddress("SMIGRATED", c, (byte[]) e.get(1)),
          slotRanges("SMIGRATED", c, (byte[]) e.get(2))));
    }
    return new SMigratedEvent((Long) c.get(1), migrations);
  }

  /** Slots-or-ranges field, e.g. {@code 123,456,789-1000}; throws when unparseable. */
  private static HashSlotRanges slotRanges(String type, List<Object> c, byte[] raw) {
    try {
      return HashSlotRanges.parse(SafeEncoder.encode(raw));
    } catch (IllegalArgumentException e) {
      throw new MalformedMaintenanceEventException("Unparseable " + type + " slots: " + c, e);
    }
  }

  /** Bare {@code host:port} node address (no labels on the wire); throws when unparseable. */
  private static HostAndPort nodeAddress(String type, List<Object> c, byte[] raw) {
    try {
      return HostAndPort.from(SafeEncoder.encode(raw));
    } catch (Exception e) {
      throw new MalformedMaintenanceEventException("Unparseable " + type + " node address: " + c,
          e);
    }
  }

  /** Diagnostic shard-id list (stringified JSON array), logging only; required on the wire. */
  private static String shardIds(List<Object> c, int i) {
    return SafeEncoder.encode((byte[]) c.get(i));
  }

  /** MOVING target {@code host:port}; throws when the target is absent or unparseable. */
  private static HostAndPort parseHostPort(List<Object> c, int i) {
    if (i >= c.size() || !(c.get(i) instanceof byte[])) {
      throw new MalformedMaintenanceEventException(
          "MOVING target must be a host:port byte[] at index " + i + ": " + c);
    }
    try {
      return HostAndPort.from(SafeEncoder.encode((byte[]) c.get(i)));
    } catch (Exception e) {
      throw new MalformedMaintenanceEventException("Unparseable MOVING target: " + c, e);
    }
  }

  private static MalformedMaintenanceEventException malformed(String type, List<Object> c) {
    return new MalformedMaintenanceEventException("Malformed " + type + " push: " + c);
  }

  private MaintenancePushCodec() {
  }
}
