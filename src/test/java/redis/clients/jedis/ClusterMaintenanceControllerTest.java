package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ClusterMaintenanceController}: the per-connection wiring (client-wide relax
 * overlay keyed by the coordinator's supplier, listener registration and its teardown) and the
 * dispatch of each event family. The connection under test is a bare, unconnected
 * {@link Connection}: its timeout chain and listener set are usable without a socket.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class ClusterMaintenanceControllerTest {

  private static final HostAndPort NODE_A = new HostAndPort("127.0.0.1", 7000);
  private static final HostAndPort NODE_B = new HostAndPort("127.0.0.1", 7001);
  private static final int SO_TIMEOUT_MS = 2000;
  private static final int RELAXED_TIMEOUT_MS = 10_000;

  @Mock
  private JedisClusterInfoCache cache;
  @Mock
  private Connection eventConn;

  private MaintenanceNotificationsConfig config;
  private ClusterMaintenanceCoordinator coordinator;
  private ClusterMaintenanceController controller;
  private Connection conn;

  @BeforeEach
  public void setUp() {
    config = MaintenanceNotificationsConfig.builder().relaxedTimeout(RELAXED_TIMEOUT_MS).build();
    coordinator = new ClusterMaintenanceCoordinator(cache, config);
    controller = new ClusterMaintenanceController(coordinator);
    conn = new Connection();
    conn.setSoTimeout(SO_TIMEOUT_MS); // a finite default, so relaxation is observable on get()
  }

  @Test
  public void configIsTheCoordinatorsConfig() {
    assertSame(config, controller.getConfig());
  }

  @Test
  public void registerInstallsTheClientWideRelaxOverlayAndTheListener() {
    controller.register(conn);

    assertTrue(conn.getMaintenanceEventListeners().contains(controller));
    ChainedTimeoutSource overlay = conn.getTimeoutSource().seekBy(coordinator.getTimeoutSupplier());
    assertNotNull(overlay, "the overlay is keyed by the coordinator's supplier for teardown");
    assertInstanceOf(ManagedTimeoutSource.class, overlay);
    // no per-connection expiring overlay: cluster relaxation is purely client-wide
    assertNull(conn.getTimeoutSource().seekBy(ExpiringTimeoutSource.class));
    assertFalse(ConnectionTestHelper.isRelaxedTimeoutActive(conn), "nothing open yet");
    assertEquals(SO_TIMEOUT_MS, conn.getTimeoutSource().get().timeout);
  }

  @Test
  public void registeredConnectionRelaxesWhileAMigrationIsOpenOnAnyConnection() {
    controller.register(conn);

    // the opener arrives on another connection: the gate is client-wide
    coordinator.onSMigrating(new SMigratingEvent(1L, HashSlotRanges.parse("0-100")), eventConn);
    assertTrue(ConnectionTestHelper.isRelaxedTimeoutActive(conn));
    assertEquals(RELAXED_TIMEOUT_MS, conn.getTimeoutSource().get().timeout);

    coordinator.onSMigrated(new SMigratedEvent(2L, delta("0-100")), eventConn);
    assertFalse(ConnectionTestHelper.isRelaxedTimeoutActive(conn));
    assertEquals(SO_TIMEOUT_MS, conn.getTimeoutSource().get().timeout);
  }

  @Test
  public void registeredConnectionRelaxesWhileASlotDeltaIsPending() {
    when(cache.hasPendingSlotDeltas()).thenReturn(true);
    controller.register(conn);

    assertTrue(ConnectionTestHelper.isRelaxedTimeoutActive(conn));
    assertEquals(RELAXED_TIMEOUT_MS, conn.getTimeoutSource().get().timeout);
  }

  @Test
  public void unregisterRemovesTheOverlayAndTheListener() {
    controller.register(conn);
    controller.unregister(conn);

    assertFalse(conn.getMaintenanceEventListeners().contains(controller));
    assertNull(conn.getTimeoutSource().seekBy(coordinator.getTimeoutSupplier()));
    coordinator.onSMigrating(new SMigratingEvent(1L, HashSlotRanges.parse("0-100")), eventConn);
    assertFalse(ConnectionTestHelper.isRelaxedTimeoutActive(conn),
      "an unregistered connection no longer follows the client-wide gate");
  }

  @Test
  public void unregisterPreservesOverlaysInstalledByOthers() {
    controller.register(conn);
    ExpiringTimeoutSource foreign = new ExpiringTimeoutSource(new TimeoutSource.TimeoutInfo(1, 1));
    conn.getTimeoutSource().addOverride(foreign);

    controller.unregister(conn);

    assertSame(foreign, conn.getTimeoutSource().seekBy(ExpiringTimeoutSource.class));
  }

  @Test
  public void clusterEventsAreForwardedToTheCoordinatorWithTheirConnection() {
    ClusterMaintenanceCoordinator forwardedTo = mock(ClusterMaintenanceCoordinator.class);
    ClusterMaintenanceController forwarding = new ClusterMaintenanceController(forwardedTo);
    SMigratingEvent opener = new SMigratingEvent(1L, HashSlotRanges.parse("0-100"));
    SMigratedEvent closer = new SMigratedEvent(2L, delta("0-100"));

    forwarding.onSMigrating(opener, eventConn);
    forwarding.onSMigrated(closer, eventConn);

    verify(forwardedTo).onSMigrating(same(opener), same(eventConn));
    verify(forwardedTo).onSMigrated(same(closer), same(eventConn));
  }

  @Test
  public void standaloneEventsNeverReachTheCoordinator() {
    ClusterMaintenanceCoordinator forwardedTo = mock(ClusterMaintenanceCoordinator.class);
    ClusterMaintenanceController forwarding = new ClusterMaintenanceController(forwardedTo);
    when(eventConn.toIdentityString()).thenReturn("conn");

    forwarding.onMoving(new MovingEvent(1L, 10, NODE_B), eventConn);
    forwarding.onMigrating(new MigratingEvent(2L, 5, "1"), eventConn);
    forwarding.onMigrated(new MigratedEvent(3L, "1"), eventConn);
    forwarding.onFailingOver(new FailingOverEvent(4L, 5, "1"), eventConn);
    forwarding.onFailedOver(new FailedOverEvent(5L, "1"), eventConn);

    verifyNoInteractions(forwardedTo);
  }

  @Test
  public void closeReleasesNothingTheCoordinatorStillNeeds() {
    controller.register(conn);

    controller.close();

    // the coordinator is client-wide and outlives any one pool's controller
    assertSame(config, controller.getConfig());
    coordinator.onSMigrating(new SMigratingEvent(1L, HashSlotRanges.parse("0-100")), eventConn);
    assertTrue(ConnectionTestHelper.isRelaxedTimeoutActive(conn));
  }

  private static List<SlotMigration> delta(String slots) {
    return Collections
        .singletonList(new SlotMigration(NODE_A, NODE_B, HashSlotRanges.parse(slots)));
  }
}
