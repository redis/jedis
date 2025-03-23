package redis.clients.jedis;

import redis.clients.jedis.csc.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

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

        // decide the time interval of the cleaning up
        long timeInterval = Math.max(this.defaultMilis / 10, 1000);

        if (defaultMilis > 0) {
            cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, timeInterval, timeInterval, TimeUnit.MILLISECONDS);
        }
    }

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
}
