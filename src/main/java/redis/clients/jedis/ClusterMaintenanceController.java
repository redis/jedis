package redis.clients.jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Per-pool controller for the cluster message family: dispatches SMIGRATING/SMIGRATED to the
 * client-wide {@link ClusterMaintenanceCoordinator} and consumes any standalone/enterprise event
 * with a warning — those must not occur on a cluster connection. Carries none of the pool-local
 * machinery of {@link MaintenanceEventController}: no registry, no marking scheduler, no retirement
 * — Case-2 teardown is pool-level.
 */
final class ClusterMaintenanceController
    implements MaintenanceController, MaintenanceEventListener {

  private static final Logger logger = LoggerFactory.getLogger(ClusterMaintenanceController.class);

  private final ClusterMaintenanceCoordinator coordinator;

  ClusterMaintenanceController(ClusterMaintenanceCoordinator coordinator) {
    this.coordinator = coordinator;
  }

  @Override
  public MaintenanceNotificationsConfig getConfig() {
    return coordinator.getConfig();
  }

  @Override
  public void register(Connection connection) {
    connection.getTimeoutSource()
        .addOverride(new ManagedTimeoutSource(coordinator.getTimeoutSupplier()));
    connection.addMaintenanceEventListener(this);
  }

  @Override
  public void unregister(Connection connection) {
    connection.removeMaintenanceEventListener(this);
    ChainedTimeoutSource cts = connection.getTimeoutSource();
    cts.removeOverride(cts.seekBy(coordinator.getTimeoutSupplier()));
  }

  @Override
  public void onSMigrating(SMigratingEvent e, Connection c) {
    coordinator.onSMigrating(e, c);
  }

  @Override
  public void onSMigrated(SMigratedEvent e, Connection c) {
    coordinator.onSMigrated(e, c);
  }

  @Override
  public void onMoving(MovingEvent e, Connection c) {
    warnUnsupported(e, c);
  }

  @Override
  public void onMigrating(MigratingEvent e, Connection c) {
    warnUnsupported(e, c);
  }

  @Override
  public void onMigrated(MigratedEvent e, Connection c) {
    warnUnsupported(e, c);
  }

  @Override
  public void onFailingOver(FailingOverEvent e, Connection c) {
    warnUnsupported(e, c);
  }

  @Override
  public void onFailedOver(FailedOverEvent e, Connection c) {
    warnUnsupported(e, c);
  }

  private void warnUnsupported(MaintenanceEvent e, Connection c) {
    logger.warn(
      "Standalone maintenance events are not supported on cluster connections: {} conn={}", e,
      c.toIdentityString());
  }

}
