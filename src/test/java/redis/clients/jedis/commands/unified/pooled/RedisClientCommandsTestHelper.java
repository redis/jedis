package redis.clients.jedis.commands.unified.pooled;

import redis.clients.jedis.*;

public class RedisClientCommandsTestHelper {

  public static final EndpointConfig nodeInfo = HostAndPorts.getRedisEndpoint("standalone0");

  public static RedisClient getClient(RedisProtocol redisProtocol) {
    return RedisClient.builder().hostAndPort(nodeInfo.getHostAndPort()).clientConfig(nodeInfo.getClientConfigBuilder()
        .protocol(redisProtocol).build()).build();
  }

  public static void clearData() {
    try (Jedis node = new Jedis(nodeInfo.getHostAndPort())) {
      node.auth(nodeInfo.getPassword());
      node.flushAll();
    }
  }
}
