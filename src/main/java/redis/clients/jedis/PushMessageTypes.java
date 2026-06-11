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
   * Slot migration notification — server instructs the client to rebind to a new target.
   * <p>
   * Format: ["MOVING", slot, "host:port"]
   */
  public static final String MOVING = "MOVING";
  public static final byte[] MOVING_BYTES = SafeEncoder.encode(MOVING);

  /**
   * Slot migration starting — relax client timeouts during migration.
   */
  public static final String MIGRATING = "MIGRATING";
  public static final byte[] MIGRATING_BYTES = SafeEncoder.encode(MIGRATING);

  /**
   * Slot migration complete — restore client timeouts.
   */
  public static final String MIGRATED = "MIGRATED";
  public static final byte[] MIGRATED_BYTES = SafeEncoder.encode(MIGRATED);

  /**
   * Failover starting — relax client timeouts during failover.
   */
  public static final String FAILING_OVER = "FAILING_OVER";
  public static final byte[] FAILING_OVER_BYTES = SafeEncoder.encode(FAILING_OVER);

  /**
   * Failover complete — restore client timeouts.
   */
  public static final String FAILED_OVER = "FAILED_OVER";
  public static final byte[] FAILED_OVER_BYTES = SafeEncoder.encode(FAILED_OVER);
}
