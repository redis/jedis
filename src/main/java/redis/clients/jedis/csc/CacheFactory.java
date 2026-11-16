package redis.clients.jedis.csc;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import redis.clients.jedis.CommandMetadataResolver;
import redis.clients.jedis.CommandMetadataResolverFactory;
import redis.clients.jedis.exceptions.JedisCacheException;
import redis.clients.jedis.util.JedisAsserts;

/**
 * Factory class for creating cache instances based on the provided configuration. Subclasses can
 * override the {@link #createCache(CacheConfig, CommandMetadataResolver)} method to supply custom
 * cache implementations to the relevant client builders.
 */
public class CacheFactory {

  /**
   * Creates the cache for a client from its cache configuration and command metadata. With the
   * default policy the cacheability of commands is derived from the given metadata resolver,
   * keeping the exclusions of the configuration. Client builders call this method during build;
   * override it to supply a custom {@link Cache} implementation.
   * @param config the cache configuration
   * @param metadataResolver the command metadata of the client the cache is created for
   * @return the cache
   * @throws IllegalArgumentException if the metadata resolver is null
   * @throws JedisCacheException if the cache cannot be created from the configuration
   * @since 8.1
   */
  public Cache createCache(CacheConfig config, CommandMetadataResolver metadataResolver) {
    Cacheable cacheable = resolveCacheable(config, metadataResolver);
    if (config.getCacheClass() == null) {
      if (cacheable == null) {
        throw new JedisCacheException("Cacheable is required to create the default cache!");
      }
      return new DefaultCache(config.getMaxSize(), cacheable,
          config.getEvictionPolicySupplier().get());
    }
    return instantiateCustomCache(config, cacheable);
  }

  // The policy a cache created for the given metadata resolver must apply: without a user
  // customization the metadata-derived policy over that resolver (exclusions kept).
  private Cacheable resolveCacheable(CacheConfig config, CommandMetadataResolver metadataResolver) {
    JedisAsserts.notNull(metadataResolver, "metadataResolver must not be null");
    Cacheable cacheable = config.getCacheable();
    if (cacheable instanceof CacheabilityResolver) {
      return ((CacheabilityResolver) cacheable).withMetadataResolver(metadataResolver);
    }
    // Only reachable through the deprecated CacheConfig.Builder.cacheable(...): a user-provided
    // Cacheable (or null). Remove both paths below once that method is dropped; the config then
    // always holds a CacheabilityResolver.
    if (cacheable instanceof CustomCacheablePolicy) {
      return ((CustomCacheablePolicy) cacheable).withMetadataResolver(metadataResolver);
    }
    return cacheable;
  }

  @Deprecated
  public static Cache getCache(CacheConfig config) {
    return new CacheFactory().createCache(config, CommandMetadataResolverFactory.defaultResolver());
  }

  /**
   * All this custom type and instantiation logic is deprecated CacheFactory will be used with
   * client builders when customization of cache instances is needed.
   */
  @Deprecated
  private static Cache instantiateCustomCache(CacheConfig config, Cacheable cacheable) {
    try {
      if (cacheable != null) {
        Constructor ctorWithCacheable = findConstructorWithCacheable(config.getCacheClass());
        if (ctorWithCacheable != null) {
          return (Cache) ctorWithCacheable.newInstance(config.getMaxSize(),
            config.getEvictionPolicySupplier().get(), cacheable);
        }
      }
      Constructor ctor = getConstructor(config.getCacheClass());
      return (Cache) ctor.newInstance(config.getMaxSize(),
        config.getEvictionPolicySupplier().get());
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | SecurityException e) {
      throw new JedisCacheException("Failed to insantiate custom cache type!", e);
    }
  }

  private static Constructor findConstructorWithCacheable(Class customCacheType) {
    return Arrays.stream(customCacheType.getConstructors())
        .filter(ctor -> Arrays.equals(ctor.getParameterTypes(),
          new Class[] { int.class, EvictionPolicy.class, Cacheable.class }))
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