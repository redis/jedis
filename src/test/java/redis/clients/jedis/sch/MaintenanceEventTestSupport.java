package redis.clients.jedis.sch;

import static org.hamcrest.Matchers.is;

import java.time.Duration;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.PushConsumer;
import redis.clients.jedis.PushConsumerChainImpl;
import redis.clients.jedis.TimeoutOptions;

/**
 * Shared helpers for SCH (Smart Client Handoff) abstract test bases. Centralizes config-builder
 * shortcuts and Hamcrest matchers that cross the {@code redis.clients.jedis} package boundary via
 * {@link ConnectionTestHelper}.
 */
public final class MaintenanceEventTestSupport {

  private MaintenanceEventTestSupport() {
  }

  /** Maintenance notifications enabled with a 10-second proactive relaxation. */
  public static MaintenanceNotificationsConfig enabledMaintConfig() {
    return MaintenanceNotificationsConfig.builder()
        .timeoutOptions(
          TimeoutOptions.builder().proactiveTimeoutsRelaxing(Duration.ofSeconds(10)).build())
        .build();
  }

  /** Matches a {@link redis.clients.jedis.Connection.MaintenanceEventConsumer} instance. */
  public static Matcher<? super PushConsumer> isMaintenanceConsumer() {
    return new TypeSafeMatcher<PushConsumer>() {
      @Override
      protected boolean matchesSafely(PushConsumer consumer) {
        return ConnectionTestHelper.isMaintenanceEventConsumer(consumer);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("a MaintenanceEventConsumer");
      }
    };
  }

  /** Matches the shared pub/sub consumer singleton. */
  public static Matcher<? super PushConsumer> isPubSubConsumer() {
    return is(PushConsumerChainImpl.PUBSUB_CONSUMER);
  }
}
