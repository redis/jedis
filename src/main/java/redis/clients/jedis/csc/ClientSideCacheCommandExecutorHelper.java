package redis.clients.jedis.csc;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.annots.Internal;

@Internal
public final class ClientSideCacheCommandExecutorHelper {

  private final ClientSideCache cache;

  public ClientSideCacheCommandExecutorHelper(ClientSideCache cache) {
    this.cache = cache;
  }

  public final <T> T get(final Connection connection, CommandObject<T> command, Object... keys) {

    if (!cache.cacheable.isCacheable(command.getArguments().getCommand(), keys)) {
      return connection.executeCommand(command);
    }

    final CacheKey cacheKey = new CacheKey(command);
    CacheEntry<T> cacheEntry = cache.get(cacheKey);
    if (cacheEntry != null) {
      // CACHE HIT!!!
      // TODO: connection ...
      cacheEntry.getConnection().ping();

      // cache entry can be invalidated; so recheck
      cacheEntry = cache.get(cacheKey);
      if (cacheEntry != null) {
        return cacheEntry.getValue();
      }
    }

    // CACHE MISS!!
    T value = connection.executeCommand(command);
    if (value != null) {
      cacheEntry = new CacheEntry(cacheKey, value, connection);
      cache.putInner(cacheEntry, keys);
    }

    return value;
  }
}
