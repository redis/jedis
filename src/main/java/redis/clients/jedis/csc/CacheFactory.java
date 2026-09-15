package redis.clients.jedis.csc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import redis.clients.jedis.exceptions.JedisCacheException;

/**
 * Factory class for creating cache instances based on the provided configuration.
 * Subclasses can override the {@link #getNewCache(CacheConfig)} method to supply
 * custom cache implementations to the relevant client builders.
 */
public class CacheFactory {

    /**
     * Creates a new {@link Cache} from the given configuration.
     * @param config cache configuration
     * @return a new cache instance
     * @since 8.1
     */
    public Cache getNewCache(CacheConfig config) {
        if (config.getCacheClass() == null) {
            if (config.getCacheable() == null) {
                throw new JedisCacheException("Cacheable is required to create the default cache!");
            }
            return new DefaultCache(config.getMaxSize(), config.getCacheable(), (config.getEvictionPolicySupplier().get()));
        }
        return instantiateCustomCache(config);
    }

    public static Cache getCache(CacheConfig config) {
        return new CacheFactory().getNewCache(config);
    }

    private static Cache instantiateCustomCache(CacheConfig config) {
        try {
            if (config.getCacheable() != null) {
                Constructor ctorWithCacheable = findConstructorWithCacheable(config.getCacheClass());
                if (ctorWithCacheable != null) {
                    return (Cache) ctorWithCacheable.newInstance(config.getMaxSize(),
                            config.getEvictionPolicySupplier().get(), config.getCacheable());
                }
            }
            Constructor ctor = getConstructor(config.getCacheClass());
            return (Cache) ctor.newInstance(config.getMaxSize(), config.getEvictionPolicySupplier().get());
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
}