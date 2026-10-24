package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static redis.clients.jedis.JedisClusterInfoCache.getNodeKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.redis.test.fi.Scenario;
import com.redis.test.fi.ScenarioKey;
import com.redis.test.fi.SlotMigrateEffect;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.scenario.TrafficRunner.Failure;

/**
 * S5, the black-box guarantee: a stock {@link RedisClusterClient} (RESP3, maintenance notifications
 * in their default AUTO mode, default retries) drives traffic over every shard before, during and
 * after a slot migration and sees zero command errors, with every key written readable afterwards.
 * No notification or timeout is asserted; the functional suite does that. A second client with
 * {@code maxAttempts(1)} runs alongside and reports how many commands the application would have
 * seen fail without retries (S5.2: accepted are a borrow from the destroyed pool of a node that
 * left, and on ASM rows a MOVED on a slot that moved). One effect per trigger, same rows as the
 * first three of {@link ClusterMaintNotificationsIT}.
 */
@Tag("scenario")
public class ClusterMaintenanceTrafficResilienceIT extends ClusterMaintNotificationsScenarioBase {

  private static final Logger log = LoggerFactory
      .getLogger(ClusterMaintenanceTrafficResilienceIT.class);

  private static final int WORKERS = 6;
  private static final int METER_WORKERS = 2;
  private static final long BASELINE_COMMANDS = 200;
  /** Traffic keeps flowing this long after the FI reports the effect done, then drains. */
  private static final long POST_EFFECT_TRAFFIC_MS = 20_000;
  private static final MaintenanceNotificationsConfig DEFAULT_MAINTENANCE = MaintenanceNotificationsConfig
      .builder().build();

