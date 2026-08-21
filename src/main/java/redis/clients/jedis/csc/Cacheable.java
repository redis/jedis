package redis.clients.jedis.csc;

import java.util.List;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.commands.ProtocolCommand;

public interface Cacheable {

  boolean isCacheable(ProtocolCommand command, List<Object> keys);

  default boolean isCacheable(CommandObject<?> commandObject) {
    return isCacheable(commandObject.getArguments().getCommand(),
      commandObject.getArguments().getKeys());
  }
}
