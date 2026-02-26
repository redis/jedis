package redis.clients.jedis.commands;

/**
 * The full set of server commands for single-node Redis connections.
 * <p>
 * This interface combines all server-related command sub-interfaces and is intended for use
 * with direct single-node connections (e.g., {@code Jedis}). It includes commands that:
 * <ul>
 *   <li>Work on any node ({@link CommonServerCommands})</li>
 *   <li>Manage persistence ({@link PersistenceCommands})</li>
 *   <li>Manage replication ({@link ReplicationCommands})</li>
 *   <li>Control server shutdown ({@link ShutdownCommands})</li>
 *   <li>Monitor latency ({@link LatencyCommands})</li>
 *   <li>Track hotkeys ({@link HotkeysCommands})</li>
 *   <li>Handle authentication ({@link AuthCommands})</li>
 * </ul>
 * <p>
 * For pooled or cluster connections, use {@link CommonServerCommands} instead, which contains
 * only the commands that are safe to execute in those environments.
 * <p>
 * <b>Migration note:</b> This interface now extends multiple sub-interfaces for better
 * modularity. Existing code using {@code ServerCommands} will continue to work unchanged.
 *
 * @see CommonServerCommands for commands safe for pooled/cluster connections
 * @see PersistenceCommands for RDB/AOF persistence commands
 * @see ReplicationCommands for replication management commands
 * @see ShutdownCommands for server shutdown commands
 * @see LatencyCommands for latency monitoring commands
 * @see HotkeysCommands for hotkey tracking commands
 * @see AuthCommands for authentication commands
 */
public interface ServerCommands extends CommonServerCommands, PersistenceCommands,
    ReplicationCommands, ShutdownCommands, LatencyCommands, HotkeysCommands, AuthCommands {

  /**
   * Reset the connection. Performs a full reset of the connection's server-side context,
   * mimicking the effect of disconnecting and reconnecting again.
   * @return RESET
   */
  String reset();
}
