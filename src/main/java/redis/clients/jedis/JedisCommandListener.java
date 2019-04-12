package redis.clients.jedis;

import redis.clients.jedis.commands.ProtocolCommand;

/**
 * A listener for command events.
 * <p>
 * All commands on the same connections are executed synchronously, however there the
 * implementation must be thread safe because the listener might be invoked from
 * different threads.
 * <p>
 * The listener is guaranteed to call {@link #commandStarted} and one of the
 * {@link #commandFinished} or {@link #commandFailed} once for each command.
 * <p>
 * The listener implementation must be exception free or else none of the aforementioned
 * contracts will be enforced.
 */
public interface JedisCommandListener {
  /**
   * Called for every command event that started execution
   *
   * @param connection the connection on which the command the executed
   * @param event the executable command, see {@link Protocol.Command}
   * @param args byte array of the command arguments
   */
  void commandStarted(Connection connection, ProtocolCommand event, byte[]... args);

  /**
   * Called after connection has been established to Redis, but before the command has executed
   * @param connection the connection on which the command the executed
   * @param event the executable command, see {@link Protocol.Command}
   */
  void commandConnected(Connection connection, ProtocolCommand event);

  /**
   * Called for every successfully executed command
   *
   * @param connection the connection on which the command the executed
   * @param event the executable command, see {@link Protocol.Command}
   */
  void commandFinished(Connection connection, ProtocolCommand event);

  /**
   * Called for every command that failed exceptionally
   *
   * @param connection the connection on which the command the executed
   * @param event the executable command, see {@link Protocol.Command}
   * @param t the triggered exception
   */
  void commandFailed(Connection connection, ProtocolCommand event, Throwable t);
}
