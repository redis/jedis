package redis.clients.jedis;

import java.util.Collection;

import redis.clients.jedis.annots.Experimental;
import redis.clients.jedis.util.JedisAsserts;

/**
 * Source of {@link CommandMetadata} for one client. Client-side caching derives the cacheability of
 * a command from the metadata resolved here, and cluster clients build their command flags and
 * request/response policies from it.
 * <p>
 * Obtain resolvers from {@link CommandMetadataResolverFactory}, combine them with
 * {@link #withFallback(CommandMetadataResolver)}, and give one to a client through the client
 * builder's {@code commandMetadataResolver(...)} method.
 * @since 8.1
 */
@Experimental
public interface CommandMetadataResolver {

  /**
   * Returns the metadata of the given command.
   * @param commandName the command name; a container subcommand is looked up by its full
   *          {@code PARENT|CHILD} name, for example {@code XINFO|STREAM}
   * @return the command's metadata, or {@code null} when this resolver does not know the command
   * @since 8.1
   */
  CommandMetadata resolve(String commandName);

  /**
   * Returns the metadata of every command this resolver knows. Container subcommands are included
   * as records of their own, named {@code PARENT|CHILD}.
   * @return all metadata records of this resolver
   * @since 8.1
   */
  Collection<CommandMetadata> allCommandMetadata();

  /**
   * Returns a resolver that asks this resolver first and the given fallback for commands this
   * resolver does not know. This resolver supplies the complete metadata of a command it knows:
   * once it knows a container command, that command's subcommands are not looked up in the
   * fallback.
   * @param fallback the resolver asked for commands this resolver does not know
   * @return the combined resolver
   * @throws IllegalArgumentException if the fallback is null
   * @since 8.1
   */
  default CommandMetadataResolver withFallback(CommandMetadataResolver fallback) {
    JedisAsserts.notNull(fallback, "fallback must not be null");
    return new ChainedCommandMetadataResolver(this, fallback);
  }
}
