package redis.clients.jedis.commands;

import redis.clients.jedis.json.RedisJsonCommands;
import redis.clients.jedis.search.RediSearchCommands;

public interface RedisModuleCommands extends RediSearchCommands, RedisJsonCommands {
}
