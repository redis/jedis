package redis.clients.jedis.mcf;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.Toxic;
import redis.clients.jedis.*;
import redis.clients.jedis.MultiDbConfig.DatabaseConfig;
import redis.clients.jedis.scenario.ActiveActiveFailoverTest;
import redis.clients.jedis.scenario.MultiThreadedFakeApp;
import redis.clients.jedis.scenario.RecommendedSettings;
import redis.clients.jedis.scenario.FaultInjectionClient.TriggerActionResponse;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;

@Tags({ @Tag("failover"), @Tag("integration") })
public class ActiveActiveLocalFailoverTest {
  private static final Logger log = LoggerFactory.getLogger(ActiveActiveFailoverTest.class);

  private static final EndpointConfig endpoint1 = HostAndPorts.getRedisEndpoint("redis-failover-1");
  private static final EndpointConfig endpoint2 = HostAndPorts.getRedisEndpoint("redis-failover-2");
  private static final ToxiproxyClient tp = new ToxiproxyClient("localhost", 8474);
  public static final int ENDPOINT_PAUSE_TIME_MS = 10000;
  private static Proxy redisProxy1;
  private static Proxy redisProxy2;

  @BeforeAll
  public static void setupAdminClients() throws IOException {
    if (tp.getProxyOrNull("redis-1") != null) {
      tp.getProxy("redis-1").delete();
    }
    if (tp.getProxyOrNull("redis-2") != null) {
      tp.getProxy("redis-2").delete();
    }

    redisProxy1 = tp.createProxy("redis-1", "0.0.0.0:29379", "redis-failover-1:9379");
    redisProxy2 = tp.createProxy("redis-2", "0.0.0.0:29380", "redis-failover-2:9380");
  }

  @AfterAll
  public static void cleanupAdminClients() throws IOException {
    if (redisProxy1 != null) redisProxy1.delete();
    if (redisProxy2 != null) redisProxy2.delete();
  }

