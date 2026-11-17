package redis.clients.jedis.csc;

import java.util.Collections;
import java.util.List;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.commands.ProtocolCommand;

/**
 * A default {@link Cacheable} implementation that uses the {@link CacheabilityResolver} to determine the cacheability of
 * commands. This class is deprecated and will be removed in future versions. Use CacheConfig builder to configure cacheability instead, like excluding commands.
 * See csc-command-cacheability.md for more details and other options.
 */
@Deprecated
public class DefaultCacheable implements Cacheable {

  @Deprecated
  public static final DefaultCacheable INSTANCE = new DefaultCacheable();

  @Deprecated
  public DefaultCacheable() {
  }

  @Deprecated
  public static boolean isDefaultCacheableCommand(ProtocolCommand command) {
    return INSTANCE.isCacheable(command, Collections.emptyList());
  }

  @Override
  @Deprecated
  public boolean isCacheable(ProtocolCommand command, List<Object> keys) {
    // resolved per call, not at construction: a metadata-load failure must reach the caller as
    // an IllegalStateException instead of failing this class's static initializer
    return CacheabilityResolver.defaultResolver().isCacheable(command, keys);
  }

  @Override
  public boolean isCacheable(CommandObject<?> commandObject) {
    // a subclass may override only the two-argument method; keep routing through it
    if (getClass() != DefaultCacheable.class) {
      return Cacheable.super.isCacheable(commandObject);
    }
    return CacheabilityResolver.defaultResolver().isCacheable(commandObject);
  }
}
