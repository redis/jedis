package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Response;
import redis.clients.jedis.args.SortedSetOption;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.KeyedZSetElement;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.KeyValue;

public interface SortedSetPipelineCommands {

  Response<Long> zadd(String key, double score, String member);

  Response<Long> zadd(String key, double score, String member, ZAddParams params);

  Response<Long> zadd(String key, Map<String, Double> scoreMembers);

  Response<Long> zadd(String key, Map<String, Double> scoreMembers, ZAddParams params);

  Response<Double> zaddIncr(String key, double score, String member, ZAddParams params);

  Response<Long> zrem(String key, String... members);

  Response<Double> zincrby(String key, double increment, String member);

  Response<Double> zincrby(String key, double increment, String member, ZIncrByParams params);

  Response<Long> zrank(String key, String member);

  Response<Long> zrevrank(String key, String member);

  Response<List<String>> zrange(String key, long start, long stop);

  Response<List<String>> zrevrange(String key, long start, long stop);

  Response<List<Tuple>> zrangeWithScores(String key, long start, long stop);

  Response<List<Tuple>> zrevrangeWithScores(String key, long start, long stop);

  Response<String> zrandmember(String key);

  Response<List<String>> zrandmember(String key, long count);

  Response<List<Tuple>> zrandmemberWithScores(String key, long count);

  Response<Long> zcard(String key);

  Response<Double> zscore(String key, String member);

  Response<List<Double>> zmscore(String key, String... members);

  Response<Tuple> zpopmax(String key);

  Response<List<Tuple>> zpopmax(String key, int count);

  Response<Tuple> zpopmin(String key);

  Response<List<Tuple>> zpopmin(String key, int count);

  Response<Long> zcount(String key, double min, double max);

  Response<Long> zcount(String key, String min, String max);

  Response<List<String>> zrangeByScore(String key, double min, double max);

  Response<List<String>> zrangeByScore(String key, String min, String max);

  Response<List<String>> zrevrangeByScore(String key, double max, double min);

  Response<List<String>> zrangeByScore(String key, double min, double max, int offset, int count);

  Response<List<String>> zrevrangeByScore(String key, String max, String min);

  Response<List<String>> zrangeByScore(String key, String min, String max, int offset, int count);

  Response<List<String>> zrevrangeByScore(String key, double max, double min, int offset, int count);

  Response<List<Tuple>> zrangeByScoreWithScores(String key, double min, double max);

  Response<List<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min);

  Response<List<Tuple>> zrangeByScoreWithScores(String key, double min, double max, int offset, int count);

  Response<List<String>> zrevrangeByScore(String key, String max, String min, int offset, int count);

  Response<List<Tuple>> zrangeByScoreWithScores(String key, String min, String max);

  Response<List<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min);

  Response<List<Tuple>> zrangeByScoreWithScores(String key, String min, String max, int offset, int count);

  Response<List<Tuple>> zrevrangeByScoreWithScores(String key, double max, double min, int offset, int count);

  Response<List<Tuple>> zrevrangeByScoreWithScores(String key, String max, String min, int offset, int count);

  Response<List<String>> zrange(String key, ZRangeParams zRangeParams);

  Response<List<Tuple>> zrangeWithScores(String key, ZRangeParams zRangeParams);

  Response<Long> zrangestore(String dest, String src, ZRangeParams zRangeParams);

  Response<Long> zremrangeByRank(String key, long start, long stop);

  Response<Long> zremrangeByScore(String key, double min, double max);

  Response<Long> zremrangeByScore(String key, String min, String max);

  Response<Long> zlexcount(String key, String min, String max);

  Response<List<String>> zrangeByLex(String key, String min, String max);

  Response<List<String>> zrangeByLex(String key, String min, String max, int offset, int count);

  Response<List<String>> zrevrangeByLex(String key, String max, String min);

  Response<List<String>> zrevrangeByLex(String key, String max, String min, int offset, int count);

  Response<Long> zremrangeByLex(String key, String min, String max);

  default Response<ScanResult<Tuple>> zscan(String key, String cursor) {
    return zscan(key, cursor, new ScanParams());
  }

  Response<ScanResult<Tuple>> zscan(String key, String cursor, ScanParams params);

  Response<KeyedZSetElement> bzpopmax(double timeout, String... keys);

  Response<KeyedZSetElement> bzpopmin(double timeout, String... keys);

  Response<Set<String>> zdiff(String... keys);

  Response<Set<Tuple>> zdiffWithScores(String... keys);

  Response<Long> zdiffStore(String dstKey, String... keys);

  Response<Long> zinterstore(String dstKey, String... sets);

  Response<Long> zinterstore(String dstKey, ZParams params, String... sets);

  Response<Set<String>> zinter(ZParams params, String... keys);

  Response<Set<Tuple>> zinterWithScores(ZParams params, String... keys);

  Response<Long> zintercard(String... keys);

  Response<Long> zintercard(long limit, String... keys);

  Response<Set<String>> zunion(ZParams params, String... keys);

  Response<Set<Tuple>> zunionWithScores(ZParams params, String... keys);

  Response<Long> zunionstore(String dstKey, String... sets);

  Response<Long> zunionstore(String dstKey, ZParams params, String... sets);

  Response<KeyValue<String, List<Tuple>>> zmpop(SortedSetOption option, String... keys);

  Response<KeyValue<String, List<Tuple>>> zmpop(SortedSetOption option, int count, String... keys);

  Response<KeyValue<String, List<Tuple>>> bzmpop(long timeout, SortedSetOption option, String... keys);

  Response<KeyValue<String, List<Tuple>>> bzmpop(long timeout, SortedSetOption option, int count, String... keys);
}
