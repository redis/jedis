package redis.clients.jedis.commands;

import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.params.MigrateParams;

public interface DatabaseCommands {

  /**
   * Select the DB with having the specified zero-based numeric index.
   * @param index the index
   * @return a simple string reply OK
   */
  String select(int index);

  /**
   * Return the number of keys in the currently-selected database.
   * @return the number of key in the currently-selected database.
   */
  long dbSize();

  /**
   * Delete all the keys of the currently selected DB. This command never fails. The time-complexity
   * for this operation is O(N), N being the number of keys in the database.
   * @param flushMode
   * @return OK
   */
  String flushDB(FlushMode flushMode);

  /**
   * This command swaps two Redis databases, so that immediately all the clients connected to a
   * given database will see the data of the other database, and the other way around.
   * @param index1
   * @param index2
   * @return Simple string reply: OK if SWAPDB was executed correctly.
   */
  String swapDB(int index1, int index2);

  long move(String key, int dbIndex);

  long move(byte[] key, int dbIndex);

  boolean copy(String srcKey, String dstKey, int db, boolean replace);

  boolean copy(byte[] srcKey, byte[] dstKey, int db, boolean replace);

  String migrate(String host, int port, byte[] key, int destinationDB, int timeout);

  String migrate(String host, int port, int destinationDB, int timeout, MigrateParams params,
      byte[]... keys);

  String migrate(String host, int port, String key, int destinationDB, int timeout);

  String migrate(String host, int port, int destinationDB, int timeout, MigrateParams params,
      String... keys);

}
