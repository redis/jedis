package redis.clients.jedis.csc.hash;

import redis.clients.jedis.CommandObject;

public interface CommandLongHashing {

  long hash(CommandObject command);
}
