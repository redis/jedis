package redis.clients.jedis.csc;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    protected Cache cache;
    protected LinkedHashMap<CacheKey, Long> accessTimes;

    protected ArrayDeque<CacheKey> pendingEvictions = new ArrayDeque<CacheKey>();

    protected ConcurrentLinkedQueue msg = new ConcurrentLinkedQueue();

    private int initialCapacity;

    /**
     *  Constructor that gets the cache passed
     *
     * @param initialCapacity
     */
    public LRUEviction(int initialCapacity) {
        this.initialCapacity = initialCapacity;
    }

    @Override
    public void setCache(Cache cache) {
        this.cache = cache;
        this.accessTimes = new LinkedHashMap<CacheKey, Long>(initialCapacity, 1f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, Long> eldest) {
                boolean evictionRequired = cache.getSize() > cache.getMaxSize()
                        || accessTimes.size() > cache.getMaxSize();
                // here the cache check is only for performance gain; we are trying to avoid the sequence add + poll + hasCacheKey
                // and prefer to check it in cache once in early stage.
                // if there is nothing to remove in actual cache as of now, stop worrying about it.
                if (evictionRequired && cache.hasCacheKey(eldest.getKey())) {
                    pendingEvictions.addLast(eldest.getKey());

                }
                return evictionRequired;
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
    public synchronized CacheKey evictNext() {
        CacheKey cacheKey = pendingEvictions.pollFirst();
        while (cacheKey != null && !cache.hasCacheKey(cacheKey)) {
            cacheKey = pendingEvictions.pollFirst();
        }
        return cacheKey;
    }

    @Override
    public synchronized List<CacheKey> evictMany(int n) {
        List<CacheKey> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            result.add(this.evictNext());
        }
        return result;
    }

    @Override
    public synchronized void touch(CacheKey cacheKey) {
        this.accessTimes.put(cacheKey, new Date().getTime());
    }

    @Override
    public synchronized boolean reset(CacheKey cacheKey) {
        return this.accessTimes.remove(cacheKey) != null;
    }

    @Override
    public synchronized int resetAll() {
        int result = this.accessTimes.size();
        accessTimes.clear();
        return result;
    }

}
