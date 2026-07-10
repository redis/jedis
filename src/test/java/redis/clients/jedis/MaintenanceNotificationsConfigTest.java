package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Duration;

import redis.clients.jedis.MaintenanceNotificationsConfig.EndpointType;
import redis.clients.jedis.MaintenanceNotificationsConfig.EndpointTypeSource;

@Tag("unit")
public class MaintenanceNotificationsConfigTest {

  private static final SocketAddress PRIVATE_ADDR = new InetSocketAddress("10.0.0.1", 6379);
  private static final SocketAddress PUBLIC_ADDR = new InetSocketAddress("8.8.8.8", 6379);

  @Test
  public void builderDefaultsToAuto() {
    MaintenanceNotificationsConfig built = MaintenanceNotificationsConfig.builder().build();
    assertEquals(MaintenanceNotificationsConfig.Mode.AUTO, built.getMode());
    assertTrue(built.isEnabledOrAuto());
    assertEquals(Duration.ofSeconds(60), built.getRelaxedWindowMaxDuration());
  }

  @Test
  public void defaultSourceAutoResolvesFromConnectionCharacteristics() {
    EndpointTypeSource source = MaintenanceNotificationsConfig.builder().build()
        .getEndpointTypeSource();

    assertEquals(EndpointType.INTERNAL_IP, source.getEndpointType(PRIVATE_ADDR, false));
    assertEquals(EndpointType.INTERNAL_FQDN, source.getEndpointType(PRIVATE_ADDR, true));
    assertEquals(EndpointType.EXTERNAL_IP, source.getEndpointType(PUBLIC_ADDR, false));
    assertEquals(EndpointType.EXTERNAL_FQDN, source.getEndpointType(PUBLIC_ADDR, true));
  }

  @Test
  public void autoResolveEndpointTypeRestoresDefaultSource() {
    EndpointTypeSource source = MaintenanceNotificationsConfig.builder()
        .endpointType(EndpointType.EXTERNAL_FQDN).autoResolveEndpointType().build()
        .getEndpointTypeSource();

    assertEquals(EndpointType.INTERNAL_IP, source.getEndpointType(PRIVATE_ADDR, false));
    assertEquals(EndpointType.EXTERNAL_FQDN, source.getEndpointType(PUBLIC_ADDR, true));
  }

  @Test
  public void fixedEndpointTypeIgnoresConnectionCharacteristics() {
    EndpointTypeSource source = MaintenanceNotificationsConfig.builder()
        .endpointType(EndpointType.EXTERNAL_FQDN).build().getEndpointTypeSource();

    assertEquals(EndpointType.EXTERNAL_FQDN, source.getEndpointType(PRIVATE_ADDR, false));
    assertEquals(EndpointType.EXTERNAL_FQDN, source.getEndpointType(PUBLIC_ADDR, true));
    assertEquals(EndpointType.EXTERNAL_FQDN, source.getEndpointType(null, false));
  }

  @Test
  public void nullEndpointTypeRejected() {
    assertThrows(IllegalArgumentException.class,
      () -> MaintenanceNotificationsConfig.builder().endpointType(null));
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
