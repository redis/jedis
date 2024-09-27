package redis.clients.jedis.csc;

import java.util.concurrent.atomic.AtomicLong;

public class CacheStats {

    private AtomicLong hits = new AtomicLong(0);
    private AtomicLong misses = new AtomicLong(0);
    private AtomicLong loads = new AtomicLong(0);
    private AtomicLong evicts = new AtomicLong(0);
    private AtomicLong nonCacheable = new AtomicLong(0);
    private AtomicLong flush = new AtomicLong(0);
    private AtomicLong invalidationsByServer = new AtomicLong(0);
    private AtomicLong invalidationMessages = new AtomicLong(0);

    protected void hit() {
        hits.incrementAndGet();
    }

    protected void miss() {
        misses.incrementAndGet();
    }

    protected void load() {
        loads.incrementAndGet();
    }

    protected void evict() {
        evicts.incrementAndGet();
    }

    protected void nonCacheable() {
        nonCacheable.incrementAndGet();
    }

    protected void flush() {
        flush.incrementAndGet();
    }

    protected void invalidationByServer(int size) {
        invalidationsByServer.addAndGet(size);
    }

    protected void invalidationMessages() {
        invalidationMessages.incrementAndGet();
    }

    public long getHitCount() {
        return hits.get();
    }

    public long getMissCount() {
        return misses.get();
    }

    public long getLoadCount() {
        return loads.get();
    }

    public long getEvictCount() {
        return evicts.get();
    }

    public long getNonCacheableCount() {
        return nonCacheable.get();
    }

    public long getFlushCount() {
        return flush.get();
    }

    public long getInvalidationCount() {
        return invalidationsByServer.get();
    }

    public String toString() {
        return "CacheStats{" +
                "hits=" + hits +
                ", misses=" + misses +
                ", loads=" + loads +
                ", evicts=" + evicts +
                ", nonCacheable=" + nonCacheable +
                ", flush=" + flush +
                ", invalidationsByServer=" + invalidationsByServer +
                ", invalidationMessages=" + invalidationMessages +
                '}';
    }

}
