package redis.clients.jedis.commands;

public interface BinaryJedisClusterCommands extends BinaryJedisCommands {

  long waitReplicas(byte[] key, int replicas, long timeout);
}
