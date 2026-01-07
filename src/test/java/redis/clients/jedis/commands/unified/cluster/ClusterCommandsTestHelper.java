package redis.clients.jedis.commands.unified.cluster;

import java.util.HashSet;

import redis.clients.jedis.*;

public class ClusterCommandsTestHelper {

  public static RedisClusterClient getCleanCluster(RedisProtocol protocol) {
    EndpointConfig endpoint = Endpoints.getRedisEndpoint("cluster-stable");

    RedisClusterClient client = RedisClusterClient.builder()
        .nodes(new HashSet<>(endpoint.getHostsAndPorts()))
        .clientConfig(endpoint.getClientConfigBuilder().protocol(protocol).build()).build();
    client.flushAll();
    return client;
  }

  public static void clearClusterData() {
    getCleanCluster(RedisProtocol.RESP2);
  }

}
