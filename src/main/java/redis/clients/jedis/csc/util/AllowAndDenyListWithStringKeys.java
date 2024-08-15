package redis.clients.jedis.csc.util;

import java.util.List;
import java.util.Set;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.csc.DefaultCacheable;
import redis.clients.jedis.csc.Cacheable;

public class AllowAndDenyListWithStringKeys implements Cacheable {

  private final Set<ProtocolCommand> allowCommands;
  private final Set<ProtocolCommand> denyCommands;

  private final Set<String> allowKeys;
  private final Set<String> denyKeys;

  public AllowAndDenyListWithStringKeys(Set<ProtocolCommand> allowCommands, Set<ProtocolCommand> denyCommands,
      Set<String> allowKeys, Set<String> denyKeys) {
    this.allowCommands = allowCommands;
    this.denyCommands = denyCommands;
    this.allowKeys = allowKeys;
    this.denyKeys = denyKeys;
  }

  @Override
  public boolean isCacheable(ProtocolCommand command, List<Object> keys) {
    if (allowCommands != null && !allowCommands.contains(command)) {
      return false;
    }
    if (denyCommands != null && denyCommands.contains(command)) {
      return false;
    }

    for (Object key : keys) {
      if (!(key instanceof String)) {
        return false;
      }
      if (allowKeys != null && !allowKeys.contains((String) key)) {
        return false;
      }
      if (denyKeys != null && denyKeys.contains((String) key)) {
        return false;
      }
    }

    return DefaultCacheable.isDefaultCacheableCommand(command);
  }
}
