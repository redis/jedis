package redis.clients.jedis.csc;

import java.util.HashMap;

public class CacheProvider {

    public Cache getCache(CacheConfig config) {
        return getCache(config, new HashMap<CacheKey, CacheEntry>());
    }

    public Cache getCache(CacheConfig config, HashMap<CacheKey, CacheEntry> map) {
        return new DefaultCache(config.getMaxSize(), map, config.getCacheable(),
                getEvictionPolicy(config));
    }

    private EvictionPolicy getEvictionPolicy(CacheConfig config) {
        if (config.getEvictionPolicy() == null) {
            // It will be default to LRUEviction, until we have other eviction implementations
            return new LRUEviction(config.getMaxSize());
        }
        return config.getEvictionPolicy();
    }
}