package redis.clients.jedis.csc.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Collection;

import redis.clients.jedis.csc.AbstractClientSideCache;
import redis.clients.jedis.csc.CacheEntry;
import redis.clients.jedis.csc.CacheKey;

public class GuavaClientSideCache extends AbstractClientSideCache {

  private final Cache<CacheKey, CacheEntry> cache;

  public GuavaClientSideCache() {
    this(DEFAULT_MAXIMUM_SIZE);
  }

  public GuavaClientSideCache(int maxSize) {
    this(CacheBuilder.newBuilder().maximumSize(maxSize).build());
  }

  public GuavaClientSideCache(Cache<CacheKey, CacheEntry> guavaCache) {
    super();
    this.cache = guavaCache;
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
    return (int) this.cache.size();
  }

  @Override
  public Collection<CacheEntry> getCacheEntries() {
    return this.cache.asMap().values();
  }
}
