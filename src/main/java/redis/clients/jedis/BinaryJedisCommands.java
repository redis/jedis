package redis.clients.jedis;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

/**
 * Common interface for sharded and non-sharded BinaryJedis
 */
public interface BinaryJedisCommands {
    String set(byte[] key, byte[] value);

    byte[] get(byte[] key);

    Boolean exists(byte[] key);

    String type(byte[] key);

    Long expire(byte[] key, int seconds);

    Long expireAt(byte[] key, long unixTime);

    Long ttl(byte[] key);

    byte[] getSet(byte[] key, byte[] value);

    Long setnx(byte[] key, byte[] value);

    String setex(byte[] key, int seconds, byte[] value);

    Long decrBy(byte[] key, long integer);

    Long decr(byte[] key);

    Long incrBy(byte[] key, long integer);

    Long incr(byte[] key);

    Long append(byte[] key, byte[] value);

    byte[] substr(byte[] key, int start, int end);

    Long hset(byte[] key, byte[] field, byte[] value);

    byte[] hget(byte[] key, byte[] field);

    Long hsetnx(byte[] key, byte[] field, byte[] value);

    String hmset(byte[] key, Map<byte[], byte[]> hash);

    List<byte[]> hmget(byte[] key, byte[]... fields);

    Long hincrBy(byte[] key, byte[] field, long value);

    Boolean hexists(byte[] key, byte[] field);

    Long hdel(byte[] key, byte[]... field);

    Long hlen(byte[] key);

    Set<byte[]> hkeys(byte[] key);

    Collection<byte[]> hvals(byte[] key);

    Map<byte[], byte[]> hgetAll(byte[] key);

    Long rpush(byte[] key, byte[]... string);

    Long lpush(byte[] key, byte[]... string);

    Long llen(byte[] key);

    List<byte[]> lrange(byte[] key, int start, int end);

    String ltrim(byte[] key, int start, int end);

    byte[] lindex(byte[] key, int index);

    String lset(byte[] key, int index, byte[] value);

    Long lrem(byte[] key, int count, byte[] value);

    byte[] lpop(byte[] key);

    byte[] rpop(byte[] key);

    Long sadd(byte[] key, byte[]... member);

    Set<byte[]> smembers(byte[] key);

    Long srem(byte[] key, byte[]... member);

    byte[] spop(byte[] key);

    Long scard(byte[] key);

    Boolean sismember(byte[] key, byte[] member);

    byte[] srandmember(byte[] key);

    Long zadd(byte[] key, double score, byte[] member);

    Long zadd(byte[] key, Map<Double, byte[]> scoreMembers);

    Set<byte[]> zrange(byte[] key, int start, int end);

    Long zrem(byte[] key, byte[]... member);

    Double zincrby(byte[] key, double score, byte[] member);

    Long zrank(byte[] key, byte[] member);

    Long zrevrank(byte[] key, byte[] member);

    Set<byte[]> zrevrange(byte[] key, int start, int end);

    Set<Tuple> zrangeWithScores(byte[] key, int start, int end);

    Set<Tuple> zrevrangeWithScores(byte[] key, int start, int end);

    Long zcard(byte[] key);

    Double zscore(byte[] key, byte[] member);

    List<byte[]> sort(byte[] key);

    List<byte[]> sort(byte[] key, SortingParams sortingParameters);

    Long zcount(byte[] key, double min, double max);

    Long zcount(byte[] key, byte[] min, byte[] max);

    Set<byte[]> zrangeByScore(byte[] key, double min, double max);

    Set<byte[]> zrangeByScore(byte[] key, double min, double max, int offset,
	    int count);

    Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max);

    Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max,
	    int offset, int count);

    Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max);

    Set<Tuple> zrangeByScoreWithScores(byte[] key, byte[] min, byte[] max,
	    int offset, int count);

    Set<byte[]> zrevrangeByScore(byte[] key, double max, double min);

    Set<byte[]> zrevrangeByScore(byte[] key, double max, double min,
	    int offset, int count);

    Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min);

    Set<byte[]> zrevrangeByScore(byte[] key, byte[] max, byte[] min,
	    int offset, int count);

    Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min);

    Set<Tuple> zrevrangeByScoreWithScores(byte[] key, double max, double min,
	    int offset, int count);

    Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min);

    Set<Tuple> zrevrangeByScoreWithScores(byte[] key, byte[] max, byte[] min,
	    int offset, int count);

    Long zremrangeByRank(byte[] key, int start, int end);

    Long zremrangeByScore(byte[] key, double start, double end);

    Long zremrangeByScore(byte[] key, byte[] start, byte[] end);

    Long linsert(byte[] key, LIST_POSITION where, byte[] pivot, byte[] value);

    Long objectRefcount(byte[] key);

    Long objectIdletime(byte[] key);

    byte[] objectEncoding(byte[] key);

    Long lpushx(byte[] key, byte[] string);

    Long rpushx(byte[] key, byte[] string);
}
