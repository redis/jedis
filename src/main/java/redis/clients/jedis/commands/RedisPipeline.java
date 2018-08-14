package redis.clients.jedis.commands;

import redis.clients.jedis.GeoCoordinate;
import redis.clients.jedis.GeoRadiusResponse;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.ListPosition;
import redis.clients.jedis.Response;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.params.GeoRadiusParam;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RedisPipeline {
  Response<Long> append(String key, String value);

  Response<List<String>> blpop(String arg);

  Response<List<String>> brpop(String arg);

  Response<Long> decr(String key);

  Response<Long> decrBy(String key, long decrement);

  Response<Long> del(String key);

  Response<Long> unlink(String key);

  Response<String> echo(String string);

  Response<Boolean> exists(String key);

  Response<Long> expire(String key, int seconds);

  Response<Long> pexpire(String key, long milliseconds);

  Response<Long> expireAt(String key, long unixTime);

  Response<Long> pexpireAt(String key, long millisecondsTimestamp);

  Response<String> get(String key);

  Response<Boolean> getbit(String key, long offset);

  Response<String> getrange(String key, long startOffset, long endOffset);

  Response<String> getSet(String key, String value);

  Response<Long> hdel(String key, String... field);

  Response<Boolean> hexists(String key, String field);

  Response<String> hget(String key, String field);

  Response<Map<String, String>> hgetAll(String key);

  Response<Long> hincrBy(String key, String field, long value);

  Response<Set<String>> hkeys(String key);

  Response<Long> hlen(String key);

  Response<List<String>> hmget(String key, String... fields);

  Response<String> hmset(String key, Map<String, String> hash);

  Response<Long> hset(String key, String field, String value);

  Response<Long> hset(String key, Map<String, String> hash);

  Response<Long> hsetnx(String key, String field, String value);

  Response<List<String>> hvals(String key);

  Response<Long> incr(String key);

  Response<Long> incrBy(String key, long increment);

  Response<String> lindex(String key, long index);

  Response<Long> linsert(String key, ListPosition where, String pivot, String value);

  Response<Long> llen(String key);

  Response<String> lpop(String key);

  Response<Long> lpush(String key, String... string);

  Response<Long> lpushx(String key, String... string);

  Response<List<String>> lrange(String key, long start, long stop);

  Response<Long> lrem(String key, long count, String value);

  Response<String> lset(String key, long index, String value);

  Response<String> ltrim(String key, long start, long stop);

  Response<Long> move(String key, int dbIndex);

  Response<Long> persist(String key);

  Response<String> rpop(String key);

  Response<Long> rpush(String key, String... string);

  Response<Long> rpushx(String key, String... string);

  Response<Long> sadd(String key, String... member);

  Response<Long> scard(String key);

  Response<Boolean> sismember(String key, String member);

  Response<String> set(String key, String value);

  Response<Boolean> setbit(String key, long offset, boolean value);

  Response<String> setex(String key, int seconds, String value);

  Response<Long> setnx(String key, String value);

  Response<Long> setrange(String key, long offset, String value);

  Response<Set<String>> smembers(String key);

  Response<List<String>> sort(String key);

  Response<List<String>> sort(String key, SortingParams sortingParameters);

  Response<String> spop(String key);

  Response<Set<String>> spop(String key, long count);

  Response<String> srandmember(String key);

  Response<Long> srem(String key, String... member);

  Response<Long> strlen(String key);

  Response<String> substr(String key, int start, int end);

  Response<Long> touch(String key);

  Response<Long> ttl(String key);

  Response<Long> pttl(String key);

  Response<String> type(String key);

  Response<Long> zadd(String key, double score, String member);

  Response<Long> zadd(String key, double score, String member, ZAddParams params);

  Response<Long> zadd(String key, Map<String, Double> scoreMembers);

  Response<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

  Response<Long> zcard(String key);

  Response<Long> zcount(String key, double min, double max);

  Response<Long> zcount(String key, String min, String max);

  Response<Double> zincrby(String key, double increment, String member);

  Response<Double> zincrby(String key, double increment, String member, ZIncrByParams params);

  Response<Set<String>> zrange(String key, long start, long stop);

  Response<Set<String>> zrangeByScore(String key, double min, double max);

  Response<Set<String>> zrangeByScore(String key, String min, String max);

  Response<Set<String>> zrangeByScore(String key, double min, double max, int offset, int count);

  Response<Set<String>> zrangeByScore(String key, String min, String max, int offset, int count);

  Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max);

  Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset,
      int count);

  Response<Set<String>> zrevrangeByScore(String key, double max, double min);

  Response<Set<String>> zrevrangeByScore(String key, String max, String min);

  Response<Set<String>> zrevrangeByScore(String key, double max, double min, int offset, int count);

  Response<Set<String>> zrevrangeByScore(String key, String max, String min, int offset, int count);

  Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min);

  Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min);

  Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset,
      int count);

  Response<Set<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min, int offset,
      int count);

  Response<Set<Tuple>> zrangeWithScores(String key, long start, long stop);

  Response<Long> zrank(String key, String member);

  Response<Long> zrem(String key, String... members);

  Response<Long> zremrangeByRank(String key, long start, long stop);

  Response<Long> zremrangeByScore(String key, double min, double max);

  Response<Long> zremrangeByScore(String key, String min, String max);

  Response<Set<String>> zrevrange(String key, long start, long stop);

  Response<Set<Tuple>> zrevrangeWithScores(String key, long start, long stop);

  Response<Long> zrevrank(String key, String member);

  Response<Double> zscore(String key, String member);

  Response<Long> zlexcount(String key, String min, String max);

  Response<Set<String>> zrangeByLex(String key, String min, String max);

  Response<Set<String>> zrangeByLex(String key, String min, String max,
      int offset, int count);

  Response<Set<String>> zrevrangeByLex(String key, String max, String min);

  Response<Set<String>> zrevrangeByLex(String key, String max, String min,
      int offset, int count);

  Response<Long> zremrangeByLex(String key, String min, String max);

  Response<Long> bitcount(String key);

  Response<Long> bitcount(String key, long start, long end);

  Response<Long> pfadd(String key, String... elements);

  Response<Long> pfcount(String key);
  
  Response<List<Long>> bitfield(String key, String... arguments);
  
  Response<Long> hstrlen(String key, String field);

  Response<byte[]> dump(String key);

  Response<String> restore(String key, int ttl, byte[] serializedValue);

  Response<String> restoreReplace(String key, int ttl, byte[] serializedValue);

  Response<String> migrate(String host, int port, String key, int destinationDB, int timeout);

  // Geo Commands

  Response<Long> geoadd(String key, double longitude, double latitude, String member);

  Response<Long> geoadd(String key, Map<String, GeoCoordinate> memberCoordinateMap);

  Response<Double> geodist(String key, String member1, String member2);

  Response<Double> geodist(String key, String member1, String member2, GeoUnit unit);

  Response<List<String>> geohash(String key, String... members);

  Response<List<GeoCoordinate>> geopos(String key, String... members);

  Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude,
      double radius, GeoUnit unit);

  Response<List<GeoRadiusResponse>> georadiusReadonly(String key, double longitude, double latitude,
      double radius, GeoUnit unit);

  Response<List<GeoRadiusResponse>> georadius(String key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param);

  Response<List<GeoRadiusResponse>> georadiusReadonly(String key, double longitude, double latitude,
      double radius, GeoUnit unit, GeoRadiusParam param);

  Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius,
      GeoUnit unit);

  Response<List<GeoRadiusResponse>> georadiusByMemberReadonly(String key, String member, double radius,
      GeoUnit unit);

  Response<List<GeoRadiusResponse>> georadiusByMember(String key, String member, double radius,
      GeoUnit unit, GeoRadiusParam param);

  Response<List<GeoRadiusResponse>> georadiusByMemberReadonly(String key, String member, double radius,
      GeoUnit unit, GeoRadiusParam param);
}
