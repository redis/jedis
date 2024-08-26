package redis.clients.jedis.scenario;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.Assert.*;

public class ActiveActiveFailoverTest {
  private static final Logger log = LoggerFactory.getLogger(ActiveActiveFailoverTest.class);

  private static EndpointConfig endpoint;

  private final FaultInjectionClient faultClient = new FaultInjectionClient();

  @BeforeClass
  public static void beforeClass() {
    try {
      ActiveActiveFailoverTest.endpoint = HostAndPorts.getRedisEndpoint("re-active-active");
    } catch (IllegalArgumentException e) {
      log.warn("Skipping test because no Redis endpoint is configured");
      org.junit.Assume.assumeTrue(false);
    }
  }

  @Test
  public void testFailover() {

    MultiClusterClientConfig.ClusterConfig[] clusterConfig = new MultiClusterClientConfig.ClusterConfig[2];

    JedisClientConfig config = endpoint.getClientConfigBuilder()
        .socketTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS)
        .connectionTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS).build();

    clusterConfig[0] = new MultiClusterClientConfig.ClusterConfig(endpoint.getHostAndPort(0),
        config, RecommendedSettings.poolConfig);
    clusterConfig[1] = new MultiClusterClientConfig.ClusterConfig(endpoint.getHostAndPort(1),
        config, RecommendedSettings.poolConfig);

    MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(clusterConfig);

    builder.circuitBreakerSlidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
    builder.circuitBreakerSlidingWindowSize(1); // SLIDING WINDOW SIZE IN SECONDS
    builder.circuitBreakerSlidingWindowMinCalls(1);
    builder.circuitBreakerFailureRateThreshold(10.0f); // percentage of failures to trigger circuit breaker

    builder.retryWaitDuration(10);
    builder.retryMaxAttempts(1);
    builder.retryWaitDurationExponentialBackoffMultiplier(1);

    class FailoverReporter implements Consumer<String> {

      String currentClusterName = "not set";

      boolean failoverHappened = false;

      Instant failoverAt = null;

      public String getCurrentClusterName() {
        return currentClusterName;
      }

      @Override
      public void accept(String clusterName) {
        this.currentClusterName = clusterName;
        log.info(
            "\n\n====FailoverEvent=== \nJedis failover to cluster: {}\n====FailoverEvent===\n\n",
            clusterName);

        failoverHappened = true;
        failoverAt = Instant.now();
      }
    }

    MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(
        builder.build());
    FailoverReporter reporter = new FailoverReporter();
    provider.setClusterFailoverPostProcessor(reporter);
    provider.setActiveMultiClusterIndex(1);

    UnifiedJedis client = new UnifiedJedis(provider);

    AtomicLong retryingThreadsCounter = new AtomicLong(0);
    AtomicLong failedCommandsAfterFailover = new AtomicLong(0);
    AtomicReference<Instant> lastFailedCommandAt = new AtomicReference<>();

    // Start thread that imitates an application that uses the client
    MultiThreadedFakeApp fakeApp = new MultiThreadedFakeApp(client, (UnifiedJedis c) -> {

      long threadId = Thread.currentThread().getId();

      int attempt = 0;
      int maxTries = 500;
      int retryingDelay = 5;
      while (true) {
        try {
          Map<String, String> executionInfo = new HashMap<String, String>() {{
            put("threadId", String.valueOf(threadId));
            put("cluster", reporter.getCurrentClusterName());
          }};
          client.xadd("execution_log", StreamEntryID.NEW_ENTRY, executionInfo);

          if (attempt > 0) {
            log.info("Thread {} recovered after {} ms. Threads still not recovered: {}", threadId,
                attempt * retryingDelay, retryingThreadsCounter.decrementAndGet());
          }

          break;
        } catch (JedisConnectionException e) {

          if (reporter.failoverHappened) {
            long failedCommands = failedCommandsAfterFailover.incrementAndGet();
            lastFailedCommandAt.set(Instant.now());
            log.warn(
                "Thread {} failed to execute command after failover. Failed commands after failover: {}",
                threadId, failedCommands);
          }

          if (attempt == 0) {
            long failedThreads = retryingThreadsCounter.incrementAndGet();
            log.warn("Thread {} failed to execute command. Failed threads: {}", threadId,
                failedThreads);
          }
          try {
            Thread.sleep(retryingDelay);
          } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
          }
          if (++attempt == maxTries) throw e;
        }
      }
      return true;
    }, 18);
    fakeApp.setKeepExecutingForSeconds(30);
    Thread t = new Thread(fakeApp);
    t.start();

    HashMap<String, Object> params = new HashMap<>();
    params.put("bdb_id", endpoint.getBdbId());
    params.put("rlutil_command", "pause_bdb");

    FaultInjectionClient.TriggerActionResponse actionResponse = null;

    try {
      log.info("Triggering bdb_pause");
      actionResponse = faultClient.triggerAction("execute_rlutil_command", params);
    } catch (IOException e) {
      fail("Fault Injection Server error:" + e.getMessage());
    }

    log.info("Action id: {}", actionResponse.getActionId());
    fakeApp.setAction(actionResponse);

    try {
      t.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    ConnectionPool pool = provider.getCluster(1).getConnectionPool();

    log.info("First connection pool state: active: {}, idle: {}", pool.getNumActive(),
        pool.getNumIdle());
    log.info("Full failover time: {} s",
        Duration.between(reporter.failoverAt, lastFailedCommandAt.get()).getSeconds());

    assertEquals(0, pool.getNumActive());
    assertTrue(fakeApp.capturedExceptions().isEmpty());

    client.close();
  }

}
