package redis.clients.jedis.commands;

import redis.clients.jedis.Response;

public interface PipelineCommands extends PipelineKeyCommands{

  Response<String> get(String key);

  Response<String> set(String key, String value);
}
