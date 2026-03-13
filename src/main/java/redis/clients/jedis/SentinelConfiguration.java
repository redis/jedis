package redis.clients.jedis;

import java.util.Set;

/**
 * Configuration interface for Redis Sentinel connections.
 * <p>
 * This interface defines the configuration needed for connecting to Redis Sentinel instances,
 * including the master name, sentinel nodes, and client configuration.
 * </p>
 */
interface SentinelConfiguration {

  /**
   * Returns the master name being monitored by the sentinels.
   * @return the master name
   */
  String getMasterName();

  /**
   * Returns the configured sentinel nodes.
   * @return a set of sentinel nodes
   */
  Set<HostAndPort> getSentinels();

  /**
   * Returns the client configuration for sentinel connections.
   * @return the sentinel client configuration
   */
  JedisClientConfig getSentinelClientConfig();
}
