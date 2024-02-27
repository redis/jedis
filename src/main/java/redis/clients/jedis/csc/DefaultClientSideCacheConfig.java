package redis.clients.jedis.csc;

import redis.clients.jedis.commands.ProtocolCommand;

public class DefaultClientSideCacheConfig implements ClientSideCacheConfig {

  private final ClientSideCache csCache;

  public DefaultClientSideCacheConfig(ClientSideCache clientSideCache) {
    this.csCache = clientSideCache;
  }

  @Override
  public ClientSideCache getClientSideCache() {
    return this.csCache;
  }

  @Override
  public boolean isCacheable(ProtocolCommand command, Object... keys) {
    return true;
  }
}
