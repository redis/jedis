package redis.clients.jedis.commands.unified.pooled;

import redis.clients.jedis.*;

public class PooledCommandsTestHelper {

  private static final EndpointConfig nodeInfo = HostAndPorts.getRedisEndpoint("standalone0");

  public static JedisPooled getPooled(RedisProtocol redisProtocol) {
    return new JedisPooled(nodeInfo.getHostAndPort(), nodeInfo.getClientConfigBuilder()
        .protocol(redisProtocol).build());
  }

  public static void clearData() {
    try (Jedis node = new Jedis(nodeInfo.getHostAndPort())) {
      node.auth(nodeInfo.getPassword());
      node.flushAll();
    }
  }
}
