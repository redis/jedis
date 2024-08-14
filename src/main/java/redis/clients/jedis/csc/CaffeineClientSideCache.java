package redis.clients.jedis.csc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import redis.clients.jedis.annots.Experimental;

import java.util.Collection;

@Experimental
public class CaffeineClientSideCache extends AbstractCache {

  private final Cache<CacheKey, CacheEntry> cache;
  private final EvictionPolicy evictionPolicy;

  public CaffeineClientSideCache(int maximumSize) {
    this(maximumSize, new LRUEviction(maximumSize));
  }

  public CaffeineClientSideCache(int maximumSize, EvictionPolicy evictionPolicy) {
    super(maximumSize);
    this.cache = Caffeine.newBuilder().build();
    this.evictionPolicy = evictionPolicy;
    this.evictionPolicy.setCache(this);
  }

  public CaffeineClientSideCache(int maximumSize, ClientSideCacheable cacheable, EvictionPolicy evictionPolicy) {
    super(maximumSize, cacheable);
    this.cache = Caffeine.newBuilder().build();
    this.evictionPolicy = evictionPolicy;
    this.evictionPolicy.setCache(this);
  }

  @Override
  protected final void clearStore() {
    cache.invalidateAll();
  }

  @Override
  public CacheEntry putIntoStore(CacheKey key, CacheEntry entry) {
    cache.put(key, entry);
    return entry;
  }

  @Override
  public CacheEntry getFromStore(CacheKey key) {
    return cache.getIfPresent(key);
  }

  // TODO: we should discuss if/how we utilize Caffeine and get back to here !

  @Override
  public int getSize() {
    return (int) cache.estimatedSize();
  }

  @Override
  public Collection<CacheEntry> getCacheEntries() {
    throw new UnsupportedOperationException("Unimplemented method 'getCacheEntries'");
  }

  @Override
  public EvictionPolicy getEvictionPolicy() {
    return this.evictionPolicy;
  }

  @Override
  protected Boolean removeFromStore(CacheKey cacheKey) {
    cache.invalidate(cacheKey);
    return true;
  }

  @Override
  protected Boolean containsKeyInStore(CacheKey cacheKey) {
    return cache.getIfPresent(cacheKey) != null;
  }

}
