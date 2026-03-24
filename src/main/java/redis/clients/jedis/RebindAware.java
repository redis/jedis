package redis.clients.jedis;

import redis.clients.jedis.annots.Experimental;

/**
 * Interface for components that support rebinding to a new host and port.
 * <p>
 * Implementations of this interface can be notified when a Redis server sends a MOVING notification
 * during maintenance events. This interface can be implemented by various components such as: -
 * Connection pools - Socket factories - Connection providers - Any component that manages
 * connections to Redis servers
 * </p>
 */
@Experimental
public interface RebindAware {

  /**
   * Notifies the component that a re-bind to a new host and port is scheduled.
   * <p>
   * Called when a MOVING notification is received.
   * </p>
   * @param newHostAndPort The new host and port to use for new connections
   */
  void rebind(HostAndPort newHostAndPort);
}