package redis.clients.jedis.csc;

import redis.clients.jedis.annots.VisibleForTesting;

public class CacheConfig {

    @VisibleForTesting
    static final int DEFAULT_CACHE_MAX_SIZE = 10_000;

    private int maxSize;
    private Cacheable cacheable;
    private EvictionPolicy evictionPolicy;

    public int getMaxSize() {
        return maxSize;
    }

    public Cacheable getCacheable() {
        return cacheable;
    }

    public EvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int maxSize = DEFAULT_CACHE_MAX_SIZE;
        private Cacheable cacheable = DefaultCacheable.INSTANCE;
        private EvictionPolicy evictionPolicy = null;

        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
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

        public CacheConfig build() {
            CacheConfig cacheConfig = new CacheConfig();
            cacheConfig.maxSize = this.maxSize;
            cacheConfig.cacheable = this.cacheable;
            cacheConfig.evictionPolicy = this.evictionPolicy;
            return cacheConfig;
        }
    }
}