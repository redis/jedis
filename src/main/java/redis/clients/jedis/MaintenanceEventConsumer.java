package redis.clients.jedis;

import java.util.List;

/**
 * Push consumer for server maintenance events: resolves and decodes each frame via
 * {@link MaintenancePushCodec}, then dispatches the typed {@link MaintenanceEvent} to the
 * connection's {@link MaintenanceEventListener}s on the read thread. A recognized maintenance frame
 * is consumed even if malformed; any other push passes through to the next consumer. Listener
 * exceptions propagate to the read loop.
 */
final class MaintenanceEventConsumer implements PushConsumer {

  private final Connection connection;
  private final List<MaintenanceEventListener> listeners;

  MaintenanceEventConsumer(Connection connection, List<MaintenanceEventListener> listeners) {
    this.connection = connection;
    this.listeners = listeners;
  }

  @Override
  public PushConsumerContext handle(PushConsumerContext context) {
    PushMessage message = context.getMessage();
    MaintenancePushCodec.PushType type = MaintenancePushCodec.PushType.resolve(message.getType());

    // not a maintenance event
    if (type == null) {
      return context;
    }

    MaintenanceEvent event = MaintenancePushCodec.build(type, message);
    if (event != null) {
      for (MaintenanceEventListener listener : listeners) {
        event.accept(listener, connection);
      }
    }
    context.drop(); // a maintenance event is consumed even if malformed
    return context;
  }
}
