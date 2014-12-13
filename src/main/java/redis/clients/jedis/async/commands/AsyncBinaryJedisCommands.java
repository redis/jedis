package redis.clients.jedis.async.commands;

import redis.clients.jedis.Client;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.async.callback.AsyncResponseCallback;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static redis.clients.jedis.Protocol.toByteArray;

public interface AsyncBinaryJedisCommands {
  // Keys section, sorted by dictionary
  void del(final AsyncResponseCallback<Long> callback, final byte[] key);

  void dump(final AsyncResponseCallback<byte[]> callback, final byte[] key);

  void exists(final AsyncResponseCallback<Boolean> callback, final byte[] key);

  void expire(final AsyncResponseCallback<Long> callback, final byte[] key, final int seconds);

  void expireAt(final AsyncResponseCallback<Long> callback, final byte[] key, final long unixTime);

  void migrate(final AsyncResponseCallback<String> callback, final byte[] host, final int port,
      final byte[] key, final int destinationDb, final int timeout);

  void move(final AsyncResponseCallback<Long> callback, final byte[] key, final int dbIndex);

  void persist(final AsyncResponseCallback<Long> callback, final byte[] key);

  void pexpire(final AsyncResponseCallback<Long> callback, final byte[] key, final long milliseconds);

  void pexpireAt(final AsyncResponseCallback<Long> callback, final byte[] key,
      final long millisecondsTimestamp);

  void pttl(final AsyncResponseCallback<Long> callback, final byte[] key);

  void restore(final AsyncResponseCallback<String> callback, final byte[] key, final int ttl,
      final byte[] serializedValue);

  void sort(final AsyncResponseCallback<List<byte[]>> callback, final byte[] key);

  void sort(final AsyncResponseCallback<List<byte[]>> callback, final byte[] key,
      SortingParams sortingParameters);

  void ttl(final AsyncResponseCallback<Long> callback, final byte[] key);

  void type(final AsyncResponseCallback<String> callback, final byte[] key);

  // string section
  void append(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[] value);

  void bitcount(final AsyncResponseCallback<Long> callback, final byte[] key);

  void bitcount(final AsyncResponseCallback<Long> callback, final byte[] key, final long start,
      final long end);

  void decr(final AsyncResponseCallback<Long> callback, final byte[] key);

  void decrBy(final AsyncResponseCallback<Long> callback, final byte[] key, final long integer);

  void get(final AsyncResponseCallback<byte[]> callback, final byte[] key);

  void getbit(final AsyncResponseCallback<Boolean> callback, final byte[] key, final long offset);

  void getrange(final AsyncResponseCallback<byte[]> callback, final byte[] key,
      final long startOffset, final long endOffset);

  void getSet(final AsyncResponseCallback<byte[]> callback, final byte[] key, final byte[] value);

  void incr(final AsyncResponseCallback<Long> callback, final byte[] key);

  void incrBy(final AsyncResponseCallback<Long> callback, final byte[] key, final long integer);

  void incrByFloat(final AsyncResponseCallback<Double> callback, final byte[] key,
      final double increment);

  void psetex(final AsyncResponseCallback<String> callback, final byte[] key,
      final int milliseconds, final byte[] value);

  void set(final AsyncResponseCallback<String> callback, final byte[] key, final byte[] value);

  void set(final AsyncResponseCallback<String> callback, final byte[] key, final byte[] value,
      final byte[] nxxx, final byte[] expx, final long time);

  void setbit(final AsyncResponseCallback<Boolean> callback, final byte[] key, final long offset,
      final boolean value);

  void setbit(final AsyncResponseCallback<Boolean> callback, final byte[] key, final long offset,
      final byte[] value);

  void setnx(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[] value);

  void setex(final AsyncResponseCallback<String> callback, final byte[] key, final int seconds,
      final byte[] value);

  void setrange(final AsyncResponseCallback<Long> callback, final byte[] key, final long offset,
      final byte[] value);

  void strlen(final AsyncResponseCallback<Long> callback, final byte[] key);

  void substr(final AsyncResponseCallback<byte[]> callback, final byte[] key, final int start,
      final int end);

  // hash section
  void hdel(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[]... field);

  void hexists(final AsyncResponseCallback<Boolean> callback, final byte[] key, final byte[] field);

  void hget(final AsyncResponseCallback<byte[]> callback, final byte[] key, final byte[] field);

  void hgetAll(final AsyncResponseCallback<Map<byte[], byte[]>> callback, final byte[] key);

  void hincrBy(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[] field,
      final long value);

