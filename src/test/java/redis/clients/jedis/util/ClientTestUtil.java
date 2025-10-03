package redis.clients.jedis.util;

import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.providers.ConnectionProvider;

public class ClientTestUtil {

  public static <T extends ConnectionProvider> T getConnectionProvider(UnifiedJedis jedis) {
    return ReflectionTestUtil.getField(jedis, "provider");
  }
}
