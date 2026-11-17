package redis.clients.jedis.csc;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.CommandMetadataResolver;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.JedisAsserts;
import redis.clients.jedis.util.SafeEncoder;

/**
 * Wraps a user-provided {@link Cacheable} next to the metadata-derived resolver. For now the custom
 * policy alone decides, preserving pre-8.1 behavior; a later major release will flip the precedence
 * so a command the resolver denies stays denied regardless of the custom policy. Until then, a
 * command the custom policy allows against the resolver's verdict is logged once, so affected
 * applications can see the upcoming change.
 */
class CustomCacheablePolicy implements Cacheable {

  private static final Logger logger = LoggerFactory.getLogger(CustomCacheablePolicy.class);

  private final CacheabilityResolver resolver;
  private final Cacheable custom;

  private final Set<String> divergenceWarned = ConcurrentHashMap.newKeySet();

  /**
   * @param custom the user-provided policy; decides every command for now
   * @param resolver the metadata-derived policy; detects the commands the planned precedence flip
   *          will stop caching
   */
  CustomCacheablePolicy(Cacheable custom, CacheabilityResolver resolver) {
    JedisAsserts.notNull(custom, "custom");
    JedisAsserts.notNull(resolver, "resolver");
    this.resolver = resolver;
    this.custom = custom;
  }

  CustomCacheablePolicy withMetadataResolver(CommandMetadataResolver metadataResolver) {
    CacheabilityResolver rebound = resolver.withMetadataResolver(metadataResolver);
    return rebound == resolver ? this : new CustomCacheablePolicy(custom, rebound);
  }

  @Override
  public boolean isCacheable(ProtocolCommand command, List<Object> keys) {
    boolean cacheable = custom.isCacheable(command, keys);
    if (cacheable) {
      warnIfNotEligible(command);
    }
    return cacheable;
  }

  @Override
  public boolean isCacheable(CommandObject<?> commandObject) {
    // the custom policy alone decides on purpose, to keep the pre-8.1 behavior; the resolver is
    // consulted only to report the commands it would deny
    boolean cacheable = custom.isCacheable(commandObject);
    if (cacheable) {
      warnIfNotEligible(resolver.effectiveCommand(commandObject));
    }
    return cacheable;
  }

  private void warnIfNotEligible(ProtocolCommand command) {
    if (resolver.isEligible(command)) {
      return;
    }
    String name = SafeEncoder.encode(command.getRaw());
    if (divergenceWarned.add(name)) {
      logger.warn(
        "The custom Cacheable allows caching {}, which the default client-side caching policy"
            + " does not. A future major release will apply a custom Cacheable only to commands"
            + " the default policy allows, and {} will then no longer be cached.",
        name, name);
    }
  }
}
