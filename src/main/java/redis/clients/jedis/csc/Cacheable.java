package redis.clients.jedis.csc;

import java.util.List;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.commands.ProtocolCommand;

public interface Cacheable {

  /**
   * @deprecated Use {@link #isCacheable(CommandObject)}; this signature cannot see a subcommand
   *             declared on the command's arguments.
   */
  @Deprecated
  boolean isCacheable(ProtocolCommand command, List<Object> keys);

  /**
   * Decides cacheability from the full command object. The default implementation delegates to
   * {@link #isCacheable(ProtocolCommand, List)} with the container command and keys, so existing
   * implementations keep their behavior; policies that need the declared subcommand (for example
   * to judge {@code XINFO STREAM} separately from {@code XINFO CONSUMERS}) override this method.
   * @param commandObject the command to judge
   * @return true if the command's reply may be cached
   * @since 8.1
   */
  default boolean isCacheable(CommandObject<?> commandObject) {
    return isCacheable(commandObject.getArguments().getCommand(),
      commandObject.getArguments().getKeys());
  }
}
