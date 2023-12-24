package redis.clients.jedis.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;

public class MultiClusterPipelineTest {

  @Test
  public void testFailover() {
    int slidingWindowMinCalls = 10;
    int slidingWindowSize = 10;

    HostAndPort node1 = new HostAndPort("localhost", 6378);
    HostAndPort node2 = new HostAndPort("localhost", 6379);

    // MultiCluster
    List<MultiClusterClientConfig.ClusterConfig> mcClientConfigs = new ArrayList<>();
    mcClientConfigs.add(new MultiClusterClientConfig.ClusterConfig(node1, DefaultJedisClientConfig.builder().build()));
    mcClientConfigs.add(new MultiClusterClientConfig.ClusterConfig(node2, DefaultJedisClientConfig.builder().build()));

    // default config for circuit breaker (fail over)
    MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(mcClientConfigs)
        .circuitBreakerSlidingWindowMinCalls(slidingWindowMinCalls)
        .circuitBreakerSlidingWindowSize(slidingWindowSize);

    RedisFailoverReporter rsasRedisFailoverReporter = new RedisFailoverReporter();
    MultiClusterPooledConnectionProvider cacheProvider = new MultiClusterPooledConnectionProvider(builder.build());
    cacheProvider.setClusterFailoverPostProcessor(rsasRedisFailoverReporter);
    UnifiedJedis jedis = new UnifiedJedis(cacheProvider);

    System.out.println("Starting calls to Redis");

    String key = "hash-" + System.nanoTime();
    jedis.hset(key, "f1", "v1");

    System.out.println(jedis.hgetAll(key));
    jedis.flushAll();

    jedis.close();
  }

  class RedisFailoverReporter implements Consumer<String> {

    boolean failedOver = false;

    @Override
    public void accept(String clusterName) {
      System.out.println("Jedis fail over to cluster: " + clusterName);
      failedOver = true;
    }
  }
}
