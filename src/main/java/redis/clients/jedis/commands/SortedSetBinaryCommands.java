package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.args.SortedSetOption;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.KeyValue;

public interface SortedSetBinaryCommands {

  long zadd(byte[] key, double score, byte[] member);

  long zadd(byte[] key, double score, byte[] member, ZAddParams params);

  long zadd(byte[] key, Map<byte[], Double> scoreMembers);

  long zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params);

  Double zaddIncr(byte[] key, double score, byte[] member, ZAddParams params);

  long zrem(byte[] key, byte[]... members);

  double zincrby(byte[] key, double increment, byte[] member);

  Double zincrby(byte[] key, double increment, byte[] member, ZIncrByParams params);

  Long zrank(byte[] key, byte[] member);

  Long zrevrank(byte[] key, byte[] member);

  KeyValue<Long, Double> zrankWithScore(byte[] key, byte[] member);

  KeyValue<Long, Double> zrevrankWithScore(byte[] key, byte[] member);

  List<byte[]> zrange(byte[] key, long start, long stop);

  List<byte[]> zrevrange(byte[] key, long start, long stop);

  List<Tuple> zrangeWithScores(byte[] key, long start, long stop);

  List<Tuple> zrevrangeWithScores(byte[] key, long start, long stop);

  List<byte[]> zrange(byte[] key, ZRangeParams zRangeParams);

  List<Tuple> zrangeWithScores(byte[] key, ZRangeParams zRangeParams);

  long zrangestore(byte[] dest, byte[] src, ZRangeParams zRangeParams);

  byte[] zrandmember(byte[] key);

  List<byte[]> zrandmember(byte[] key, long count);

  List<Tuple> zrandmemberWithScores(byte[] key, long count);

  long zcard(byte[] key);

  Double zscore(byte[] key, byte[] member);

  List<Double> zmscore(byte[] key, byte[]... members);

  Tuple zpopmax(byte[] key);

  List<Tuple> zpopmax(byte[] key, int count);

  Tuple zpopmin(byte[] key);

  List<Tuple> zpopmin(byte[] key, int count);

  long zcount(byte[] key, double min, double max);

  long zcount(byte[] key, byte[] min, byte[] max);

  List<byte[]> zrangeByScore(byte[] key, double min, double max);

  List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max);

  List<byte[]> zrevrangeByScore(byte[] key, double max, double min);

  List<byte[]> zrangeByScore(byte[] key, double min, double max, int offset, int count);

  List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min);

  List<byte[]> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count);

  List<byte[]> zrevrangeByScore(byte[] key, double max, double min, int offset, int count);

  List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max);

  List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min);

  List<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count);

  List<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count);

  List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max);

  List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min);

  List<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count);

  List<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count);

  List<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count);

  long zremrangeByRank(byte[] key, long start, long stop);

  long zremrangeByScore(byte[] key, double min, double max);

  long zremrangeByScore(byte[] key, byte[] min, byte[] max);

  long zlexcount(byte[] key, byte[] min, byte[] max);

  List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max);

  List<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count);

  List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min);

  List<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count);

  long zremrangeByLex(byte[] key, byte[] min, byte[] max);

  default ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
    return zscan(key, cursor, new ScanParams());
  }

  ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params);

  KeyValue<byte[], Tuple> bzpopmax(double timeout, byte[]... keys);

  KeyValue<byte[], Tuple> bzpopmin(double timeout, byte[]... keys);

  List<byte[]> zdiff(byte[]... keys);

  List<Tuple> zdiffWithScores(byte[]... keys);

  /**
   * @deprecated Use {@link #zdiffstore(byte[], byte[][])}.
   */
  @Deprecated
  long zdiffStore(byte[] dstkey, byte[]... keys);

  long zdiffstore(byte[] dstkey, byte[]... keys);

  List<byte[]> zinter(ZParams params, byte[]... keys);

  List<Tuple> zinterWithScores(ZParams params, byte[]... keys);

  long zinterstore(byte[] dstkey, byte[]... sets);

  long zinterstore(byte[] dstkey, ZParams params, byte[]... sets);

  /**
   * Similar to {@link #zinter(ZParams, byte[][]) ZINTER}, but instead of returning the result set,
   * it returns just the cardinality of the result.
   * <p>
   * Time complexity O(N*K) worst case with N being the smallest input sorted set, K
   * being the number of input sorted sets
   * @see #zinter(ZParams, byte[][])
   * @param keys group of sets
   * @return The number of elements in the resulting intersection
   */
  long zintercard(byte[]... keys);

  /**
   * Similar to {@link #zinter(ZParams, byte[][]) ZINTER}, but instead of returning the result set,
   * it returns just the cardinality of the result.
   * <p>
   * Time complexity O(N*K) worst case with N being the smallest input sorted set, K
   * being the number of input sorted sets
   * @see #zinter(ZParams, byte[][])
   * @param limit If the intersection cardinality reaches limit partway through the computation,
   *              the algorithm will exit and yield limit as the cardinality
   * @param keys group of sets
   * @return The number of elements in the resulting intersection
   */
  long zintercard(long limit, byte[]... keys);

  List<byte[]> zunion(ZParams params, byte[]... keys);

  List<Tuple> zunionWithScores(ZParams params, byte[]... keys);

  long zunionstore(byte[] dstkey, byte[]... sets);

  long zunionstore(byte[] dstkey, ZParams params, byte[]... sets);

  KeyValue<byte[], List<Tuple>> zmpop(SortedSetOption option, byte[]... keys);

  KeyValue<byte[], List<Tuple>> zmpop(SortedSetOption option, int count, byte[]... keys);

  KeyValue<byte[], List<Tuple>> bzmpop(double timeout, SortedSetOption option, byte[]... keys);

  KeyValue<byte[], List<Tuple>> bzmpop(double timeout, SortedSetOption option, int count, byte[]... keys);
}
