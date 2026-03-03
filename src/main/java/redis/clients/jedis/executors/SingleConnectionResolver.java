package redis.clients.jedis.executors;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;

/**
 * Connection resolver that returns a pre-configured connection.
 * <p>
 * This resolver is used for broadcast commands where we already have a specific connection
 * from a connection pool and want to use it with the standard retry logic in
 * {@link ClusterCommandExecutor#doExecuteCommand}.
 * <p>
 * <b>Important:</b> This resolver does not manage the connection lifecycle. The connection
 * is expected to be managed externally (e.g., by the caller who obtained it from a pool).
 * The {@code doExecuteCommand} method will close the connection after use.
 *
 * @see ClusterCommandExecutor#broadcastCommand(CommandObject, boolean)
 */
final class SingleConnectionResolver implements ConnectionResolver {

  private final Connection connection;

  /**
   * Creates a resolver that always returns the given connection.
   *
   * @param connection the connection to return for all resolve calls
   */
  SingleConnectionResolver(Connection connection) {
    this.connection = connection;
  }

  @Override
  public Connection resolve(CommandObject<?> cmd) {
    return connection;
  }
}

