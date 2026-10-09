package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.apache.commons.pool2.impl.EvictionPolicy;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

/**
 * Unit tests for {@link PoolMaintenance}: what each family contributes to its pool — controller
 * identity, factory knobs, pool-side reactions — and the controller lifetime the kit owns.
 */
@Tag("unit")
public class PoolMaintenanceTest {

  private static final HostAndPort NODE = new HostAndPort("127.0.0.1", 6379);

  private final MaintenanceNotificationsConfig enabled = MaintenanceNotificationsConfig.builder()
      .build();

  // --- OFF ---

  @Test
  public void standaloneIsOffForNullOrDisabledConfig() {
    assertSame(PoolMaintenance.OFF, PoolMaintenance.standalone(null));
    assertSame(PoolMaintenance.OFF,
      PoolMaintenance.standalone(MaintenanceNotificationsConfig.DISABLED));
  }

  @Test
  public void clusterIsOffForNullCoordinator() {
    assertSame(PoolMaintenance.OFF, PoolMaintenance.cluster(null));
  }

  @Test
  public void offHasNoControllerConfiguresNothingAndAttachesNothing() {
    ConnectionFactory.Builder factoryBuilder = mock(ConnectionFactory.Builder.class);
    ConnectionPool pool = mock(ConnectionPool.class);

    assertNull(PoolMaintenance.OFF.controller());
    assertSame(factoryBuilder, PoolMaintenance.OFF.configure(factoryBuilder));
    PoolMaintenance.OFF.attach(pool);
    assertDoesNotThrow(PoolMaintenance.OFF::close);

    verifyNoInteractions(factoryBuilder, pool);
  }

  // --- standalone family ---

  @Test
  public void standaloneWiresItsControllerAsControllerAndAddressMapper() {
    PoolMaintenance kit = PoolMaintenance.standalone(enabled);
    try {
      MaintenanceEventController controller = assertInstanceOf(MaintenanceEventController.class,
        kit.controller());
      assertSame(enabled, controller.getConfig());
      ConnectionFactory.Builder factoryBuilder = mock(ConnectionFactory.Builder.class);
      when(factoryBuilder.maintenanceController(controller)).thenReturn(factoryBuilder);
      when(factoryBuilder.socketAddressMapper(controller)).thenReturn(factoryBuilder);

      assertSame(factoryBuilder, kit.configure(factoryBuilder));

      verify(factoryBuilder).maintenanceController(same(controller));
      verify(factoryBuilder).socketAddressMapper(same(controller));
    } finally {
      kit.close();
    }
  }

  @Test
  public void standaloneAttachInstallsRetiredAwareEvictionAndEvictsOnHandoff() throws Exception {
    PoolMaintenance kit = PoolMaintenance.standalone(enabled);
    try {
      MaintenanceEventController controller = (MaintenanceEventController) kit.controller();
      ConnectionPool pool = mock(ConnectionPool.class);
      @SuppressWarnings("unchecked")
      EvictionPolicy<Connection> delegate = mock(EvictionPolicy.class);
      when(pool.getEvictionPolicy()).thenReturn(delegate);

      kit.attach(pool);

      verify(pool).setEvictionPolicy(isA(RebindAwareEvictionPolicy.class));
      verify(pool, never()).evict();
      controller.getHandoffHook().run(); // a processed handoff evicts the retired idles right away
      verify(pool).evict();
    } finally {
      kit.close();
    }
  }

  @Test
  public void standaloneHandoffEvictionSkipsAClosedPoolAndNeverPropagates() throws Exception {
    PoolMaintenance kit = PoolMaintenance.standalone(enabled);
    try {
      MaintenanceEventController controller = (MaintenanceEventController) kit.controller();
      ConnectionPool pool = mock(ConnectionPool.class);
      kit.attach(pool);

      when(pool.isClosed()).thenReturn(true);
      controller.getHandoffHook().run();
      verify(pool, never()).evict();

      when(pool.isClosed()).thenReturn(false);
      doThrow(new IllegalStateException("evictor failed")).when(pool).evict();
      assertDoesNotThrow(controller.getHandoffHook()::run,
        "a failed pass degrades to lazy recycling on return; it must not reach the notifier");
      verify(pool).evict();
    } finally {
      kit.close();
    }
  }

