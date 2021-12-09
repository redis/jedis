package redis.clients.jedis.tairhash;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import redis.clients.jedis.Response;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface TairHashPipelineCommands {
  Response<Long> exhset(String key, String field, String value);

  Response<Long> exhset(String key, String field, String value, ExhsetParams params);

  Response<String> exhget(String key, String field);

  Response<String> exhmset(String key, Map<String, String> hash);

  Response<Long> exhexpire(String key, String field, long seconds);

  Response<Long> exhexpireWithVer(String key, String field, long seconds, long version);

  Response<Long> exhexpireWithAbsVer(String key, String field, long seconds, long absVer);

  Response<Long> exhexpireAt(String key, String field, long unixTime);

  Response<Long> exhexpireAtWithVer(String key, String field, long unixTime, long version);

  Response<Long> exhexpireAtWithAbsVer(String key, String field, long unixTime, long absVer);

  Response<Long> exhpexpire(String key, String field, long milliseconds);

  Response<Long> exhpexpireWithVer(String key, String field, long milliseconds, long version);

  Response<Long> exhpexpireWithAbsVer(String key, String field, long milliseconds, long absVer);

  Response<Long> exhpexpireAt(String key, String field, long unixTime);

  Response<Long> exhpexpireAtWithVer(String key, String field, long unixTime, long version);

  Response<Long> exhpexpireAtWithAbsVer(String key, String field, long unixTime, long absVer);

  Response<Long> exhttl(String key, String field);

  Response<Long> exhpttl(String key, String field);

  Response<Long> exhver(String key, String field);

  Response<Long> exhsetVer(String key, String field, long version);

  Response<Long> exhincrBy(String key, String field, long value);

  Response<Long> exhincrBy(String key, String field, long value, ExhincrByParams<Long> params);

  Response<Double> exhincrByFloat(String key, String field, double value);

  Response<Double> exhincrByFloat(String key, String field, double value,
      ExhincrByParams<Double> params);

  Response<ExhgetWithVerResult<String>> exhgetWithVer(String key, String field);

  Response<List<String>> exhmget(String key, String... fields);

  Response<List<ExhgetWithVerResult<String>>> exhmgetWithVer(String key, String... fields);

  Response<Long> exhdel(String key, String... fields);

  Response<Long> exhlen(String key);

  Response<Long> exhlenNoExp(String key);

  Response<Long> exhexists(String key, String field);

  Response<Long> exhstrlen(String key, String field);

  Response<List<String>> exhkeys(String key);

  Response<List<String>> exhvals(String key);

  Response<Map<String, String>> exhgetall(String key);

  Response<ScanResult<Entry<String, String>>> exhscan(String key, String cursor, ScanParams params);
}
