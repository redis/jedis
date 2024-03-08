package redis.clients.jedis.commands.unified.cluster;

import java.util.Collections;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.RedisProtocol;

public class ClusterCommandsTestHelper {

  static JedisCluster getCleanCluster(RedisProtocol protocol) {
    clearClusterData();
    return new JedisCluster(
        Collections.singleton(HostAndPorts.getStableClusterServers().get(0)),
        DefaultJedisClientConfig.builder().password("cluster").protocol(protocol).build());
  }

  static void clearClusterData() {
    for (int i = 0; i < 3; i++) {
      try (Jedis jedis = new Jedis(HostAndPorts.getStableClusterServers().get(i))) {
        jedis.auth("cluster");
        jedis.flushAll();
      }
    }
  }
}
