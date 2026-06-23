package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Duration;

@Tag("unit")
public class MaintenanceNotificationsConfigTest {

  @Test
  public void defaultModeIsAuto() {
    assertEquals(MaintenanceNotificationsConfig.Mode.AUTO,
      MaintenanceNotificationsConfig.DEFAULT.getMode());
    assertTrue(MaintenanceNotificationsConfig.DEFAULT.isEnabledOrAuto());
    assertEquals(Duration.ofSeconds(60),
      MaintenanceNotificationsConfig.DEFAULT.getRelaxedWindowMaxDuration());
  }

  @Test
  public void builderDefaultsMatchDefault() {
    MaintenanceNotificationsConfig built = MaintenanceNotificationsConfig.builder().build();
    assertEquals(MaintenanceNotificationsConfig.DEFAULT.getMode(), built.getMode());
    assertEquals(MaintenanceNotificationsConfig.DEFAULT.getEndpointType(), built.getEndpointType());
    assertTrue(built.isEnabledOrAuto());
  }

  @Test
  public void disabledModeReportsDisabled() {
    MaintenanceNotificationsConfig disabled = MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.DISABLED).build();
    assertFalse(disabled.isEnabledOrAuto());
  }

  @Test
  public void defaultRelaxedTimeouts() {
    assertEquals(MaintenanceNotificationsConfig.DEFAULT_RELAXED_SOCKET_TIMEOUT_MS,
      MaintenanceNotificationsConfig.DEFAULT.getRelaxedSocketTimeoutMillis());
    assertEquals(MaintenanceNotificationsConfig.DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS,
      MaintenanceNotificationsConfig.DEFAULT.getRelaxedBlockingSocketTimeoutMillis());
    assertEquals(JedisClientConfig.UNSET_TIMEOUT_MS,
      MaintenanceNotificationsConfig.DEFAULT.getRelaxedBlockingSocketTimeoutMillis());
  }

  @Test
  public void builderSetsRelaxedTimeouts() {
    MaintenanceNotificationsConfig config = MaintenanceNotificationsConfig.builder()
        .relaxedSocketTimeoutMillis(20_000).relaxedBlockingSocketTimeoutMillis(30_000).build();
    assertEquals(20_000, config.getRelaxedSocketTimeoutMillis());
    assertEquals(30_000, config.getRelaxedBlockingSocketTimeoutMillis());
  }
}
