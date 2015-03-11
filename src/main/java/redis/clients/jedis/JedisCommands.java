package redis.clients.jedis;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Common interface for sharded and non-sharded Jedis
 */
public interface JedisCommands {
  String set(String key, String value);

  String set(String key, String value, String nxxx, String expx, long time);

  String get(String key);

  Boolean exists(String key);

  Long persist(String key);

  String type(String key);

  Long expire(String key, int seconds);

  Long pexpire(String key, long milliseconds);

  Long expireAt(String key, long unixTime);

  Long pexpireAt(String key, long millisecondsTimestamp);

  Long ttl(String key);

  Boolean setbit(String key, long offset, boolean value);

  Boolean setbit(String key, long offset, String value);

  Boolean getbit(String key, long offset);

  Long setrange(String key, long offset, String value);

  String getrange(String key, long startOffset, long endOffset);

  String getSet(String key, String value);

  Long setnx(String key, String value);

  String setex(String key, int seconds, String value);

  Long decrBy(String key, long integer);

  Long decr(String key);

  Long incrBy(String key, long integer);

  Double incrByFloat(String key, double value);

  Long incr(String key);

  Long append(String key, String value);

  String substr(String key, int start, int end);

  Long hset(String key, String field, String value);

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

  List<String> lrange(String key, long start, long end);

  String ltrim(String key, long start, long end);

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

  Long zadd(String key, Map<String, Double> scoreMembers);

  Set<String> zrange(String key, long start, long end);

  Long zrem(String key, String... member);

  Double zincrby(String key, double score, String member);

  Long zrank(String key, String member);

  Long zrevrank(String key, String member);

  Set<String> zrevrange(String key, long start, long end);

  Set<Tuple> zrangeWithScores(String key, long start, long end);

  Set<Tuple> zrevrangeWithScores(String key, long start, long end);

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

  Long zremrangeByRank(String key, long start, long end);

  Long zremrangeByScore(String key, double start, double end);

  Long zremrangeByScore(String key, String start, String end);

  Long zlexcount(final String key, final String min, final String max);

  Set<String> zrangeByLex(final String key, final String min, final String max);

  Set<String> zrangeByLex(final String key, final String min, final String max, final int offset,
      final int count);

  Set<String> zrevrangeByLex(final String key, final String max, final String min);

  Set<String> zrevrangeByLex(final String key, final String max, final String min,
      final int offset, final int count);

  Long zremrangeByLex(final String key, final String min, final String max);

  Long linsert(String key, Client.LIST_POSITION where, String pivot, String value);

  Long lpushx(String key, String... string);

  Long rpushx(String key, String... string);

  /**
   * @deprecated unusable command, this will be removed in 3.0.0.
   */
  @Deprecated
  List<String> blpop(String arg);

  List<String> blpop(int timeout, String key);

  /**
   * @deprecated unusable command, this will be removed in 3.0.0.
   */
  @Deprecated
  List<String> brpop(String arg);

  List<String> brpop(int timeout, String key);

  Long del(String key);

  String echo(String string);

  Long move(String key, int dbIndex);

  Long bitcount(final String key);

  Long bitcount(final String key, long start, long end);

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  ScanResult<Map.Entry<String, String>> hscan(final String key, int cursor);

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  ScanResult<String> sscan(final String key, int cursor);

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  ScanResult<Tuple> zscan(final String key, int cursor);

  ScanResult<Map.Entry<String, String>> hscan(final String key, final String cursor);

  ScanResult<String> sscan(final String key, final String cursor);

  ScanResult<Tuple> zscan(final String key, final String cursor);

  Long pfadd(final String key, final String... elements);

  long pfcount(final String key);

}
