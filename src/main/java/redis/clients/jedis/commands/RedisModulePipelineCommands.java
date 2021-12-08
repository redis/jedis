package redis.clients.jedis.commands;

import redis.clients.jedis.json.RedisJsonPipelineCommands;
import redis.clients.jedis.search.RediSearchPipelineCommands;

public interface RedisModulePipelineCommands extends RediSearchPipelineCommands, RedisJsonPipelineCommands {
}
