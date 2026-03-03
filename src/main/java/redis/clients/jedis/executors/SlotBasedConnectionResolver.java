package redis.clients.jedis.executors;

import redis.clients.jedis.CommandFlagsRegistry;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.providers.ClusterConnectionProvider;

/**
 * Connection resolver for keyed commands that acquires connections based on hash slot.
 * <p>
 * This resolver routes commands to the appropriate cluster node based on the key's hash slot. All
 * commands (both read and write) are routed to the primary node for the slot.
 */
final class SlotBasedConnectionResolver implements ConnectionResolver {

  private final ClusterConnectionProvider provider;
  private final CommandFlagsRegistry flags;

  SlotBasedConnectionResolver(ClusterConnectionProvider provider, CommandFlagsRegistry flags) {
    this.provider = provider;
    this.flags = flags;
  }

  @Override
  public Connection resolve(CommandObject<?> cmd) {
    // Always route to primary node for slot-based routing
    return provider.getConnection(cmd.getArguments());
  }
}
