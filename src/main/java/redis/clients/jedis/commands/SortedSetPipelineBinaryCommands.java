package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.Response;
import redis.clients.jedis.args.SortedSetOption;
import redis.clients.jedis.params.*;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;
import redis.clients.jedis.util.KeyValue;

public interface SortedSetPipelineBinaryCommands {

  Response<Long> zadd(byte[] key, double score, byte[] member);

  Response<Long> zadd(byte[] key, double score, byte[] member, ZAddParams params);

  Response<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers);

  Response<Long> zadd(byte[] key, Map<byte[], Double> scoreMembers, ZAddParams params);

  Response<Double> zaddIncr(byte[] key, double score, byte[] member, ZAddParams params);

  Response<Long> zrem(byte[] key, byte[]... members);

  Response<Double> zincrby(byte[] key, double increment, byte[] member);

  Response<Double> zincrby(byte[] key, double increment, byte[] member, ZIncrByParams params);

  Response<Long> zrank(byte[] key, byte[] member);

  Response<Long> zrevrank(byte[] key, byte[] member);

  Response<List<byte[]>> zrange(byte[] key, long start, long stop);

  Response<List<byte[]>> zrevrange(byte[] key, long start, long stop);

  Response<List<Tuple>> zrangeWithScores(byte[] key, long start, long stop);

  Response<List<Tuple>> zrevrangeWithScores(byte[] key, long start, long stop);

  Response<byte[]> zrandmember(byte[] key);

  Response<List<byte[]>> zrandmember(byte[] key, long count);

  Response<List<Tuple>> zrandmemberWithScores(byte[] key, long count);

  Response<Long> zcard(byte[] key);

  Response<Double> zscore(byte[] key, byte[] member);

  Response<List<Double>> zmscore(byte[] key, byte[]... members);

  Response<Tuple> zpopmax(byte[] key);

  Response<List<Tuple>> zpopmax(byte[] key, int count);

  Response<Tuple> zpopmin(byte[] key);

  Response<List<Tuple>> zpopmin(byte[] key, int count);

  Response<Long> zcount(byte[] key, double min, double max);

  Response<Long> zcount(byte[] key, byte[] min, byte[] max);

  Response<List<byte[]>> zrangeByScore(byte[] key, double min, double max);

  Response<List<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max);

  Response<List<byte[]>> zrevrangeByScore(byte[] key, double max, double min);

  Response<List<byte[]>> zrangeByScore(byte[] key, double min, double max, int offset, int count);

  Response<List<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min);

  Response<List<byte[]>> zrangeByScore(byte[] key, byte[] min, byte[] max, int offset, int count);

  Response<List<byte[]>> zrevrangeByScore(byte[] key, double max, double min, int offset, int count);

  Response<List<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max);

  Response<List<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min);

  Response<List<Tuple>> zrangeByScoreWithScores(byte[] key, double min, double max, int offset, int count);

  Response<List<byte[]>> zrevrangeByScore(byte[] key, byte[] max, byte[] min, int offset, int count);

  Response<List<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max);

  Response<List<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min);

  Response<List<Tuple>> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max, int offset, int count);

  Response<List<Tuple>> zrevrangeByScoreWithScores(byte[] key, double max, double min, int offset, int count);

  Response<List<Tuple>> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min, int offset, int count);

  Response<Long> zremrangeByRank(byte[] key, long start, long stop);

  Response<Long> zremrangeByScore(byte[] key, double min, double max);

  Response<Long> zremrangeByScore(byte[] key, byte[] min, byte[] max);

  Response<Long> zlexcount(byte[] key, byte[] min, byte[] max);

  Response<List<byte[]>> zrangeByLex(byte[] key, byte[] min, byte[] max);

  Response<List<byte[]>> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count);

  Response<List<byte[]>> zrevrangeByLex(byte[] key, byte[] max, byte[] min);

  Response<List<byte[]>> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count);

  Response<List<byte[]>> zrange(byte[] key, ZRangeParams zRangeParams);

  Response<List<Tuple>> zrangeWithScores(byte[] key, ZRangeParams zRangeParams);

  Response<Long> zrangestore(byte[] dest, byte[] src, ZRangeParams zRangeParams);

  Response<Long> zremrangeByLex(byte[] key, byte[] min, byte[] max);

  default Response<ScanResult<Tuple>> zscan(byte[] key, byte[] cursor) {
    return zscan(key, cursor, new ScanParams());
  }

  Response<ScanResult<Tuple>> zscan(byte[] key, byte[] cursor, ScanParams params);

  Response<List<byte[]>> bzpopmax(double timeout, byte[]... keys);

  Response<List<byte[]>> bzpopmin(double timeout, byte[]... keys);

  Response<Set<byte[]>> zdiff(byte[]... keys);

  Response<Set<Tuple>> zdiffWithScores(byte[]... keys);

  Response<Long> zdiffStore(byte[] dstkey, byte[]... keys);

  Response<Set<byte[]>> zinter(ZParams params, byte[]... keys);

  Response<Set<Tuple>> zinterWithScores(ZParams params, byte[]... keys);

  Response<Long> zinterstore(byte[] dstkey, byte[]... sets);

  Response<Long> zinterstore(byte[] dstkey, ZParams params, byte[]... sets);

  Response<Long> zintercard(byte[]... keys);

  Response<Long> zintercard(long limit, byte[]... keys);

  Response<Set<byte[]>> zunion(ZParams params, byte[]... keys);

  Response<Set<Tuple>> zunionWithScores(ZParams params, byte[]... keys);

  Response<Long> zunionstore(byte[] dstkey, byte[]... sets);

  Response<Long> zunionstore(byte[] dstkey, ZParams params, byte[]... sets);

  Response<KeyValue<byte[], List<Tuple>>> zmpop(SortedSetOption option, byte[]... keys);

  Response<KeyValue<byte[], List<Tuple>>> zmpop(SortedSetOption option, int count, byte[]... keys);

  Response<KeyValue<byte[], List<Tuple>>> bzmpop(long timeout, SortedSetOption option, byte[]... keys);

  Response<KeyValue<byte[], List<Tuple>>> bzmpop(long timeout, SortedSetOption option, int count, byte[]... keys);
}
