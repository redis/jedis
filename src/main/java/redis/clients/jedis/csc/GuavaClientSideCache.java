package redis.clients.jedis.csc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import redis.clients.jedis.annots.Experimental;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Experimental
public class GuavaClientSideCache extends AbstractCache {

  private final Cache<CacheKey, CacheEntry> cache;
  private final EvictionPolicy evictionPolicy;

  public GuavaClientSideCache(int maximumSize) {
    this(maximumSize, new LRUEviction(maximumSize));
  }

  public GuavaClientSideCache(int maximumSize, EvictionPolicy evictionPolicy) {
    super(maximumSize);
    this.cache = CacheBuilder.newBuilder().build();
    this.evictionPolicy = evictionPolicy;
    this.evictionPolicy.setCache(this);
  }

  public GuavaClientSideCache(int maximumSize, ClientSideCacheable clientSideCacheable, EvictionPolicy evictionPolicy) {
    super(maximumSize, clientSideCacheable);
    this.cache = CacheBuilder.newBuilder().build();
    this.evictionPolicy = evictionPolicy;
    this.evictionPolicy.setCache(this);
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
