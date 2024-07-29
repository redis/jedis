package redis.clients.jedis.csc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;

public class CaffeineClientSideCache extends ClientSideCache {

  private final Cache<CacheKey, CacheEntry> cache;

  public CaffeineClientSideCache(Cache<CacheKey, CacheEntry> caffeineCache) {
    this.cache = caffeineCache;
  }

  @Override
  protected final void clear() {
    cache.invalidateAll();
  }

  @Override
  protected void remove(Iterable<CacheKey<?>> keys) {
    cache.invalidateAll(keys);
  }

  @Override
  protected void put(CacheKey key, CacheEntry entry) {
    cache.put(key, entry);
  }

  @Override
  protected CacheEntry get(CacheKey key) {
    return cache.getIfPresent(key);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private long maximumSize = DEFAULT_MAXIMUM_SIZE;
    private long expireTime = DEFAULT_EXPIRE_SECONDS;
    private final TimeUnit expireTimeUnit = TimeUnit.SECONDS;

    private Builder() { }

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
}
