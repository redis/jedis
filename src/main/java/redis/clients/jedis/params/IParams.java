package redis.clients.jedis.params;

import redis.clients.jedis.CommandArguments;

public interface IParams {

  void addParams(CommandArguments args);
}
