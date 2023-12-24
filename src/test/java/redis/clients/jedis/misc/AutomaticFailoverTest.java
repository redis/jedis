package redis.clients.jedis.misc;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.AbstractPipeline;
import redis.clients.jedis.AbstractTransaction;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MultiClusterClientConfig;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.util.IOUtils;

public class AutomaticFailoverTest {

  private static final Logger log = LoggerFactory.getLogger(AutomaticFailoverTest.class);

  private final HostAndPort hostPort1 = new HostAndPort(HostAndPorts.getRedisServers().get(0).getHost(), 6378);
  private final HostAndPort hostPort2 = HostAndPorts.getRedisServers().get(7);

  private final JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().build();

  private Jedis jedis2;

  private List<MultiClusterClientConfig.ClusterConfig> clusterConfigs;

  @Before
  public void setUp() {

    clusterConfigs = new ArrayList<>();
    clusterConfigs.add(new MultiClusterClientConfig.ClusterConfig(hostPort1, clientConfig));
    clusterConfigs.add(new MultiClusterClientConfig.ClusterConfig(hostPort2, clientConfig));

    jedis2 = new Jedis(hostPort2, clientConfig);
    jedis2.flushAll();
  }

  @After
  public void cleanUp() {
    IOUtils.closeQuietly(jedis2);
  }

  @Test
  public void pipelineSwitch() {
    MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
        new MultiClusterClientConfig.Builder(clusterConfigs).build());
    try (UnifiedJedis client = new UnifiedJedis(provider)) {
      AbstractPipeline pipe = client.pipelined();
      pipe.set("pstr", "foobar");
      pipe.hset("phash", "foo", "bar");
      pipe.sync();
    }

    assertEquals("foobar", jedis2.get("pstr"));
    assertEquals("bar", jedis2.hget("phash", "foo"));
  }

  @Test
  public void transactionSwitch() {
    MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
        new MultiClusterClientConfig.Builder(clusterConfigs).build());
    try (UnifiedJedis client = new UnifiedJedis(provider)) {
      AbstractTransaction tx = client.multi();
      tx.set("tstr", "foobar");
      tx.hset("thash", "foo", "bar");
      assertEquals(Arrays.asList("OK", Long.valueOf(1L)), tx.exec());
    }

    assertEquals("foobar", jedis2.get("tstr"));
    assertEquals("bar", jedis2.hget("thash", "foo"));
  }

  @Test
  public void commandFailover() {
    int slidingWindowMinCalls = 10;
    int slidingWindowSize = 10;

    // MultiCluster
    List<MultiClusterClientConfig.ClusterConfig> mcClientConfigs = new ArrayList<>();
    mcClientConfigs.add(new MultiClusterClientConfig.ClusterConfig(hostPort1, clientConfig));
    mcClientConfigs.add(new MultiClusterClientConfig.ClusterConfig(hostPort2, clientConfig));

    // default config for circuit breaker (fail over)
    MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(mcClientConfigs)
        .circuitBreakerSlidingWindowMinCalls(slidingWindowMinCalls)
        .circuitBreakerSlidingWindowSize(slidingWindowSize);

    RedisFailoverReporter rsasRedisFailoverReporter = new RedisFailoverReporter();
    MultiClusterPooledConnectionProvider cacheProvider = new MultiClusterPooledConnectionProvider(builder.build());
    cacheProvider.setClusterFailoverPostProcessor(rsasRedisFailoverReporter);
    UnifiedJedis jedis = new UnifiedJedis(cacheProvider);

    log.info("Starting calls to Redis");

    String key = "hash-" + System.nanoTime();
    jedis.hset(key, "f1", "v1");

    assertEquals(Collections.singletonMap("f1", "v1"), jedis.hgetAll(key));
    jedis.flushAll();

    jedis.close();
  }

  class RedisFailoverReporter implements Consumer<String> {

    boolean failedOver = false;

    @Override
    public void accept(String clusterName) {
      log.info("Jedis fail over to cluster: " + clusterName);
      failedOver = true;
    }
  }
}
