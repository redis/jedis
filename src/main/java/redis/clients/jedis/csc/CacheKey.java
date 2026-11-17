package redis.clients.jedis.csc;

import java.util.List;
import java.util.Objects;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.commands.ProtocolCommand;

public class CacheKey<T> {

  private final CommandObject<T> command;

  public CacheKey(CommandObject<T> command) {
    this.command = Objects.requireNonNull(command);
  }

  @Override
  public int hashCode() {
    return command.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    final CacheKey other = (CacheKey) obj;
    return Objects.equals(this.command, other.command);
  }

  public List<Object> getRedisKeys() {
    return command.getArguments().getKeys();
  }

  /**
   * The container command of this cache entry; a declared subcommand is not reflected here.
   * @deprecated Use {@link #getCommandObject()}, whose arguments also expose the declared
   *             subcommand.
   */
  @Deprecated
  public ProtocolCommand getRedisCommand() {
    return command.getArguments().getCommand();
  }

  /**
   * The command object this cache key was built from.
   * @since 8.1
   */
  public CommandObject<T> getCommandObject() {
    return command;
  }
}
