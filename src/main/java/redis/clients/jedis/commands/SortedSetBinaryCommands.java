package redis.clients.jedis.commands;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.params.ZAddParams;
import redis.clients.jedis.params.ZIncrByParams;
import redis.clients.jedis.params.ZParams;
import redis.clients.jedis.resps.ScanResult;
import redis.clients.jedis.resps.Tuple;

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

  Set<byte[]> zrange(byte[] key, long start, long stop);

  Set<byte[]> zrevrange(byte[] key, long start, long stop);

  Set<Tuple> zrangeWithScores(byte[] key, long start, long stop);

  Set<Tuple> zrevrangeWithScores(byte[] key, long start, long stop);

  byte[] zrandmember(byte[] key);

  Set<byte[]> zrandmember(byte[] key, long count);

  Set<Tuple> zrandmemberWithScores(byte[] key, long count);

  long zcard(byte[] key);

  Double zscore(byte[] key, byte[] member);

  List<Double> zmscore(byte[] key, byte[]... members);

  Tuple zpopmax(byte[] key);

  Set<Tuple> zpopmax(byte[] key, int count);

  Tuple zpopmin(byte[] key);

  Set<Tuple> zpopmin(byte[] key, int count);

  long zcount(byte[] key, double min, double max);

  long zcount(byte[] key, byte[] min, byte[] max);

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

  long zremrangeByRank(byte[] key, long start, long stop);

  long zremrangeByScore(byte[] key, double min, double max);

  long zremrangeByScore(byte[] key, byte[] min, byte[] max);

  long zlexcount(byte[] key, byte[] min, byte[] max);

  Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max);

  Set<byte[]> zrangeByLex(byte[] key, byte[] min, byte[] max, int offset, int count);

  Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min);

  Set<byte[]> zrevrangeByLex(byte[] key, byte[] max, byte[] min, int offset, int count);

  long zremrangeByLex(byte[] key, byte[] min, byte[] max);

  default ScanResult<Tuple> zscan(byte[] key, byte[] cursor) {
    return zscan(key, cursor, new ScanParams());
  }

  ScanResult<Tuple> zscan(byte[] key, byte[] cursor, ScanParams params);

  List<byte[]> bzpopmax(double timeout, byte[]... keys);

  List<byte[]> bzpopmin(double timeout, byte[]... keys);

  Set<byte[]> zdiff(byte[]... keys);

  Set<Tuple> zdiffWithScores(byte[]... keys);

  long zdiffStore(byte[] dstkey, byte[]... keys);

  Set<byte[]> zinter(ZParams params, byte[]... keys);

  Set<Tuple> zinterWithScores(ZParams params, byte[]... keys);

  long zinterstore(byte[] dstkey, byte[]... sets);

  long zinterstore(byte[] dstkey, ZParams params, byte[]... sets);

  Set<byte[]> zunion(ZParams params, byte[]... keys);

  Set<Tuple> zunionWithScores(ZParams params, byte[]... keys);

  long zunionstore(byte[] dstkey, byte[]... sets);

  long zunionstore(byte[] dstkey, ZParams params, byte[]... sets);

}
