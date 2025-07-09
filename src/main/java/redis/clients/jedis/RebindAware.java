package redis.clients.jedis;

import redis.clients.jedis.annots.Experimental;

/**
 * Interface for components that support rebinding to a new host and port.
 * Implementations of this interface can be notified when a Redis server sends
 * a MOVING notification during maintenance events.
 *
 * This interface can be implemented by various components such as:
 * - Connection pools
 * - Socket factories
 * - Connection providers
 * - Any component that manages connections to Redis servers
 */
@Experimental
public interface RebindAware {

  /**
   * Notifies the component that a re-bind to a new host and port is scheduled. This is called when
   * a MOVING notification is received. Components that implement this interface should update their
   * internal state to reflect the new host and port, and return true if the re-bind was accepted.
   * Components might decide to reject the re-bind request if they are not in a state to support
   * it.
   * @param newHostAndPort The new host and port to use for new connections
   */
  void rebind(HostAndPort newHostAndPort);
}