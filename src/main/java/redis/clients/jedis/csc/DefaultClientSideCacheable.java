package redis.clients.jedis.csc;

import redis.clients.jedis.commands.ProtocolCommand;

public class DefaultClientSideCacheable implements ClientSideCacheable {

  public static final DefaultClientSideCacheable INSTANCE = new DefaultClientSideCacheable();

  public DefaultClientSideCacheable() { }

  @Override
  public boolean isCacheable(ProtocolCommand command, Object... keys) {
    return true;
  }
}
