package redis.clients.jedis;

import redis.clients.jedis.annots.Internal;

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

  // ==================== Pub/Sub Messages ====================

  /**
   * Channel message.
   * <p>
   * Format: ["message", channel, message]
   */
  public static final String MESSAGE = "message";

  /**
   * Pattern message.
   * <p>
   * Format: ["pmessage", pattern, channel, message]
   */
  public static final String PMESSAGE = "pmessage";

  /**
   * Shard channel message.
   * <p>
   * Format: ["smessage", channel, message]
   */
  public static final String SMESSAGE = "smessage";

  // ==================== Pub/Sub Subscriptions ====================

  /**
   * Channel subscription confirmation.
   * <p>
   * Format: ["subscribe", channel, count]
   */
  public static final String SUBSCRIBE = "subscribe";

  /**
   * Pattern subscription confirmation.
   * <p>
   * Format: ["psubscribe", pattern, count]
   */
  public static final String PSUBSCRIBE = "psubscribe";

  /**
   * Shard channel subscription confirmation.
   * <p>
   * Format: ["ssubscribe", channel, count]
   */
  public static final String SSUBSCRIBE = "ssubscribe";

  // ==================== Pub/Sub Unsubscriptions ====================

  /**
   * Channel unsubscription confirmation.
   * <p>
   * Format: ["unsubscribe", channel, count]
   */
  public static final String UNSUBSCRIBE = "unsubscribe";

  /**
   * Pattern unsubscription confirmation.
   * <p>
   * Format: ["punsubscribe", pattern, count]
   */
  public static final String PUNSUBSCRIBE = "punsubscribe";

  /**
   * Shard channel unsubscription confirmation.
   * <p>
   * Format: ["sunsubscribe", channel, count]
   */
  public static final String SUNSUBSCRIBE = "sunsubscribe";
}
