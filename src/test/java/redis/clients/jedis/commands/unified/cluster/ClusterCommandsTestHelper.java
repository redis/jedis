package redis.clients.jedis.commands.unified.cluster;

import java.util.HashSet;

import redis.clients.jedis.*;

public class ClusterCommandsTestHelper {

  static RedisClusterClient getCleanCluster(RedisProtocol protocol) {
    EndpointConfig endpoint = HostAndPorts.getRedisEndpoint("cluster-stable");

    try (RedisClusterClient client = RedisClusterClient.builder()
        .nodes(new HashSet<>(endpoint.getHostsAndPorts()))
        .clientConfig(endpoint.getClientConfigBuilder().protocol(protocol).build()).build()) {
      client.flushAll();
      return client;
    }
  }

  static void clearClusterData() {
    getCleanCluster(RedisProtocol.RESP2);
  }
}
