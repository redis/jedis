package redis.clients.jedis.commands;

import java.util.List;
import java.util.Set;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface SetCommands {

  long sadd(String key, String... member);

  Set<String> smembers(String key);

  long srem(String key, String... member);

  String spop(String key);

  Set<String> spop(String key, long count);

  long scard(String key);

  boolean sismember(String key, String member);

  List<Boolean> smismember(String key, String... members);

  String srandmember(String key);

  List<String> srandmember(String key, int count);

  default ScanResult<String> sscan(String key, String cursor) {
    return sscan(key, cursor, new ScanParams());
  }

  ScanResult<String> sscan(String key, String cursor, ScanParams params);
}