  void hkeys(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] key);

  void hlen(final AsyncResponseCallback<Long> callback, final byte[] key);

  void hmget(final AsyncResponseCallback<List<byte[]>> callback, final byte[] key,
      final byte[]... fields);

  void hmset(final AsyncResponseCallback<String> callback, final byte[] key,
      Map<byte[], byte[]> hash);

  void hset(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[] field,
      final byte[] value);

  void hsetnx(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[] field,
      final byte[] value);

  void hvals(final AsyncResponseCallback<List<byte[]>> callback, final byte[] key);

  // list section

  void lindex(final AsyncResponseCallback<byte[]> callback, final byte[] key, final long index);

  void linsert(final AsyncResponseCallback<Long> callback, final byte[] key,
      final Client.LIST_POSITION where, final byte[] pivot, final byte[] value);

  void llen(final AsyncResponseCallback<Long> callback, final byte[] key);

  void lpop(final AsyncResponseCallback<byte[]> callback, final byte[] key);

  void lpush(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[]... args);

  void lpushx(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[]... arg);

  void lrange(final AsyncResponseCallback<List<byte[]>> callback, final byte[] key,
      final long start, final long end);

  void lrem(final AsyncResponseCallback<Long> callback, final byte[] key, final long count,
      final byte[] value);

  void lset(final AsyncResponseCallback<String> callback, final byte[] key, final long index,
      final byte[] value);

  void ltrim(final AsyncResponseCallback<String> callback, final byte[] key, final long start,
      final long end);

  void rpop(final AsyncResponseCallback<byte[]> callback, final byte[] key);

  void rpush(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[]... args);

  void rpushx(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[]... arg);

  // set section

  void sadd(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[]... member);

  void scard(final AsyncResponseCallback<Long> callback, final byte[] key);

  void sismember(final AsyncResponseCallback<Boolean> callback, final byte[] key,
      final byte[] member);

  void smembers(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] key);

  void spop(final AsyncResponseCallback<byte[]> callback, final byte[] key);

  void srandmember(final AsyncResponseCallback<byte[]> callback, final byte[] key);

  void srem(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[]... member);

  // sorted set selection

  void zadd(final AsyncResponseCallback<Long> callback, final byte[] key, final double score,
      final byte[] member);

  void zadd(final AsyncResponseCallback<Long> callback, final byte[] key,
      Map<byte[], Double> scoreMembers);

  void zcard(final AsyncResponseCallback<Long> callback, final byte[] key);

  void zcount(final AsyncResponseCallback<Long> callback, final byte[] key, final double min,
      final double max);

  void zcount(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[] min,
      final byte[] max);

  void zincrby(final AsyncResponseCallback<Double> callback, final byte[] key, final double score,
      final byte[] member);

  void zrange(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] key,
      final long start, final long end);

  void zrangeWithScores(final AsyncResponseCallback<Set<Tuple>> callback, final byte[] key,
      final long start, final long end);

  void zrangeByScore(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] key,
      final double min, final double max);

  void zrangeByScore(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] key,
      final byte[] min, final byte[] max);

  void zrangeByScore(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] key,
      final double min, final double max, final int offset, final int count);

  void zrangeByScore(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] key,
      final byte[] min, final byte[] max, final int offset, final int count);

  void zrangeByScoreWithScores(final AsyncResponseCallback<Set<Tuple>> callback, final byte[] key,
      final double min, final double max);

  void zrangeByScoreWithScores(final AsyncResponseCallback<Set<Tuple>> callback, final byte[] key,
      final double min, final double max, final int offset, final int count);

  void zrangeByScoreWithScores(final AsyncResponseCallback<Set<Tuple>> callback, final byte[] key,
      final byte[] min, final byte[] max, final int offset, final int count);

  void zrangeByScoreWithScores(final AsyncResponseCallback<Set<Tuple>> callback, final byte[] key,
      final byte[] min, final byte[] max);

  void zrank(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[] member);

  void zrem(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[]... member);

  void zremrangeByRank(final AsyncResponseCallback<Long> callback, final byte[] key,
      final long start, final long end);

  void zremrangeByScore(final AsyncResponseCallback<Long> callback, final byte[] key,
      final double start, final double end);

  void zremrangeByScore(final AsyncResponseCallback<Long> callback, final byte[] key,
      final byte[] start, final byte[] end);

  void zrevrange(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] key,
      final long start, final long end);

  void zrevrangeWithScores(final AsyncResponseCallback<Set<Tuple>> callback, final byte[] key,
      final long start, final long end);

  void zrevrangeByScore(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] key,
      final double max, final double min);

  void zrevrangeByScore(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] key,
      final byte[] max, final byte[] min);

  void zrevrangeByScore(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] key,
      final double max, final double min, final int offset, final int count);

  void zrevrangeByScore(final AsyncResponseCallback<Set<byte[]>> callback, final byte[] key,
      final byte[] max, final byte[] min, final int offset, final int count);

  void zrevrangeByScoreWithScores(final AsyncResponseCallback<Set<Tuple>> callback,
      final byte[] key, final double max, final double min);

  void zrevrangeByScoreWithScores(final AsyncResponseCallback<Set<Tuple>> callback,
      final byte[] key, final byte[] max, final byte[] min);

  void zrevrangeByScoreWithScores(final AsyncResponseCallback<Set<Tuple>> callback,
      final byte[] key, final double max, final double min, final int offset, final int count);

  void zrevrangeByScoreWithScores(final AsyncResponseCallback<Set<Tuple>> callback,
      final byte[] key, final byte[] max, byte[] min, final int offset, final int count);

  void zrevrank(final AsyncResponseCallback<Long> callback, final byte[] key, final byte[] member);

  void zscore(final AsyncResponseCallback<Double> callback, final byte[] key, final byte[] member);

}
