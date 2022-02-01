package redis.clients.jedis.commands;

import redis.clients.jedis.json.RedisJsonPipelineCommands;
import redis.clients.jedis.search.RediSearchPipelineCommands;
import redis.clients.jedis.timeseries.RedisTimeSeriesPipelineCommands;

public interface RedisModulePipelineCommands extends
    RediSearchPipelineCommands,
    RedisJsonPipelineCommands,
    RedisTimeSeriesPipelineCommands {

}
