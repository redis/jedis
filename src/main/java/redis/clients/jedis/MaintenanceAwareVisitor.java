package redis.clients.jedis;

import java.util.Set;

import org.slf4j.Logger;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.TimeoutSource.TimeoutInfo;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisAsserts;

public class MaintenanceAwareVisitor implements InitVisitor {

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
    Logger logger = Connection.logger;
    RedisProtocol protocol = connection.getRedisProtocol();
    Set<MaintenanceEventListener> maintenanceEventListeners = connection
        .getMaintenanceEventListeners();
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

    maintenanceEventListeners.add(controller);

    // The server must accept CLIENT MAINT_NOTIFICATIONS ON. Pre-register the consumer so a
    // push
    // frame the server emits immediately on accepting the subscription cannot race ahead.
    MaintenanceEventConsumer consumer = new MaintenanceEventConsumer(connection,
        maintenanceEventListeners);
    connection.addPushConsumer(consumer);
    ExpiringTimeoutSource relaxedTimeoutSource = new ExpiringTimeoutSource(
        new TimeoutInfo(Math.max(connection.getSoTimeout(), maintenanceConfig.getRelaxedTimeout()),
            Math.max(connection.getBlockingSoTimeout(),
              maintenanceConfig.getRelaxedBlockingTimeout())));

    ChainedTimeoutSource rebindTimeoutSource = new RebindTimeoutSource(controller,
        connection.getSoTimeout(), connection.getBlockingSoTimeout());
    relaxedTimeoutSource.addOverride(rebindTimeoutSource);
    connection.enableTimeoutRelaxing(relaxedTimeoutSource);

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
    private final int configuredTimeout;
    private final int configuredBlockingTimeout;

    RebindTimeoutSource(MaintenanceEventController controller, int configuredTimeout,
        int configuredBlockingTimeout) {
      super(null);
      this.controller = controller;
      this.configuredTimeout = configuredTimeout;
      this.configuredBlockingTimeout = configuredBlockingTimeout;
    }

    @Override
    protected TimeoutInfo getOwnInfo() {
      TimeoutInfo raw = controller.getTimeoutSupplier().get();
      if (raw == null) return null;
      return new TimeoutInfo(Math.max(configuredTimeout, raw.timeout),
          Math.max(configuredBlockingTimeout, raw.blockingTimeout));
    }
  }
}
