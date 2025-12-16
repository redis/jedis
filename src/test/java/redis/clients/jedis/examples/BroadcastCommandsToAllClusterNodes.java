package redis.clients.jedis.examples;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.RedisClusterClient;

/**
 * When using the <a href="https://redis.io/docs/reference/cluster-spec/">Open Source Redis Cluster
 * API</a>, some commands must be executed against all primary nodes. To simplify this task, Jedis
 * provides an easy way to broadcast commands.
 *
 * For example, to update the server configuration of all nodes in the Redis Cluster, we broadcast
 * the command [CONFIG SET](https://redis.io/commands/config-set/) to all nodes.
 */
public class BroadcastCommandsToAllClusterNodes {

  public static void main(String[] args) {

    HostAndPort clusterNode = new HostAndPort("127.0.0.1", 7000);
    RedisClusterClient client = RedisClusterClient.create(clusterNode);

    String reply = client.configSet("maxmemory", "100mb"); // reply is "OK"
  }
}
