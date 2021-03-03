package redis.clients.jedis.commands;

public interface JedisClusterCommands extends JedisCommands {

  /**
   * @deprecated Redis Cluster does not support MOVE command
   */
  @Override
  @Deprecated
  default Long move(String key, int dbIndex) {
    throw new UnsupportedOperationException("Redis Cluster does not support MOVE command");
  }

  Long waitReplicas(final String key, final int replicas, final long timeout);
}
