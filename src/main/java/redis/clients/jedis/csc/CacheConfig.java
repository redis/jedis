package redis.clients.jedis.csc;

import java.util.Collections;
import java.util.List;

public class CacheConfig {

    private int maxSize;
    private Cacheable cacheable;
    private EvictionPolicy evictionPolicy;
    private Class cacheClass;
    private boolean broadcastMode;
    private List<String> prefixes;
    private boolean noLoop;

    public int getMaxSize() {
        return maxSize;
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

    public boolean isBroadcastMode() {
        return broadcastMode;
    }

    public List<String> getPrefixes() {
        return prefixes;
    }

    public boolean noloop() {
        return noLoop;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final int DEFAULT_MAX_SIZE = 10000;
        private int maxSize = DEFAULT_MAX_SIZE;
        private Cacheable cacheable = DefaultCacheable.INSTANCE;
        private EvictionPolicy evictionPolicy;
        private Class cacheClass;
        private boolean broadcastMode = false;
        private boolean noLoop = false;
        private List<String> prefixes = Collections.emptyList();

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

        public Builder cacheClass(Class cacheClass) {
            this.cacheClass = cacheClass;
            return this;
        }

        public Builder bcast() {
            this.broadcastMode = true;
            return this;
        }

        public Builder noloop() {
            this.noLoop = true;
            return this;
        }

        public Builder prefixes(List<String> prefixes) {
            this.prefixes = (prefixes == null) ? Collections.emptyList() : prefixes;
            return this;
        }

        public Builder prefixes(String... prefixes) {
            if (prefixes == null || prefixes.length == 0) {
                this.prefixes = Collections.emptyList();
            } else {
                this.prefixes = java.util.Arrays.asList(prefixes);
            }
            return this;
        }

        public CacheConfig build() {
            CacheConfig cacheConfig = new CacheConfig();
            cacheConfig.maxSize = this.maxSize;
            cacheConfig.cacheable = this.cacheable;
            cacheConfig.evictionPolicy = this.evictionPolicy;
            cacheConfig.cacheClass = this.cacheClass;
            cacheConfig.broadcastMode = this.broadcastMode;
            cacheConfig.prefixes = this.prefixes;
            cacheConfig.noLoop = this.noLoop;
            return cacheConfig;
        }
    }
}
