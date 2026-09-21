package redis.clients.jedis;

/**
 * Typed listener for server maintenance push events. Registered on a {@link Connection} via
 * {@link Connection#addMaintenanceEventListener}; the connection dispatches each parsed event to
 * the matching method synchronously on its read thread, before the triggering read returns. A
 * listener may mutate the delivering connection (e.g. relax timeouts, request rebind); exceptions
 * propagate to the read loop.
 */
interface MaintenanceEventListener {

  void onMoving(MovingEvent e, Connection c);

  void onMigrating(MigratingEvent e, Connection c);

  void onMigrated(MigratedEvent e, Connection c);

  void onFailingOver(FailingOverEvent e, Connection c);

  void onFailedOver(FailedOverEvent e, Connection c);
}
