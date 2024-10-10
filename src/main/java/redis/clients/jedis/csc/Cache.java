package redis.clients.jedis.csc;

import java.util.Collection;
import java.util.List;

/**
 * The cache that is used by a connection
 */
public interface Cache {

    /**
     * @return The size of the cache
     */
    int getMaxSize();

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
    CacheEntry get(CacheKey cacheKey);

    /**
     * Puts a value into the cache
     *
     * @param cacheKey The key by which the value can be accessed within the cache
     * @param value The value to be put into the cache
     * @return The cache entry
     */
    CacheEntry set(CacheKey cacheKey, CacheEntry value);

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
    List<Boolean> delete(List<CacheKey> cacheKeys);

    /**
     * Delete an entry by the Redis key from the cache
     *
     * @param key The Redis key as binary
     * @return True if the entry could be deleted. False if the entry was not there.
     */
    List<CacheKey> deleteByRedisKey(Object key);

    /**
     * Delete entries by the Redis key from the cache
     *
     * @param keys The Redis keys as binaries
     * @return True for every entry that could be deleted. False if the entry was not there.
     */
    List<CacheKey> deleteByRedisKeys(List keys);

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

    /**
     * @return The eviction policy that is used by the cache
     */
    EvictionPolicy getEvictionPolicy();

    /**
     * @return The statistics of the cache
     */
    CacheStats getStats();

    /**
     * @return The statistics of the cache
     */
    CacheStats getAndResetStats();

    /**
     * @return The compatibility of cache against different Redis versions
     */
    boolean compatibilityMode();
}
