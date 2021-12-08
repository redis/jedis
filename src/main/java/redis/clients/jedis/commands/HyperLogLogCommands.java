package redis.clients.jedis.commands;

public interface HyperLogLogCommands {

  long pfadd(String key, String... elements);

  String pfmerge(String destkey, String... sourcekeys);

  long pfcount(String key);

  long pfcount(String... keys);

}
