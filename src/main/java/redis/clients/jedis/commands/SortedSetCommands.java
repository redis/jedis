package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;
import redis.clients.jedis.params.ScanParams;

import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;
import redis.clients.jedis.params.ZParams;
import redis.clients.jedis.resps.KeyedZSetElement;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;

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

  Set<String> zrange(String key, long start, long stop);

  Set<String> zrevrange(String key, long start, long stop);

  Set<Tuple> zrangeWithScores(String key, long start, long stop);

  Set<Tuple> zrevrangeWithScores(String key, long start, long stop);

  String zrandmember(String key);

  Set<String> zrandmember(String key, long count);

  Set<Tuple> zrandmemberWithScores(String key, long count);

  long zcard(String key);

  Double zscore(String key, String member);

  List<Double> zmscore(String key, String... members);

  Tuple zpopmax(String key);

  Set<Tuple> zpopmax(String key, int count);

  Tuple zpopmin(String key);

  Set<Tuple> zpopmin(String key, int count);

  long zcount(String key, double min, double max);

  long zcount(String key, String min, String max);

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

  long zremrangeByRank(String key, long start, long stop);

  long zremrangeByScore(String key, double min, double max);

  long zremrangeByScore(String key, String min, String max);

  long zlexcount(String key, String min, String max);

  Set<String> zrangeByLex(String key, String min, String max);

  Set<String> zrangeByLex(String key, String min, String max, int offset, int count);

  Set<String> zrevrangeByLex(String key, String max, String min);

  Set<String> zrevrangeByLex(String key, String max, String min, int offset, int count);

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
