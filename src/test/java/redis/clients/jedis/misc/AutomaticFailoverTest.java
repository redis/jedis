package redis.clients.jedis.misc;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisAccessControlException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.mcf.ClusterSwitchEventArgs;
import redis.clients.jedis.mcf.MultiDatabaseConnectionProvider;
import redis.clients.jedis.mcf.MultiDatabaseConnectionProviderHelper;
import redis.clients.jedis.mcf.SwitchReason;
import redis.clients.jedis.util.IOUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("failover")
@Tag("integration")
public class AutomaticFailoverTest {

  private static final Logger log = LoggerFactory.getLogger(AutomaticFailoverTest.class);

  private final HostAndPort hostPortWithFailure = new HostAndPort(
      HostAndPorts.getRedisEndpoint("standalone0").getHost(), 6378);
  private final EndpointConfig endpointForAuthFailure = HostAndPorts
      .getRedisEndpoint("standalone0");
  private final EndpointConfig workingEndpoint = HostAndPorts
      .getRedisEndpoint("standalone7-with-lfu-policy");

  private final JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().build();

  private Jedis jedis2;

  private List<MultiDatabaseConfig.DatabaseConfig> getDatabaseConfigs(
      JedisClientConfig clientConfig, HostAndPort... hostPorts) {
    return Arrays.stream(hostPorts)
        .map(hp -> new MultiDatabaseConfig.DatabaseConfig(hp, clientConfig))
        .collect(Collectors.toList());
  }

  @BeforeEach
  public void setUp() {
    jedis2 = new Jedis(workingEndpoint.getHostAndPort(),
        workingEndpoint.getClientConfigBuilder().build());
    jedis2.flushAll();
  }

  @AfterEach
  public void cleanUp() {
    IOUtils.closeQuietly(jedis2);
  }

  @Test
  public void pipelineWithSwitch() {
    MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(
        new MultiDatabaseConfig.Builder(
            getDatabaseConfigs(clientConfig, hostPortWithFailure, workingEndpoint.getHostAndPort()))
                .build());

    try (UnifiedJedis client = new UnifiedJedis(provider)) {
      AbstractPipeline pipe = client.pipelined();
      pipe.set("pstr", "foobar");
      pipe.hset("phash", "foo", "bar");
      MultiDatabaseConnectionProviderHelper.switchToHealthyCluster(provider,
        SwitchReason.HEALTH_CHECK, provider.getDatabase());
      pipe.sync();
    }

    assertEquals("foobar", jedis2.get("pstr"));
    assertEquals("bar", jedis2.hget("phash", "foo"));
  }

  @Test
  public void transactionWithSwitch() {
    MultiDatabaseConnectionProvider provider = new MultiDatabaseConnectionProvider(
        new MultiDatabaseConfig.Builder(
            getDatabaseConfigs(clientConfig, hostPortWithFailure, workingEndpoint.getHostAndPort()))
                .build());

    try (UnifiedJedis client = new UnifiedJedis(provider)) {
      AbstractTransaction tx = client.multi();
      tx.set("tstr", "foobar");
      tx.hset("thash", "foo", "bar");
      MultiDatabaseConnectionProviderHelper.switchToHealthyCluster(provider,
        SwitchReason.HEALTH_CHECK, provider.getDatabase());
      assertEquals(Arrays.asList("OK", 1L), tx.exec());
    }

    assertEquals("foobar", jedis2.get("tstr"));
    assertEquals("bar", jedis2.hget("thash", "foo"));
  }

  @Test
  public void commandFailoverUnresolvableHost() {
    int slidingWindowMinFails = 2;
    int slidingWindowSize = 2;

    HostAndPort unresolvableHostAndPort = new HostAndPort("unresolvable", 6379);
    MultiDatabaseConfig.Builder builder = new MultiDatabaseConfig.Builder(
        getDatabaseConfigs(clientConfig, unresolvableHostAndPort, workingEndpoint.getHostAndPort()))
            .retryWaitDuration(1).retryMaxAttempts(1)
            .circuitBreakerSlidingWindowSize(slidingWindowSize)
            .circuitBreakerMinNumOfFailures(slidingWindowMinFails);

    RedisFailoverReporter failoverReporter = new RedisFailoverReporter();
    MultiDatabaseConnectionProvider connectionProvider = new MultiDatabaseConnectionProvider(
        builder.build());
    connectionProvider.setDatabaseSwitchListener(failoverReporter);

    UnifiedJedis jedis = new UnifiedJedis(connectionProvider);

    String key = "hash-" + System.nanoTime();
    log.info("Starting calls to Redis");
    assertFalse(failoverReporter.failedOver);

    for (int attempt = 0; attempt < slidingWindowMinFails; attempt++) {
      assertFalse(failoverReporter.failedOver);
      Throwable thrown = assertThrows(JedisConnectionException.class,
        () -> jedis.hset(key, "f1", "v1"));
      assertThat(thrown.getCause(), instanceOf(UnknownHostException.class));
    }

    // already failed over now
    assertTrue(failoverReporter.failedOver);
    jedis.hset(key, "f1", "v1");

    assertEquals(Collections.singletonMap("f1", "v1"), jedis.hgetAll(key));
    jedis.flushAll();

    jedis.close();
  }

