package redis.clients.jedis.commands.unified.pooled;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.RedisProtocol;

public class PooledCommandsTestHelper {

  private static final HostAndPort nodeInfo = HostAndPorts.getRedisServers().get(0);

  public static JedisPooled getPooled(RedisProtocol redisProtocol) {
    return new JedisPooled(nodeInfo, DefaultJedisClientConfig.builder()
        .protocol(redisProtocol).password("foobared").build());
  }

  public static void clearData() {
    try (Jedis node = new Jedis(nodeInfo)) {
      node.auth("foobared");
      node.flushAll();
    }
  }
}
