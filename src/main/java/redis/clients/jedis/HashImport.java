package redis.clients.jedis;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.providers.ConnectionProvider;

/**
 * Factory for the connection-holding {@code HIMPORT} handles (each implements both
 * {@link redis.clients.jedis.commands.HashImportCommands} and
 * {@link redis.clients.jedis.commands.HashImportBinaryCommands}). Selects the standalone or cluster
 * implementation by the provider type, mirroring {@link UnifiedJedis#pipelined()}.
 * @since 8.0
 */
@Experimental
final class HashImport {

  private HashImport() {
  }

  static AbstractHashImportHandler create(ConnectionProvider provider,
      CommandObjects commandObjects) {
    if (provider instanceof ClusterConnectionProvider) {
      return new HashImportClusterHandler((ClusterConnectionProvider) provider, commandObjects);
    }
    return new HashImportHandler(provider, commandObjects);
  }
}
