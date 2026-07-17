package redis.clients.jedis;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Push consumer for server maintenance events: resolves and decodes each frame via
 * {@link MaintenancePushCodec}, then dispatches the typed {@link MaintenanceEvent} to the
 * connection's {@link MaintenanceEventHandler}s on the read thread. A recognized maintenance frame
 * is consumed even if malformed (logged and discarded); any other push passes through to the next
 * consumer. Listener exceptions propagate to the read loop.
 */
final class MaintenanceEventConsumer implements PushConsumer {

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceEventConsumer.class);

  private final Connection connection;
  private final Set<MaintenanceEventHandler> listeners;

  MaintenanceEventConsumer(Connection connection, Set<MaintenanceEventHandler> listeners) {
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

    MaintenanceEvent event;
    try {
      event = MaintenancePushCodec.build(type, message);
    } catch (MalformedMaintenanceEventException e) {
      logger.warn("Ignoring malformed maintenance push: {}", message.getContent(), e);
      context.drop(); // recognized maintenance frame is consumed even when malformed
      return context;
    }

    for (MaintenanceEventHandler listener : listeners) {
      event.accept(listener, connection);
    }
    context.drop();
    return context;
  }
}
