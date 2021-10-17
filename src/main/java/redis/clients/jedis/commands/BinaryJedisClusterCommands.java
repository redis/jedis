package redis.clients.jedis.commands;

//Legacy
//public interface BinaryJedisClusterCommands extends BinaryJedisCommands {
public interface BinaryJedisClusterCommands {

  long waitReplicas(byte[] key, int replicas, long timeout);
}