  @Test
  public void standaloneCloseClosesItsController() {
    try (MockedConstruction<MaintenanceEventController> constructed = mockConstruction(
      MaintenanceEventController.class)) {
      PoolMaintenance kit = PoolMaintenance.standalone(enabled);
      assertEquals(1, constructed.constructed().size());
      MaintenanceEventController controller = constructed.constructed().get(0);
      assertSame(controller, kit.controller());

      kit.close();

      verify(controller).close();
    }
  }

  // --- cluster family ---

  @Test
  public void clusterWiresItsControllerWithoutAnAddressMapper() {
    ClusterMaintenanceCoordinator coordinator = new ClusterMaintenanceCoordinator(
        mock(JedisClusterInfoCache.class), enabled);
    PoolMaintenance kit = PoolMaintenance.cluster(coordinator);
    try {
      ClusterMaintenanceController controller = assertInstanceOf(ClusterMaintenanceController.class,
        kit.controller());
      assertSame(enabled, controller.getConfig());
      ConnectionFactory.Builder factoryBuilder = mock(ConnectionFactory.Builder.class);
      when(factoryBuilder.maintenanceController(controller)).thenReturn(factoryBuilder);

      assertSame(factoryBuilder, kit.configure(factoryBuilder));

      verify(factoryBuilder).maintenanceController(same(controller));
      // no MOVING in the cluster family: new connections are never remapped
      verify(factoryBuilder, never()).socketAddressMapper(any());
    } finally {
      kit.close();
    }
  }

  @Test
  public void clusterAttachesNothingToThePool() {
    ClusterMaintenanceCoordinator coordinator = new ClusterMaintenanceCoordinator(
        mock(JedisClusterInfoCache.class), enabled);
    PoolMaintenance kit = PoolMaintenance.cluster(coordinator);
    try {
      ConnectionPool pool = mock(ConnectionPool.class);

      kit.attach(pool);

      verifyNoInteractions(pool);
    } finally {
      kit.close();
    }
  }

  @Test
  public void clusterCloseClosesItsController() {
    ClusterMaintenanceCoordinator coordinator = new ClusterMaintenanceCoordinator(
        mock(JedisClusterInfoCache.class), enabled);
    try (MockedConstruction<ClusterMaintenanceController> constructed = mockConstruction(
      ClusterMaintenanceController.class)) {
      PoolMaintenance kit = PoolMaintenance.cluster(coordinator);
      ClusterMaintenanceController controller = constructed.constructed().get(0);
      assertSame(controller, kit.controller());

      kit.close();

      verify(controller).close();
    }
  }

  // --- the pool runs the kit ---

  @Test
  public void poolExposesTheKitsControllerAndClosesItOnDestroy() {
    ClusterMaintenanceCoordinator coordinator = new ClusterMaintenanceCoordinator(
        mock(JedisClusterInfoCache.class), enabled);
    try (MockedConstruction<ClusterMaintenanceController> constructed = mockConstruction(
      ClusterMaintenanceController.class,
      (mock, context) -> when(mock.getConfig()).thenReturn(enabled))) {
      ConnectionPool pool = new ConnectionPool(NODE, DefaultJedisClientConfig.builder().build(),
          null, new GenericObjectPoolConfig<>(), PoolMaintenance.cluster(coordinator));
      ClusterMaintenanceController controller = constructed.constructed().get(0);
      assertSame(controller, pool.getMaintenanceController());
      verify(controller, never()).close();

      pool.destroy();

      verify(controller).close();
    }
  }

  @Test
  public void poolWithMaintenanceOffHasNoController() {
    ConnectionPool pool = new ConnectionPool(NODE, DefaultJedisClientConfig.builder().build(), null,
        new GenericObjectPoolConfig<>(), PoolMaintenance.OFF);
    try {
      assertNull(pool.getMaintenanceController());
    } finally {
      pool.destroy();
    }
  }
}
