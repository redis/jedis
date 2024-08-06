package redis.clients.jedis.csc;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple L(east) R(ecently) U(sed) eviction policy
 * ATTENTION: this class is not thread safe
 */
public class LRUEviction implements EvictionPolicy {

    // For future reference, in case there is a need to make it thread safe,
    // the LinkedHashMap can be wrapped in a Collections.synchronizedMap

    /**
     * The cache that is associated to that policy instance
     */
    private Cache cache;
    private LinkedHashMap<CacheKey, Long> accessTimes;
    private CacheKey lastEvicted = null;
    private AtomicLong evictedCount = new AtomicLong(0);
    private int initialCapacity;

    /**
     *  Constructor that gets the cache passed
     *
     * @param cache
     * @param initialCapacity
     */
    public LRUEviction(int initialCapacity) {
        this.initialCapacity = initialCapacity;
    }

    @Override
    public void setCache(Cache cache) {
        this.cache = cache;
        this.accessTimes = new LinkedHashMap<CacheKey, Long>(initialCapacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, Long> eldest) {
                return evictFromCache(eldest);
            }
        };
    }

    @Override
    public Cache getCache() {
        return this.cache;
    }

    @Override
    public EvictionType getType() {
        return EvictionType.AGE;
    }

    @Override
    public String getName() {
        return "Simple L(east) R(ecently) U(sed)";
    }

    @Override
    public CacheKey evictNext() {
        // its already done, thanks to the LinkedHashMap
        return evictedCount.decrementAndGet() == 0 ? null : lastEvicted;
    }

    @Override
    public List<CacheKey> evictMany(int n) {
        List<CacheKey> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(this.evictNext());
        }

        return result;
    }

    @Override
    public void touch(CacheKey cacheKey) {
        this.accessTimes.put(cacheKey, new Date().getTime());
    }

    @Override
    public boolean reset(CacheKey cacheKey) {
        return this.accessTimes.remove(cacheKey) != null;
    }

    @Override
    public int resetAll() {
        int result = this.accessTimes.size();
        accessTimes.clear();
        lastEvicted = null;
        evictedCount.set(0);
        return result;
    }

    private boolean evictFromCache(Map.Entry<CacheKey, Long> eldest) {
        boolean result = accessTimes.size() > cache.getMaxSize();
        if (result) {
            if (cache.delete(eldest.getKey())) {
                lastEvicted = eldest.getKey();
                evictedCount.incrementAndGet();
            }
        }
        return result;
    }
}
