package redis.clients.jedis.csc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import redis.clients.jedis.annots.Experimental;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Experimental
public class GuavaClientSideCache extends ClientSideCache {

  private final Cache<CacheKey, CacheEntry> cache;
  protected static final int DEFAULT_MAXIMUM_SIZE = 10_000;
  protected static final int DEFAULT_EXPIRE_SECONDS = 100;
  private final LRUEviction evictionPolicy;

  public GuavaClientSideCache(Cache<CacheKey, CacheEntry> guavaCache) {
    super(DEFAULT_MAXIMUM_SIZE);
    this.cache = guavaCache;
    this.evictionPolicy = new LRUEviction(this, DEFAULT_MAXIMUM_SIZE);
  }

  @Override
  public final void clearStore() {
    cache.invalidateAll();
  }

  public List<CacheKey> remove(Iterable<CacheKey<?>> keys) {
    cache.invalidateAll(keys);
    return StreamSupport.stream(keys.spliterator(), false)
        .collect(Collectors.toList());
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

    public GuavaClientSideCache build() {
      CacheBuilder cb = CacheBuilder.newBuilder();

      cb.maximumSize(maximumSize);

      cb.expireAfterWrite(expireTime, expireTimeUnit);

      return new GuavaClientSideCache(cb.build());
    }
  }

  // TODO: we should discuss if/how we utilize Guava and get back to here !

  @Override
  public int getSize() {
    return (int) cache.size();
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
