package redis.clients.jedis.commands;

public interface ServerBinaryKeyedCommands {

  long waitReplicas(byte[] key, int replicas, long timeout);
}
