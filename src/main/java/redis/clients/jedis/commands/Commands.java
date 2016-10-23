package redis.clients.jedis.commands;

import java.util.Map;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.jedis.BitOP;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.ZParams;
import redis.clients.jedis.params.set.SetParams;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

public interface Commands {

  void set(final String key, final String value);

  void set(final String key, final String value, SetParams params);

  void get(final String key);

  void exists(final String key);

  void exists(final String... keys);

  void del(final String... keys);

  void type(final String key);

  void keys(final String pattern);

  void rename(final String oldkey, final String newkey);

  void renamenx(final String oldkey, final String newkey);

  void expire(final String key, final int seconds);

  void expireAt(final String key, final long unixTime);

  void ttl(final String key);

  void setbit(String key, long offset, boolean value);

  void setbit(String key, long offset, String value);

  void getbit(String key, long offset);

  void setrange(String key, long offset, String value);

  void getrange(String key, long startOffset, long endOffset);

  void move(final String key, final int dbIndex);

  void getSet(final String key, final String value);

  void mget(final String... keys);

  void setnx(final String key, final String value);

  void setex(final String key, final int seconds, final String value);

  void mset(final String... keysvalues);

  void msetnx(final String... keysvalues);

  void decrBy(final String key, final long integer);

  void decr(final String key);

  void incrBy(final String key, final long integer);

  void incrByFloat(final String key, final double value);

  void incr(final String key);

  void append(final String key, final String value);

  void substr(final String key, final int start, final int end);

  void hset(final String key, final String field, final String value);

  void hget(final String key, final String field);

  void hsetnx(final String key, final String field, final String value);

  void hmset(final String key, final Map<String, String> hash);

  void hmget(final String key, final String... fields);

  void hincrBy(final String key, final String field, final long value);

  void hincrByFloat(final String key, final String field, final double value);

  void hexists(final String key, final String field);

  void hdel(final String key, final String... fields);

  void hlen(final String key);

  void hkeys(final String key);

  void hvals(final String key);

  void hgetAll(final String key);

  void rpush(final String key, final String... strings);

  void lpush(final String key, final String... strings);

  void llen(final String key);

  void lrange(final String key, final long start, final long end);

  void ltrim(final String key, final long start, final long end);

  void lindex(final String key, final long index);

  void lset(final String key, final long index, final String value);

  void lrem(final String key, final long count, final String value);

  void lpop(final String key);

  void rpop(final String key);

  void rpoplpush(final String srckey, final String dstkey);

  void sadd(final String key, final String... members);

  void smembers(final String key);

  void srem(final String key, final String... member);

  void spop(final String key);

  void spop(final String key, final long count);

  void smove(final String srckey, final String dstkey, final String member);

  void scard(final String key);

  void sismember(final String key, final String member);

  void sinter(final String... keys);

  void sinterstore(final String dstkey, final String... keys);

  void sunion(final String... keys);

  void sunionstore(final String dstkey, final String... keys);

  void sdiff(final String... keys);

  void sdiffstore(final String dstkey, final String... keys);

  void srandmember(final String key);

  void zadd(final String key, final double score, final String member);

  void zadd(final String key, final double score, final String member, final ZAddParams params);

  void zadd(final String key, final Map<String, Double> scoreMembers);

  void zadd(final String key, final Map<String, Double> scoreMembers, final ZAddParams params);

  void zrange(final String key, final long start, final long end);

  void zrem(final String key, final String... members);

  void zincrby(final String key, final double score, final String member);

  void zincrby(final String key, final double score, final String member, final ZIncrByParams params);

  void zrank(final String key, final String member);

  void zrevrank(final String key, final String member);

  void zrevrange(final String key, final long start, final long end);

  void zrangeWithScores(final String key, final long start, final long end);

  void zrevrangeWithScores(final String key, final long start, final long end);

  void zcard(final String key);

  void zscore(final String key, final String member);

  void watch(final String... keys);

  void sort(final String key);

  void sort(final String key, final SortingParams sortingParameters);

  void blpop(final String[] args);

  void sort(final String key, final SortingParams sortingParameters, final String dstkey);

  void sort(final String key, final String dstkey);

  void brpop(final String[] args);

  void brpoplpush(final String source, final String destination, final int timeout);

  void zcount(final String key, final double min, final double max);

  void zcount(final String key, final String min, final String max);

  void zrangeByScore(final String key, final double min, final double max);

  void zrangeByScore(final String key, final String min, final String max);

  void zrangeByScore(final String key, final double min, final double max, final int offset,
      int count);

  void zrangeByScoreWithScores(final String key, final double min, final double max);

  void zrangeByScoreWithScores(final String key, final double min, final double max,
      final int offset, final int count);

  void zrangeByScoreWithScores(final String key, final String min, final String max);

  void zrangeByScoreWithScores(final String key, final String min, final String max,
      final int offset, final int count);

  void zrevrangeByScore(final String key, final double max, final double min);

  void zrevrangeByScore(final String key, final String max, final String min);

  void zrevrangeByScore(final String key, final double max, final double min, final int offset,
      int count);

  void zrevrangeByScoreWithScores(final String key, final double max, final double min);

  void zrevrangeByScoreWithScores(final String key, final double max, final double min,
      final int offset, final int count);

  void zrevrangeByScoreWithScores(final String key, final String max, final String min);

  void zrevrangeByScoreWithScores(final String key, final String max, final String min,
      final int offset, final int count);

  void zremrangeByRank(final String key, final long start, final long end);

  void zremrangeByScore(final String key, final double start, final double end);

  void zremrangeByScore(final String key, final String start, final String end);

  void zunionstore(final String dstkey, final String... sets);

  void zunionstore(final String dstkey, final ZParams params, final String... sets);

  void zinterstore(final String dstkey, final String... sets);

  void zinterstore(final String dstkey, final ZParams params, final String... sets);

  void strlen(final String key);

  void lpushx(final String key, final String... string);

  void persist(final String key);

  void rpushx(final String key, final String... string);

  void echo(final String string);

  void linsert(final String key, final LIST_POSITION where, final String pivot, final String value);

  void bgrewriteaof();

  void bgsave();

  void lastsave();

  void save();

  void configSet(final String parameter, final String value);

  void configGet(final String pattern);

  void configResetStat();

  void multi();

  void exec();

  void discard();

  void objectRefcount(String key);

  void objectIdletime(String key);

  void objectEncoding(String key);

  void bitcount(final String key);

  void bitcount(final String key, long start, long end);

  void bitop(BitOP op, final String destKey, String... srcKeys);

  void scan(final String cursor, final ScanParams params);

  void hscan(final String key, final String cursor, final ScanParams params);

  void sscan(final String key, final String cursor, final ScanParams params);

  void zscan(final String key, final String cursor, final ScanParams params);

  void waitReplicas(int replicas, long timeout);
  
  /**
   * Used for BITFIELD Redis command
   * @param key 
   * @param args
   */
  void bitfield(final String key, final String...arguments);
  
  /**
   * Used for HSTRLEN Redis command
   * @param key 
   * @param field
   */
  void hstrlen(final String key, final String field);
}
