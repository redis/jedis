package redis.clients.jedis;

import redis.clients.jedis.util.SafeEncoder;

import java.util.List;

/**
 * Represents a push message received from Redis server.
 * <p>
 * Push messages are asynchronous notifications sent by the server. See {@link PushMessageTypes} for
 * known message types.
 */
public class PushMessage {

  private String type;
  private final List<Object> content;

  public PushMessage(List<Object> content) {
    this.content = content;
    if (content != null && !content.isEmpty()) {
      type = SafeEncoder.encode((byte[]) content.get(0));
    }
  }

  public String getType() {
    return type;
  }

  public List<Object> getContent() {
    return content;
  }
}