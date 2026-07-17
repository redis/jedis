package redis.clients.jedis;

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

  /**
   * Installs the pool-wide MOVING rebind timeout overlay before the handshake runs, so a connection
   * opened while a rebind window is open already relaxes its AUTH/HELLO handshake reads. The
   * overlay is torn down again in {@link #visit(Connection)} if the feature turns out to be
   * unsupported on this connection.
   */
  @Override
  public void visitBeforeHandshake(Connection connection) {
    MaintenanceNotificationsConfig mConfig = builder.getMaintenanceConfig();
    if (!isMaintenanceEnabled(mConfig)) {
      return;
    }

    ChainedTimeoutSource dts = connection.getTimeoutSource();
    dts.addOverride(new RebindTimeoutSource(controller));

    TimeoutInfo relaxedTimeout = new TimeoutInfo(mConfig.getRelaxedTimeout(),
        mConfig.getRelaxedBlockingTimeout());
    dts.addOverride(new ExpiringTimeoutSource(relaxedTimeout));
  }

  @Override
  public void visitAfterHandshake(Connection connection) {
    MaintenanceNotificationsConfig mConfig = builder.getMaintenanceConfig();
    if (!isMaintenanceEnabled(mConfig)) {
      return;
    }

    boolean strict = mConfig.getMode() == MaintenanceNotificationsConfig.Mode.ENABLED;
    boolean keepOverrides = false;

    try {
      // Maintenance push frames require RESP3.
      RedisProtocol protocol = connection.getRedisProtocol();
      if (protocol != RedisProtocol.RESP3) {
        String reason = "RESP3 is required but the established protocol is "
            + (protocol == null ? "RESP2" : protocol);
        if (strict) {
          throw new JedisConnectionException("Maintenance notifications: " + reason);
        }
        logger.debug("Maintenance notifications disabled: {}.", reason);
        return;
      }

      Set<MaintenanceEventListener> maintenanceEventListeners = connection
          .getMaintenanceEventListeners();
      maintenanceEventListeners.add(controller);

      // The server must accept CLIENT MAINT_NOTIFICATIONS ON. Pre-register the consumer so a
      // push
      // frame the server emits immediately on accepting the subscription cannot race ahead.
      MaintenanceEventConsumer consumer = new MaintenanceEventConsumer(connection,
          maintenanceEventListeners);
      connection.addPushConsumer(consumer);

      connection.sendCommand(Command.CLIENT, "MAINT_NOTIFICATIONS", "ON", "moving-endpoint-type",
        resolveEndpointType(mConfig.getEndpointType()));
      try {
        connection.getStatusCodeReply();
        keepOverrides = true;
      } catch (JedisDataException e) {
        connection.removePushConsumer(consumer);

        if (strict) {
          throw new JedisConnectionException(
              "Maintenance notifications: events not supported on server", e);
        }
        logger.debug(
          "Maintenance notifications disabled: server rejected CLIENT MAINT_NOTIFICATIONS ({}).",
          e.getMessage());
      }
    } finally {
      if (!keepOverrides) {
        // Undo the rebind overlay installed before the handshake — this connection does NOT support
        // maintenance notifications.
        ChainedTimeoutSource dts = connection.getTimeoutSource();
        dts.removeOverride(dts.seekBy(controller));
        dts.removeOverride(dts.seekBy(ExpiringTimeoutSource.class));
      }
    }
  }

  private static boolean isMaintenanceEnabled(MaintenanceNotificationsConfig maintenanceConfig) {
    return maintenanceConfig != null
        && maintenanceConfig.getMode() != MaintenanceNotificationsConfig.Mode.DISABLED;
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
      super(null, controller);
      this.controller = controller;
    }

    @Override
    protected TimeoutInfo getOwnInfo() {
      return controller.getTimeoutSupplier().get();
    }
  }
}