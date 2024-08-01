package redis.clients.jedis.csc;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import redis.clients.jedis.annots.Experimental;

/**
 * The class to manage the client-side caching. User can provide any of implementation of this class to the client
 * object; e.g. {@link redis.clients.jedis.csc.util.CaffeineClientSideCache CaffeineClientSideCache} or
 * {@link redis.clients.jedis.csc.util.GuavaClientSideCache GuavaClientSideCache} or a custom implementation of their own.
 */
@Experimental
public class DefaultClientSideCache extends AbstractClientSideCache {

  private final Map<CacheKey, CacheEntry> cache;

  public DefaultClientSideCache() {
    this(DEFAULT_MAXIMUM_SIZE);
  }

  public DefaultClientSideCache(int maxSize) {
    this(new ConcurrentHashMap<CacheKey, CacheEntry>(maxSize));
  }

  public DefaultClientSideCache(Map<CacheKey, CacheEntry> cache) {
    this.cache = cache;
  }

  @Override
  protected <T> CacheEntry<T> getCacheEntry(CacheKey<T> cacheKey) {
    return this.cache.get(cacheKey);
  }

  @Override
  protected <T> void putCacheEntry(CacheKey<T> cacheKey, CacheEntry<T> cacheEntry) {
    this.cache.put(cacheKey, cacheEntry);
  }

  @Override
  protected boolean removeCacheEntry(CacheKey cacheKey) {
    return this.cache.remove(cacheKey) != null;
  }

  @Override
  protected void clearCacheEntries() {
    this.cache.clear();
  }

  @Override
  public int getSize() {
    return this.cache.size();
  }

  @Override
  public Collection<CacheEntry> getCacheEntries() {
    return this.cache.values();
  }

  @Override
  public boolean hasCacheKey(CacheKey cacheKey) {
    return cache.containsKey(cacheKey);
  }
}