  static Stream<ScenarioKey> oneEffectPerTrigger() {
    return Stream.of(ScenarioKey.of(SlotMigrateEffect.REMOVE_ADD, "migrate", "external/hostname"),
      ScenarioKey.of(SlotMigrateEffect.SLOT_SHUFFLE, "failover", "external/ip"),
      ScenarioKey.of(SlotMigrateEffect.REMOVE, "reshard", "external/hostname+asm"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("oneEffectPerTrigger")
  @Timeout(900)
  void trafficContinues(ScenarioKey row) {
    Scenario scenario = assumeOffered(row);
    assumeFalse(scenario.requirement().asm() && Boolean.getBoolean("sch.skipAsm"),
      "ASM rows skipped by -Dsch.skipAsm=true");
    deployDatabase(scenario);
    RedisClusterClient stock = openClient(database, DEFAULT_MAINTENANCE, config -> {
    }, builder -> {
    });
    RedisClusterClient meter = openClient(database, DEFAULT_MAINTENANCE, config -> {
    }, builder -> builder.maxAttempts(1));
    HostAndPort[] owners = ownersBySlot(stock);
    List<int[]> shards = shardRanges(scenario, stock);

    TrafficRunner traffic = new TrafficRunner(stock, WORKERS,
        worker -> keysAcrossRanges("traffic:" + worker, shards));
    TrafficRunner metered = new TrafficRunner(meter, METER_WORKERS,
        worker -> keysAcrossRanges("meter:" + worker, shards));
    runAcrossEffect(scenario, traffic, metered);

    // S5.1
    log.info("{}: stock client {}", scenario, traffic.summary());
    if (traffic.errorCount() != 0) {
      fail("the migration must be hitless for the stock client: " + traffic.summary(),
        traffic.firstError());
    }
    for (String key : traffic.keys()) {
      assertTrue(stock.exists(key), "key written during the effect is gone: " + key);
    }
    assertEquals("PONG", stock.ping(), "stock client must be healthy after the effect");
    // S5.2: what an application without retries saw. Two kinds of failure are accepted:
    // - a borrow from the destroyed pool of a node that left the cluster (its slots all moved)
    // - on ASM rows, a MOVED for a key whose slot moved
    HostAndPort[] ownersAfter = ownersBySlot(stock);
    Set<String> nodesAfter = nodeKeys(stock);
    List<Failure> unexpected = new ArrayList<>();
    int exemptMoved = 0;
    int exemptPool = 0;
    for (Failure failure : metered.failures()) {
      HostAndPort ownerBefore = owners[failure.slot];
      boolean slotMoved = !ownerBefore.equals(ownersAfter[failure.slot]);
      boolean ownerLeft = !nodesAfter.contains(getNodeKey(ownerBefore));
      if (ownerLeft && isDestroyedPoolBorrow(failure)) {
        exemptPool++;
      } else if (slotMoved && scenario.requirement().asm() && isMoved(failure)) {
        exemptMoved++;
      } else {
        unexpected.add(failure);
      }
    }
    log.info(
      "{}: maxAttempts(1) client {}; exempt: {} MOVED on moved slots (ASM), {} borrows from"
          + " the destroyed pool of a departed node; unexpected: {}",
      scenario, metered.summary(), exemptMoved, exemptPool, unexpected);
    if (!unexpected.isEmpty()) {
      fail("S5.2: failures without retries that the design does not account for: " + unexpected,
        unexpected.get(0).error);
    }
  }

  /** A redirect the client ran out of attempts to follow. */
  private static boolean isMoved(Failure failure) {
    for (Throwable t = failure.error; t != null; t = t.getCause()) {
      if (t instanceof JedisMovedDataException) {
        return true;
      }
    }
    return false;
  }

  /** A borrow from the pool of a node the delta had just dropped (commons-pool "Pool not open"). */
  private static boolean isDestroyedPoolBorrow(Failure failure) {
    Throwable root = failure.rootCause();
    return root instanceof IllegalStateException && "Pool not open".equals(root.getMessage());
  }

  /**
   * S5.3, negative control (meta-test, off by default): with maintenance notifications DISABLED and
   * no retries, a migration is expected to surface as MOVED failures, showing the positive result
   * is attributable to the notifications. Run with {@code -Dsch.negativeControl=true} (forward it
   * to the failsafe fork via {@code -DJVM_OPTS=-Dsch.negativeControl=true}).
   */
  @Test
  @Timeout(900)
  void trafficDisruptedWithoutMaintenance() {
    assumeTrue(Boolean.getBoolean("sch.negativeControl"), "run with -Dsch.negativeControl=true");
    Scenario scenario = assumeOffered(
      ScenarioKey.of(SlotMigrateEffect.REMOVE_ADD, "migrate", "external/ip"));
    deployDatabase(scenario);
    RedisClusterClient unaware = openClient(database, MaintenanceNotificationsConfig.DISABLED,
      config -> {
      }, builder -> builder.maxAttempts(1));
    List<int[]> shards = shardRanges(scenario, unaware);

    TrafficRunner traffic = new TrafficRunner(unaware, WORKERS,
        worker -> keysAcrossRanges("control:" + worker, shards));
    runAcrossEffect(scenario, traffic);

    log.info("{}: negative control {}", scenario, traffic.summary());
    assertTrue(traffic.errorCount() > 0,
      "negative control: expected failures with notifications DISABLED and no retries, got none: "
          + traffic.summary());
  }

  /**
   * Starts the runners, checks the {@code primary} runner's baseline is error-free, runs the effect
   * to completion while traffic keeps flowing, keeps it flowing through the post-effect tail, then
   * stops and drains every runner so every submitted command reached a terminal state. The
   * {@code observers} (the {@code maxAttempts(1)} meter) only run alongside; the test classifies
   * their failures afterwards, so they gate nothing here.
   */
  private void runAcrossEffect(Scenario scenario, TrafficRunner primary,
      TrafficRunner... observers) {
    List<TrafficRunner> runners = new ArrayList<>();
    runners.add(primary);
    runners.addAll(Arrays.asList(observers));
    for (TrafficRunner runner : runners) {
      runner.start();
    }
    try {
      primary.awaitSubmitted(BASELINE_COMMANDS);
      assertEquals(0, primary.errorCount(), "errors before the effect: " + primary.summary());

      long submittedBefore = primary.submittedCount();
      faultInjector.triggerEffect(database, scenario);

      long submittedAtCompletion = primary.submittedCount();
      sleepQuietly(POST_EFFECT_TRAFFIC_MS);
      assertTrue(primary.submittedCount() > submittedAtCompletion,
        "no traffic after the effect completed");
      log.info("{}: commands submitted before / at completion / final = {} / {} / {}", scenario,
        submittedBefore, submittedAtCompletion, primary.submittedCount());
    } finally {
      for (TrafficRunner runner : runners) {
        runner.stop();
      }
    }
    for (TrafficRunner runner : runners) {
      runner.awaitAllProcessed();
    }
  }
}
