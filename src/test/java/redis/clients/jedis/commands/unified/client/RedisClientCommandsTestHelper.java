package redis.clients.jedis.commands.unified.client;

import redis.clients.jedis.*;

public class RedisClientCommandsTestHelper {

  private static EndpointConfig endpoint;

  private static EndpointConfig getEndpointImpl() {
    if (endpoint == null) {
      endpoint = Endpoints.getRedisEndpoint("standalone0");
    }
    return endpoint;
  }

  /**
   * Returns the endpoint configuration for standalone0.
   * This method lazily initializes the endpoint to avoid class loading issues.
   */
  public static EndpointConfig getEndpointConfig() {
    return getEndpointImpl();
  }

  public static RedisClient getClient(RedisProtocol redisProtocol) {
    EndpointConfig info = getEndpointImpl();
    return RedisClient.builder().hostAndPort(info.getHostAndPort()).clientConfig(info.getClientConfigBuilder()
        .protocol(redisProtocol).build()).build();
  }

  public static void clearData() {
    EndpointConfig info = getEndpointImpl();
    try (Jedis node = new Jedis(info.getHostAndPort())) {
      node.auth(info.getPassword());
      node.flushAll();
    }
  }
}
