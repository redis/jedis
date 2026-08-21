package redis.clients.jedis.csc;

import java.util.List;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.JedisAsserts;

/**
 * Wraps a user-provided {@link Cacheable} with per-command verdict overrides, applying the same
 * precedence as the default policy: an override wins, every other command falls back to the wrapped
 * policy.
 */
class CustomCacheablePolicy implements Cacheable {

  private final CacheabilityResolver resolver;
  private final Cacheable custom;

  /**
   * @param custom decides commands the resolver already allows; it can only narrow the set
   * @param resolver the metadata-derived policy; a command it denies stays denied
   */
  CustomCacheablePolicy(Cacheable custom, CacheabilityResolver resolver) {
    JedisAsserts.notNull(custom, "custom");
    JedisAsserts.notNull(resolver, "resolver");
    this.resolver = resolver;
    this.custom = custom;
  }

  /**
   * Determines whether the given command with the specified keys is cacheable according to the
   * custom policy, taking into account per-command overrides.
   */
  @Override
  public boolean isCacheable(ProtocolCommand command, List<Object> keys) {
    // here we are applying only the custom one on purpose to keep the same behavior as before
    // we will replace the logic to not let custom cacheable to override the resolver when command
    // is not eligible.
    // DefaultCacheable deprecated as initial step towards the behaviour change.
    return custom.isCacheable(command, keys);
  }
}
