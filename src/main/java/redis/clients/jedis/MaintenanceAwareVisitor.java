package redis.clients.jedis;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.TimeoutSource.TimeoutInfo;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisAsserts;

public class MaintenanceAwareVisitor implements InitVisitor {

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceAwareVisitor.class);

  private final Connection.Builder builder;
  private final MaintenanceEventController controller;

  MaintenanceAwareVisitor(Connection.Builder builder,
      MaintenanceEventController maintenanceController) {
    JedisAsserts.notNull(builder, "Connection.Builder must not be null");
    JedisAsserts.notNull(maintenanceController, "MaintenanceEventController must not be null");
    this.builder = builder;
    this.controller = maintenanceController;
  }

  @Override
  public void visit(Connection connection) {
    RedisProtocol protocol = connection.getRedisProtocol();
    MaintenanceNotificationsConfig maintenanceConfig = builder.getMaintenanceConfig();

    if (maintenanceConfig == null
        || maintenanceConfig.getMode() == MaintenanceNotificationsConfig.Mode.DISABLED) {
      return;
    }
    boolean strict = maintenanceConfig.getMode() == MaintenanceNotificationsConfig.Mode.ENABLED;

    // Maintenance push frames require RESP3.
    if (protocol != RedisProtocol.RESP3) {
      String reason = "RESP3 is required but the established protocol is "
          + (protocol == null ? "RESP2" : protocol);
      if (strict) {
        throw new JedisConnectionException("Maintenance notifications: " + reason);
      }
      logger.debug("Maintenance notifications disabled: {}.", reason);
      return;
    }

    /*
     * Listeners notified synchronously of this connection's maintenance events. One for controller,
     * one for the config's listener (if any). Iteration order is significant and must stay
     * controller-first: the controller registers the pool rebind off this connection's peer
     * address, while the public listener may mutate the connection (e.g. close it). A LinkedHashSet
     * preserves insertion order so the controller always completes its handoff bookkeeping before
     * any user callback runs.
     */
    Set<MaintenanceEventListener> maintenanceListeners;
    if (maintenanceConfig.getMaintenanceListener() == null) {
      maintenanceListeners = Collections.singleton(controller);
    } else {
      maintenanceListeners = new LinkedHashSet<>(2, 1f);
      maintenanceListeners.add(controller);
      maintenanceListeners.add(maintenanceConfig.getMaintenanceListener());
    }

    // The server must accept CLIENT MAINT_NOTIFICATIONS ON. Pre-register the consumer so a
    // push
    // frame the server emits immediately on accepting the subscription cannot race ahead.
    MaintenanceEventConsumer consumer = new MaintenanceEventConsumer(connection,
        maintenanceListeners);
    connection.addPushConsumer(consumer);
    ExpiringTimeoutSource relaxedTimeoutSource = new ExpiringTimeoutSource(new TimeoutInfo(
        maintenanceConfig.getRelaxedTimeout(), maintenanceConfig.getRelaxedBlockingTimeout()));
    connection.enableTimeoutRelaxing(relaxedTimeoutSource);

    ChainedTimeoutSource rebindTimeoutSource = new RebindTimeoutSource(controller);
    relaxedTimeoutSource.addOverride(rebindTimeoutSource);

    connection.sendCommand(Command.CLIENT, "MAINT_NOTIFICATIONS", "ON", "moving-endpoint-type",
      resolveEndpointType(maintenanceConfig.getEndpointType()));
    try {
      connection.getStatusCodeReply();
    } catch (JedisDataException e) {
      connection.removePushConsumer(consumer);
      relaxedTimeoutSource.removeOverride(rebindTimeoutSource);
      connection.disableTimeoutRelaxing();
      if (strict) {
        throw new JedisConnectionException(
            "Maintenance notifications: events not supported on server", e);
      }
      logger.debug(
        "Maintenance notifications disabled: server rejected CLIENT MAINT_NOTIFICATIONS ({}).",
        e.getMessage());
    }
  }

  private String resolveEndpointType(MaintenanceNotificationsConfig.EndpointType endpointType) {
    switch (endpointType) {
      case INTERNAL_IP:
        return "internal-ip";
      case INTERNAL_FQDN:
        return "internal-fqdn";
      case EXTERNAL_IP:
        return "external-ip";
      case EXTERNAL_FQDN:
        return "external-fqdn";
      default:
        throw new JedisException("Unknown endpoint type: " + endpointType);
    }
  }

  /** Pool-wide MOVING rebind overlay: active while the controller reports a valid rebind window. */
  private static final class RebindTimeoutSource extends ChainedTimeoutSource {

    private final MaintenanceEventController controller;

    RebindTimeoutSource(MaintenanceEventController controller) {
      super(null);
      this.controller = controller;
    }

    @Override
    protected TimeoutInfo getOwnInfo() {
      return controller.getTimeoutSupplier().get();
    }
  }
}