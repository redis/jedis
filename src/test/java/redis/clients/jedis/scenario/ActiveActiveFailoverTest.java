package redis.clients.jedis.scenario;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.*;
import redis.clients.jedis.MultiClusterClientConfig.ClusterConfig;
import redis.clients.jedis.providers.MultiClusterPooledConnectionProvider;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.mcf.ClusterSwitchEventArgs;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.hamcrest.MatcherAssert.assertThat;

@Tags({ @Tag("failover"), @Tag("scenario") })
public class ActiveActiveFailoverTest {
  private static final Logger log = LoggerFactory.getLogger(ActiveActiveFailoverTest.class);

  private static EndpointConfig endpoint;

  private final FaultInjectionClient faultClient = new FaultInjectionClient();

  @BeforeAll
  public static void beforeClass() {
    try {
      endpoint = HostAndPorts.getRedisEndpoint("re-active-active");
    } catch (IllegalArgumentException e) {
      log.warn("Skipping test because no Redis endpoint is configured");
      assumeTrue(false);
    }
  }

  @Test
  public void testFailover() {

    MultiClusterClientConfig.ClusterConfig[] clusterConfig = new MultiClusterClientConfig.ClusterConfig[2];

    JedisClientConfig config = endpoint.getClientConfigBuilder()
      .socketTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS)
      .connectionTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS).build();

    clusterConfig[0] = ClusterConfig.builder(endpoint.getHostAndPort(0), config)
      .connectionPoolConfig(RecommendedSettings.poolConfig).weight(1.0f).build();
    clusterConfig[1] = ClusterConfig.builder(endpoint.getHostAndPort(1), config)
      .connectionPoolConfig(RecommendedSettings.poolConfig).weight(0.5f).build();

    MultiClusterClientConfig.Builder builder = new MultiClusterClientConfig.Builder(clusterConfig);

    builder.circuitBreakerSlidingWindowType(CircuitBreakerConfig.SlidingWindowType.TIME_BASED);
    builder.circuitBreakerSlidingWindowSize(1); // SLIDING WINDOW SIZE IN SECONDS
    builder.circuitBreakerSlidingWindowMinCalls(1);
    builder.circuitBreakerFailureRateThreshold(10.0f); // percentage of failures to trigger circuit breaker

    builder.failbackSupported(true);
    builder.failbackCheckInterval(1000);
    builder.gracePeriod(10000);

    builder.retryWaitDuration(10);
    builder.retryMaxAttempts(1);
    builder.retryWaitDurationExponentialBackoffMultiplier(1);

    class FailoverReporter implements Consumer<ClusterSwitchEventArgs> {

      String currentClusterName = "not set";

      boolean failoverHappened = false;

      Instant failoverAt = null;

      boolean failbackHappened = false;

      Instant failbackAt = null;

      public String getCurrentClusterName() {
        return currentClusterName;
      }

      @Override
      public void accept(ClusterSwitchEventArgs e) {
        this.currentClusterName = e.getClusterName();
        log.info("\n\n====FailoverEvent=== \nJedis failover to cluster: {}\n====FailoverEvent===\n\n",
          e.getClusterName());

        if (failoverHappened) {
          failbackHappened = true;
          failbackAt = Instant.now();
        } else {
          failoverHappened = true;
          failoverAt = Instant.now();
        }
      }
    }

    MultiClusterPooledConnectionProvider provider = new MultiClusterPooledConnectionProvider(builder.build());
    FailoverReporter reporter = new FailoverReporter();
    provider.setClusterSwitchListener(reporter);
    provider.setActiveCluster(endpoint.getHostAndPort(0));

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
          Map<String, String> executionInfo = new HashMap<String, String>() {
            {
              put("threadId", String.valueOf(threadId));
              put("cluster", reporter.getCurrentClusterName());
            }
          };
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
            log.warn("Thread {} failed to execute command after failover. Failed commands after failover: {}", threadId,
              failedCommands);
          }

          if (attempt == 0) {
            long failedThreads = retryingThreadsCounter.incrementAndGet();
            log.warn("Thread {} failed to execute command. Failed threads: {}", threadId, failedThreads);
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
    params.put("actions",
      "[{\"type\":\"execute_rlutil_command\",\"params\":{\"rlutil_command\":\"pause_bdb\"}},{\"type\":\"wait\",\"params\":{\"wait_time\":\"15\"}},{\"type\":\"execute_rlutil_command\",\"params\":{\"rlutil_command\":\"resume_bdb\"}}]");

    FaultInjectionClient.TriggerActionResponse actionResponse = null;

    try {
      log.info("Triggering bdb_pause + wait 15 seconds + bdb_resume");
      actionResponse = faultClient.triggerAction("sequence_of_actions", params);
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

    ConnectionPool pool = provider.getCluster(endpoint.getHostAndPort(0)).getConnectionPool();

    log.info("First connection pool state: active: {}, idle: {}", pool.getNumActive(), pool.getNumIdle());
    log.info("Failover happened at: {}", reporter.failoverAt);
    log.info("Failback happened at: {}", reporter.failbackAt);
    log.info("Last failed command at: {}", lastFailedCommandAt.get());
    Duration fullFailoverTime = Duration.between(reporter.failoverAt, lastFailedCommandAt.get());
    log.info("Full failover time: {} s", fullFailoverTime.getSeconds());

    assertEquals(0, pool.getNumActive());
    assertTrue(fakeApp.capturedExceptions().isEmpty());
    assertTrue(reporter.failoverHappened);
    assertTrue(reporter.failbackHappened);
    assertThat(fullFailoverTime.getSeconds(), Matchers.greaterThanOrEqualTo(30L));

    client.close();
  }

}