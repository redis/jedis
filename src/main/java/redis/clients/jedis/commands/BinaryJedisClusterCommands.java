package redis.clients.jedis.commands;

//public interface BinaryJedisClusterCommands extends BinaryJedisCommands {
public interface BinaryJedisClusterCommands {

  long waitReplicas(byte[] key, int replicas, long timeout);
}
