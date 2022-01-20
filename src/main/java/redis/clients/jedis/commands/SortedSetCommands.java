package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.args.ZMPopOption;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.KeyedZSetElement;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.resps.ZMPopResponse;

public interface SortedSetCommands {

  long zadd(String key, double score, String member);

  long zadd(String key, double score, String member, ZAddParams params);

  long zadd(String key, Map<String, Double> scoreMembers);

  long zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

  Double zaddIncr(String key, double score, String member, ZAddParams params);

  long zrem(String key, String... members);

  double zincrby(String key, double increment, String member);

  Double zincrby(String key, double increment, String member, ZIncrByParams params);

  Long zrank(String key, String member);

  Long zrevrank(String key, String member);

  List<String> zrange(String key, long start, long stop);

  List<String> zrevrange(String key, long start, long stop);

  List<Tuple> zrangeWithScores(String key, long start, long stop);

  List<Tuple> zrevrangeWithScores(String key, long start, long stop);

  List<String> zrange(String key, ZRangeParams zRangeParams);

  List<Tuple> zrangeWithScores(String key, ZRangeParams zRangeParams);

  long zrangestore(String dest, String src, ZRangeParams zRangeParams);

  String zrandmember(String key);

  List<String> zrandmember(String key, long count);

  List<Tuple> zrandmemberWithScores(String key, long count);

  long zcard(String key);

  Double zscore(String key, String member);

  List<Double> zmscore(String key, String... members);

  /**
   * Pop one or more elements, that are member-score pairs, from the first non-empty sorted set in
   * the provided list of key names.
   * <p>
   * Similar to {@link SortedSetCommands#zpopmin(String) ZPOPMIN} or {@link SortedSetCommands#zpopmax(String) ZPOPMAX}
   * which take only one key.
   * <p>
   * Time complexity O(K) + O(N*log(M)) where K is the number of provided keys, N being the number
   * of elements in the sorted set, and M being the number of elements popped.
   * @param option {@link ZMPopOption}
   * @param keys
   * @return {@link ZMPopResponse} including the name of the key from which elements were popped, and the
   * popped elements.
   */
  ZMPopResponse zmpop(ZMPopOption option, String... keys);

  /**
   * @see #zmpop(ZMPopOption, String...)
   * @param option {@link ZMPopOption} can be MIN or MAX
   * @param count pop up to count elements
   * @param keys
   * @return {@link ZMPopResponse} including the name of the key from which elements were popped, and the
   * popped elements.
   */
  ZMPopResponse zmpop(ZMPopOption option, int count, String... keys);

  /**
   * Blocking version of {@link SortedSetCommands#zmpop(ZMPopOption, String...) ZMPOP}
   */
  ZMPopResponse bzmpop(double timeout, ZMPopOption option, String... keys);

  /**
   * Blocking version of {@link SortedSetCommands#zmpop(ZMPopOption, int, String...) ZMPOP}
   */
  ZMPopResponse bzmpop(double timeout, ZMPopOption option, int count, String... keys);

  Tuple zpopmax(String key);

  List<Tuple> zpopmax(String key, int count);

  Tuple zpopmin(String key);

  List<Tuple> zpopmin(String key, int count);

  long zcount(String key, double min, double max);

  long zcount(String key, String min, String max);

  List<String> zrangeByScore(String key, double min, double max);

  List<String> zrangeByScore(String key, String min, String max);

  List<String> zrevrangeByScore(String key, double max, double min);

  List<String> zrangeByScore(String key, double min, double max, int offset, int count);

  List<String> zrevrangeByScore(String key, String max, String min);

  List<String> zrangeByScore(String key, String min, String max, int offset, int count);

  List<String> zrevrangeByScore(String key, double max, double min, int offset, int count);

  List<Tuple> zrangeByScoreWithScores(String key, double min, double max);

  List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min);

  List<Tuple> zrangeByScoreWithScores(String key, double min, double max, int offset, int count);

  List<String> zrevrangeByScore(String key, String max, String min, int offset, int count);

  List<Tuple> zrangeByScoreWithScores(String key, String min, String max);

  List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min);

  List<Tuple> zrangeByScoreWithScores(String key, String min, String max, int offset, int count);

  List<Tuple> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count);

  List<Tuple> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count);

  long zremrangeByRank(String key, long start, long stop);

  long zremrangeByScore(String key, double min, double max);

  long zremrangeByScore(String key, String min, String max);

  long zlexcount(String key, String min, String max);

  List<String> zrangeByLex(String key, String min, String max);

  List<String> zrangeByLex(String key, String min, String max, int offset, int count);

  List<String> zrevrangeByLex(String key, String max, String min);

  List<String> zrevrangeByLex(String key, String max, String min, int offset, int count);

  long zremrangeByLex(String key, String min, String max);

  default ScanResult<Tuple> zscan(String key, String cursor) {
    return zscan(key, cursor, new ScanParams());
  }

  ScanResult<Tuple> zscan(String key, String cursor, ScanParams params);

  KeyedZSetElement bzpopmax(double timeout, String... keys);

  KeyedZSetElement bzpopmin(double timeout, String... keys);

  Set<String> zdiff(String... keys);

  Set<Tuple> zdiffWithScores(String... keys);

  long zdiffStore(String dstkey, String... keys);

  long zinterstore(String dstkey, String... sets);

  long zinterstore(String dstkey, ZParams params, String... sets);

  Set<String> zinter(ZParams params, String... keys);

  Set<Tuple> zinterWithScores(ZParams params, String... keys);

  Set<String> zunion(ZParams params, String... keys);

  Set<Tuple> zunionWithScores(ZParams params, String... keys);

  long zunionstore(String dstkey, String... sets);

  long zunionstore(String dstkey, ZParams params, String... sets);

}
