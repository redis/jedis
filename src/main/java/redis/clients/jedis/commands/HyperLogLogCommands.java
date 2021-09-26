package redis.clients.jedis.commands;

public interface HyperLogLogCommands {

  long pfadd(String key, String... elements);

  long pfcount(String key);
}
