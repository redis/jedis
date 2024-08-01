package redis.clients.jedis.csc.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Collection;

import redis.clients.jedis.csc.AbstractClientSideCache;
import redis.clients.jedis.csc.CacheEntry;
import redis.clients.jedis.csc.CacheKey;

public class CaffeineClientSideCache extends AbstractClientSideCache {

  private final Cache<CacheKey, CacheEntry> cache;

  public CaffeineClientSideCache() {
    this(DEFAULT_MAXIMUM_SIZE);
  }

  public CaffeineClientSideCache(int maxSize) {
    this(Caffeine.newBuilder().maximumSize(maxSize).build());
  }

  public CaffeineClientSideCache(Cache<CacheKey, CacheEntry> caffeineCache) {
    this.cache = caffeineCache;
  }

  @Override
  protected <T> CacheEntry<T> getCacheEntry(CacheKey<T> cacheKey) {
    return cache.getIfPresent(cacheKey);
  }

  @Override
  protected <T> void putCacheEntry(CacheKey<T> cacheKey, CacheEntry<T> cacheEntry) {
    this.cache.put(cacheKey, cacheEntry);
  }

  /**
   * Always returns {@code true}.
   * @param cacheKey
   * @return true
   */
  @Override
  protected boolean removeCacheEntry(CacheKey cacheKey) {
    this.cache.invalidate(cacheKey);
    return true;
  }

  @Override
  protected void clearCacheEntries() {
    this.cache.invalidateAll();
  }

  @Override
  public int getSize() {
    return (int) this.cache.estimatedSize();
  }

  @Override
  public Collection<CacheEntry> getCacheEntries() {
    return this.cache.asMap().values();
  }
}
