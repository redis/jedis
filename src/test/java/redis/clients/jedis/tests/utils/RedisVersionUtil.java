package redis.clients.jedis.tests.utils;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.tests.HostAndPortUtil;

public class RedisVersionUtil {

  public static int getRedisMajorVersionNumber() {
    String completeVersion = null;

    try (Connection jedis = new Connection(HostAndPortUtil.getRedisServers().get(0),
        DefaultJedisClientConfig.builder().password("foobared").build())) {
      String info = jedis.executeCommand(new CommandObject<>(new CommandArguments(Protocol.Command.INFO), BuilderFactory.STRING));
      String[] splitted = info.split("\\s+|:");
      for (int i = 0; i < splitted.length; i++) {
        if (splitted[i].equalsIgnoreCase("redis_version")) {
          completeVersion = splitted[i + 1];
          break;
        }
      }
    }

    if (completeVersion == null) {
      return 0;
    }
    return Integer.parseInt(completeVersion.substring(0, completeVersion.indexOf(".")));
  }

  public static boolean checkRedisMajorVersionNumber(int minVersion) {
    return getRedisMajorVersionNumber() >= minVersion;
  }
}
