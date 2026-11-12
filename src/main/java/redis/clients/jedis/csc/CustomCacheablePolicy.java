package redis.clients.jedis.csc;

import java.util.List;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.JedisAsserts;

/**
 * Wraps a user-provided {@link Cacheable} next to the metadata-derived resolver. For now the custom
 * policy alone decides, preserving pre-8.1 behavior; a later major release will flip the precedence
 * so a command the resolver denies stays denied regardless of the custom policy.
 */
class CustomCacheablePolicy implements Cacheable {

  private final CacheabilityResolver resolver;
  private final Cacheable custom;

  /**
   * @param custom the user-provided policy; decides every command for now
   * @param resolver the metadata-derived policy; retained for the planned precedence flip
   */
  CustomCacheablePolicy(Cacheable custom, CacheabilityResolver resolver) {
    JedisAsserts.notNull(custom, "custom");
    JedisAsserts.notNull(resolver, "resolver");
    this.resolver = resolver;
    this.custom = custom;
  }

  @Override
  public boolean isCacheable(ProtocolCommand command, List<Object> keys) {
    return custom.isCacheable(command, keys);
  }

  @Override
  public boolean isCacheable(CommandObject<?> commandObject) {
    // here we are applying only the custom one on purpose to keep the same behavior as before
    // we will replace the logic to not let custom cacheable to override the resolver when command
    // is not eligible.
    // DefaultCacheable deprecated as initial step towards the behaviour change.
    return custom.isCacheable(commandObject);
  }
}
