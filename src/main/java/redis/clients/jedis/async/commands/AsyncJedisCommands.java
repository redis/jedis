package redis.clients.jedis.async.commands;

import redis.clients.jedis.Client;
import redis.clients.jedis.ScanResult;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.async.callback.AsyncResponseCallback;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Common interface for sharded and non-sharded Jedis
 */
public interface AsyncJedisCommands {
  // Keys section, sorted by dictionary

  void del(AsyncResponseCallback<Long> callback, String key);

  void dump(AsyncResponseCallback<byte[]> callback, String key);

  void exists(AsyncResponseCallback<Boolean> callback, String key);

  void expire(AsyncResponseCallback<Long> callback, String key, int seconds);

  void expireAt(AsyncResponseCallback<Long> callback, String key, long unixTime);

  void migrate(final AsyncResponseCallback<String> callback, final String host, final int port,
      final String key, final int destinationDb, final int timeout);

  void move(AsyncResponseCallback<Long> callback, String key, int dbIndex);

  void persist(AsyncResponseCallback<Long> callback, String key);

  void pexpire(final AsyncResponseCallback<Long> callback, final String key, final long milliseconds);

  void pexpireAt(final AsyncResponseCallback<Long> callback, final String key,
      final long millisecondsTimestamp);

  void pttl(final AsyncResponseCallback<Long> callback, final String key);

  void restore(final AsyncResponseCallback<String> callback, final String key, final int ttl,
      final byte[] serializedValue);

  void sort(AsyncResponseCallback<List<String>> callback, String key);

  void sort(AsyncResponseCallback<List<String>> callback, String key,
      SortingParams sortingParameters);

  void ttl(AsyncResponseCallback<Long> callback, String key);

  void type(AsyncResponseCallback<String> callback, String key);

  // string section

  void append(AsyncResponseCallback<Long> callback, String key, String value);

  void bitcount(AsyncResponseCallback<Long> callback, final String key);

  void bitcount(AsyncResponseCallback<Long> callback, final String key, long start, long end);

  void decr(AsyncResponseCallback<Long> callback, String key);

  void decrBy(AsyncResponseCallback<Long> callback, String key, long integer);

  void get(AsyncResponseCallback<String> callback, String key);

  void getbit(AsyncResponseCallback<Boolean> callback, String key, long offset);

  void getrange(AsyncResponseCallback<String> callback, String key, long startOffset, long endOffset);

  void getSet(AsyncResponseCallback<String> callback, String key, String value);

  void incr(AsyncResponseCallback<Long> callback, String key);

  void incrBy(AsyncResponseCallback<Long> callback, String key, long integer);

  void incrByFloat(final AsyncResponseCallback<Double> callback, final String key,
      final double increment);

  void psetex(final AsyncResponseCallback<String> callback, final String key,
      final int milliseconds, final String value);

  void set(AsyncResponseCallback<String> callback, String key, String value);

  void set(final AsyncResponseCallback<String> callback, final String key, final String value,
      final String nxxx, final String expx, final long time);

  void setbit(AsyncResponseCallback<Boolean> callback, String key, long offset, boolean value);

  void setbit(AsyncResponseCallback<Boolean> callback, String key, long offset, String value);

  void setnx(AsyncResponseCallback<Long> callback, String key, String value);

  void setex(AsyncResponseCallback<String> callback, String key, int seconds, String value);

  void setrange(AsyncResponseCallback<Long> callback, String key, long offset, String value);

  void strlen(AsyncResponseCallback<Long> callback, String key);

  void substr(AsyncResponseCallback<String> callback, String key, int start, int end);

  // hash section

  void hdel(AsyncResponseCallback<Long> callback, String key, String... field);

  void hexists(AsyncResponseCallback<Boolean> callback, String key, String field);

  void hget(AsyncResponseCallback<String> callback, String key, String field);

  void hgetAll(AsyncResponseCallback<Map<String, String>> callback, String key);

  void hincrBy(AsyncResponseCallback<Long> callback, String key, String field, long value);

  void hkeys(AsyncResponseCallback<Set<String>> callback, String key);

  void hlen(AsyncResponseCallback<Long> callback, String key);

  void hmget(AsyncResponseCallback<List<String>> callback, String key, String... fields);

