package redis.clients.jedis.executors;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;

/**
 * Connection resolver that acquires connections from a specific connection pool.
 * <p>
 * This resolver is used for broadcast commands where we want to execute commands on a specific
 * node's pool with the standard retry logic in {@link ClusterCommandExecutor#doExecuteCommand}.
 * <p>
 * Unlike other resolvers, this one is bound to a specific pool rather than using the cluster's
 * routing logic. A fresh connection is obtained from the pool on each call to {@link #resolve},
 * which allows retry logic to work correctly since the {@code doExecuteCommand} method closes the
 * connection after each attempt.
 * @see ClusterCommandExecutor#broadcastCommand(CommandObject, boolean)
 */
final class SingleConnectionResolver implements ConnectionResolver {

  private final ConnectionPool pool;

  /**
   * Creates a resolver that acquires connections from the given pool.
   * @param pool the connection pool to acquire connections from
   */
  SingleConnectionResolver(ConnectionPool pool) {
    this.pool = pool;
  }

  @Override
  public Connection resolve(CommandObject<?> cmd) {
    return pool.getResource();
  }
}
