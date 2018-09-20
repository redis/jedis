package redis.clients.jedis.commands;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.ListPosition;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.SetParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface JedisClusterCommands {
  String set(String key, String value);

  String set(String key, String value, SetParams params);

  String get(String key);

  Boolean exists(String key);

  Long persist(String key);

  String type(String key);

  byte[] dump(String key);

  String restore(String key, int ttl, byte[] serializedValue);

  Long expire(String key, int seconds);

  Long pexpire(String key, long milliseconds);

  Long expireAt(String key, long unixTime);

  Long pexpireAt(String key, long millisecondsTimestamp);

  Long ttl(String key);

  Long pttl(String key);

  Long touch(String key);

  Boolean setbit(String key, long offset, boolean value);

  Boolean setbit(String key, long offset, String value);

  Boolean getbit(String key, long offset);

  Long setrange(String key, long offset, String value);

  String getrange(String key, long startOffset, long endOffset);

  String getSet(String key, String value);

  Long setnx(String key, String value);

  String setex(String key, int seconds, String value);

  String psetex(String key, long milliseconds, String value);

  Long decrBy(String key, long decrement);

  Long decr(String key);

  Long incrBy(String key, long increment);

  Double incrByFloat(String key, double increment);

  Long incr(String key);

  Long append(String key, String value);

  String substr(String key, int start, int end);

  Long hset(String key, String field, String value);

  Long hset(String key, Map<String, String> hash);

  String hget(String key, String field);

  Long hsetnx(String key, String field, String value);

  String hmset(String key, Map<String, String> hash);

  List<String> hmget(String key, String... fields);

  Long hincrBy(String key, String field, long value);

  Boolean hexists(String key, String field);

  Long hdel(String key, String... field);

  Long hlen(String key);

  Set<String> hkeys(String key);

  List<String> hvals(String key);

  Map<String, String> hgetAll(String key);

  Long rpush(String key, String... string);

  Long lpush(String key, String... string);

  Long llen(String key);

  List<String> lrange(String key, long start, long stop);

  String ltrim(String key, long start, long stop);

  String lindex(String key, long index);

  String lset(String key, long index, String value);

  Long lrem(String key, long count, String value);

  String lpop(String key);

  String rpop(String key);

  Long sadd(String key, String... member);

  Set<String> smembers(String key);

  Long srem(String key, String... member);

  String spop(String key);

  Set<String> spop(String key, long count);

  Long scard(String key);

  Boolean sismember(String key, String member);

  String srandmember(String key);

  List<String> srandmember(String key, int count);

  Long strlen(String key);

  Long zadd(String key, double score, String member);

  Long zadd(String key, double score, String member, ZAddParams params);

  Long zadd(String key, Map<String, Double> scoreMembers);

  Long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

  Set<String> zrange(String key, long start, long stop);

  Long zrem(String key, String... members);

  Double zincrby(String key, double increment, String member);

  Double zincrby(String key, double increment, String member, ZIncrByParams params);

  Long zrank(String key, String member);

  Long zrevrank(String key, String member);

  Set<String> zrevrange(String key, long start, long stop);

  Set<Tuple> zrangeWithScores(String key, long start, long stop);

  Set<Tuple> zrevrangeWithScores(String key, long start, long stop);

  Long zcard(String key);

  Double zscore(String key, String member);

  List<String> sort(String key);

  List<String> sort(String key, SortingParams sortingParameters);

  Long zcount(String key, double min, double max);

  Long zcount(String key, String min, String max);

  Set<String> zrangeByScore(String key, double min, double max);

  Set<String> zrangeByScore(String key, String min, String max);

  Set<String> zrevrangeByScore(String key, double max, double min);

  Set<String> zrangeByScore(String key, double min, double max, int offset, int count);

  Set<String> zrevrangeByScore(String key, String max, String min);

  Set<String> zrangeByScore(String key, String min, String max, int offset, int count);

  Set<String> zrevrangeByScore(String key, double max, double min, int offset, int count);

  Set<Tuple> zrangeByScoreWithScores(String key, double min, double max);

  Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min);

  Set<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count);

  Set<String> zrevrangeByScore(String key, String max, String min, int offset, int count);

  Set<Tuple> zrangeByScoreWithScores(String key, String min, String max);

  Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min);

  Set<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count);

  Set<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count);

  Set<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count);

  Long zremrangeByRank(String key, long start, long stop);

  Long zremrangeByScore(String key, double min, double max);

  Long zremrangeByScore(String key, String min, String max);

  Long zlexcount(String key, String min, String max);

  Set<String> zrangeByLex(String key, String min, String max);

  Set<String> zrangeByLex(String key, String min, String max, int offset,
      int count);

  Set<String> zrevrangeByLex(String key, String max, String min);

  Set<String> zrevrangeByLex(String key, String max, String min,
      int offset, int count);

  Long zremrangeByLex(String key, String min, String max);

  Long linsert(String key, ListPosition where, String pivot, String value);

  Long lpushx(String key, String... string);

  Long rpushx(String key, String... string);

  List<String> blpop(int timeout, String key);

  List<String> brpop(int timeout, String key);

  Long del(String key);

  Long unlink(String key);

  String echo(String string);

  Long bitcount(String key);

  Long bitcount(String key, long start, long end);

  ScanResult<Map.Entry<String, String>> hscan(String key, String cursor);

  ScanResult<String> sscan(String key, String cursor);

  ScanResult<Tuple> zscan(String key, String cursor);

  Long pfadd(String key, String... elements);

  long pfcount(String key);

  // Geo Commands

  Long geoadd(String key, double longitude, double latitude, String member);

  Long geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap);

  Double geodist(String key, String member1, String member2);

  Double geodist(String key, String member1, String member2, GeoUnit unit);

  List<String> geohash(String key, String... members);

  List<GeoCoordinate> geopos(String key, String... members);

  List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius,
      GeoUnit unit);

  List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude, double radius,
      GeoUnit unit);

  List<GeoRadiusResponse> georadius(String key, double longitude, double latitude, double radius,
      GeoUnit unit, GeoRadiusParam param);

  List<GeoRadiusResponse> georadiusReadonly(String key, double longitude, double latitude, double radius,
      GeoUnit unit, GeoRadiusParam param);

  List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit);

  List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit);

  List<GeoRadiusResponse> georadiusByMember(String key, String member, double radius, GeoUnit unit,
      GeoRadiusParam param);

  List<GeoRadiusResponse> georadiusByMemberReadonly(String key, String member, double radius, GeoUnit unit,
      GeoRadiusParam param);

  /**
   * Executes BITFIELD Redis command
   * @param key
   * @param arguments
   */
  List<Long> bitfield(String key, String...arguments);
  
  /**
   * Used for HSTRLEN Redis command
   * @param key 
   * @param field
   * @return lenth of the value for key
   */
  Long hstrlen(String key, String field);
  
  Long waitReplicas(final String key, final int replicas, final long timeout);
}
