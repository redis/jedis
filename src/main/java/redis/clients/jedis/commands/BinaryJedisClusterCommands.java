package redis.clients.jedis.commands;

public interface BinaryJedisClusterCommands extends BinaryJedisCommands {

  /**
   * @deprecated Redis Cluster does not support MOVE command
   */
  @Override
  @Deprecated
  default Long move(byte[] key, int dbIndex) {
    throw new UnsupportedOperationException("Redis Cluster does not support MOVE command");
  }

  Long waitReplicas(byte[] key, final int replicas, final long timeout);
}
