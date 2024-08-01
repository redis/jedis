package redis.clients.jedis.csc;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.SafeEncoder;

/**
 * The class to manage the client-side caching. User can provide any of implementation of this class to the client
 * object; e.g. {@link redis.clients.jedis.csc.util.CaffeineClientSideCache CaffeineClientSideCache} or
 * {@link redis.clients.jedis.csc.util.GuavaClientSideCache GuavaClientSideCache} or a custom implementation of their own.
 */
@Experimental
public abstract class AbstractClientSideCache implements ClientSideCache {

  protected static final int DEFAULT_MAXIMUM_SIZE = 10_000;

  private final Map<ByteBuffer, Set<CacheKey<?>>> redisKeysToCacheKeys = new ConcurrentHashMap<>();
  private ClientSideCacheable cacheable = DefaultClientSideCacheable.INSTANCE; // TODO: volatile ??

  protected AbstractClientSideCache() {
  }

  public final void setCacheable(ClientSideCacheable cacheable) {
    this.cacheable = Objects.requireNonNull(cacheable, "'cacheable' must not be null");
  }

  protected abstract <T> CacheEntry<T> getCacheEntry(CacheKey<T> cacheKey);

  protected abstract <T> void putCacheEntry(CacheKey<T> cacheKey, CacheEntry<T> cacheEntry);

  protected abstract boolean removeCacheEntry(CacheKey cacheKey);

  protected abstract void clearCacheEntries();

  @Override
  public <T> T get(CacheKey<T> cacheKey) {
    CacheEntry<T> cacheEntry = getCacheEntry(cacheKey);
    if (cacheEntry != null) {
      return cacheEntry.getValue();
    }
    return null;
  }

  @Override
  public <T> CacheEntry<T> set(CacheKey<T> cacheKey, T value, CacheConnection cacheConnection) {
    CacheEntry<T> cacheEntry = new CacheEntry(cacheKey, value, cacheConnection);
    putInner(cacheEntry);
    return cacheEntry;
  }

  @Override
  public boolean delete(CacheKey cacheKey) {
    boolean deleted = removeCacheEntry(cacheKey);

    if (deleted) {
      List commandKeys = cacheKey.getCommandKeys();
      deleteByRedisKey(commandKeys);
    }

    return deleted;
  }

  @Override
  public Set<CacheKey> deleteByRedisKey(Object key) {
    final ByteBuffer mapKey = makeKeyForRedisKeysToCacheKeys(key);

    Set<CacheKey<?>> commands = redisKeysToCacheKeys.get(mapKey);
    if (commands != null) {
      commands.stream().filter(this::removeCacheEntry).collect(Collectors.toSet());
      redisKeysToCacheKeys.remove(mapKey);
    }
    return Collections.emptySet();
  }

  @Override
  public int flush() {
    int size = getSize();
    clearCacheEntries();
    redisKeysToCacheKeys.clear();
    return size;
  }

  @Override
  public boolean isCacheable(CacheKey cacheKey) {
    return cacheable.isCacheable(cacheKey.getProtocolCommand(), cacheKey.getCommandKeys());
  }

  @Override
  public boolean hasCacheKey(CacheKey cacheKey) {
    return getCacheEntry(cacheKey) != null;
  }

  @Override
  public final <T> T get(final CacheConnection connection, CommandObject<T> command) {

    if (!cacheable.isCacheable(command.getArguments().getCommand(), command.getArguments().getKeys())) {
      return connection.executePlainCommand(command);
    }

    final CacheKey cacheKey = new CacheKey(command);
    CacheEntry<T> cacheEntry = getCacheEntry(cacheKey);
    if (cacheEntry != null) {
      // CACHE HIT!!!
      // TODO: connection ...
      cacheEntry.getConnection().ping();

      // cache entry can be invalidated; so recheck
      cacheEntry = getCacheEntry(cacheKey);
      if (cacheEntry != null) {
        return cacheEntry.getValue();
      }
    }

    // CACHE MISS!!
    T value = connection.executePlainCommand(command);
    if (value != null) {
      cacheEntry = new CacheEntry(cacheKey, value, connection);
      putInner(cacheEntry);
    }

    return value;
  }

  private void putInner(final CacheEntry cacheEntry) {
    final CacheKey cacheKey = cacheEntry.getCacheKey();
    putCacheEntry(cacheKey, cacheEntry);
    for (Object key : cacheEntry.getCacheKey().getCommandKeys()) {
      ByteBuffer mapKey = makeKeyForRedisKeysToCacheKeys(key);
      if (redisKeysToCacheKeys.containsKey(mapKey)) {
        redisKeysToCacheKeys.get(mapKey).add(cacheKey);
      } else {
        Set<CacheKey<?>> set = ConcurrentHashMap.newKeySet();
        set.add(cacheKey);
        redisKeysToCacheKeys.put(mapKey, set);
      }
    }
  }

  private ByteBuffer makeKeyForRedisKeysToCacheKeys(Object key) {
    if (key instanceof byte[]) {
      return makeKeyForRedisKeysToCacheKeys((byte[]) key);
    } else if (key instanceof String) {
      return makeKeyForRedisKeysToCacheKeys(SafeEncoder.encode((String) key));
    } else {
      throw new IllegalArgumentException(key.getClass().getSimpleName() + " is not supported."
          + " Value: \"" + String.valueOf(key) + "\".");
    }
  }

  private static ByteBuffer makeKeyForRedisKeysToCacheKeys(byte[] b) {
    return ByteBuffer.wrap(b);
  }
}
