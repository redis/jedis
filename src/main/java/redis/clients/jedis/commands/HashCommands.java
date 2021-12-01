package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface HashCommands {

  long hset(String key, String field, String value);

  long hset(String key, Map<String, String> hash);

  String hget(String key, String field);

  long hsetnx(String key, String field, String value);

  String hmset(String key, Map<String, String> hash);

  List<String> hmget(String key, String... fields);

  long hincrBy(String key, String field, long value);

  double hincrByFloat(String key, String field, double value);

  boolean hexists(String key, String field);

  long hdel(String key, String... field);

  long hlen(String key);

  Set<String> hkeys(String key);

  List<String> hvals(String key);

  Map<String, String> hgetAll(String key);

  String hrandfield(String key);

  List<String> hrandfield(String key, long count);

  Map<String, String> hrandfieldWithValues(String key, long count);

  default ScanResult<Map.Entry<String, String>> hscan(String key, String cursor) {
    return hscan(key, cursor, new ScanParams());
  }

  ScanResult<Map.Entry<String, String>> hscan(String key, String cursor, ScanParams params);

  long hstrlen(String key, String field);
}
