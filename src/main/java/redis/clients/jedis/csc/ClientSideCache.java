package redis.clients.jedis.csc;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.annots.Internal;

/**
 * The cache that is used by a connection
 */
@Experimental
public interface ClientSideCache {

  @Internal
  default void invalidate(List list) {
    if (list == null) {
      flush();
    } else {
      list.forEach(this::deleteByRedisKey);
    }
  }

  @Internal
  <T> T get(final CacheConnection connection, CommandObject<T> command);
//
//    /**
//     * @return The size of the cache
//     */
//    int getMaxSize();

    /**
     * @return The current size of the cache
     */
    int getSize();

    /**
     * @return All the entries within the cache
     */
    Collection<CacheEntry> getCacheEntries();

    /**
     * Fetches a value from the cache
     *
     * @param cacheKey The key within the cache
     * @return The entry within the cache
     */
    <T> T get(CacheKey<T> cacheKey);

    /**
     * Puts a value into the cache
     *
     * @param cacheKey The key by which the value can be accessed within the cache
     * @param value The value to be put into the cache
     * @param cacheConnection
     * @return The cache entry
     */
    <T> CacheEntry<T> set(CacheKey<T> cacheKey, T value, CacheConnection cacheConnection);

    /**
     * Delete an entry by cache key
     * @param cacheKey The cache key of the entry in the cache
     * @return True if the entry could be deleted, false if the entry wasn't found.
     */
    boolean delete(CacheKey cacheKey);

    /**
     * Delete entries by cache key from the cache
     *
     * @param cacheKeys The cache keys of the entries that should be deleted
     * @return True for every entry that could be deleted. False if the entry was not there.
     */
    default List<Boolean> delete(List<CacheKey> cacheKeys) {
      return cacheKeys.stream().map(this::delete).collect(Collectors.toList());
    }

    /**
     * Delete an entry by the Redis key from the cache
     *
     * @param key The Redis key as binary
     * @return True if the entry could be deleted. False if the entry was not there.
     */
    Set<CacheKey> deleteByRedisKey(Object key);

    /**
     * Delete entries by the Redis key from the cache
     *
     * @param keys The Redis keys as binaries
     * @return True for every entry that could be deleted. False if the entry was not there.
     */
    default Set<CacheKey> deleteByRedisKey(List<Object> keys) {
      return keys.stream().map(this::deleteByRedisKey).flatMap(Set::stream).collect(Collectors.toSet());
    }

    /**
     * Flushes the entire cache
     *
     * @return Return the number of entries that were flushed
     */
    int flush();

    /**
     * @param cacheKey The key of the cache entry
     * @return True if the entry is cachable, false otherwise
     */
    boolean isCacheable(CacheKey cacheKey);

    /**
     *
     * @param cacheKey The key of the cache entry
     * @return True if the cache already contains the key
     */
    boolean hasCacheKey(CacheKey cacheKey);
//
//    /**
//     * @return The eviction policy that is used by the cache
//     */
//    IEvictionPolicy getEvictionPolicy();
}
