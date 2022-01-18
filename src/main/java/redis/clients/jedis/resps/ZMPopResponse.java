package redis.clients.jedis.resps;

import java.util.List;

public class ZMPopResponse {
  private String key;
  private List<Tuple> elements;

  public ZMPopResponse(String key, List<Tuple> elements) {
    this.key = key;
    this.elements = elements;
  }

  public String getKey() { return this.key; }

  public List<Tuple> getElements() { return this.elements; }
}
