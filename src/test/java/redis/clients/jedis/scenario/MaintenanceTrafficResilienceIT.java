package redis.clients.jedis.scenario;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redis.test.fi.FaultInjectorClient;
import com.redis.test.fi.Scenario;
import com.redis.test.fi.StandaloneEffect;
import com.redis.test.fi.StandaloneTriggerCatalog;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.RedisClient;

/**
 * Traffic-continuity resiliency: with a stock {@link RedisClient} (RESP3 auto-negotiated, SCH in
 * its default AUTO mode), a topology change must be hitless — continuous traffic before, during,
 * and after the fault-injector effect completes sees zero command or connectivity errors. This is a
 * black-box guarantee (no notification/timeout assertions), complementary to the functional suite.
 * <p>
 * Runs each MOVING-coordinated or hitless topology change the fault injector can drive on a single
 * standalone db: conn_drop and data_movement_conn_drop endpoint rebinds, and the hitless
 * data_movement_no_conn_drop migrate and failover. Rolling upgrades are out of scope (multi-node
 * orchestration, not a single standalone effect), and dns_resolution_change is excluded — it emits
 * no notifications, so connections drop abruptly with no handoff to coordinate and recover only via
 * DNS re-resolution.
 */
@Tag("scenario")
public class MaintenanceTrafficResilienceIT {

  private static final Logger logger = LoggerFactory
      .getLogger(MaintenanceTrafficResilienceIT.class);

  // referencing the base's static field also runs its DNS-cache TTL static block before any lookup
  private static final FaultInjectorClient faultInjector = MaintNotificationsScenarioBase.faultInjector;
  private static final StandaloneTriggerCatalog CATALOG = MaintNotificationsScenarioBase.CATALOG;

  private static final int WORKERS = 6;
  private static final long BASELINE_COMMANDS = 200;
  // Keep traffic flowing this long after the effect completes to cover the MOVING grace tail — the
  // client-side grace timer runs past the server-reported completion — then drain and assert.
  private static final long POST_EFFECT_TRAFFIC_MS = 20_000;
  /** The stock maintenance-notifications config (mode AUTO) the hitless guarantee is made for. */
  private static final MaintenanceNotificationsConfig DEFAULT_MAINTENANCE = MaintenanceNotificationsConfig
      .builder().build();

  private RedisClient client;
  private long bdbId = -1;

