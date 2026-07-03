package redis.clients.jedis;

import java.util.List;

import org.slf4j.Logger;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.TimeoutSupplierChain.TimeoutInfo;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisAsserts;

public class MaintenanceAwareVisitor implements InitVisitor {

  private final Connection.Builder builder;

  MaintenanceAwareVisitor(Connection.Builder builder) {
    JedisAsserts.notNull(builder, "Connection.Builder must not be null");
    this.builder = builder;
  }

  /**
   * Activates server-side maintenance notifications on this connection so the client can react to
   * cluster maintenance events ({@code MIGRATING}, {@code MIGRATED}, {@code FAILING_OVER},
   * {@code FAILED_OVER}, {@code MOVING}). Each parsed event is dispatched synchronously to the
   * registered {@link MaintenanceEventListener}s.
   * <p>
   * Gated by {@link #maintenanceConfig}, set by the builder:
   * <ul>
   * <li>{@code null} or {@code DISABLED} — maintenance off; no-op.</li>
   * <li>negotiated protocol is not RESP3 — {@code ENABLED}: throws; {@code AUTO}: debug-log and
   * return.</li>
   * <li>server rejects {@code CLIENT MAINT_NOTIFICATIONS ON} — same strict/lax split.</li>
   * </ul>
   * @throws JedisConnectionException in {@code ENABLED} mode when a prerequisite is not met or the
   *           server rejects the subscription
   */
  @Override
  public void visit(Connection connection) {
    Logger logger = Connection.logger;
    RedisProtocol protocol = connection.getRedisProtocol();
    List<MaintenanceEventListener> maintenanceEventListeners = builder
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

    // The server must accept CLIENT MAINT_NOTIFICATIONS ON. Pre-register the consumer so a
    // push
    // frame the server emits immediately on accepting the subscription cannot race ahead.
    MaintenanceEventConsumer consumer = new MaintenanceEventConsumer(connection,
        maintenanceEventListeners);
    connection.addPushConsumer(consumer);
    ExpiringTimeoutSupplier relaxedTimeout = new ExpiringTimeoutSupplier(new TimeoutInfo(
        maintenanceConfig.getRelaxedTimeout(), maintenanceConfig.getRelaxedBlockingTimeout()));
    connection.enableRelaxedTimeouts(relaxedTimeout);
    connection.sendCommand(Command.CLIENT, "MAINT_NOTIFICATIONS", "ON", "moving-endpoint-type",
      resolveEndpointType(maintenanceConfig.getEndpointType()));
    try {
      connection.getStatusCodeReply();
      connection.relaxTimeouts(0);
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
}