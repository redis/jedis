package redis.clients.jedis.csc;

import java.util.List;
import redis.clients.jedis.commands.ProtocolCommand;

public interface ClientSideCacheable {

  boolean isCacheable(ProtocolCommand command, List keys);
}
