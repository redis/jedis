package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.SortingParams;

public interface KeyBinaryCommands {

  boolean exists(byte[] key);

  long persist(byte[] key);

  String type(byte[] key);

  byte[] dump(byte[] key);

  String restore(byte[] key, long ttl, byte[] serializedValue);

  String restore(byte[] key, long ttl, byte[] serializedValue, RestoreParams params);

  long expire(byte[] key, long seconds);

  long pexpire(byte[] key, long milliseconds);

  long expireAt(byte[] key, long unixTime);

  long pexpireAt(byte[] key, long millisecondsTimestamp);

  long ttl(byte[] key);

  long pttl(byte[] key);

  long touch(byte[] key);

  List<byte[]> sort(byte[] key);

  List<byte[]> sort(byte[] key, SortingParams sortingParameters);

  long del(byte[] key);

  long unlink(byte[] key);

  Long memoryUsage(byte[] key);

  Long memoryUsage(byte[] key, int samples);

}
