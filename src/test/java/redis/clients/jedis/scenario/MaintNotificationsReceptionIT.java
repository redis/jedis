package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.redis.test.fi.Scenario;
import com.redis.test.fi.StandaloneEffect;
import com.redis.test.fi.Trigger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.MaintNotificationsTestSupport;
import redis.clients.jedis.PushMessageTypes;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvent;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvents;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Basic push-notification reception: verifies the client receives and processes every supported
 * maintenance notification type — MIGRATING, MIGRATED, FAILING_OVER, FAILED_OVER, MOVING — with a
 * parseable payload, each delivered by a real cluster operation.
 */
@Tag("scenario")
public class MaintNotificationsReceptionIT extends MaintNotificationsObserverBase {

  private static final Logger log = LoggerFactory.getLogger(MaintNotificationsReceptionIT.class);

  /** No new event and effect finished for this long = the notification stream is drained. */
  private static final long SETTLE_MILLIS = 3_000;
  private static final long PUMP_DEADLINE_MILLIS = 240_000;

  /** Expected notification sequence per trigger, covering every supported type. */
  // dns_resolution_change is excluded: it drops connections without any notification
  // (RE emits MOVING only when a proxy is removed)
  private static final Map<Trigger, List<String>> EXPECTATIONS = new LinkedHashMap<>();
  static {
    EXPECTATIONS.put(
      STANDALONE_CATALOG.effect(StandaloneEffect.DATA_MOVEMENT_NO_CONN_DROP).trigger("migrate"),
      Arrays.asList(PushMessageTypes.MIGRATING, PushMessageTypes.MIGRATED));
    EXPECTATIONS.put(
      STANDALONE_CATALOG.effect(StandaloneEffect.DATA_MOVEMENT_NO_CONN_DROP).trigger("failover"),
      Arrays.asList(PushMessageTypes.FAILING_OVER, PushMessageTypes.FAILED_OVER));
    EXPECTATIONS.put(
      STANDALONE_CATALOG.effect(StandaloneEffect.CONN_DROP).trigger("endpoint_rebind"),
      Arrays.asList(PushMessageTypes.MOVING));
  }

  static Stream<Scenario> oneTriggerPerNotificationType() {
    return EXPECTATIONS.keySet().stream().map(Trigger::scenario);
  }

  /**
   * T.1 Push Notification Handling / T.1.1 Basic Push Notification Reception — verify clients can
   * receive and process different types of push notifications. Covers
   * receive{Migrating,Migrated,FailingOver,FailedOver,Moving}PushNotificationTest.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("oneTriggerPerNotificationType")
  @Timeout(300)
  void receivesExpectedNotificationSequence(Scenario scenario) {
    List<String> expected = EXPECTATIONS.get(scenario.trigger());

    openClient(deployDatabase(scenario), relaxedMaintenance().autoResolveEndpointType().build());
    pinned = pinConnection();
    ReceivedEvents received = MaintNotificationsTestSupport.record(pinned);

    startEffect(scenario);
    observeUntilDrained(received);

    joinEffectThreads();
    assertEffectSucceeded();

    List<ReceivedEvent> events = received.all();
    log.info("{} received: {}", scenario, events);
    events.forEach(MaintNotificationsReceptionIT::validatePayload);
    validateWindowShardIds(events);

    List<String> notifications = new ArrayList<>();
    events.forEach(event -> notifications.add(event.type));
    assertEquals(expected, notifications);
  }

  /**
   * Observes the pinned connection until the stream is drained: MOVING is always terminal (the
   * server disconnects the old endpoint afterwards, so a connection error after MOVING is the
   * expected end of stream); otherwise until a settle period passed since both the effect finished
   * and the last event arrived.
   */
  private void observeUntilDrained(ReceivedEvents received) {
    long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(PUMP_DEADLINE_MILLIS);
    long effectDoneAt = -1;
    while (System.nanoTime() < deadline) {
      try {
        observe();
      } catch (JedisConnectionException e) {
        if (received.movingReceived()) {
          return; // rebound endpoint dropped the old connection — expected after MOVING
        }
        fail("pinned connection dropped before the expected sequence completed; received: "
            + received.all() + ", cause: " + e);
      }
      if (received.movingReceived()) {
        return;
      }
      if (effectDoneAt < 0 && effectDone()) {
        effectDoneAt = System.nanoTime();
      }
      // the closing can trail the fault injector's success report
      if (settledSince(effectDoneAt) && settledSince(received.lastEventAtNanos())) {
        return;
      }
      sleepQuietly(100);
    }
    fail("notification stream did not drain within " + PUMP_DEADLINE_MILLIS + " ms; received: "
        + received.all());
  }

  /** {@code -1} = not happened yet. */
  private static boolean settledSince(long atNanos) {
    return atNanos >= 0 && elapsedMillis(atNanos) >= SETTLE_MILLIS;
  }

  /** Payload sanity of one event, independent of its position in the sequence. */
  private static void validatePayload(ReceivedEvent event) {
    switch (event.type) {
      case PushMessageTypes.MIGRATING:
      case PushMessageTypes.FAILING_OVER:
        assertTrue(event.timeSeconds > 0, "opening without a lead time: " + event);
        assertNotNull(event.shardIds, "opening without shard ids: " + event);
        break;
      case PushMessageTypes.MIGRATED:
      case PushMessageTypes.FAILED_OVER:
        assertNotNull(event.shardIds, "closing without shard ids: " + event);
        break;
      case PushMessageTypes.MOVING:
        assertTrue(event.timeSeconds > 0, "MOVING without a grace period: " + event);
        assertNotNull(event.target, "MOVING without a target endpoint: " + event);
        break;
      default:
        fail("unknown event type: " + event);
    }
  }

  /**
   * Windows are never nested, so a closing always directly follows its opening — and the codec
   * guarantees shard ids on closings, so they must match the opening's.
   */
  private static void validateWindowShardIds(List<ReceivedEvent> events) {
    for (int i = 1; i < events.size(); i++) {
      ReceivedEvent event = events.get(i);
      if (PushMessageTypes.MIGRATED.equals(event.type)
          || PushMessageTypes.FAILED_OVER.equals(event.type)) {
        assertEquals(events.get(i - 1).shardIds, event.shardIds, "closing " + event
            + " for different shards than " + events.get(i - 1) + "; events: " + events);
      }
    }
  }

}
