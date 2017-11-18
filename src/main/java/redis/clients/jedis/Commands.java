package redis.clients.jedis;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

import java.util.Map;

public interface Commands {

  void set(String key, String value);

  public void set(final String key, final String value, final String nxxx, final String expx,
      final long time);

  void get(String key);

  public void exists(final String key);

  void exists(String... keys);

  void del(String... keys);

  void type(String key);

  void keys(String pattern);

  void rename(String oldkey, String newkey);

  void renamenx(String oldkey, String newkey);

  void expire(String key, int seconds);

  void expireAt(String key, long unixTime);

  void ttl(String key);

  void pttl(final String key);

  void setbit(String key, long offset, boolean value);

  void setbit(String key, long offset, String value);

  void getbit(String key, long offset);

  void setrange(String key, long offset, String value);

  void getrange(String key, long startOffset, long endOffset);

  void move(String key, int dbIndex);

  void getSet(String key, String value);

  void mget(String... keys);

  void setnx(String key, String value);

  void setex(String key, int seconds, String value);

  void mset(String... keysvalues);

  void msetnx(String... keysvalues);

  void decrBy(String key, long integer);

  void decr(String key);

  void incrBy(String key, long integer);

  void incrByFloat(String key, double value);

  void incr(String key);

  void append(String key, String value);

  void substr(String key, int start, int end);

  void hset(String key, String field, String value);

  void hget(String key, String field);

  void hsetnx(String key, String field, String value);

  void hmset(String key, Map<String, String> hash);

  void hmget(String key, String... fields);

  void hincrBy(String key, String field, long value);

  void hincrByFloat(String key, String field, double value);

  void hexists(String key, String field);

  void hdel(String key, String... fields);

  void hlen(String key);

  void hkeys(String key);

  void hvals(String key);

  void hgetAll(String key);

  void rpush(String key, String... strings);

  void lpush(String key, String... strings);

  void llen(String key);

  void lrange(String key, long start, long end);

  void ltrim(String key, long start, long end);

  void lindex(String key, long index);

  void lset(String key, long index, String value);

  void lrem(String key, long count, String value);

  void lpop(String key);

  void rpop(String key);

  void rpoplpush(String srckey, String dstkey);

  void sadd(String key, String... members);

  void smembers(String key);

  void srem(String key, String... member);

  void spop(String key);

  void spop(String key, long count);

  void smove(String srckey, String dstkey, String member);

  void scard(String key);

  void sismember(String key, String member);

  void sinter(String... keys);

  void sinterstore(String dstkey, String... keys);

  void sunion(String... keys);

  void sunionstore(String dstkey, String... keys);

  void sdiff(String... keys);

  void sdiffstore(String dstkey, String... keys);

  void srandmember(String key);

  void zadd(String key, double score, String member);

  void zadd(String key, double score, String member, ZAddParams params);

  void zadd(String key, Map<String, Double> scoreMembers);

  void zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

  void zrange(String key, long start, long end);

  void zrem(String key, String... members);

  void zincrby(String key, double score, String member);

  void zincrby(String key, double score, String member, ZIncrByParams params);

  void zrank(String key, String member);

  void zrevrank(String key, String member);

  void zrevrange(String key, long start, long end);

  void zrangeWithScores(String key, long start, long end);

  void zrevrangeWithScores(String key, long start, long end);

  void zcard(String key);

  void zscore(String key, String member);

  void watch(String... keys);

  void sort(String key);

  void sort(String key, SortingParams sortingParameters);

  void blpop(String[] args);

  void sort(String key, SortingParams sortingParameters, String dstkey);

  void sort(String key, String dstkey);

  void brpop(String[] args);

  void brpoplpush(String source, String destination, int timeout);

  void zcount(String key, double min, double max);

  void zcount(String key, String min, String max);

  void zrangeByScore(String key, double min, double max);

  void zrangeByScore(String key, String min, String max);

  void zrangeByScore(String key, double min, double max, int offset,
      int count);

  void zrangeByScoreWithScores(String key, double min, double max);

  void zrangeByScoreWithScores(String key, double min, double max,
      int offset, int count);

  void zrangeByScoreWithScores(String key, String min, String max);

  void zrangeByScoreWithScores(String key, String min, String max,
      int offset, int count);

  void zrevrangeByScore(String key, double max, double min);

  void zrevrangeByScore(String key, String max, String min);

  void zrevrangeByScore(String key, double max, double min, int offset,
      int count);

  void zrevrangeByScoreWithScores(String key, double max, double min);

  void zrevrangeByScoreWithScores(String key, double max, double min,
      int offset, int count);

  void zrevrangeByScoreWithScores(String key, String max, String min);

  void zrevrangeByScoreWithScores(String key, String max, String min,
      int offset, int count);

  void zremrangeByRank(String key, long start, long end);

  void zremrangeByScore(String key, double start, double end);

  void zremrangeByScore(String key, String start, String end);

  void zunionstore(String dstkey, String... sets);

  void zunionstore(String dstkey, ZParams params, String... sets);

  void zinterstore(String dstkey, String... sets);

  void zinterstore(String dstkey, ZParams params, String... sets);

  void strlen(String key);

  void lpushx(String key, String... string);

  void persist(String key);

  void rpushx(String key, String... string);

  void echo(String string);

  void linsert(String key, LIST_POSITION where, String pivot, String value);

  void bgrewriteaof();

  void bgsave();

  void lastsave();

  void save();

  void configSet(String parameter, String value);

  void configGet(String pattern);

  void configResetStat();

  void multi();

  void exec();

  void discard();

  void objectRefcount(String key);

  void objectIdletime(String key);

  void objectEncoding(String key);

  void bitcount(String key);

  void bitcount(String key, long start, long end);

  void bitop(BitOP op, String destKey, String... srcKeys);

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  public void scan(int cursor, final ScanParams params);

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  public void hscan(final String key, int cursor, final ScanParams params);

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  public void sscan(final String key, int cursor, final ScanParams params);

  @Deprecated
  /**
   * This method is deprecated due to bug (scan cursor should be unsigned long)
   * And will be removed on next major release
   * @see https://github.com/xetorthio/jedis/issues/531 
   */
  public void zscan(final String key, int cursor, final ScanParams params);

  void scan(String cursor, ScanParams params);

  void hscan(String key, String cursor, ScanParams params);

  void sscan(String key, String cursor, ScanParams params);

  void zscan(String key, String cursor, ScanParams params);

  void waitReplicas(int replicas, long timeout);

  /**
   * Used for BITFIELD Redis command
   * @param key
   * @param arguments
   */
  void bitfield(String key, String... arguments);
}