  void hmset(AsyncResponseCallback<String> callback, String key, Map<String, String> hash);

  void hset(AsyncResponseCallback<Long> callback, String key, String field, String value);

  void hsetnx(AsyncResponseCallback<Long> callback, String key, String field, String value);

  void hvals(AsyncResponseCallback<List<String>> callback, String key);

  // list section

  void lindex(AsyncResponseCallback<String> callback, String key, long index);

  void linsert(AsyncResponseCallback<Long> callback, String key, Client.LIST_POSITION where,
      String pivot, String value);

  void llen(AsyncResponseCallback<Long> callback, String key);

  void lpop(AsyncResponseCallback<String> callback, String key);

  void lpush(AsyncResponseCallback<Long> callback, String key, String... strings);

  void lpushx(AsyncResponseCallback<Long> callback, String key, String... strings);

  void lrange(AsyncResponseCallback<List<String>> callback, String key, long start, long end);

  void lrem(AsyncResponseCallback<Long> callback, String key, long count, String value);

  void lset(AsyncResponseCallback<String> callback, String key, long index, String value);

  void ltrim(AsyncResponseCallback<String> callback, String key, long start, long end);

  void rpop(AsyncResponseCallback<String> callback, String key);

  void rpush(AsyncResponseCallback<Long> callback, String key, String... strings);

  void rpushx(AsyncResponseCallback<Long> callback, String key, String... strings);

  // set section

  void sadd(AsyncResponseCallback<Long> callback, String key, String... members);

  void scard(AsyncResponseCallback<Long> callback, String key);

  void sismember(AsyncResponseCallback<Boolean> callback, String key, String members);

  void smembers(AsyncResponseCallback<Set<String>> callback, String key);

  void spop(AsyncResponseCallback<String> callback, String key);

  void srandmember(AsyncResponseCallback<String> callback, String key);

  void srem(AsyncResponseCallback<Long> callback, String key, String... members);

  // sorted set section

  void zadd(AsyncResponseCallback<Long> callback, String key, double score, String member);

  void zadd(AsyncResponseCallback<Long> callback, String key, Map<String, Double> scoreMembers);

  void zcard(AsyncResponseCallback<Long> callback, String key);

  void zcount(AsyncResponseCallback<Long> callback, String key, double min, double max);

  void zcount(AsyncResponseCallback<Long> callback, String key, String min, String max);

  void zincrby(AsyncResponseCallback<Double> callback, String key, double score, String member);

  void zrange(AsyncResponseCallback<Set<String>> callback, String key, long start, long end);

  void zrangeWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key, long start, long end);

  void zrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, double min, double max);

  void zrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, String min, String max);

  void zrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, double min,
      double max, int offset, int count);

  void zrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, String min,
      String max, int offset, int count);

  void zrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key, double min,
      double max);

  void zrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key, double min,
      double max, int offset, int count);

  void zrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key, String min,
      String max, int offset, int count);

  void zrank(AsyncResponseCallback<Long> callback, String key, String member);

  void zrem(AsyncResponseCallback<Long> callback, String key, String... members);

  void zremrangeByRank(AsyncResponseCallback<Long> callback, String key, long start, long end);

  void zremrangeByScore(AsyncResponseCallback<Long> callback, String key, double start, double end);

  void zremrangeByScore(AsyncResponseCallback<Long> callback, String key, String start, String end);

  void zrevrange(AsyncResponseCallback<Set<String>> callback, String key, long start, long end);

  void zrevrangeWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key, long start,
      long end);

  void zrevrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, double max,
      double min);

  void zrevrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, String max,
      String min);

  void zrevrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, double max,
      double min, int offset, int count);

  void zrevrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      double max, double min);

  void zrevrangeByScore(AsyncResponseCallback<Set<String>> callback, String key, String max,
      String min, int offset, int count);

  void zrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key, String min,
      String max);

  void zrevrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      String max, String min);

  void zrevrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      double max, double min, int offset, int count);

  void zrevrangeByScoreWithScores(AsyncResponseCallback<Set<Tuple>> callback, String key,
      String max, String min, int offset, int count);

  void zrevrank(AsyncResponseCallback<Long> callback, String key, String member);

  void zscore(AsyncResponseCallback<Double> callback, String key, String member);

}
