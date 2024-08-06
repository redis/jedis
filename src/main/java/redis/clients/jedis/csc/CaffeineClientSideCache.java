package redis.clients.jedis.csc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import redis.clients.jedis.annots.Experimental;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Experimental
public class CaffeineClientSideCache extends AbstractCache {

  private final Cache<CacheKey, CacheEntry> cache;
  protected static final int DEFAULT_MAXIMUM_SIZE = 10_000;
  protected static final int DEFAULT_EXPIRE_SECONDS = 100;
  private final EvictionPolicy evictionPolicy;

  public CaffeineClientSideCache(Cache<CacheKey, CacheEntry> caffeineCache) {
    this(caffeineCache, new LRUEviction(DEFAULT_MAXIMUM_SIZE));
  }

  public CaffeineClientSideCache(Cache<CacheKey, CacheEntry> caffeineCache, EvictionPolicy evictionPolicy) {
    super(DEFAULT_MAXIMUM_SIZE);
    this.cache = caffeineCache;
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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long maximumSize = DEFAULT_MAXIMUM_SIZE;
    private long expireTime = DEFAULT_EXPIRE_SECONDS;
    private final TimeUnit expireTimeUnit = TimeUnit.SECONDS;

    private Builder() {
    }

    public Builder maximumSize(int size) {
      this.maximumSize = size;
      return this;
    }

    public Builder ttl(int seconds) {
      this.expireTime = seconds;
      return this;
    }

    public CaffeineClientSideCache build() {
      Caffeine cb = Caffeine.newBuilder();

      cb.maximumSize(maximumSize);

      cb.expireAfterWrite(expireTime, expireTimeUnit);

      return new CaffeineClientSideCache(cb.build());
    }
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
