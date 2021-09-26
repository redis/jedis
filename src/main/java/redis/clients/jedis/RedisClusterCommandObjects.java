package redis.clients.jedis;

import redis.clients.jedis.commands.ProtocolCommand;

public class RedisClusterCommandObjects extends RedisCommandObjects {

  @Override
  protected ClusterCommandArguments commandArguments(ProtocolCommand command) {
    return new ClusterCommandArguments(command);
  }
}
