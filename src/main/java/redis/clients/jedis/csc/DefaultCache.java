package redis.clients.jedis.csc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DefaultCache extends AbstractCache {

    protected final Map<CacheKey, CacheEntry> cache;
    private final EvictionPolicy evictionPolicy;

    protected DefaultCache(int maximumSize) {
        this(maximumSize, new HashMap<CacheKey, CacheEntry>());
    }

    protected DefaultCache(int maximumSize, Map<CacheKey, CacheEntry> map) {
        this(maximumSize, map, DefaultCacheable.INSTANCE, new LRUEviction(maximumSize));
    }

    protected DefaultCache(int maximumSize, Cacheable cacheable) {
        this(maximumSize, new HashMap<CacheKey, CacheEntry>(), cacheable, new LRUEviction(maximumSize));
    }

    protected DefaultCache(int maximumSize, Cacheable cacheable, EvictionPolicy evictionPolicy) {
        this(maximumSize, new HashMap<CacheKey, CacheEntry>(), cacheable, evictionPolicy);
    }

    protected DefaultCache(int maximumSize, Map<CacheKey, CacheEntry> map, Cacheable cacheable, EvictionPolicy evictionPolicy) {
        super(maximumSize, cacheable);
        this.cache = map;
        this.evictionPolicy = evictionPolicy;
        this.evictionPolicy.setCache(this);
    }

    @Override
    public int getSize() {
        return cache.size();
    }

    @Override
    public Collection<CacheEntry> getCacheEntries() {
        return cache.values();
    }

    @Override
    public EvictionPolicy getEvictionPolicy() {
        return this.evictionPolicy;
    }

    @Override
    public CacheEntry getFromStore(CacheKey key) {
        return cache.get(key);
    }

    @Override
    public CacheEntry putIntoStore(CacheKey key, CacheEntry entry) {
        return cache.put(key, entry);
    }

    @Override
    public boolean removeFromStore(CacheKey key) {
        return cache.remove(key) != null;
    }

    @Override
    protected final void clearStore() {
        cache.clear();
    }

    @Override
    protected boolean containsKeyInStore(CacheKey cacheKey) {
        return cache.containsKey(cacheKey);
    }

}