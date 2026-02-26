package redis.clients.jedis.commands;

import redis.clients.jedis.args.SaveMode;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.params.ShutdownParams;

/**
 * Commands for shutting down a Redis server.
 * <p>
 * These commands are only available for single-node connections (e.g., {@code Jedis}).
 * They are NOT suitable for pooled or cluster connections because:
 * <ul>
 *   <li>They terminate the Redis server process</li>
 *   <li>In a cluster, shutting down nodes should be done carefully to maintain availability</li>
 *   <li>Pool connections would lose their server</li>
 * </ul>
 * <p>
 * For cluster deployments, shutdown operations should be performed by connecting
 * directly to individual nodes as part of a controlled maintenance procedure.
 * 
 * @see ServerCommands for the full set of server commands
 */
public interface ShutdownCommands {

  /**
   * Stop all the client. Perform a SAVE (if one save point is configured). Flush the append only
   * file if AOF is enabled quit the server
   * @throws JedisException only in case of error.
   */
  void shutdown() throws JedisException;

  /**
   * Shutdown the server with the specified save mode.
   * @param saveMode the save mode (SAVE, NOSAVE)
   * @throws JedisException only in case of error.
   */
  default void shutdown(SaveMode saveMode) throws JedisException {
    shutdown(ShutdownParams.shutdownParams().saveMode(saveMode));
  }

  /**
   * Shutdown the server with the specified parameters.
   * @see SaveMode
   * @param shutdownParams set commands parameters
   * @throws JedisException only in case of error.
   */
  void shutdown(ShutdownParams shutdownParams) throws JedisException;

  /**
   * Abort a server shutdown operation that is in progress.
   * @return OK if the shutdown was aborted
   */
  String shutdownAbort();
}

