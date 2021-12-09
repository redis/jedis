package redis.clients.jedis.commands;

import redis.clients.jedis.json.RedisJsonCommands;
import redis.clients.jedis.search.RediSearchCommands;
import redis.clients.jedis.tairhash.TairHashCommands;

public interface RedisModuleCommands extends RediSearchCommands, RedisJsonCommands, TairHashCommands {
}
