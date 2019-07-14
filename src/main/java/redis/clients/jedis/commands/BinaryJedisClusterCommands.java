package redis.clients.jedis.commands;

public interface BinaryJedisClusterCommands extends BinaryJedisCommands {

  @Override
  default Long move(byte[] key, int dbIndex) {
    throw new UnsupportedOperationException();
  }

  Long waitReplicas(byte[] key, final int replicas, final long timeout);

  @Override
  default Object sendCommand(ProtocolCommand cmd, byte[]... args) {
    throw new UnsupportedOperationException();
  }

  Object sendCommand(final byte[] sampleKey, ProtocolCommand cmd, byte[]... args);
}
