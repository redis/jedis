package redis.clients.jedis.commands;

public interface JedisClusterCommands extends JedisBaseCommands {
    Long waitReplicas(final String key, final int replicas, final long timeout);
}
