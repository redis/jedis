package redis.clients.jedis.csc.util;

import java.util.Set;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.csc.ClientSideCacheable;

public class StringWhiteListBlackListClientSideCacheable implements ClientSideCacheable {

  private final Set<ProtocolCommand> whiteCommands;
  private final Set<ProtocolCommand> blackCommands;

  private final Set<String> whiteKeys;
  private final Set<String> blackKeys;

  public StringWhiteListBlackListClientSideCacheable(
      Set<ProtocolCommand> whiteListCommands, Set<ProtocolCommand> blackListCommands,
      Set<String> whiteListKeys, Set<String> blackListKeys) {
    this.whiteCommands = whiteListCommands;
    this.blackCommands = blackListCommands;
    this.whiteKeys = whiteListKeys;
    this.blackKeys = blackListKeys;
  }

  @Override
  public boolean isCacheable(ProtocolCommand command, Object... keys) {
    if (whiteCommands != null && !whiteCommands.contains(command)) return false;
    if (blackCommands != null && blackCommands.contains(command)) return false;

    for (Object key : keys) {
      if (!(key instanceof String)) return false;
      if (whiteKeys != null && !whiteKeys.contains((String) key)) return false;
      if (blackKeys != null && blackKeys.contains((String) key)) return false;
    }

    return true;
  }
}
