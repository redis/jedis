package redis.clients.jedis.csc;

import redis.clients.jedis.commands.ProtocolCommand;

public interface ClientSideCacheConfig {

  /**
   * MUST NOT be {@code null}.
   */
  ClientSideCache getClientSideCache();

  default boolean isCacheable(ProtocolCommand command, Object... keys) {
    return true;
  }
}
