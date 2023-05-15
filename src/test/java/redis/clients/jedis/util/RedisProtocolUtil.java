package redis.clients.jedis.util;

import redis.clients.jedis.RedisProtocol;

public class RedisProtocolUtil {
  
  public static RedisProtocol getRedisProtocol() {
    String ver = System.getProperty("jedisProtocol");
    if (ver != null && !ver.isEmpty()) {
      for (RedisProtocol proto : RedisProtocol.values()) {
        if (proto.version().equals(ver)) {
          return proto;
        }
      }
      throw new IllegalArgumentException("Unknown protocol " + ver);
    }
    return null;
  }
}
