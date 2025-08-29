package redis.clients.jedis.scenario;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;

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

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static redis.clients.jedis.Protocol.DEFAULT_TIMEOUT;

@Tags({ @Tag("failover"), @Tag("scenario") })
public class ActiveActiveFailoverTest {
  private static final Logger log = LoggerFactory.getLogger(ActiveActiveFailoverTest.class);
  private static final int NUM_OF_THREADS = 18;
  private static final int SOCKET_TIMEOUT_MS = DEFAULT_TIMEOUT;
  private static final int CONNECTION_TIMEOUT_MS = DEFAULT_TIMEOUT;
  private static final long NETWORK_FAILURE_INTERVAL = 15L;

  private static EndpointConfig endpoint;

  private final FaultInjectionClient faultClient = new FaultInjectionClient();

  @BeforeAll
  public static void beforeClass() {
    try {
      ActiveActiveFailoverTest.endpoint = HostAndPorts.getRedisEndpoint("re-active-active");
    } catch (IllegalArgumentException e) {
      log.warn("Skipping test because no Redis endpoint is configured");
      assumeTrue(false);
    }
  }

  @Test
  public void testFailover() {

    MultiClusterClientConfig.ClusterConfig[] clusterConfig = new MultiClusterClientConfig.ClusterConfig[2];

    JedisClientConfig config = endpoint.getClientConfigBuilder()
      .socketTimeoutMillis(SOCKET_TIMEOUT_MS)
      .connectionTimeoutMillis(CONNECTION_TIMEOUT_MS).build();

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
    builder.gracePeriod(2000);

    builder.retryWaitDuration(10);
    builder.retryMaxAttempts(1);
    builder.retryWaitDurationExponentialBackoffMultiplier(1);
    builder.fastFailover(true);
    builder.retryOnFailover(false);

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

    AtomicLong executedCommands = new AtomicLong(0);
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
          executedCommands.incrementAndGet();

          if (attempt > 0) {
            log.info("Thread {} recovered after {} ms. Threads still not recovered: {}", threadId,
              attempt * retryingDelay, retryingThreadsCounter.decrementAndGet());
          }

          break;
        } catch (JedisConnectionException e) {

          if (reporter.failoverHappened) {
            failedCommandsAfterFailover.incrementAndGet();
            lastFailedCommandAt.set(Instant.now());
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
    }, NUM_OF_THREADS);
    fakeApp.setKeepExecutingForSeconds(30);
    Thread t = new Thread(fakeApp);
    t.start();

    while (executedCommands.get() == 0) {
      // Wait for fake app to start
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    log.info("Fake app started.");

    HashMap<String, Object> params = new HashMap<>();
    params.put("bdb_id", endpoint.getBdbId());
    params.put("delay", NETWORK_FAILURE_INTERVAL);

    FaultInjectionClient.TriggerActionResponse actionResponse = null;

    try {
      log.info("Triggering network_failure for ~{} seconds", NETWORK_FAILURE_INTERVAL);
      actionResponse = faultClient.triggerAction("network_failure", params);
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


    ConnectionPool pool1 = provider.getCluster(endpoint.getHostAndPort(0)).getConnectionPool();
    ConnectionPool pool2 = provider.getCluster(endpoint.getHostAndPort(1)).getConnectionPool();

    await().atMost(Duration.ofSeconds(1)).until(() -> pool1.getNumActive() == 0);
    await().atMost(Duration.ofSeconds(1)).until(() -> pool2.getNumActive() == 0);

    log.info("Connection pool {}: active: {}, idle: {}", endpoint.getHostAndPort(0), pool1.getNumActive(), pool1.getNumIdle());
    log.info("Connection pool {}: active: {}, idle: {}", endpoint.getHostAndPort(1), pool2.getNumActive(), pool2.getNumIdle());
    log.info("Failover happened at: {}", reporter.failoverAt);
    log.info("Failback happened at: {}", reporter.failbackAt);
    log.info("Last failed command at: {}", lastFailedCommandAt.get());
    log.info("Failed commands after failover: {}", failedCommandsAfterFailover.get());
    Duration fullFailoverTime = Duration.between(reporter.failoverAt, lastFailedCommandAt.get());
    log.info("Full failover time: {} s", fullFailoverTime.getSeconds());

    assertEquals(0, pool1.getNumActive());
    assertTrue(fakeApp.capturedExceptions().isEmpty());
    assertTrue(reporter.failoverHappened);
    assertTrue(reporter.failbackHappened);
    assertThat( Duration.between(reporter.failoverAt, reporter.failbackAt).getSeconds(), greaterThanOrEqualTo(NETWORK_FAILURE_INTERVAL));

    client.close();
  }

}