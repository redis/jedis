package redis.clients.jedis.csc;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DefaultCache extends AbstractCache {

    protected final Map<CacheKey, CacheEntry> cache;
    private final EvictionPolicy evictionPolicy;
    private final ScheduledExecutorService scheduler;

    protected DefaultCache(int maximumSize, long ttl) {
        this(maximumSize, ttl, new HashMap<CacheKey, CacheEntry>());
    }

    protected DefaultCache(int maximumSize, long ttl, Map<CacheKey, CacheEntry> map) {
        this(maximumSize, ttl, map, DefaultCacheable.INSTANCE, new LRUEviction(maximumSize));
    }

    protected DefaultCache(int maximumSize, long ttl, Cacheable cacheable) {
        this(maximumSize, ttl, new HashMap<CacheKey, CacheEntry>(), cacheable, new LRUEviction(maximumSize));
    }

    protected DefaultCache(int maximumSize, long ttl, Cacheable cacheable, EvictionPolicy evictionPolicy) {
        this(maximumSize, ttl, new HashMap<CacheKey, CacheEntry>(), cacheable, evictionPolicy);
    }

    protected DefaultCache(int maximumSize, long ttl, Map<CacheKey, CacheEntry> map, Cacheable cacheable, EvictionPolicy evictionPolicy) {
        super(maximumSize, ttl, cacheable);
        this.cache = map;
        this.evictionPolicy = evictionPolicy;
        this.evictionPolicy.setCache(this);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        // Periodically clear expired cache every 2 seconds
        this.scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                cleanupExpiredEntries();
            }
        }, ttl, 2000, TimeUnit.MILLISECONDS);
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
    public void close() {
        this.scheduler.shutdown();
    }

    @Override
    public CacheEntry getFromStore(CacheKey key) {
        CacheEntry entry = cache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry;
        }
        return null;
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

    private void cleanupExpiredEntries() {
        this.lock.lock();
        try {
            Iterator<Map.Entry<CacheKey, CacheEntry>> iterator = cache.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<CacheKey, CacheEntry> entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    iterator.remove();
                    evictionPolicy.reset(entry.getKey());
                }
            }
        } finally {
            lock.unlock();
        }
    }

}