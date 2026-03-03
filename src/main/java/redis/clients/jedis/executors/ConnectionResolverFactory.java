package redis.clients.jedis.executors;

import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.providers.ClusterConnectionProvider;

/**
 * Factory for creating {@link ConnectionResolver} instances.
 * <p>
 * This factory provides access to the package-private connection resolver implementations.
 */
final class ConnectionResolverFactory {

  private ConnectionResolverFactory() {
    // Utility class
  }

  /**
   * Creates a slot-based connection resolver for keyed commands.
   * @param provider the cluster connection provider
   * @param flags the command flags registry
   * @return a new SlotBasedConnectionResolver
   */
  public static ConnectionResolver createSlotBasedResolver(ClusterConnectionProvider provider,
      CommandFlagsRegistry flags) {
    return new SlotBasedConnectionResolver(provider, flags);
  }

  /**
   * Creates a round-robin connection resolver for keyless commands.
   * @param provider the cluster connection provider
   * @param flags the command flags registry
   * @return a new RoundRobinConnectionResolver
   */
  public static ConnectionResolver createRoundRobinResolver(ClusterConnectionProvider provider,
      CommandFlagsRegistry flags) {
    return new RoundRobinConnectionResolver(provider, flags);
  }

  /**
   * Creates a replica-only connection resolver.
   * @param provider the cluster connection provider
   * @return a new ReplicaOnlyConnectionResolver
   */
  public static ConnectionResolver createReplicaOnlyResolver(ClusterConnectionProvider provider) {
    return new ReplicaOnlyConnectionResolver(provider);
  }
}
