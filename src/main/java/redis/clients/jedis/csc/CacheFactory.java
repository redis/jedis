package redis.clients.jedis.csc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import redis.clients.jedis.exceptions.JedisCacheException;

public final class CacheFactory {

    public static Cache getCache(CacheConfig config) {
        if (config.getCacheClass() == null) {
            if (config.getCacheable() == null) {
                throw new JedisCacheException("Cacheable is required to create the default cache!");
            }
            return new DefaultCache(config.getMaxSize(), config.getCacheable(), getEvictionPolicy(config));
        }
        return instantiateCustomCache(config);
    }

    private static Cache instantiateCustomCache(CacheConfig config) {
        try {
            if (config.getCacheable() != null) {
                Constructor ctorWithCacheable = findConstructorWithCacheable(config.getCacheClass());
                if (ctorWithCacheable != null) {
                    return (Cache) ctorWithCacheable.newInstance(config.getMaxSize(), getEvictionPolicy(config), config.getCacheable());
                }
            }
            Constructor ctor = getConstructor(config.getCacheClass());
            return (Cache) ctor.newInstance(config.getMaxSize(), getEvictionPolicy(config));
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | SecurityException e) {
            throw new JedisCacheException("Failed to insantiate custom cache type!", e);
        }
    }

    private static Constructor findConstructorWithCacheable(Class customCacheType) {
        return Arrays.stream(customCacheType.getConstructors())
                .filter(ctor -> Arrays.equals(ctor.getParameterTypes(), new Class[] { int.class, EvictionPolicy.class, Cacheable.class }))
                .findFirst().orElse(null);
    }

    private static Constructor getConstructor(Class customCacheType) {
        try {
            return customCacheType.getConstructor(int.class, EvictionPolicy.class);
        } catch (NoSuchMethodException e) {
            String className = customCacheType.getName();
            throw new JedisCacheException(String.format(
                "Failed to find compatible constructor for custom cache type!  Provide one of these;"
                        // give hints about the compatible constructors
                        + "\n - %s(int maxSize, EvictionPolicy evictionPolicy)\n - %s(int maxSize, EvictionPolicy evictionPolicy, Cacheable cacheable)",
                className, className), e);
        }
    }

    private static EvictionPolicy getEvictionPolicy(CacheConfig config) {
        if (config.getEvictionPolicy() == null) {
            // It will be default to LRUEviction, until we have other eviction implementations
            return new LRUEviction(config.getMaxSize());
        }
        return config.getEvictionPolicy();
    }
}