  @Test
  public void commandFailover() {
    int slidingWindowMinFails = 6;
    int slidingWindowSize = 6;
    int retryMaxAttempts = 3;

    MultiDatabaseConfig.Builder builder = new MultiDatabaseConfig.Builder(
        getDatabaseConfigs(clientConfig, hostPortWithFailure, workingEndpoint.getHostAndPort()))
            .retryMaxAttempts(retryMaxAttempts) // Default
            // is
            // 3
            .circuitBreakerFailureRateThreshold(50)
            .circuitBreakerMinNumOfFailures(slidingWindowMinFails)
            .circuitBreakerSlidingWindowSize(slidingWindowSize);

    RedisFailoverReporter failoverReporter = new RedisFailoverReporter();
    MultiDatabaseConnectionProvider connectionProvider = new MultiDatabaseConnectionProvider(
        builder.build());
    connectionProvider.setDatabaseSwitchListener(failoverReporter);

    UnifiedJedis jedis = new UnifiedJedis(connectionProvider);

    String key = "hash-" + System.nanoTime();
    log.info("Starting calls to Redis");
    assertFalse(failoverReporter.failedOver);
    // First call fails - will be retried 3 times
    // this will increase the CircuitBreaker failure count to 3
    assertThrows(JedisConnectionException.class, () -> jedis.hset(key, "c1", "v1"));

    // Second call fails - will be retried 3 times
    // this will increase the CircuitBreaker failure count to 6
    // should failover now
    assertThrows(JedisConnectionException.class, () -> jedis.hset(key, "c2", "v1"));

    // CB is in OPEN state now, next call should cause failover
    assertEquals(1L, jedis.hset(key, "c3", "v1"));
    assertTrue(failoverReporter.failedOver);

    assertEquals(Collections.singletonMap("c3", "v1"), jedis.hgetAll(key));
    jedis.flushAll();

    jedis.close();
  }

  @Test
  public void pipelineFailover() {
    int slidingWindowSize = 10;

    MultiDatabaseConfig.Builder builder = new MultiDatabaseConfig.Builder(
        getDatabaseConfigs(clientConfig, hostPortWithFailure, workingEndpoint.getHostAndPort()))
            .circuitBreakerSlidingWindowSize(slidingWindowSize)
            .fallbackExceptionList(Collections.singletonList(JedisConnectionException.class));

    RedisFailoverReporter failoverReporter = new RedisFailoverReporter();
    MultiDatabaseConnectionProvider cacheProvider = new MultiDatabaseConnectionProvider(
        builder.build());
    cacheProvider.setDatabaseSwitchListener(failoverReporter);

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
    int slidingWindowSize = 10;

    MultiDatabaseConfig.Builder builder = new MultiDatabaseConfig.Builder(
        getDatabaseConfigs(clientConfig, endpointForAuthFailure.getHostAndPort(),
          workingEndpoint.getHostAndPort())).circuitBreakerSlidingWindowSize(slidingWindowSize)
              .fallbackExceptionList(Collections.singletonList(JedisAccessControlException.class));

    RedisFailoverReporter failoverReporter = new RedisFailoverReporter();
    MultiDatabaseConnectionProvider cacheProvider = new MultiDatabaseConnectionProvider(
        builder.build());
    cacheProvider.setDatabaseSwitchListener(failoverReporter);

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

  static class RedisFailoverReporter implements Consumer<ClusterSwitchEventArgs> {

    boolean failedOver = false;

    @Override
    public void accept(ClusterSwitchEventArgs e) {
      log.info("Jedis fail over to cluster: " + e.getClusterName());
      failedOver = true;
    }
  }
}
