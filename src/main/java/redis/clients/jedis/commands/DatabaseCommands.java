package redis.clients.jedis.commands;

import redis.clients.jedis.args.FlushMode;

/**
 * The full set of database commands for single-node Redis connections.
 * <p>
 * This interface combines node-specific database operations with common server commands
 * that relate to database management. It is intended for use with direct single-node
 * connections (e.g., {@code Jedis}).
 * <p>
 * For pooled or cluster connections, note that:
 * <ul>
 *   <li>Redis Cluster only supports database 0</li>
 *   <li>Commands like SELECT, SWAPDB, MOVE are not available in cluster mode</li>
 *   <li>Use {@link CommonServerCommands} for dbSize and flushDB in cluster environments</li>
 * </ul>
 * <p>
 * <b>Migration note:</b> This interface now extends {@link NodeDatabaseCommands} for better
 * modularity. Existing code using {@code DatabaseCommands} will continue to work unchanged.
 *
 * @see NodeDatabaseCommands for node-specific database operations
 * @see CommonServerCommands for cluster-safe database operations (dbSize, flushDB)
 */
public interface DatabaseCommands extends NodeDatabaseCommands {

  /**
   * Return the number of keys in the currently-selected database.
   * <p>
   * Note: This method is also available in {@link CommonServerCommands} for cluster-safe usage.
   * @return The number of keys
   */
  long dbSize();

  /**
   * Delete all the keys of the currently selected DB. This command never fails. The time-complexity
   * for this operation is O(N), N being the number of keys in the database.
   * <p>
   * Note: This method is also available in {@link CommonServerCommands} for cluster-safe usage.
   * @return OK
   */
  String flushDB();

  /**
   * Delete all the keys of the currently selected DB. This command never fails. The time-complexity
   * for this operation is O(N), N being the number of keys in the database.
   * <p>
   * Note: This method is also available in {@link CommonServerCommands} for cluster-safe usage.
   * @param flushMode can be SYNC or ASYNC
   * @return OK
   */
  String flushDB(FlushMode flushMode);
}
