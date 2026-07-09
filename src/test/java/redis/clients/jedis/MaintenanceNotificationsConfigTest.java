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
  public void builderDefaultsToAuto() {
    MaintenanceNotificationsConfig built = MaintenanceNotificationsConfig.builder().build();
    assertEquals(MaintenanceNotificationsConfig.Mode.AUTO, built.getMode());
    assertEquals(MaintenanceNotificationsConfig.EndpointType.EXTERNAL_IP, built.getEndpointType());
    assertTrue(built.isEnabledOrAuto());
    assertEquals(Duration.ofSeconds(60), built.getRelaxedWindowMaxDuration());
  }

  @Test
  public void disabledModeReportsDisabled() {
    MaintenanceNotificationsConfig disabled = MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.DISABLED).build();
    assertFalse(disabled.isEnabledOrAuto());
  }

  @Test
  public void defaultRelaxedTimeouts() {
    MaintenanceNotificationsConfig built = MaintenanceNotificationsConfig.builder().build();
    assertEquals(MaintenanceNotificationsConfig.DEFAULT_RELAXED_SOCKET_TIMEOUT_MS,
      built.getRelaxedTimeout());
    assertEquals(MaintenanceNotificationsConfig.DEFAULT_RELAXED_BLOCKING_SOCKET_TIMEOUT_MS,
      built.getRelaxedBlockingTimeout());
    assertEquals(0, built.getRelaxedBlockingTimeout());
  }

  @Test
  public void builderSetsRelaxedTimeouts() {
    MaintenanceNotificationsConfig config = MaintenanceNotificationsConfig.builder()
        .relaxedTimeout(20_000).relaxedBlockingTimeout(30_000).build();
    assertEquals(20_000, config.getRelaxedTimeout());
    assertEquals(30_000, config.getRelaxedBlockingTimeout());
  }

  @Test
  public void disabledConstantIsDisabled() {
    assertEquals(MaintenanceNotificationsConfig.Mode.DISABLED,
      MaintenanceNotificationsConfig.DISABLED.getMode());
    assertFalse(MaintenanceNotificationsConfig.DISABLED.isEnabledOrAuto());
  }
}
