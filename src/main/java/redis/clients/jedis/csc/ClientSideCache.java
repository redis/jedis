package redis.clients.jedis.csc;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.SafeEncoder;

/**
 * The class to manage the client-side caching. User can provide any of implementation of this class to the client
 * object; e.g. {@link redis.clients.jedis.csc.CaffeineClientSideCache CaffeineClientSideCache} or
 * {@link redis.clients.jedis.csc.GuavaClientSideCache GuavaClientSideCache} or a custom implementation of their own.
 */
@Experimental
public abstract class ClientSideCache {

  protected static final int DEFAULT_MAXIMUM_SIZE = 10_000;
  protected static final int DEFAULT_EXPIRE_SECONDS = 100;

  private final Map<ByteBuffer, Set<CacheKey<?>>> redisKeysToCacheKeys = new ConcurrentHashMap<>();
  private ClientSideCacheable cacheable = DefaultClientSideCacheable.INSTANCE; // TODO: volatile

  protected ClientSideCache() {
  }

  public void setCacheable(ClientSideCacheable cacheable) {
    this.cacheable = Objects.requireNonNull(cacheable, "'cacheable' must not be null");
  }

  protected abstract void clear();

  protected abstract void remove(Iterable<CacheKey<?>> keys);

  protected abstract void put(CacheKey key, CacheEntry entry);

  protected abstract CacheEntry get(CacheKey key);

  public final void flush() {
    invalidateAllRedisKeysAndCacheEntries();
  }

  public final void invalidateKey(Object key) {
    invalidateRedisKeyAndRespectiveCacheEntries(key);
  }

  public final void invalidate(List list) {
    if (list == null) {
      invalidateAllRedisKeysAndCacheEntries();
      return;
    }

    list.forEach(this::invalidateRedisKeyAndRespectiveCacheEntries);
  }

  private void invalidateAllRedisKeysAndCacheEntries() {
    clear();
    redisKeysToCacheKeys.clear();
  }

  private void invalidateRedisKeyAndRespectiveCacheEntries(Object key) {
//    if (!(key instanceof byte[])) {
//      // This should be called internally. That's why throwing AssertionError instead of IllegalArgumentException.
//      throw new AssertionError("" + key.getClass().getSimpleName() + " is not supported. Value: " + String.valueOf(key));
//    }
//
//    final ByteBuffer mapKey = makeKeyForKeyToCommandHashes((byte[]) key);
    final ByteBuffer mapKey = makeKeyForRedisKeysToCacheKeys(key);

    Set<CacheKey<?>> commands = redisKeysToCacheKeys.get(mapKey);
    if (commands != null) {
      remove(commands);
      redisKeysToCacheKeys.remove(mapKey);
    }
  }

  public final <T> T get(Function<CommandObject<T>, T> loader, CommandObject<T> command, Object... keys) {

    if (!cacheable.isCacheable(command.getArguments().getCommand(), keys)) {
      return loader.apply(command);
    }

    final CacheKey cacheKey = new CacheKey(command);
    CacheEntry cacheEntry = get(cacheKey);
    if (cacheEntry != null) {
      // CACHE HIT!!!
      // TODO: connection ...
      return (T) cacheEntry.getValue();
    }

    // CACHE MISS!!
    T value = loader.apply(command);
    if (value != null) {
      cacheEntry = new CacheEntry(cacheKey, value, /*connection*/ null); // TODO: connection
      put(cacheKey, cacheEntry);
      for (Object key : keys) {
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

    return value;
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
