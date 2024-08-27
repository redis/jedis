package redis.clients.jedis.csc;

public class CacheConfig {

    public enum EvictionPolicyType {
        DEFAULT, LRU
    }

    private int maxSize;
    private EvictionPolicyType evictionPolicyType;
    private Cacheable cacheable;

    public int getMaxSize() {
        return maxSize;
    }

    public EvictionPolicyType getEvictionPolicyType() {
        return evictionPolicyType;
    }

    public Cacheable getCacheable() {
        return cacheable;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int maxSize;
        private EvictionPolicyType evictionPolicyType = EvictionPolicyType.DEFAULT;
        private Cacheable cacheable = DefaultCacheable.INSTANCE;

        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }
        
        public Builder evictionPolicyType(EvictionPolicyType evictionPolicyType) {
            this.evictionPolicyType = evictionPolicyType;
            return this;
        }

        public Builder cacheable(Cacheable cacheable) {
            this.cacheable = cacheable;
            return this;
        }

        public CacheConfig build() {
            CacheConfig cacheConfig = new CacheConfig();
            cacheConfig.maxSize = this.maxSize;
            cacheConfig.evictionPolicyType = this.evictionPolicyType;
            cacheConfig.cacheable = this.cacheable;
            return cacheConfig;
        }
    }
}