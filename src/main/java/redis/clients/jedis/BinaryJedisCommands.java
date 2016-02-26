package redis.clients.jedis;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.params.geo.GeoRadiusParam;
import redis.clients.jedis.params.sortedset.ZAddParams;
import redis.clients.jedis.params.sortedset.ZIncrByParams;

/**
 * Common interface for sharded and non-sharded BinaryJedis
 */
public interface BinaryJedisCommands {
  String set(byte[] key, byte[] value);

  String set(byte[] key, byte[] value, byte[] nxxx);

  String set(byte[] key, byte[] value, byte[] nxxx, byte[] expx, long time);

  byte[] get(byte[] key);

  Boolean exists(byte[] key);

  Long persist(byte[] key);

  String type(byte[] key);

  Long expire(byte[] key, int seconds);

  /**
   * @deprecated String key operation on BinaryCommand. Use byte[] or JedisCommands
   */
  @Deprecated
  Long pexpire(final String key, final long milliseconds);

  Long pexpire(byte[] key, final long milliseconds);

  Long expireAt(byte[] key, long unixTime);

  Long pexpireAt(byte[] key, long millisecondsTimestamp);

  Long ttl(byte[] key);

  Boolean setbit(byte[] key, long offset, boolean value);

  Boolean setbit(byte[] key, long offset, byte[] value);

  Boolean getbit(byte[] key, long offset);

  Long setrange(byte[] key, long offset, byte[] value);

  byte[] getrange(byte[] key, long startOffset, long endOffset);

  byte[] getSet(byte[] key, byte[] value);

  Long setnx(byte[] key, byte[] value);

  String setex(byte[] key, int seconds, byte[] value);

  Long decrBy(byte[] key, long integer);

  Long decr(byte[] key);

  Long incrBy(byte[] key, long integer);

  Double incrByFloat(byte[] key, double value);

  Long incr(byte[] key);

  Long append(byte[] key, byte[] value);

  byte[] substr(byte[] key, int start, int end);

  Long hset(byte[] key, byte[] field, byte[] value);

  byte[] hget(byte[] key, byte[] field);

  Long hsetnx(byte[] key, byte[] field, byte[] value);

  String hmset(byte[] key, Map<byte[], byte[]> hash);

  List<byte[]> hmget(byte[] key, byte[]... fields);

  Long hincrBy(byte[] key, byte[] field, long value);

  Double hincrByFloat(byte[] key, byte[] field, double value);

  Boolean hexists(byte[] key, byte[] field);

  Long hdel(byte[] key, byte[]... field);

  Long hlen(byte[] key);

  Set<byte[]> hkeys(byte[] key);

  Collection<byte[]> hvals(byte[] key);

  Map<byte[], byte[]> hgetAll(byte[] key);

  Long rpush(byte[] key, byte[]... args);

  Long lpush(byte[] key, byte[]... args);

  Long llen(byte[] key);

  List<byte[]> lrange(byte[] key, long start, long end);

  String ltrim(byte[] key, long start, long end);

  byte[] lindex(byte[] key, long index);

  String lset(byte[] key, long index, byte[] value);

  Long lrem(byte[] key, long count, byte[] value);

  byte[] lpop(byte[] key);

  byte[] rpop(byte[] key);

  Long sadd(byte[] key, byte[]... member);

  Set<byte[]> smembers(byte[] key);

  Long srem(byte[] key, byte[]... member);

  byte[] spop(byte[] key);

  Set<byte[]> spop(byte[] key, long count);

  Long scard(byte[] key);

  Boolean sismember(byte[] key, byte[] member);

  byte[] srandmember(byte[] key);

  List<byte[]> srandmember(final byte[] key, final int count);

  Long strlen(byte[] key);

  Long zadd(byte[] key, double score, byte[] member);

  Long zadd(byte[] key, double score, byte[] member, ZAddParams params);

  Long zadd(byte[] key, Map<byte[], Double> scoreMembers);

