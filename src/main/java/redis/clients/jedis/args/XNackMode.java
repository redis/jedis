package redis.clients.jedis.args;

import redis.clients.jedis.util.SafeEncoder;

/**
 * Nacking mode for the XNACK command. Determines how the delivery counter is adjusted
 * when negatively acknowledging messages in a consumer group.
 */
public enum XNackMode implements Rawable {

  /**
   * Used when the consumer is NACKing due to internal errors or shutdown, not because the message
   * is problematic. The delivery counter of each specified message is decremented by 1.
   */
  SILENT,

  /**
   * Used when the message causes problems for this consumer but may succeed for other consumers
   * (e.g., requires more resources than available). The delivery counter stays the same.
   */
  FAIL,

  /**
   * Used for invalid or suspected malicious messages. The delivery counter is set to LLONG_MAX.
   */
  FATAL;

  private final byte[] raw;

  XNackMode() {
    raw = SafeEncoder.encode(name());
  }

  @Override
  public byte[] getRaw() {
    return raw;
  }
}
