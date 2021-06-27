package redis.clients.jedis.commands;

import java.util.List;

import redis.clients.jedis.StreamConsumersInfo;
import redis.clients.jedis.StreamGroupInfo;
import redis.clients.jedis.StreamInfo;
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
   * @deprecated Use {@link #xinfoStreamBinary(byte[])}.
   */
  @Override
  @Deprecated
  default StreamInfo xinfoStream(byte[] key) {
    throw new UnsupportedOperationException("Use other version of XINFO STREAM.");
  }

  /**
   * @deprecated Use {@link #xinfoGroupBinary(byte[])}.
   */
  @Override
  @Deprecated
  default List<StreamGroupInfo> xinfoGroup(byte[] key) {
    throw new UnsupportedOperationException("Use other version of XINFO GROUPS.");
  }

  /**
   * @deprecated Use {@link #xinfoConsumersBinary(byte[], byte[])}.
   */
  @Override
  @Deprecated
  default List<StreamConsumersInfo> xinfoConsumers(byte[] key, byte[] group) {
    throw new UnsupportedOperationException("Use other version of XINFO CONSUMERS.");
  }

  long waitReplicas(byte[] key, int replicas, long timeout);
}
