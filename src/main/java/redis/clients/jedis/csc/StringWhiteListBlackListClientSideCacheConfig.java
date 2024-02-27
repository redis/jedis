package redis.clients.jedis.csc;

import java.util.Set;
import redis.clients.jedis.commands.ProtocolCommand;

public class StringWhiteListBlackListClientSideCacheConfig implements ClientSideCacheConfig {

  private final ClientSideCache csCache;

  private final Set<ProtocolCommand> whiteCommands;
  private final Set<ProtocolCommand> blackCommands;

  private final Set<String> whiteKeys;
  private final Set<String> blackKeys;

  public StringWhiteListBlackListClientSideCacheConfig(ClientSideCache clientSideCache,
      Set<ProtocolCommand> whiteListCommands, Set<ProtocolCommand> blackListCommands,
      Set<String> whiteListKeys, Set<String> blackListKeys) {
    this.csCache = clientSideCache;
    this.whiteCommands = whiteListCommands;
    this.blackCommands = blackListCommands;
    this.whiteKeys = whiteListKeys;
    this.blackKeys = blackListKeys;
  }

  @Override
  public ClientSideCache getClientSideCache() {
    return this.csCache;
  }

  @Override
  public boolean isCacheable(ProtocolCommand command, Object... keys) {
    if (!(keys instanceof String[])) {
      throw new IllegalArgumentException(this.getClass() + " can only process String keys.");
    }

    if (!whiteCommands.contains(command)) return false;
    if (blackCommands.contains(command)) return false;

    String[] strs = (String[]) keys;
    for (String str : strs) {
      if (!whiteKeys.contains(str)) return false;
      if (blackKeys.contains(str)) return false;
    }

    return true;
  }
}
