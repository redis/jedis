package redis.clients.jedis.commands.unified.pooled;

import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;
import redis.clients.jedis.*;

public class PooledCommandsTestHelper {

  public static final EndpointConfig nodeInfo = HostAndPorts.getRedisEndpoint("standalone0");

  public static BaseRedisClient getCleanClient(RedisProtocol protocol, Class<? extends BaseRedisClient> clientType) {
    clearData();

    DefaultJedisClientConfig conf = DefaultJedisClientConfig.builder()
        .password(nodeInfo.getPassword())
        .protocol(protocol)
        .build();

    if (clientType == JedisPooled.class) {
      return new JedisPooled(nodeInfo.getHostAndPort(), conf);
    } else if (clientType == RedisClient.class) {
      return RedisClient.builder()
          .hostAndPort(nodeInfo.getHostAndPort())
          .config(conf)
          .build();
    } else {
      throw new IllegalArgumentException("Unknown client type: " + clientType);
    }
  }

  public static void clearData() {
    try (Jedis node = new Jedis(nodeInfo.getHostAndPort())) {
      node.auth(nodeInfo.getPassword());
      node.flushAll();
    }
  }

  public static Stream<Arguments> testParamsProvider() {
    return Stream.of(
        Arguments.of(RedisProtocol.RESP2, JedisPooled.class),
        Arguments.of(RedisProtocol.RESP2, RedisClient.class),
        Arguments.of(RedisProtocol.RESP3, JedisPooled.class),
        Arguments.of(RedisProtocol.RESP3, RedisClient.class)
    );
  }

}