  Long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params);

  Set<byte[]> zrange(byte[] key, long start, long end);

  Long zrem(byte[] key, byte[]... member);

  Double zincrby(byte[] key, double score, byte[] member);

  Double zincrby(byte[] key, double score, byte[] member, ZIncrByParams params);

  Long zrank(byte[] key, byte[] member);

  Long zrevrank(byte[] key, byte[] member);

  Set<byte[]> zrevrange(byte[] key, long start, long end);

  Set<Tuple> zrangeWithScores(byte[] key, long start, long end);

  Set<Tuple> zrevrangeWithScores(byte[] key, long start, long end);

  Long zcard(byte[] key);

  Double zscore(byte[] key, byte[] member);

  List<byte[]> sort(byte[] key);

  List<byte[]> sort(byte[] key, SortingParams sortingParameters);

  Long zcount(byte[] key, double min, double max);

  Long zcount(byte[] key, byte[] min, byte[] max);

  Set<byte[]> zrangeByScore(byte[] key, double min, double max);

  Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max);

  Set<byte[]> zrevrangeByScore(byte[] key, double max, double min);

  Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count);

  Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min);

  Set<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count);

  Set<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count);

  Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max);

  Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min);

  Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count);

  Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count);

  Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max);

  Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min);

  Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count);

  Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count);

  Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count);

  Long zremrangeByRank(byte[] key, long start, long end);

  Long zremrangeByScore(byte[] key, double start, double end);

  Long zremrangeByScore(byte[] key, byte[] start, byte[] end);

  Long zlexcount(final byte[] key, final byte[] min, final byte[] max);

  Set<byte[]> zrangeByLex(final byte[] key, final byte[] min, final byte[] max);

  Set<byte[]> zrangeByLex(final byte[] key, final byte[] min, final byte[] max, int offset,
      int count);

  Set<byte[]> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min);

  Set<byte[]> zrevrangeByLex(final byte[] key, final byte[] max, final byte[] min, int offset,
      int count);

  Long zremrangeByLex(final byte[] key, final byte[] min, final byte[] max);

  Long linsert(byte[] key, Client.LIST_POSITION where, byte[] pivot, byte[] value);

  Long lpushx(byte[] key, byte[]... arg);

  Long rpushx(byte[] key, byte[]... arg);

  /**
   * @deprecated unusable command, this command will be removed in 3.0.0.
   */
  @Deprecated
  List<byte[]> blpop(byte[] arg);

  /**
   * @deprecated unusable command, this command will be removed in 3.0.0.
   */
  @Deprecated
  List<byte[]> brpop(byte[] arg);

  Long del(byte[] key);

  byte[] echo(byte[] arg);

  Long move(byte[] key, int dbIndex);

  Long bitcount(final byte[] key);

  Long bitcount(final byte[] key, long start, long end);

  Long pfadd(final byte[] key, final byte[]... elements);

  long pfcount(final byte[] key);

  // Geo Commands

  Long geoadd(byte[] key, double longitude, double latitude, byte[] member);

  Long geoadd(byte[] key, Map<byte[], GeoCoordinate> memberCoordinateMap);

  Double geodist(byte[] key, byte[] member1, byte[] member2);

  Double geodist(byte[] key, byte[] member1, byte[] member2, GeoUnit unit);

  List<byte[]> geohash(byte[] key, byte[]... members);

  List<GeoCoordinate> geopos(byte[] key, byte[]... members);

  List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius,
      GeoUnit unit);

  List<GeoRadiusResponse> georadius(byte[] key, double longitude, double latitude, double radius,
      GeoUnit unit, GeoRadiusParam param);

  List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit);

  List<GeoRadiusResponse> georadiusByMember(byte[] key, byte[] member, double radius, GeoUnit unit,
      GeoRadiusParam param);

  ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor);

  ScanResult<Map.Entry<byte[], byte[]>> hscan(byte[] key, byte[] cursor, ScanParams params);

  ScanResult<byte[]> sscan(byte[] key, byte[] cursor);

  ScanResult<byte[]> sscan(byte[] key, byte[] cursor, ScanParams params);

  ScanResult<Tuple> zscan(byte[] key, byte[] cursor);

  ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params);

}
