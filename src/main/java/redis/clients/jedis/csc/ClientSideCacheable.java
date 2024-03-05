package redis.clients.jedis.csc;

import redis.clients.jedis.commands.ProtocolCommand;

public interface ClientSideCacheable {

  boolean isCacheable(ProtocolCommand command, Object... keys);
}
