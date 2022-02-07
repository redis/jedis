package redis.clients.jedis.commands;

import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.params.MigrateParams;

public interface DatabaseCommands {

  /**
   * Select the DB with having the specified zero-based numeric index.
   * @param index the index
   * @return OK
   */
  String select(int index);

  /**
   * Return the number of keys in the currently-selected database.
   * @return The number of keys
   */
  long dbSize();

  /**
   * Delete all the keys of the currently selected DB. This command never fails. The time-complexity
   * for this operation is O(N), N being the number of keys in the database.
   * @param flushMode can be SYNC or ASYNC
   * @return OK
   */
  String flushDB(FlushMode flushMode);

  /**
   * This command swaps two Redis databases, so that immediately all the clients connected to a
   * given database will see the data of the other database, and the other way around.
   * @param index1
   * @param index2
   * @return OK
   */
  String swapDB(int index1, int index2);

  /**
   * Move the specified key from the currently selected DB to the specified destination DB. Note
   * that this command returns 1 only if the key was successfully moved, and 0 if the target key was
   * already there or if the source key was not found at all, so it is possible to use MOVE as a
   * locking primitive.
   * @param key  The specified key
   * @param dbIndex Specified destination database
   * @return 1 if the key was moved, 0 if the key was not moved because already present on the target
   * DB or was not found in the current DB
   */
  long move(String key, int dbIndex);

  /**
   * Binary version of {@link DatabaseCommands#move(String, int) MOVE}.
   * @see DatabaseCommands#move(String, int)
   */
  long move(byte[] key, int dbIndex);

  /**
   * Copy the value stored at the source key to the destination key.
   * @param srcKey the source key.
   * @param dstKey the destination key.
   * @param db allows specifying an alternative logical database index for the destination key.
   * @param replace removes the destination key before copying the value to it, in order to avoid error.
   */
  boolean copy(String srcKey, String dstKey, int db, boolean replace);

  /**
   * Binary version of {@link DatabaseCommands#copy(String, String, int, boolean) COPY}.
   * @see DatabaseCommands#copy(String, String, int, boolean)
   */
  boolean copy(byte[] srcKey, byte[] dstKey, int db, boolean replace);

  /**
   * <b><a href="http://redis.io/commands/migrate">Migrate Command</a></b>
   * Atomically transfer a key from a source Redis instance to a destination Redis instance.
   * On success the key is deleted from the original instance and is guaranteed to exist in
   * the target instance.
   *
   * @param host          target host
   * @param port          target port
   * @param key           migrate key
   * @param destinationDB target db
   * @param timeout       the maximum idle time in any moment of the communication with the
   *                      destination instance in milliseconds.
   * @return OK on success, or NOKEY if no keys were found in the source instance
   */
  String migrate(String host, int port, String key, int destinationDB, int timeout);

  /**
   * Binary version of {@link DatabaseCommands#migrate(String, int, String, int, int) MIGRATE}.
   * @see DatabaseCommands#migrate(String, int, String, int, int)
   */
  String migrate(String host, int port, byte[] key, int destinationDB, int timeout);

  /**
   * <b><a href="http://redis.io/commands/migrate">Migrate Command</a></b>
   * Atomically transfer a key from a source Redis instance to a destination Redis instance.
   * On success the key is deleted from the original instance and is guaranteed to exist in
   * the target instance.
   * @param host          target host
   * @param port          target port
   * @param destinationDB target db
   * @param timeout the maximum idle time in any moment of the communication with the
   *               destination instance in milliseconds.
   * @param params {@link MigrateParams}
   * @param keys to migrate
   * @return OK on success, or NOKEY if no keys were found in the source instance.
   */
  String migrate(String host, int port, int destinationDB, int timeout, MigrateParams params,
      String... keys);

  /**
   * Binary version of {@link DatabaseCommands#migrate(String, int, int, int, MigrateParams, String...) MIGRATE}.
   * @see DatabaseCommands#migrate(String, int, int, int, MigrateParams, String...)
   */
  String migrate(String host, int port, int destinationDB, int timeout, MigrateParams params,
      byte[]... keys);
}
