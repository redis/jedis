package redis.clients.jedis.commands;

public interface HyperLogLogBinaryCommands {

  long pfadd(byte[] key, byte[]... elements);

  long pfcount(byte[] key);

}
