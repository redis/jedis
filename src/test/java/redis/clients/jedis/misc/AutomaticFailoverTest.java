package redis.clients.jedis.misc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.util.IOUtils;

public class AutomaticFailoverTest {

  private static final Logger log = LoggerFactory.getLogger(AutomaticFailoverTest.class);

  private final HostAndPort hostPortWithFailure = new HostAndPort(HostAndPorts.getRedisEndpoint("standalone0").getHost(), 6378);
  private final EndpointConfig endpointForAuthFailure = HostAndPorts.getRedisEndpoint("standalone0");
  private final EndpointConfig workingEndpoint = HostAndPorts.getRedisEndpoint("standalone7-with-lfu-policy");

  private final JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().build();

  private Jedis jedis2;

  private List<MultiClusterClientConfig.ClusterConfig> getClusterConfigs(
      JedisClientConfig clientConfig, HostAndPort... hostPorts) {
    return Arrays.stream(hostPorts)
        .map(hp -> new MultiClusterClientConfig.ClusterConfig(hp, clientConfig))
        .collect(Collectors.toList());
  }

  @Before
  public void setUp() {
    jedis2 = new Jedis(workingEndpoint.getHostAndPort(),
        workingEndpoint.getClientConfigBuilder().build());
    jedis2.flushAll();
  }

  @After
  public void cleanUp() {
    IOUtils.closeQuietly(jedis2);
  }

  @Test
  public void pipelineWithSwitch() {
    MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
        new MultiClusterClientConfig.Builder(getClusterConfigs(clientConfig, hostPortWithFailure, workingEndpoint.getHostAndPort())).build());

    try (UnifiedJedis client = new UnifiedJedis(provider)) {
      AbstractPipeline pipe = client.pipelined();
      pipe.set("pstr", "foobar");
      pipe.hset("phash", "foo", "bar");
      provider.incrementActiveMultiClusterIndex();
      pipe.sync();
    }

    assertEquals("foobar", jedis2.get("pstr"));
    assertEquals("bar", jedis2.hget("phash", "foo"));
  }

  @Test
  public void transactionWithSwitch() {
    MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
        new MultiClusterClientConfig.Builder(getClusterConfigs(clientConfig, hostPortWithFailure, workingEndpoint.getHostAndPort())).build());

    try (UnifiedJedis client = new UnifiedJedis(provider)) {
      AbstractTransaction tx = client.multi();
      tx.set("tstr", "foobar");
      tx.hset("thash", "foo", "bar");
      provider.incrementActiveMultiClusterIndex();
      assertEquals(Arrays.asList("OK", Long.valueOf(1L)), tx.exec());
    }

    assertEquals("foobar", jedis2.get("tstr"));
    assertEquals("bar", jedis2.hget("thash", "foo"));
  }

  @Test
  public void commandFailover() {
    int slidingWindowMinCalls = 10;
    int slidingWindowSize = 10;

    MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(
        getClusterConfigs(clientConfig, hostPortWithFailure, workingEndpoint.getHostAndPort()))
        .circuitBreakerSlidingWindowMinCalls(slidingWindowMinCalls)
        .circuitBreakerSlidingWindowSize(slidingWindowSize);

    RedisFailoverReporter failoverReporter = new RedisFailoverReporter();
    MultiClusterPooledConnectionProvider cacheProvider = new MultiClusterPooledConnectionProvider(builder.build());
    cacheProvider.setClusterFailoverPostProcessor(failoverReporter);

    UnifiedJedis jedis = new UnifiedJedis(cacheProvider);

    String key = "hash-" + System.nanoTime();
    log.info("Starting calls to Redis");
    assertFalse(failoverReporter.failedOver);
    for (int attempt = 0; attempt < 10; attempt++) {
      try {
        jedis.hset(key, "f1", "v1");
      } catch (JedisConnectionException jce) {
        //
      }
      assertFalse(failoverReporter.failedOver);
    }

    // should failover now
    jedis.hset(key, "f1", "v1");
    assertTrue(failoverReporter.failedOver);

    assertEquals(Collections.singletonMap("f1", "v1"), jedis.hgetAll(key));
    jedis.flushAll();

    jedis.close();
  }

  @Test
  public void pipelineFailover() {
    int slidingWindowMinCalls = 10;
    int slidingWindowSize = 10;

    MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(
        getClusterConfigs(clientConfig, hostPortWithFailure, workingEndpoint.getHostAndPort()))
        .circuitBreakerSlidingWindowMinCalls(slidingWindowMinCalls)
        .circuitBreakerSlidingWindowSize(slidingWindowSize)
        .fallbackExceptionList(Arrays.asList(JedisConnectionException.class));

    RedisFailoverReporter failoverReporter = new RedisFailoverReporter();
    MultiClusterPooledConnectionProvider cacheProvider = new MultiClusterPooledConnectionProvider(builder.build());
    cacheProvider.setClusterFailoverPostProcessor(failoverReporter);

    UnifiedJedis jedis = new UnifiedJedis(cacheProvider);

    String key = "hash-" + System.nanoTime();
    log.info("Starting calls to Redis");
    assertFalse(failoverReporter.failedOver);
    AbstractPipeline pipe = jedis.pipelined();
    assertFalse(failoverReporter.failedOver);
    pipe.hset(key, "f1", "v1");
    assertFalse(failoverReporter.failedOver);
    pipe.sync();
    assertTrue(failoverReporter.failedOver);

    assertEquals(Collections.singletonMap("f1", "v1"), jedis.hgetAll(key));
    jedis.flushAll();

    jedis.close();
  }

  @Test
  public void failoverFromAuthError() {
    int slidingWindowMinCalls = 10;
    int slidingWindowSize = 10;

    MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(
        getClusterConfigs(clientConfig, endpointForAuthFailure.getHostAndPort(), workingEndpoint.getHostAndPort()))
        .circuitBreakerSlidingWindowMinCalls(slidingWindowMinCalls)
        .circuitBreakerSlidingWindowSize(slidingWindowSize)
        .fallbackExceptionList(Arrays.asList(JedisAccessControlException.class));

    RedisFailoverReporter failoverReporter = new RedisFailoverReporter();
    MultiClusterPooledConnectionProvider cacheProvider = new MultiClusterPooledConnectionProvider(builder.build());
    cacheProvider.setClusterFailoverPostProcessor(failoverReporter);

    UnifiedJedis jedis = new UnifiedJedis(cacheProvider);

    String key = "hash-" + System.nanoTime();
    log.info("Starting calls to Redis");
    assertFalse(failoverReporter.failedOver);
    jedis.hset(key, "f1", "v1");
    assertTrue(failoverReporter.failedOver);

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
