package redis.clients.jedis;

import java.util.Collection;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.JedisAsserts;

@Experimental
public interface CommandMetadataResolver {

  CommandMetadata resolve(String commandName);

  Collection<CommandMetadata> allCommandMetadata();

  default CommandMetadataResolver withFallback(CommandMetadataResolver fallback) {
    JedisAsserts.notNull(fallback, "fallback must not be null");
    return new ChainedCommandMetadataResolver(this, fallback);
  }
}
