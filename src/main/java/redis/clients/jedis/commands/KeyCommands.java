package redis.clients.jedis.commands;

import java.util.List;
import redis.clients.jedis.params.RestoreParams;
import redis.clients.jedis.params.SortingParams;

public interface KeyCommands {

  boolean exists(String key);

  long persist(String key);

  String type(String key);

  byte[] dump(String key);

  String restore(String key, long ttl, byte[] serializedValue);

  String restore(String key, long ttl, byte[] serializedValue, RestoreParams params);

  long expire(String key, long seconds);

  long pexpire(String key, long milliseconds);

  long expireAt(String key, long unixTime);

  long pexpireAt(String key, long millisecondsTimestamp);

  long ttl(String key);

  long pttl(String key);

  long touch(String key);

  List<String> sort(String key);

  List<String> sort(String key, SortingParams sortingParameters);

  long del(String key);

  long unlink(String key);

  Long memoryUsage(String key);

  Long memoryUsage(String key, int samples);
}
