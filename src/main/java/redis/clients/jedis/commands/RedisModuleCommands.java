package redis.clients.jedis.commands;

import redis.clients.jedis.bloom.commands.RedisBloomCommands;
import redis.clients.jedis.graph.RedisGraphCommands;
import redis.clients.jedis.json.RedisJsonCommands;
import redis.clients.jedis.search.RediSearchCommands;
import redis.clients.jedis.timeseries.RedisTimeSeriesCommands;

public interface RedisModuleCommands extends
    RediSearchCommands,
    RedisJsonCommands,
    RedisTimeSeriesCommands,
    RedisBloomCommands,
    RedisGraphCommands {

}