  static Stream<Scenario> scenarios() {
    return Stream.of(
      CATALOG.effect(StandaloneEffect.CONN_DROP).trigger("endpoint_rebind").scenario(),
      CATALOG.effect(StandaloneEffect.DATA_MOVEMENT_CONN_DROP).trigger("endpoint_rebind")
          .scenario(),
      CATALOG.effect(StandaloneEffect.DATA_MOVEMENT_NO_CONN_DROP).trigger("migrate").scenario(),
      CATALOG.effect(StandaloneEffect.DATA_MOVEMENT_NO_CONN_DROP).trigger("failover").scenario());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("scenarios")
  @Timeout(420)
  void trafficUninterruptedDuringMaintenance(Scenario scenario) {
    TrafficRunner traffic = runTrafficAcrossEffect(scenario, DEFAULT_MAINTENANCE);
    if (traffic.errorCount() != 0) {
      fail("topology change must be hitless — " + traffic.summary(), traffic.firstError());
    }
    assertEquals("PONG", client.ping(), "client must be healthy after the effect");
  }

  /**
   * Negative control (meta-test, off by default): a drop scenario with maintenance notifications
   * DISABLED must NOT be hitless — the endpoint rebind drops in-flight commands with no handoff to
   * coordinate. It guards the positive test from passing vacuously — its hitless result must be
   * attributable to the maintenance-notifications feature. Run with
   * {@code -Dsch.negativeControl=true} (forward it to the failsafe fork, e.g. via
   * {@code -DJVM_OPTS=-Dsch.negativeControl=true}).
   */
  @Test
  @Timeout(420)
  void trafficDisruptedWithoutMaintenance() {
    assumeTrue(Boolean.getBoolean("sch.negativeControl"), "run with -Dsch.negativeControl=true");
    Scenario scenario = CATALOG.effect(StandaloneEffect.CONN_DROP).trigger("endpoint_rebind")
        .scenario();
    TrafficRunner traffic = runTrafficAcrossEffect(scenario,
      MaintenanceNotificationsConfig.DISABLED);
    assertTrue(traffic.errorCount() > 0,
      "negative control: expected errors with maintenance notifications DISABLED, got none — "
          + traffic.summary());
  }

  /**
   * Deploys a fresh db, drives traffic before/during/after the effect with a stock client (or the
   * given maintenance config), then stops and drains. Returns the runner for the caller to assert.
   */
  private TrafficRunner runTrafficAcrossEffect(Scenario scenario,
      MaintenanceNotificationsConfig maint) {
    Map<String, Object> dbConfig = faultInjector.getStandaloneTriggers(scenario.effect())
        .trigger(scenario.trigger().name()).requirement(scenario.requirement().config()).dbConfig();
    Map<String, Object> output = faultInjector.createDatabase(dbConfig);
    bdbId = ((Number) output.get("bdb_id")).longValue();
    URI endpoint = URI.create((String) ((List<?>) output.get("endpoints")).get(0));
    MaintNotificationsScenarioBase.awaitEndpointConnectable(endpoint);
    client = buildClient(output, endpoint, maint);

    TrafficRunner traffic = new TrafficRunner(client, WORKERS);
    traffic.start();
    try {
      // before: a healthy baseline must be error-free (no effect yet, regardless of config)
      traffic.awaitSubmitted(BASELINE_COMMANDS);
      assertEquals(0, traffic.errorCount(), "errors before the effect: " + traffic.summary());

      // during: run the effect to completion while traffic keeps flowing
      long submittedBeforeEffect = traffic.submittedCount();
      faultInjector.triggerEffect(bdbId, scenario.effect(), scenario.trigger().name());

      // after: keep traffic flowing through the handoff grace tail
      long submittedAtCompletion = traffic.submittedCount();
      sleepQuietly(POST_EFFECT_TRAFFIC_MS);
      assertTrue(traffic.submittedCount() > submittedAtCompletion,
        "no traffic after the effect completed");

      logger.info("{} (maint={}): commands submitted before/at-completion/final = {}/{}/{}",
        scenario, maint.getMode(), submittedBeforeEffect, submittedAtCompletion,
        traffic.submittedCount());
    } finally {
      // stop submitting new commands and wait up to the grace timeout for in-flight ones to finish
      traffic.stop();
    }

    // confirm every submitted command reached a terminal state — none left in-flight
    traffic.awaitAllProcessed();
    logger.info("{} (maint={}) result: {}", scenario, maint.getMode(), traffic.summary());
    return traffic;
  }

  @AfterEach
  void tearDown() {
    try {
      if (client != null) {
        client.close();
        client = null;
      }
    } finally {
      if (bdbId >= 0) {
        faultInjector.deleteDatabase(bdbId);
        bdbId = -1;
      }
    }
  }

  /**
   * RedisClient with only credentials otherwise (RESP3 auto-negotiated, default pool and executor)
   * and the given maintenance-notifications config.
   */
  private static RedisClient buildClient(Map<String, Object> output, URI endpoint,
      MaintenanceNotificationsConfig maint) {
    DefaultJedisClientConfig.Builder config = DefaultJedisClientConfig.builder()
        .password((String) output.get("password"));
    String username = (String) output.get("username");
    if (username != null && !"default".equals(username)) {
      config.user(username);
    }
    return RedisClient.builder().hostAndPort(endpoint.getHost(), endpoint.getPort())
        .clientConfig(config.build()).maintenanceNotifications(maint).build();
  }

  private static void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Fixed pool of workers issuing individual SET/GET commands on per-worker keys. Tracks submitted
   * vs processed commands (so the test can confirm none were left in-flight) and every unique error
   * signature (type: message) seen, for post-run troubleshooting.
   */
  private static final class TrafficRunner {

    private static final long BASELINE_TIMEOUT_SECONDS = 30;
    private static final long POLL_INTERVAL_MS = 50;
    private static final long GRACEFUL_STOP_TIMEOUT_SECONDS = 30;
    private static final long AWAIT_PROCESSED_TIMEOUT_SECONDS = 10;

    private final RedisClient client;
    private final int workers;
    private final ExecutorService pool;
    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong processed = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();
    private final AtomicReference<Throwable> firstError = new AtomicReference<>();
    // unique error signature (type: message) -> occurrence count, for troubleshooting
    private final Map<String, AtomicLong> uniqueErrors = new ConcurrentHashMap<>();
    private volatile boolean running = true;

    TrafficRunner(RedisClient client, int workers) {
      this.client = client;
      this.workers = workers;
      this.pool = Executors.newFixedThreadPool(workers);
    }

    void start() {
      for (int i = 0; i < workers; i++) {
        final String key = "traffic:" + i;
        pool.submit(() -> {
          long n = 0;
          while (running) {
            final long value = n++;
            runCommand(() -> client.set(key, Long.toString(value)));
            runCommand(() -> client.get(key));
          }
        });
      }
    }

    /** Runs one command, counting it as submitted then processed; records any error. */
    private void runCommand(Runnable command) {
      submitted.incrementAndGet();
      try {
        command.run();
      } catch (RuntimeException e) {
        errors.incrementAndGet();
        firstError.compareAndSet(null, e);
        uniqueErrors.computeIfAbsent(errorSignature(e), k -> new AtomicLong()).incrementAndGet();
      } finally {
        processed.incrementAndGet();
      }
    }

    /** Waits until at least {@code target} commands have been submitted (baseline warm-up). */
    void awaitSubmitted(long target) {
      await().atMost(BASELINE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
          .until(() -> submitted.get() >= target || errors.get() > 0);
    }

    /** Stops submitting new commands and waits up to the grace timeout for workers to finish. */
    void stop() {
      running = false;
      pool.shutdown();
      try {
        if (!pool.awaitTermination(GRACEFUL_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
          pool.shutdownNow();
        }
      } catch (InterruptedException e) {
        pool.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    /** Confirms every submitted command reached a terminal state (submitted == processed). */
    void awaitAllProcessed() {
      await().atMost(AWAIT_PROCESSED_TIMEOUT_SECONDS, TimeUnit.SECONDS)
          .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
          .until(() -> submitted.get() == processed.get());
    }

    long submittedCount() {
      return submitted.get();
    }

    long errorCount() {
      return errors.get();
    }

    Throwable firstError() {
      return firstError.get();
    }

    String summary() {
      return errors.get() + " errors over " + processed.get() + " processed / " + submitted.get()
          + " submitted commands"
          + (uniqueErrors.isEmpty() ? "" : "; unique types: " + uniqueErrors);
    }

    private static String errorSignature(Throwable e) {
      return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
  }
}
