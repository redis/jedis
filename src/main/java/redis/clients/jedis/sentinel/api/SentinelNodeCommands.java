package redis.clients.jedis.sentinel.api;

import redis.clients.jedis.commands.SentinelCommands;

/**
 * Commands interface for Redis Sentinel instance operations.
 * <p>
 * Redis Sentinel instances support a subset of Redis' commands in addition to all SENTINEL
 * subcommands.
 * </p>
 * @see SentinelCommands
 * @see <a href="https://redis.io/docs/latest/operate/oss_and_stack/management/sentinel/">Redis
 *      Sentinel Documentation</a>
 * @since 7.5.0
 */
public interface SentinelNodeCommands extends SentinelCommands {

  /**
   * Test if the connection to the node is alive.
   * @return PONG
   */
  String ping();
}
