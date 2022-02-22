package redis.clients.jedis.resps;

import java.util.List;

public class KeyedFlags {
  private final String key;
  private final List<String> flags;

  public KeyedFlags(String name, List<String> flags) {
    this.key = name;
    this.flags = flags;
  }

  public String getKey() {
      return key;
  }

  public List<String> getFlags() {
    return flags;
  }
}
