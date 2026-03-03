package redis.clients.jedis.executors;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.providers.ClusterConnectionProvider;

/**
 * Connection resolver that always routes to replica nodes.
 * <p>
 * This resolver is used to enforce command execution on replicas, regardless of the
 * command type or global ReadFrom configuration.
 *
 * @see redis.clients.jedis.executors.ClusterCommandExecutor#executeCommandToReplica(CommandObject)
 */
final class ReplicaOnlyConnectionResolver implements ConnectionResolver {

  private final ClusterConnectionProvider provider;

  ReplicaOnlyConnectionResolver(ClusterConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public Connection resolve(CommandObject<?> cmd) {
    return provider.getReplicaConnection(cmd.getArguments());
  }
}

