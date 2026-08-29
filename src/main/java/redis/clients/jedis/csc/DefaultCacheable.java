package redis.clients.jedis.csc;

import java.util.Collections;
import java.util.List;

import redis.clients.jedis.commands.ProtocolCommand;

/**
 * A default {@link Cacheable} implementation that uses the {@link CacheabilityResolver} to determine the cacheability of
 * commands. This class is deprecated and will be removed in future versions. Use CacheConfig builder to configure cacheability instead, like excluding commands or providing a fallback Cacheable.
 * See csc-command-cacheability.md for more details and other options.
 */
@Deprecated
public class DefaultCacheable implements Cacheable {

  @Deprecated
  public static final DefaultCacheable INSTANCE = new DefaultCacheable();

  private final CacheabilityResolver resolver;

  @Deprecated
  public DefaultCacheable() {
    this.resolver = CacheabilityResolver.defaultResolver();
  }

  @Deprecated
  public static boolean isDefaultCacheableCommand(ProtocolCommand command) {
    return INSTANCE.isCacheable(command, Collections.emptyList());
  }

  @Override
  @Deprecated
  public boolean isCacheable(ProtocolCommand command, List<Object> keys) {
    return resolver.isCacheable(command, keys);
  }
}
