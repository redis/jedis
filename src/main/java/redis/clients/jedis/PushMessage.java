package redis.clients.jedis;

import java.util.List;

/**
 * Represents a push message received from Redis server.
 * <p>
 * Push messages are asynchronous notifications sent by the server. See {@link PushMessageTypes} for
 * known message types.
 */
public class PushMessage {

  private final List<Object> content;

  public PushMessage(List<Object> content) {
    this.content = content;
  }

  /**
   * Get the raw type as byte array.
   * <p>
   * This method returns the type without decoding, making it efficient for byte-level comparisons
   * using {@link java.util.Arrays#equals(byte[], byte[])}.
   * </p>
   * @return The message type as byte array, or null if content is empty
   */
  public byte[] getType() {
    return (content != null && !content.isEmpty()) ? (byte[]) content.get(0) : null;
  }

  public List<Object> getContent() {
    return content;
  }
}