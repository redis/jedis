package redis.clients.jedis.csc;

import java.util.Collections;
import java.util.List;

import redis.clients.jedis.MetadataResolver;
import redis.clients.jedis.commands.ProtocolCommand;


/**
 * The former default {@link Cacheable}, now backed by the metadata-driven resolver so its verdicts
 * match the current default policy.
 * @deprecated The default policy is applied automatically; to customize it, use the
 *             {@link CacheConfig.Builder} instead — exclude commands with
 *             {@code excludeCommands(...)} or supply a fallback with {@code withFallback(...)}.
 *             See the {@code docs/csc-command-cacheability.md} page for details.
 */
@Deprecated
public class DefaultCacheable implements Cacheable {

  private static final CacheabilityResolver DEFAULT_RESOLVER = new CacheabilityResolver(
      new MetadataResolver());

  @Deprecated
  public static final DefaultCacheable INSTANCE = new DefaultCacheable();

  private final CacheabilityResolver resolver;

  @Deprecated
  public DefaultCacheable() {
    this.resolver = DEFAULT_RESOLVER;
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
