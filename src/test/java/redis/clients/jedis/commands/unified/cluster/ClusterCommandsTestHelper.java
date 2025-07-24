package redis.clients.jedis.commands.unified.cluster;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;
import redis.clients.jedis.*;

public class ClusterCommandsTestHelper {

  static BaseRedisClient getCleanCluster(RedisProtocol protocol, Class<? extends BaseRedisClient> clientType) {
    clearClusterData();

    DefaultJedisClientConfig conf = DefaultJedisClientConfig.builder().password("cluster").protocol(protocol).build();

    if (clientType == JedisCluster.class) {
      return new JedisCluster(
          Collections.singleton(HostAndPorts.getStableClusterServers().get(0)),
          conf);
    } else if (clientType == RedisClusterClient.class) {
      return RedisClusterClient.builder().node(HostAndPorts.getStableClusterServers().get(0)).clientConfig(conf).build();
    } else {
      throw new IllegalArgumentException("Unknown client type: " + clientType);
    }
  }

  static void clearClusterData() {
    for (int i = 0; i < 3; i++) {
      try (Jedis jedis = new Jedis(HostAndPorts.getStableClusterServers().get(i))) {
        jedis.auth("cluster");
        jedis.flushAll();
      }
    }
  }

  public static Stream<Arguments> testParamsProvider() {
    return Stream.of(
        Arguments.of(RedisProtocol.RESP2, JedisCluster.class),
        Arguments.of(RedisProtocol.RESP2, RedisClusterClient.class),
        Arguments.of(RedisProtocol.RESP3, JedisCluster.class),
        Arguments.of(RedisProtocol.RESP3, RedisClusterClient.class)
    );
  }

}
