package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Response;
import redis.clients.jedis.args.ExpiryOption;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface HashPipelineCommands {

  Response<Long> hset(String key, String field, String value);

  Response<Long> hset(String key, Map<String, String> hash);

  Response<String> hget(String key, String field);

  Response<Long> hsetnx(String key, String field, String value);

  Response<String> hmset(String key, Map<String, String> hash);

  Response<List<String>> hmget(String key, String... fields);

  Response<Long> hincrBy(String key, String field, long value);

  Response<Double> hincrByFloat(String key, String field, double value);

  Response<Boolean> hexists(String key, String field);

  Response<Long> hdel(String key, String... field);

  Response<Long> hlen(String key);

  Response<Set<String>> hkeys(String key);

  Response<List<String>> hvals(String key);

  Response<Map<String, String>> hgetAll(String key);

  Response<String> hrandfield(String key);

  Response<List<String>> hrandfield(String key, long count);

  Response<List<Map.Entry<String, String>>> hrandfieldWithValues(String key, long count);

  default Response<ScanResult<Map.Entry<String, String>>> hscan(String key, String cursor) {
    return hscan(key, cursor, new ScanParams());
  }

  Response<ScanResult<Map.Entry<String, String>>> hscan(String key, String cursor, ScanParams params);

  default Response<ScanResult<String>> hscanNoValues(String key, String cursor) {
    return hscanNoValues(key, cursor, new ScanParams());
  }

  Response<ScanResult<String>> hscanNoValues(String key, String cursor, ScanParams params);

  Response<Long> hstrlen(String key, String field);

  Response<List<Long>> hexpire(String key, long seconds, String... fields);

  Response<List<Long>> hexpire(String key, long seconds, ExpiryOption condition, String... fields);

  Response<List<Long>> hpexpire(String key, long milliseconds, String... fields);

  Response<List<Long>> hpexpire(String key, long milliseconds, ExpiryOption condition, String... fields);

  Response<List<Long>> hexpireAt(String key, long unixTimeSeconds, String... fields);

  Response<List<Long>> hexpireAt(String key, long unixTimeSeconds, ExpiryOption condition, String... fields);

  Response<List<Long>> hpexpireAt(String key, long unixTimeMillis, String... fields);

  Response<List<Long>> hpexpireAt(String key, long unixTimeMillis, ExpiryOption condition, String... fields);

  Response<List<Long>> hexpireTime(String key, String... fields);

  Response<List<Long>> hpexpireTime(String key, String... fields);

  Response<List<Long>> httl(String key, String... fields);

  Response<List<Long>> hpttl(String key, String... fields);

  Response<List<Long>> hpersist(String key, String... fields);
}
