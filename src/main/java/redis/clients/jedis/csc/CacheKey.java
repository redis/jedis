package redis.clients.jedis.csc;

import java.util.Objects;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.annots.Internal;

@Internal
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

  public Object[] getRedisKeys() {
    return command.getArguments().getKeys();
  }

  public CommandObject<T> getCommand() {
    return command;
  }
}
