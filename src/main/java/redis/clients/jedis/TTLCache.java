package redis.clients.jedis;

import redis.clients.jedis.csc.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * @author PengJingzhao
 * @date 2025-03-23
 * add a ttl support for the default cache
 */
public class TTLCache extends DefaultCache {

    private final Map<CacheKey, Long> expirationTimes;
    private final Long defaultMilis;
    private final ScheduledExecutorService cleanupExecutor;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    /**
     * create a ttlCache object with thd ttl support
     *
     * @param maximumSize the maximum size of the cache
     * @param ttl         time to live
     * @param timeUnit    the unit of the time
     */
    public TTLCache(int maximumSize, Long ttl, TimeUnit timeUnit) {
        super(maximumSize);
        this.defaultMilis = timeUnit.toMillis(ttl);

        this.expirationTimes = new ConcurrentHashMap<>();
        this.cleanupExecutor = new ScheduledThreadPoolExecutor(1);
        initCleanupTask();
    }


    protected TTLCache(int maximumSize, Map<CacheKey, Long> expirationTimes, Long defaultMilis, ScheduledExecutorService cleanupExecutor) {
        super(maximumSize);
        this.expirationTimes = expirationTimes;
        this.defaultMilis = defaultMilis;
        this.cleanupExecutor = cleanupExecutor;

        initCleanupTask();
    }

    private void initCleanupTask() {

        if (defaultMilis > 0) {
            // decide the time interval of the cleaning up
            long timeInterval = Math.max(this.defaultMilis / 10, 1000);
            cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, timeInterval, timeInterval, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * remove the key from the expirationTimes map before remove from cache
     *
     * @param key cache key
     * @return
     */
    public boolean removeFromStore(CacheKey key) {
        expirationTimes.remove(key);
        return super.removeFromStore(key);
    }

    /**
     * use the scheduled thread to clean up all the expired entries at the fixed rate
     */
    public void cleanupExpiredEntries() {
        // if the thread is shut down
        if (isShutdown.get()) {
            return;
        }

        // get the time millis of now
        long now = System.currentTimeMillis();
        // remove all the expired key
        expirationTimes.entrySet().removeIf(new Predicate<Map.Entry<CacheKey, Long>>() {
            @Override
            public boolean test(Map.Entry<CacheKey, Long> entry) {
                Long expiredTime = entry.getValue();
                CacheKey cacheKey = entry.getKey();

                // if the expired time of the key less than now show the key is expired
                if (expiredTime < now) {
                    removeFromStore(cacheKey);
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * before put the cache into store,we should put the key into the expirationTimes Map
     *
     * @param key   cache key
     * @param entry cache value
     * @return cache entry
     */
    @Override
    public CacheEntry putIntoStore(CacheKey key, CacheEntry entry) {
        if (defaultMilis > 0) {
            // set up the ttl from now
            expirationTimes.put(key, System.currentTimeMillis() + defaultMilis);
        }
        return super.putIntoStore(key, entry);
    }

    /**
     * clear the expirationTimes map before clear the cache
     */
    protected void clearStoreWithTTL() {
        expirationTimes.clear();
        super.clearStore();
    }

    /**
     * decide the key is expired or not
     *
     * @param key cache key
     * @return if the key is expired return true
     */
    private boolean isExpired(CacheKey key) {
        Long expirationTime = expirationTimes.get(key);
        if (expirationTime == null) {
            // the key has not yet been set up a expirationTime
            return false;
        }
        return System.currentTimeMillis() > expirationTime;
    }

    /**
     * decide the key is contained in the store or not
     *
     * @param key cache key
     * @return if the key belong to the cache return true
     */
    protected boolean containsKeyInStore(CacheKey key) {
        if (isExpired(key)) {
            // if the key is expired , we should remove it from the store
            removeFromStore(key);
            return false;
        }
        return super.containsKeyInStore(key);
    }

    /**
     * set up a ttl for any other key
     *
     * @param key      the cache key
     * @param ttl      time to live
     * @param timeUnit time unit
     */
    public void setTTL(CacheKey key, Long ttl, TimeUnit timeUnit) {
        // if the ttl less than zero , remove the key from the expirationTimes
        if (ttl <= 0) {
            expirationTimes.remove(key);
        } else {
            expirationTimes.put(key, System.currentTimeMillis() + ttl);
        }
    }

    /**
     * get the remaining ttl of the key
     *
     * @param key cache key
     * @return the remaining ttl of the key
     */
    public long getTTL(CacheKey key) {
        Long expirationTime = expirationTimes.get(key);
        if (expirationTime == null) {
            return -1;
        }

        long remaining = expirationTime - System.currentTimeMillis();
        return remaining > 0 ? remaining : 0;
    }

    /**
     * shut down the cleanup executor
     */
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            cleanupExecutor.shutdown();
        }
    }

}
