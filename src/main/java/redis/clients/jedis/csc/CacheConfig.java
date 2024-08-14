package redis.clients.jedis.csc;

public class CacheConfig {

    public enum EvictionPolicyType {
        DEFAULT, LRU
    }

    public enum CacheType {
        DEFAULT, GUAVA, CAFFEINE
    }

    private int maxSize;
    private EvictionPolicyType evictionPolicyType;
    private ClientSideCacheable cacheable;
    private CacheType cacheType;

    public int getMaxSize() {
        return maxSize;
    }

    public CacheType getCacheType() {
        return cacheType;
    }

    public EvictionPolicyType getEvictionPolicyType() {
        return evictionPolicyType;
    }

    public ClientSideCacheable getCacheable() {
        return cacheable;
    }

    public static class Builder {
        private int maxSize;
        private EvictionPolicyType evictionPolicyType = EvictionPolicyType.DEFAULT;
        private ClientSideCacheable cacheable = DefaultClientSideCacheable.INSTANCE;
        private CacheType cacheType;

        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder cacheType(CacheType cacheType) {
            this.cacheType = cacheType;
            return this;
        }
        
        public Builder evictionPolicyType(EvictionPolicyType evictionPolicyType) {
            this.evictionPolicyType = evictionPolicyType;
            return this;
        }

        public Builder cacheable(ClientSideCacheable cacheable) {
            this.cacheable = cacheable;
            return this;
        }

        public CacheConfig build() {
            CacheConfig cacheConfig = new CacheConfig();
            cacheConfig.maxSize = this.maxSize;
            cacheConfig.evictionPolicyType = this.evictionPolicyType;
            cacheConfig.cacheable = this.cacheable;
            cacheConfig.cacheType = this.cacheType;
            return cacheConfig;
        }
    }
}