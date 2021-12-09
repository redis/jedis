package redis.clients.jedis.tairhash;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public interface TairHashCommands {

  long exhset(String key, String field, String value);

  long exhset(String key, String field, String value, ExhsetParams params);

  String exhget(String key, String field);

  String exhmset(String key, Map<String, String> hash);

  long exhexpire(String key, String field, long seconds);

  long exhexpireWithVer(String key, String field, long seconds, long version);

  long exhexpireWithAbsVer(String key, String field, long seconds, long absVer);

  long exhexpireAt(String key, String field, long unixTime);

  long exhexpireAtWithVer(String key, String field, long unixTime, long version);

  long exhexpireAtWithAbsVer(String key, String field, long unixTime, long absVer);

  long exhpexpire(String key, String field, long milliseconds);

  long exhpexpireWithVer(String key, String field, long milliseconds, long version);

  long exhpexpireWithAbsVer(String key, String field, long milliseconds, long absVer);

  long exhpexpireAt(String key, String field, long unixTime);

  long exhpexpireAtWithVer(String key, String field, long unixTime, long version);

  long exhpexpireAtWithAbsVer(String key, String field, long unixTime, long absVer);

  long exhttl(String key, String field);

  long exhpttl(String key, String field);

  long exhver(String key, String field);

  long exhsetVer(String key, String field, long version);

  long exhincrBy(String key, String field, long value);

  long exhincrBy(String key, String field, long value, ExhincrByParams<Long> params);

  double exhincrByFloat(String key, String field, double value);

  double exhincrByFloat(String key, String field, double value, ExhincrByParams<Double> params);

  ExhgetWithVerResult<String> exhgetWithVer(String key, String field);

  List<String> exhmget(String key, String... fields);

  List<ExhgetWithVerResult<String>> exhmgetWithVer(String key, String... fields);

  long exhdel(String key, String... fields);

  long exhlen(String key);

  long exhlenNoExp(String key);

  long exhexists(String key, String field);

  long exhstrlen(String key, String field);

  List<String> exhkeys(String key);

  List<String> exhvals(String key);

  Map<String, String> exhgetall(String key);

  ScanResult<Map.Entry<String, String>> exhscan(String key, String cursor, ScanParams params);
}
