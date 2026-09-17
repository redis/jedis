package redis.clients.jedis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisAsserts;

/**
 * Connection-init visitor wiring maintenance notifications, identically for both deployment types:
 * performs the {@code CLIENT MAINT_NOTIFICATIONS} handshake, registers the pool's controller as the
 * connection's maintenance listener, and installs the controller's relax overlays. What those
 * overlays are, whether the connection is tracked, and how events are reacted to is entirely the
 * {@link MaintenanceController} subtype's business.
 */
class MaintenanceAwareVisitor implements InitVisitor {

  private static final Logger logger = LoggerFactory.getLogger(MaintenanceAwareVisitor.class);

  private final Connection.Builder builder;
  private final MaintenanceController controller;

  MaintenanceAwareVisitor(Connection.Builder builder, MaintenanceController controller) {
    JedisAsserts.notNull(builder, "Connection.Builder must not be null");
    JedisAsserts.notNull(controller, "MaintenanceController must not be null");
    this.builder = builder;
    this.controller = controller;
  }

  /**
   * Tracks the connection (before {@code connect()}, so a connect racing a maintenance operation is
   * visible to the controller) and installs the controller's relax overlays before the handshake
   * runs, so a connection opened while a relax window is open already relaxes its AUTH/HELLO
   * handshake reads. The overlays are torn down again in {@link #visitAfterHandshake(Connection)}
   * if the feature turns out to be unsupported on this connection.
   */
  @Override
  public void visitBeforeHandshake(Connection connection) {
    MaintenanceNotificationsConfig mConfig = builder.getMaintenanceConfig();
    if (!isMaintenanceEnabled(mConfig)) {
      return;
    }
    controller.register(connection);
  }

  @Override
  public void visitAfterHandshake(Connection connection) {
    MaintenanceNotificationsConfig mConfig = builder.getMaintenanceConfig();
    if (!isMaintenanceEnabled(mConfig)) {
      return;
    }

    boolean strict = mConfig.getMode() == MaintenanceNotificationsConfig.Mode.ENABLED;
    boolean keepOverrides = false;

    MaintenanceEventConsumer consumer = null;
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

      // The server must accept CLIENT MAINT_NOTIFICATIONS ON. Pre-register the consumer so a push
      // frame the server emits immediately on accepting the subscription cannot race ahead.
      consumer = new MaintenanceEventConsumer(connection,
          connection.getMaintenanceEventListeners());
      connection.addPushConsumer(consumer);

      connection.sendCommand(Command.CLIENT, "MAINT_NOTIFICATIONS", "ON", "moving-endpoint-type",
        resolveEndpointType(connection, mConfig));
      try {
        connection.getStatusCodeReply();
        keepOverrides = true;
      } catch (JedisDataException e) {

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
        connection.removePushConsumer(consumer);
        // Undo the relax overlays installed before the handshake — this connection does NOT
        // support maintenance notifications.
        controller.unregister(connection);
      }
    }
  }

  private static boolean isMaintenanceEnabled(MaintenanceNotificationsConfig maintenanceConfig) {
    return maintenanceConfig != null
        && maintenanceConfig.getMode() != MaintenanceNotificationsConfig.Mode.DISABLED;
  }

  /** The {@code moving-endpoint-type} value for this connection. */
  private String resolveEndpointType(Connection connection, MaintenanceNotificationsConfig config) {
    // TLS as declared by the client config: enabled when either ssl(true) or sslOptions is set,
    // mirroring DefaultJedisSocketFactory#createSocket.
    JedisClientConfig clientConfig = builder.getClientConfig();
    boolean sslEnabled = clientConfig.isSsl() || clientConfig.getSslOptions() != null;
    MaintenanceNotificationsConfig.EndpointType endpointType = config.getEndpointTypeResolver()
        .getEndpointType(connection.getRemoteSocketAddress(), sslEnabled);
    switch (endpointType) {
      case INTERNAL_IP:
        return "internal-ip";
      case INTERNAL_FQDN:
        return "internal-fqdn";
      case EXTERNAL_IP:
        return "external-ip";
      case EXTERNAL_FQDN:
        return "external-fqdn";
      case NONE:
        return "none";
      default:
        throw new JedisException("Unknown endpoint type: " + endpointType);
    }
  }
}
