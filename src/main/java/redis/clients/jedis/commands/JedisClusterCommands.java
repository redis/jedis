package redis.clients.jedis.commands;

import redis.clients.jedis.params.RestoreParams;

public interface JedisClusterCommands extends JedisCommands {

  /**
   * @deprecated Use {@link #restore(java.lang.String, long, byte[], redis.clients.jedis.params.RestoreParams)}.
   */
  @Deprecated
  default String restoreReplace(String key, long ttl, byte[] serializedValue) {
    return restore(key, ttl, serializedValue, RestoreParams.restoreParams().replace());
  }

  /**
   * @throws UnsupportedOperationException Redis Cluster does not support MOVE command.
   */
  @Override
  default Long move(String key, int dbIndex) {
    throw new UnsupportedOperationException("Redis Cluster does not support MOVE command.");
  }

  Long waitReplicas(String key, int replicas, long timeout);
}
