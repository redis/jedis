package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class MaintenanceNotificationsConfigTest {

  @Test
  public void defaultModeIsAuto() {
    assertEquals(MaintenanceNotificationsConfig.Mode.AUTO,
      MaintenanceNotificationsConfig.DEFAULT.getMode());
    assertTrue(MaintenanceNotificationsConfig.DEFAULT.isEnabledOrAuto());
    assertNotNull(MaintenanceNotificationsConfig.DEFAULT.getRelaxedWindowMaxDuration());
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
}
