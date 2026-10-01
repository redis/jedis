package redis.clients.jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Everything one maintenance family contributes to the {@link ConnectionPool} hosting it, assembled
 * where the family is known so the pool itself never asks which family it serves. The pool runs the
 * kit in two phases around its own construction: {@link #configure} shapes the connection factory
 * before the pool exists (the factory is built inside {@code super(...)}), and {@link #attach}
 * installs the pool-side reactions once it does. Both are no-ops here; a family overrides only what
 * it needs. The kit owns the controller's lifetime on the pool's behalf: {@link #close()} is the
 * pool's last step in {@code destroy()}.
 */
abstract class PoolMaintenance implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(PoolMaintenance.class);

  /** Maintenance off: no controller, nothing to configure, nothing to attach. */
  static final PoolMaintenance OFF = new PoolMaintenance(null) {
  };

  private final MaintenanceController controller; // null = maintenance off

  private PoolMaintenance(MaintenanceController controller) {
    this.controller = controller;
  }

  /**
   * The standalone/enterprise family: MOVING remaps new connections to the announced endpoint and
   * retires the affected ones, so the pool must recycle retired connections and evict the retired
   * idles once a handoff has been processed. {@code null} or disabled config yields {@link #OFF}.
   */
  static PoolMaintenance standalone(MaintenanceNotificationsConfig config) {
    if (config == null || !config.isEnabledOrAuto()) {
      return OFF;
    }
    return new StandalonePoolMaintenance(MaintenanceEventController.from(config));
  }

  /**
   * The cluster family: SMIGRATING/SMIGRATED are dispatched to the client-wide coordinator; nothing
   * is remapped or retired per pool (Case-2 teardown is pool-level). {@code null} coordinator
   * yields {@link #OFF}.
   */
  static PoolMaintenance cluster(ClusterMaintenanceCoordinator coordinator) {
    if (coordinator == null) {
      return OFF;
    }
    return new ClusterPoolMaintenance(new ClusterMaintenanceController(coordinator));
  }

  /** The connection-facing controller ({@code null} when off); exposed for test clock injection. */
  MaintenanceController controller() {
    return controller;
  }

  /** Phase 1, before the pool exists: wire the controller and any family-specific factory knobs. */
  ConnectionFactory.Builder configure(ConnectionFactory.Builder factoryBuilder) {
    return factoryBuilder;
  }

  /** Phase 2, once the pool exists: install the family's pool-side reactions. */
  void attach(ConnectionPool pool) {
  }

  @Override
  public void close() {
    if (controller != null) {
      controller.close();
    }
  }

  /** Cluster pools need nothing beyond the controller itself. */
  private static final class ClusterPoolMaintenance extends PoolMaintenance {

    ClusterPoolMaintenance(ClusterMaintenanceController controller) {
      super(controller);
    }
  }

  /** Standalone pools remap new connections during MOVING and recycle the retired ones. */
  private static final class StandalonePoolMaintenance extends PoolMaintenance {

    private final MaintenanceEventController controller;

    StandalonePoolMaintenance(MaintenanceEventController controller) {
      super(controller);
      this.controller = controller;
    }

    @Override
    ConnectionFactory.Builder configure(ConnectionFactory.Builder factoryBuilder) {
      return factoryBuilder.maintenanceController(controller).socketAddressMapper(controller);
    }

    @Override
    void attach(ConnectionPool pool) {
      // evictor passes destroy retired idles ahead of the delegate policy
      pool.setEvictionPolicy(new RebindAwareEvictionPolicy(pool.getEvictionPolicy()));
      // handoff processed: evict the retired idles right away
      controller.setHandoffHook(() -> evictQuietly(pool));
    }

    /**
     * Evicts retired idles once a handoff has been processed. Runs on the maintenance scheduler
     * thread or inline on a notifying thread; must never propagate (a failed pass degrades to lazy
     * recycling on return).
     */
    private void evictQuietly(ConnectionPool pool) {
      if (pool.isClosed()) {
        return;
      }
      try {
        pool.evict();
      } catch (Exception e) {
        log.warn("Maintenance eviction pass failed; retired connections recycle on return", e);
      }
    }
  }
}
