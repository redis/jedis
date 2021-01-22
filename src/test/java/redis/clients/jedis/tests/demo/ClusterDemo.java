package redis.clients.jedis.tests.demo;

import java.util.HashSet;
import java.util.Set;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPoolConfig;

// See: https://github.com/redis/jedis/issues/2347
public class ClusterDemo {
  // Expect to find a Redis cluster on this and the five following ports
  private final static int BASE_PORT = 6379;

  // This program should survive any node being down for up to 30s, and keep printing. Having
  // printouts pause while the node is down is fine.
  public static void main(String[] args) throws InterruptedException {
    JedisPoolConfig poolConfig = new JedisPoolConfig();

    Set<HostAndPort> nodes = new HashSet<>();
    for (int i = 0; i < 6; i++) {
      nodes.add(new HostAndPort("127.0.0.1", BASE_PORT + i));
    }
    JedisCluster cluster = new JedisCluster(nodes, 10_000, 10, poolConfig);

    // noinspection InfiniteLoopStatement
    while (true) {
      final Long foo = cluster.incr("foo");
      System.out.printf("foo=%d%n", foo);
      // noinspection BusyWait
      Thread.sleep(1000);
    }
  }
}
