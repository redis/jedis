package redis.clients.jedis.csc;

import redis.clients.jedis.commands.ProtocolCommand;

public class DefaultClientSideCacheConfig implements ClientSideCacheConfig {

  private final ClientSideCache clientSideCache;

  public DefaultClientSideCacheConfig(ClientSideCache clientSideCache) {
    this.clientSideCache = clientSideCache;
  }

  @Override
  public ClientSideCache getClientSideCache() {
    return this.clientSideCache;
  }

  @Override
  public boolean isCacheable(ProtocolCommand command, Object... keys) {
    return true;
  }
}
