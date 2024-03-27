package redis.clients.jedis.csc.util;

import java.util.Set;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.csc.ClientSideCacheable;

public class AllowAndDenyListWithStringKeys implements ClientSideCacheable {

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
  public boolean isCacheable(ProtocolCommand command, Object... keys) {
    if (allowCommands != null && !allowCommands.contains(command)) return false;
    if (denyCommands != null && denyCommands.contains(command)) return false;

    for (Object key : keys) {
      if (!(key instanceof String)) return false;
      if (allowKeys != null && !allowKeys.contains((String) key)) return false;
      if (denyKeys != null && denyKeys.contains((String) key)) return false;
    }

    return true;
  }
}
