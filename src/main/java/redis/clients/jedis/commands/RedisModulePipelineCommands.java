package redis.clients.jedis.commands;

import redis.clients.jedis.bloom.commands.RedisBloomPipelineCommands;
import redis.clients.jedis.json.commands.RedisJsonPipelineCommands;
import redis.clients.jedis.search.RediSearchPipelineCommands;
import redis.clients.jedis.timeseries.RedisTimeSeriesPipelineCommands;

public interface RedisModulePipelineCommands extends
    RediSearchPipelineCommands,
    RedisJsonPipelineCommands,
    RedisTimeSeriesPipelineCommands,
    RedisBloomPipelineCommands {

}
