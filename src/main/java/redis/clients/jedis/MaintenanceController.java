package redis.clients.jedis;

/**
 * Per-pool maintenance controller contract: the connection-facing surface that
 * {@link MaintenanceAwareVisitor} wires identically for both deployment types — the handshake
 * config, the per-connection relax overlays with their stable teardown identities, the event
 * listener to register on the connection, optional connection tracking, and an optional post-DNS
 * address mapper. Deliberately separate from {@link MaintenanceEventListener}: reacting to the
 * events is the implementations' business ({@link MaintenanceEventController} for the
 * standalone/enterprise family, {@link ClusterMaintenanceController} for the cluster family). A
 * controller has exactly one owner (its pool), which creates it and must {@link #close()} it. Never
 * shared.
 */
interface MaintenanceController extends AutoCloseable {

  /** The config this controller was built from; drives the MAINT_NOTIFICATIONS handshake. */
  MaintenanceNotificationsConfig getConfig();

  // TODO : verify if this should be a part of the interface
  /** Post-DNS address mapper for this controller's connections; {@code null} = no remap. */
  default SocketAddressMapper socketAddressMapper() {
    return null;
  }

  void register(Connection connection);

  void unregister(Connection connection);

  @Override
  default void close() {
  }
}
