package redis.clients.jedis;

import redis.clients.jedis.util.SafeEncoder;

import java.util.List;

public class PushMessage {
  private String type;
  private List<Object> content;

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