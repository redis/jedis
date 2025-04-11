package redis.clients.jedis.csc;

public class CacheConfig {

    private int maxSize;
    private long ttl;
    private Cacheable cacheable;
    private EvictionPolicy evictionPolicy;
    private Class cacheClass;

    public int getMaxSize() {
        return maxSize;
    }

    public long getTtl() {
        return ttl;
    }

    public Cacheable getCacheable() {
        return cacheable;
    }

    public EvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }

    public Class getCacheClass() {
        return cacheClass;
    }
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final int DEFAULT_MAX_SIZE = 10000;
        /**
         * 30000ms
         */
        private final long DEFAULT_TTL = 30000;
        private int maxSize = DEFAULT_MAX_SIZE;
        private long ttl = DEFAULT_TTL;
        private Cacheable cacheable = DefaultCacheable.INSTANCE;
        private EvictionPolicy evictionPolicy;
        private Class cacheClass;

        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder ttl(long ttl) {
            this.ttl = ttl;
            return this;
        }

        public Builder evictionPolicy(EvictionPolicy policy) {
            this.evictionPolicy = policy;
            return this;
        }

        public Builder cacheable(Cacheable cacheable) {
            this.cacheable = cacheable;
            return this;
        }

        public Builder cacheClass(Class cacheClass) {
            this.cacheClass = cacheClass;
            return this;
        }

        public CacheConfig build() {
            CacheConfig cacheConfig = new CacheConfig();
            cacheConfig.maxSize = this.maxSize;
            cacheConfig.ttl = this.ttl;
            cacheConfig.cacheable = this.cacheable;
            cacheConfig.evictionPolicy = this.evictionPolicy;
            cacheConfig.cacheClass = this.cacheClass;
            return cacheConfig;
        }
    }
}