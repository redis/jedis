package redis.clients.jedis.csc;

import java.util.HashMap;

import redis.clients.jedis.csc.CacheConfig.EvictionPolicyType;

public class CacheProvider {

    public Cache getCache(CacheConfig config) {
        switch (config.getEvictionPolicyType()) {
            case LRU:
                return new DefaultCache(config.getMaxSize(), new HashMap<CacheKey, CacheEntry>(), config.getCacheable(),
                        new LRUEviction(0));
            default:
                return new DefaultCache(config.getMaxSize(), config.getCacheable());
        }
    }

    public Cache getCache(CacheConfig config, HashMap<CacheKey, CacheEntry> map) {
        switch (config.getCacheType()) {
            case GUAVA:
                return new GuavaClientSideCache(config.getMaxSize(), config.getCacheable(),
                        getEvictionPolicy(config.getEvictionPolicyType(), config.getMaxSize()));
            case CAFFEINE:
                return new CaffeineClientSideCache(config.getMaxSize(), config.getCacheable(),
                        getEvictionPolicy(config.getEvictionPolicyType(), config.getMaxSize()));
            default:
                return new DefaultCache(config.getMaxSize(), map, config.getCacheable(),
                        getEvictionPolicy(config.getEvictionPolicyType(), config.getMaxSize()));
        }
    }

    private EvictionPolicy getEvictionPolicy(EvictionPolicyType evictionPolicyType, int initialCapacity) {
        switch (evictionPolicyType) {
            default:
                return new LRUEviction(initialCapacity);
        }
    }
}