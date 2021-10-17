package redis.clients.jedis.commands;

//Legacy
//public interface JedisClusterCommands extends JedisCommands {
public interface JedisClusterCommands {

  long waitReplicas(String key, int replicas, long timeout);
}
