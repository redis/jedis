package redis.clients.jedis.commands;

import redis.clients.jedis.params.MigrateParams;

/**
 * Commands for database management that operate on a specific Redis node.
 * <p>
 * These commands are only available for single-node connections (e.g., {@code Jedis}). They are NOT
 * suitable for pooled or cluster connections because:
 * <ul>
 * <li>They operate on node-specific database indices (SELECT, SWAPDB)</li>
 * <li>They move/copy data between databases on the same node (MOVE)</li>
 * <li>Cross-database operations don't exist in cluster mode (only DB 0)</li>
 * </ul>
 * <p>
 * For cluster deployments, these operations should be performed by connecting directly to
 * individual nodes if needed. Note that Redis Cluster only supports database 0, so most of these
 * commands are not applicable.
 * @see DatabaseCommands for the full set of database commands
 */
public interface NodeDatabaseCommands {

  /**
   * Select the DB with having the specified zero-based numeric index.
   * <p>
   * Note: Redis Cluster only supports database 0.
   * @param index the database index
   * @return OK
   */
  String select(int index);

  /**
   * This command swaps two Redis databases, so that immediately all the clients connected to a
   * given database will see the data of the other database, and the other way around.
   * <p>
   * Note: Not available in Redis Cluster.
   * @param index1 first database index
   * @param index2 second database index
   * @return OK
   */
  String swapDB(int index1, int index2);

  /**
   * Move the specified key from the currently selected DB to the specified destination DB. Note
   * that this command returns 1 only if the key was successfully moved, and 0 if the target key was
   * already there or if the source key was not found at all, so it is possible to use MOVE as a
   * locking primitive.
   * <p>
   * Note: Not available in Redis Cluster.
   * @param key the specified key
   * @param dbIndex specified destination database
   * @return 1 if the key was moved, 0 if the key was not moved because already present on the
   *         target DB or was not found in the current DB
   */
  long move(String key, int dbIndex);

  /**
   * Binary version of {@link NodeDatabaseCommands#move(String, int) MOVE}.
   * @see NodeDatabaseCommands#move(String, int)
   */
  long move(byte[] key, int dbIndex);

  /**
   * Copy the value stored at the source key to the destination key in a specific database.
   * <p>
   * This version allows specifying a destination database and is only available for single-node
   * connections. For cluster-safe copy without database selection, use
   * {@link KeyCommands#copy(String, String, boolean)}.
   * @param srcKey the source key
   * @param dstKey the destination key
   * @param db the destination database index
   * @param replace removes the destination key before copying the value to it
   * @return {@code true} if source was copied, {@code false} otherwise
   */
  boolean copy(String srcKey, String dstKey, int db, boolean replace);

  /**
   * Binary version of {@link NodeDatabaseCommands#copy(String, String, int, boolean) COPY}.
   * @see NodeDatabaseCommands#copy(String, String, int, boolean)
   */
  boolean copy(byte[] srcKey, byte[] dstKey, int db, boolean replace);

  /**
   * Atomically transfer a key from a source Redis instance to a destination Redis instance with a
   * specific destination database.
   * <p>
   * This version allows specifying a destination database and is only available for single-node
   * connections. For cluster-safe migrate without database selection, use
   * {@link KeyCommands#migrate(String, int, String, int)}.
   * @param host target host
   * @param port target port
   * @param key migrate key
   * @param destinationDB target database index
   * @param timeout the maximum idle time in milliseconds
   * @return OK on success, or NOKEY if no keys were found
   */
  String migrate(String host, int port, String key, int destinationDB, int timeout);

  /**
   * Binary version of {@link NodeDatabaseCommands#migrate(String, int, String, int, int) MIGRATE}.
   * @see NodeDatabaseCommands#migrate(String, int, String, int, int)
   */
  String migrate(String host, int port, byte[] key, int destinationDB, int timeout);

  /**
   * Atomically transfer multiple keys from a source Redis instance to a destination Redis instance
   * with a specific destination database.
   * <p>
   * This version allows specifying a destination database and is only available for single-node
   * connections. For cluster-safe migrate without database selection, use
   * {@link KeyCommands#migrate(String, int, int, MigrateParams, String...)}.
   * @param host target host
   * @param port target port
   * @param destinationDB target database index
   * @param timeout the maximum idle time in milliseconds
   * @param params {@link MigrateParams}
   * @param keys keys to migrate
   * @return OK on success, or NOKEY if no keys were found
   */
  String migrate(String host, int port, int destinationDB, int timeout, MigrateParams params,
      String... keys);

  /**
   * Binary version of
   * {@link NodeDatabaseCommands#migrate(String, int, int, int, MigrateParams, String...) MIGRATE}.
   * @see NodeDatabaseCommands#migrate(String, int, int, int, MigrateParams, String...)
   */
  String migrate(String host, int port, int destinationDB, int timeout, MigrateParams params,
      byte[]... keys);
}
