package redis.clients.jedis;

import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Known push message types sent by Redis server.
 * <p>
 * Push messages are asynchronous notifications sent by the server for various events:
 * <ul>
 * <li>Client-side cache invalidations</li>
 * <li>Pub/Sub messages and subscription confirmations</li>
 * </ul>
 * @see PushMessage
 * @see PushConsumer
 */
@Internal
public final class PushMessageTypes {

  private PushMessageTypes() {
    throw new InstantiationError("Must not instantiate this class");
  }

  // ==================== Client-Side Caching ====================

  /**
   * Cache invalidation notification.
   * <p>
   * Format: ["invalidate", [key1, key2, ...]]
   */
  public static final String INVALIDATE = "invalidate";
  public static final byte[] INVALIDATE_BYTES = SafeEncoder.encode(INVALIDATE);

  // ==================== Pub/Sub Messages ====================

  /**
   * Channel message.
   * <p>
   * Format: ["message", channel, message]
   */
  public static final String MESSAGE = "message";
  public static final byte[] MESSAGE_BYTES = SafeEncoder.encode(MESSAGE);

  /**
   * Pattern message.
   * <p>
   * Format: ["pmessage", pattern, channel, message]
   */
  public static final String PMESSAGE = "pmessage";
  public static final byte[] PMESSAGE_BYTES = SafeEncoder.encode(PMESSAGE);

  /**
   * Shard channel message.
   * <p>
   * Format: ["smessage", channel, message]
   */
  public static final String SMESSAGE = "smessage";
  public static final byte[] SMESSAGE_BYTES = SafeEncoder.encode(SMESSAGE);

  // ==================== Pub/Sub Subscriptions ====================

  /**
   * Channel subscription confirmation.
   * <p>
   * Format: ["subscribe", channel, count]
   */
  public static final String SUBSCRIBE = "subscribe";
  public static final byte[] SUBSCRIBE_BYTES = SafeEncoder.encode(SUBSCRIBE);

  /**
   * Pattern subscription confirmation.
   * <p>
   * Format: ["psubscribe", pattern, count]
   */
  public static final String PSUBSCRIBE = "psubscribe";
  public static final byte[] PSUBSCRIBE_BYTES = SafeEncoder.encode(PSUBSCRIBE);

  /**
   * Shard channel subscription confirmation.
   * <p>
   * Format: ["ssubscribe", channel, count]
   */
  public static final String SSUBSCRIBE = "ssubscribe";
  public static final byte[] SSUBSCRIBE_BYTES = SafeEncoder.encode(SSUBSCRIBE);

  // ==================== Pub/Sub Unsubscriptions ====================

  /**
   * Channel unsubscription confirmation.
   * <p>
   * Format: ["unsubscribe", channel, count]
   */
  public static final String UNSUBSCRIBE = "unsubscribe";
  public static final byte[] UNSUBSCRIBE_BYTES = SafeEncoder.encode(UNSUBSCRIBE);

  /**
   * Pattern unsubscription confirmation.
   * <p>
   * Format: ["punsubscribe", pattern, count]
   */
  public static final String PUNSUBSCRIBE = "punsubscribe";
  public static final byte[] PUNSUBSCRIBE_BYTES = SafeEncoder.encode(PUNSUBSCRIBE);

  /**
   * Shard channel unsubscription confirmation.
   * <p>
   * Format: ["sunsubscribe", channel, count]
   */
  public static final String SUNSUBSCRIBE = "sunsubscribe";
  public static final byte[] SUNSUBSCRIBE_BYTES = SafeEncoder.encode(SUNSUBSCRIBE);

  // ==================== Maintenance Events ====================

  /**
   * Endpoint handoff notification — connections to this endpoint must move to the new target within
   * the grace period, after which the old endpoint hard-disconnects.
   * <p>
   * Format: ["MOVING", seq, graceSeconds, "host:port"] — the target is null for the "none" endpoint
   * type: reconnect to the configured endpoint instead.
   * @since 8.1
   */
  public static final String MOVING = "MOVING";
  public static final byte[] MOVING_BYTES = SafeEncoder.encode(MOVING);

  /**
   * Shard migration starts within the lead time — relax client timeouts until MIGRATED.
   * <p>
   * Format: ["MIGRATING", seq, startsInSeconds, shard-ids]
   * @since 8.1
   */
  public static final String MIGRATING = "MIGRATING";
  public static final byte[] MIGRATING_BYTES = SafeEncoder.encode(MIGRATING);

  /**
   * Shard migration complete — restore client timeouts.
   * <p>
   * Format: ["MIGRATED", seq, shard-ids]
   * @since 8.1
   */
  public static final String MIGRATED = "MIGRATED";
  public static final byte[] MIGRATED_BYTES = SafeEncoder.encode(MIGRATED);

  /**
   * Shard failover starts within the lead time — relax client timeouts until FAILED_OVER.
   * <p>
   * Format: ["FAILING_OVER", seq, startsInSeconds, shard-ids]
   * @since 8.1
   */
  public static final String FAILING_OVER = "FAILING_OVER";
  public static final byte[] FAILING_OVER_BYTES = SafeEncoder.encode(FAILING_OVER);

  /**
   * Shard failover complete — restore client timeouts.
   * <p>
   * Format: ["FAILED_OVER", seq, shard-ids]
   * @since 8.1
   */
  public static final String FAILED_OVER = "FAILED_OVER";
  public static final byte[] FAILED_OVER_BYTES = SafeEncoder.encode(FAILED_OVER);

  // ==================== Cluster Maintenance Events ====================

  /**
   * Cluster slot migration starts — relax client timeouts until the matching SMIGRATED.
   * <p>
   * Format: ["SMIGRATING", seq, slots-or-ranges] — comma-separated slots and/or {@code from-to}
   * ranges, e.g. {@code "123,456,789-1000"}.
   * @since 8.1
   */
  public static final String SMIGRATING = "SMIGRATING";
  public static final byte[] SMIGRATING_BYTES = SafeEncoder.encode(SMIGRATING);

  /**
   * Cluster slot migration complete — restore client timeouts and apply the slot delta.
   * <p>
   * Format: ["SMIGRATED", seq, [["src-host:port", "dest-host:port", slots-or-ranges], ...]] — each
   * entry moves the given slots from the source node to the destination node.
   * @since 8.1
   */
  public static final String SMIGRATED = "SMIGRATED";
  public static final byte[] SMIGRATED_BYTES = SafeEncoder.encode(SMIGRATED);
}
