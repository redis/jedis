package redis.clients.jedis.commands;

import redis.clients.jedis.Response;

public interface PipelineCommands extends PipelineKeyCommands, PipelineStringCommands, PipelineListCommands,
PipelineHashCommands, PipelineSetCommands{

  }
