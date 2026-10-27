package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link MaintenanceController} surface of {@link MaintenanceEventController}:
 * what {@link MaintenanceEventController#register} installs on a connection, what
 * {@link MaintenanceEventController#unregister} tears down. No sockets: the connections are bare.
 */
public class MaintenanceEventControllerRegistrationTest {

  private static final int SO_TIMEOUT_MS = 2000;
  private static final int RELAXED_TIMEOUT_MS = 10_000;

  private MaintenanceEventController controller;
  private Connection conn;

  @BeforeEach
  public void setUp() {
    controller = MaintenanceEventController
        .from(MaintenanceNotificationsConfig.builder().relaxedTimeout(RELAXED_TIMEOUT_MS).build());
    conn = new Connection();
    conn.setSoTimeout(SO_TIMEOUT_MS);
  }

  @AfterEach
  public void tearDown() {
    controller.close();
    ConnectionTestHelper.resetClockNanos();
  }

  @Test
  public void registerInstallsThePoolWideAndPerConnectionOverlaysAndTheListener() {
    controller.register(conn);

    assertTrue(conn.getMaintenanceEventListeners().contains(controller));
    ChainedTimeoutSource chain = conn.getTimeoutSource();
    assertInstanceOf(ManagedTimeoutSource.class, chain.seekBy(controller),
      "the pool-wide MOVING rebind overlay is keyed by the installing controller");
    assertInstanceOf(ExpiringTimeoutSource.class, chain.seekBy(ExpiringTimeoutSource.class),
      "the per-connection MIGRATING/FAILING_OVER window overlay");
    assertFalse(ConnectionTestHelper.isRelaxedTimeoutActive(conn), "nothing open yet");
  }

  @Test
  public void perConnectionOverlayCarriesTheConfiguredRelaxedTimeouts() {
    controller.register(conn);

    ConnectionTestHelper.relaxTimeouts(conn, Duration.ofMinutes(1)); // open the window

    assertTrue(ConnectionTestHelper.isRelaxedTimeoutActive(conn));
    assertEquals(RELAXED_TIMEOUT_MS, ConnectionTestHelper.getRelaxedSoTimeout(conn));
    assertEquals(RELAXED_TIMEOUT_MS, conn.getTimeoutSource().get().timeout);
  }

  @Test
  public void unregisterRemovesBothOverlaysAndTheListener() {
    controller.register(conn);

    controller.unregister(conn);

    assertFalse(conn.getMaintenanceEventListeners().contains(controller));
    ChainedTimeoutSource chain = conn.getTimeoutSource();
    assertNull(chain.seekBy(controller));
    assertNull(chain.seekBy(ExpiringTimeoutSource.class));
    assertEquals(SO_TIMEOUT_MS, chain.get().timeout);
  }

  @Test
  public void unregisterPreservesOverlaysInstalledByOthers() {
    controller.register(conn);
    Object foreignOwner = new Object();
    ManagedTimeoutSource foreign = new ManagedTimeoutSource(() -> null, foreignOwner);
    conn.getTimeoutSource().addOverride(foreign);

    controller.unregister(conn);

    assertSame(foreign, conn.getTimeoutSource().seekBy(foreignOwner));
  }

}
