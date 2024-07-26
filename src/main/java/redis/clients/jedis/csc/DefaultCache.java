package redis.clients.jedis.csc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class DefaultCache extends ClientSideCache {

    protected final Map<CacheKey, CacheEntry> cache;
    private final LRUEviction evictionPolicy;

    public DefaultCache(int maximumSize) {
        this(maximumSize, new HashMap<CacheKey, CacheEntry>());
    }

    public DefaultCache(int maximumSize, Map<CacheKey, CacheEntry> map) {
        super(maximumSize);
        this.cache = map;
        this.evictionPolicy = new LRUEviction(this, maximumSize);
    }

    public DefaultCache(int maximumSize, ClientSideCacheable cacheable) {
        this(maximumSize, null, cacheable);
    }

    public DefaultCache(int maximumSize, Map<CacheKey, CacheEntry> map, ClientSideCacheable cacheable) {
        super(maximumSize, cacheable);
        this.cache = map;
        this.evictionPolicy = new LRUEviction(this, maximumSize);
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
    public Boolean removeFromStore(CacheKey key) {
        return cache.remove(key) != null;
    }

    @Override
    protected final void clearStore() {
        cache.clear();
    }

    @Override
    protected Boolean containsKeyInStore(CacheKey cacheKey) {
        return cache.containsKey(cacheKey);
    }

}