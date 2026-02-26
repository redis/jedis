package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.args.FlushMode;
import redis.clients.jedis.params.LolwutParams;

/**
 * Server commands that can be safely executed on both single-node and pooled/cluster connections.
 * <p>
 * These commands either:
 * <ul>
 *   <li>Work on any arbitrary node (ping, echo, lolwut)</li>
 *   <li>Can be broadcast to all shards with meaningful aggregation (info, flushDB, flushAll, dbSize)</li>
 * </ul>
 * <p>
 * This interface is implemented by both {@code Jedis} (via {@code ServerCommands}) and 
 * {@code UnifiedJedis} for use in pooled and cluster environments.
 * 
 * @see ServerCommands for the full set of server commands (single-node only)
 */
public interface CommonServerCommands {

  /**
   * This command is often used to test if a connection is still alive, or to measure latency.
   * @return PONG
   */
  String ping();

  /**
   * Returns the message.
   * @param message the message to return
   * @return the message
   */
  String ping(String message);

  /**
   * Returns the string.
   * @param string the string to echo
   * @return the string
   */
  String echo(String string);

  /**
   * Binary version of {@link #echo(String)}.
   * @param arg the bytes to echo
   * @return the bytes
   */
  byte[] echo(byte[] arg);

  /**
   * Delete all the keys of the currently selected DB. This command never fails. The time-complexity
   * for this operation is O(N), N being the number of keys in the database.
   * <p>
   * In cluster mode, this command is broadcast to all primary nodes.
   * @return OK
   */
  String flushDB();

  /**
   * Delete all the keys of the currently selected DB. This command never fails. The time-complexity
   * for this operation is O(N), N being the number of keys in the database.
   * <p>
   * In cluster mode, this command is broadcast to all primary nodes.
   * @param flushMode can be SYNC or ASYNC
   * @return OK
   */
  String flushDB(FlushMode flushMode);

  /**
   * Delete all the keys of all the existing databases, not just the currently selected one.
   * <p>
   * In cluster mode, this command is broadcast to all primary nodes.
   * @return a simple string reply (OK)
   */
  String flushAll();

  /**
   * Delete all the keys of all the existing databases, not just the currently selected one.
   * <p>
   * In cluster mode, this command is broadcast to all primary nodes.
   * @param flushMode SYNC or ASYNC
   * @return a simple string reply (OK)
   */
  String flushAll(FlushMode flushMode);

  /**
   * The INFO command returns information and statistics about the server in a format that is simple
   * to parse by computers and easy to read by humans.
   * @return information on the server
   */
  String info();

  /**
   * The INFO command returns information and statistics about the server in a format that is simple
   * to parse by computers and easy to read by humans.
   * @param section (all: Return all sections, default: Return only the default set of sections,
   *          server: General information about the Redis server, clients: Client connections
   *          section, memory: Memory consumption related information, persistence: RDB and AOF
   *          related information, stats: General statistics, replication: Master/slave replication
   *          information, cpu: CPU consumption statistics, commandstats: Redis command statistics,
   *          cluster: Redis Cluster section, keyspace: Database related statistics)
   * @return info
   */
  String info(String section);

  /**
   * Return the number of keys in the currently-selected database.
   * <p>
   * In cluster mode, this returns the sum across all primary nodes.
   * @return The number of keys
   */
  long dbSize();

  /**
   * The LOLWUT command displays the Redis version and a piece of computer art.
   * @return the Redis version and computer art
   */
  String lolwut();

  /**
   * The LOLWUT command displays the Redis version and a piece of computer art.
   * @param lolwutParams options for the LOLWUT command
   * @return the Redis version and computer art
   */
  String lolwut(LolwutParams lolwutParams);

  /**
   * The TIME command returns the current server time as a two items list:
   * a Unix timestamp and the amount of microseconds already elapsed in the current second.
   * <p>
   * This command is useful for scripts that need to handle time-related operations
   * or for synchronization purposes.
   * <p>
   * In cluster mode, this command can be executed on any node.
   * @return a list of two strings: the first is the Unix timestamp (seconds),
   *         the second is the microseconds elapsed in the current second
   */
  List<String> time();
}

