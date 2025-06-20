package redis.clients.jedis;

import redis.clients.jedis.util.SafeEncoder;

import java.util.List;

public class PushEvent {
  String type;
  List<Object> content;

  public PushEvent(List<Object> content) {
    this.content = content;
    if (content.size() > 0) {
      type = SafeEncoder.encode((byte[]) content.get(0));
    }
  }

 public  String getType(){
    return  type;
  }

  public List<Object> getContent(){
    return content;
  }
}