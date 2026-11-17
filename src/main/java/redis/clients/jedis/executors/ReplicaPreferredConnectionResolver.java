package redis.clients.jedis.executors;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.providers.ClusterConnectionProvider;

/**
 * Connection resolver that prefers replica nodes.
 * <p>
 * A keyed command goes to a replica of the key's slot, a keyless command to any replica. Falls back
 * to a primary when no replica is known, which is always the case unless
 * {@link redis.clients.jedis.JedisClientConfig#isReadOnlyForRedisClusterReplicas()} is enabled.
 * @see redis.clients.jedis.executors.ClusterCommandExecutor#executeCommandToReplica(CommandObject)
 */
final class ReplicaPreferredConnectionResolver implements ConnectionResolver {

  private final ClusterConnectionProvider provider;

  ReplicaPreferredConnectionResolver(ClusterConnectionProvider provider) {
    this.provider = provider;
  }

  @Override
  public Connection resolve(CommandObject<?> cmd) {
    return provider.getReplicaConnection(cmd.getArguments());
  }
}
