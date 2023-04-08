package redis.clients.jedis.examples;

import java.util.ArrayList;
import java.util.Collection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.ScanRoundRobin;
import redis.clients.jedis.resps.ScanResult;

/**
 * When using the <a href="https://redis.io/docs/reference/cluster-spec/">Open Source Redis Cluster
 * API</a>, some commands must be executed against all primary nodes one by one and until the
 * iteration is completed in each node. To simplify this task, Jedis provides an easy way to round
 * robin commands.
 *
 * For example, to fetch all keys matching a specific pattern in the Redis Cluster, we round robin
 * the command [SCAN](https://redis.io/commands/scan/) to all nodes.
 */
public class RoundRobinCommandsToAllClusterNodes {

  public static void main(String[] args) {

    HostAndPort clusterNode = new HostAndPort("127.0.0.1", 7000);
    JedisCluster client = new JedisCluster(clusterNode);

    Collection<String> allMatchingKeys = new ArrayList<>();
    ScanRoundRobin scan = client.scan(10, "*");
    while (!scan.isRoundRobinCompleted()) {
      ScanResult<String> singleBatch = scan.get();
      allMatchingKeys.addAll(singleBatch.getResult());
    }
  }
}
