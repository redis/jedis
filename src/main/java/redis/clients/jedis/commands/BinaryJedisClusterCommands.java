package redis.clients.jedis.commands;

import redis.clients.jedis.params.RestoreParams;

public interface BinaryJedisClusterCommands extends BinaryJedisCommands {

  /**
   * @deprecated Use {@link #restore(byte[], long, byte[], redis.clients.jedis.params.RestoreParams)}.
   */
  @Deprecated
  @Override
  default String restoreReplace(byte[] key, long ttl, byte[] serializedValue) {
    return restore(key, ttl, serializedValue, RestoreParams.restoreParams().replace());
  }

  /**
   * @throws UnsupportedOperationException Redis Cluster does not support MOVE command.
   */
  @Override
  default Long move(byte[] key, int dbIndex) {
    throw new UnsupportedOperationException("Redis Cluster does not support MOVE command.");
  }

  Long waitReplicas(byte[] key, int replicas, long timeout);

  Long memoryUsage(byte[] key);

  Long memoryUsage(byte[] key, int samples);
}