  @BeforeEach
  public void setup() throws IOException {
    tp.getProxies().forEach(proxy -> {
      try {
        proxy.enable();
        for (Toxic toxic : proxy.toxics().getAll()) {
          toxic.remove();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });

  }

  @ParameterizedTest
  @CsvSource({ "true, 0, 2, 4", "true, 0, 2, 6", "true, 0, 2, 7", "true, 0, 2, 8", "true, 0, 2, 9",
      "true, 0, 2, 16", })
  public void testFailover(boolean fastFailover, long minFailoverCompletionDuration,
      long maxFailoverCompletionDuration, int numberOfThreads) {

    log.info(
      "TESTING WITH PARAMETERS: fastFailover: {} numberOfThreads: {} minFailoverCompletionDuration: {} maxFailoverCompletionDuration: {] ",
      fastFailover, numberOfThreads, minFailoverCompletionDuration, maxFailoverCompletionDuration);

    MultiDbConfig.DatabaseConfig[] clusterConfig = new MultiDbConfig.DatabaseConfig[2];

    JedisClientConfig config = endpoint1.getClientConfigBuilder()
        .socketTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS)
        .connectionTimeoutMillis(RecommendedSettings.DEFAULT_TIMEOUT_MS).build();

    clusterConfig[0] = DatabaseConfig.builder(endpoint1.getHostAndPort(), config)
        .connectionPoolConfig(RecommendedSettings.poolConfig).weight(1.0f).build();
    clusterConfig[1] = DatabaseConfig.builder(endpoint2.getHostAndPort(), config)
        .connectionPoolConfig(RecommendedSettings.poolConfig).weight(0.5f).build();

    MultiDbConfig.Builder builder = new MultiDbConfig.Builder(clusterConfig);

    builder.circuitBreakerSlidingWindowSize(1); // SLIDING WINDOW SIZE IN SECONDS
    builder.circuitBreakerFailureRateThreshold(10.0f); // percentage of failures to trigger circuit
                                                       // breaker

    builder.failbackSupported(false);
    // builder.failbackCheckInterval(1000);
    builder.gracePeriod(10000);

    builder.commandRetry(MultiDbConfig.RetryConfig.builder().waitDuration(10).maxAttempts(1)
        .exponentialBackoffMultiplier(1).build());

    // Use the parameterized fastFailover setting
    builder.fastFailover(fastFailover);

    class FailoverReporter implements Consumer<DatabaseSwitchEvent> {

      String currentClusterName = "not set";

      boolean failoverHappened = false;

      Instant failoverAt = null;

      boolean failbackHappened = false;

      Instant failbackAt = null;

      public String getCurrentClusterName() {
        return currentClusterName;
      }

      @Override
      public void accept(DatabaseSwitchEvent e) {
        this.currentClusterName = e.getDatabaseName();
        log.info("\n\n===={}=== \nJedis switching to cluster: {}\n====End of log===\n",
          e.getReason(), e.getDatabaseName());
        if ((e.getReason() == SwitchReason.CIRCUIT_BREAKER
            || e.getReason() == SwitchReason.HEALTH_CHECK)) {
          failoverHappened = true;
          failoverAt = Instant.now();
        }
        if (e.getReason() == SwitchReason.FAILBACK) {
          failbackHappened = true;
          failbackAt = Instant.now();
        }
      }
    }

    // Ensure endpoints are healthy
    assertTrue(redisProxy1.isEnabled());
    assertTrue(redisProxy2.isEnabled());
    ensureEndpointAvailability(endpoint1.getHostAndPort(), config);
    ensureEndpointAvailability(endpoint2.getHostAndPort(), config);

    // Create the connection provider
    MultiDbConnectionProvider provider = new MultiDbConnectionProvider(builder.build());
    FailoverReporter reporter = new FailoverReporter();
    provider.setDatabaseSwitchListener(reporter);
    provider.setActiveDatabase(endpoint1.getHostAndPort());

    UnifiedJedis client = new UnifiedJedis(provider);

    AtomicLong retryingThreadsCounter = new AtomicLong(0);
    AtomicLong failedCommandsAfterFailover = new AtomicLong(0);
    AtomicReference<Instant> lastFailedCommandAt = new AtomicReference<>();
    AtomicReference<Instant> lastFailedBeforeFailover = new AtomicReference<>();
    AtomicBoolean errorOccuredAfterFailover = new AtomicBoolean(false);
    AtomicBoolean unexpectedErrors = new AtomicBoolean(false);
    AtomicReference<Exception> lastException = new AtomicReference<Exception>();
    AtomicLong stopRunningAt = new AtomicLong();
    String cluster2Id = provider.getDatabase(endpoint2.getHostAndPort()).getCircuitBreaker()
        .getName();

    // Start thread that imitates an application that uses the client
    RateLimiterConfig rateLimiterConfig = RateLimiterConfig.custom().limitForPeriod(100)
        .limitRefreshPeriod(Duration.ofSeconds(1)).timeoutDuration(Duration.ofSeconds(1)).build();

    MultiThreadedFakeApp fakeApp = new MultiThreadedFakeApp(client, (UnifiedJedis c) -> {

      long threadId = Thread.currentThread().getId();

      int attempt = 0;
      int maxTries = 500;
      int retryingDelay = 5;
      String currentClusterId = null;
      while (true) {
        try {
          if (System.currentTimeMillis() > stopRunningAt.get()) break;
          currentClusterId = provider.getDatabase().getCircuitBreaker().getName();
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
          if (cluster2Id.equals(currentClusterId)) {
            break;
          }
          lastException.set(e);
          lastFailedBeforeFailover.set(Instant.now());

          if (reporter.failoverHappened) {
            errorOccuredAfterFailover.set(true);

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
        } catch (Exception e) {
          if (cluster2Id.equals(currentClusterId)) {
            break;
          }
          lastException.set(e);
          unexpectedErrors.set(true);
          lastFailedBeforeFailover.set(Instant.now());
          log.error("UNEXPECTED exception", e);
          if (reporter.failoverHappened) {
            errorOccuredAfterFailover.set(true);
            lastFailedCommandAt.set(Instant.now());
          }
        }
      }
      return true;
    }, numberOfThreads, rateLimiterConfig);
    fakeApp.setKeepExecutingForSeconds(30);
    Thread t = new Thread(fakeApp);
    t.start();

    stopRunningAt.set(System.currentTimeMillis() + 30000);

    log.info("Triggering issue on endpoint1");
    try (Jedis jedis = new Jedis(endpoint1.getHostAndPort(),
        endpoint1.getClientConfigBuilder().build())) {
      jedis.clientPause(ENDPOINT_PAUSE_TIME_MS);
    }

    fakeApp.setAction(new TriggerActionResponse(null) {
      private long completeAt = System.currentTimeMillis() + 10000;

      @Override
      public boolean isCompleted(Duration checkInterval, Duration delayAfter, Duration timeout) {
        return System.currentTimeMillis() > completeAt;
      }
    });

    log.info("Waiting for fake app to complete");
    try {
      t.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    log.info("Fake app completed");

    ConnectionPool pool = provider.getDatabase(endpoint1.getHostAndPort()).getConnectionPool();

    log.info("First connection pool state: active: {}, idle: {}", pool.getNumActive(),
      pool.getNumIdle());
    log.info("Failover happened at: {}", reporter.failoverAt);
    log.info("Failback happened at: {}", reporter.failbackAt);

    assertEquals(0, pool.getNumActive());
    assertTrue(fakeApp.capturedExceptions().isEmpty());
    assertTrue(reporter.failoverHappened);
    if (errorOccuredAfterFailover.get()) {
      log.info("Last failed command at: {}", lastFailedCommandAt.get());
      Duration fullFailoverTime = Duration.between(reporter.failoverAt, lastFailedCommandAt.get());
      log.info("Full failover time: {} s", fullFailoverTime.getSeconds());
      log.info("Last failed command exception: {}", lastException.get());

      // assertTrue(reporter.failbackHappened);
      assertThat(fullFailoverTime.getSeconds(),
        Matchers.greaterThanOrEqualTo(minFailoverCompletionDuration));
      assertThat(fullFailoverTime.getSeconds(),
        Matchers.lessThanOrEqualTo(maxFailoverCompletionDuration));
    } else {
      log.info("No failed commands after failover!");
    }

    if (lastFailedBeforeFailover.get() != null) {
      log.info("Last failed command before failover at: {}", lastFailedBeforeFailover.get());
    }
    assertFalse(unexpectedErrors.get());

    client.close();
  }

  private static void ensureEndpointAvailability(HostAndPort endpoint, JedisClientConfig config) {
    await().atMost(Duration.ofSeconds(ENDPOINT_PAUSE_TIME_MS)).until(() -> {
      try (Jedis jedis = new Jedis(endpoint, config)) {
        return "PONG".equals(jedis.ping());
      } catch (Exception e) {
        log.info("Waiting for endpoint {} to become available...", endpoint);
        return false;
      }
    });
  }

}