package redis.clients.jedis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

/**
 * Common interface for sharded and non-sharded BinaryJedis
 */
public interface BinaryJedisCommands {
    String set(byte[] key, byte[] value);

    String get(byte[] key);

    Integer exists(byte[] key);

    String type(byte[] key);

    Integer expire(byte[] key, int seconds);

    Integer expireAt(byte[] key, long unixTime);

    Integer ttl(byte[] key);

    String getSet(byte[] key, byte[] value);

    Integer setnx(byte[] key, byte[] value);

    String setex(byte[] key, int seconds, byte[] value);

    Integer decrBy(byte[] key, int integer);

    Integer decr(byte[] key);

    Integer incrBy(byte[] key, int integer);

    Integer incr(byte[] key);

    Integer append(byte[] key, byte[] value);

    String substr(byte[] key, int start, int end);

    Integer hset(byte[] key, byte[] field, byte[] value);

    String hget(byte[] key, byte[] field);

    Integer hsetnx(byte[] key, byte[] field, byte[] value);

    String hmset(byte[] key, Map<String, String> hash);

    List<String> hmget(byte[] key, String... fields);

    Integer hincrBy(byte[] key, byte[] field, int value);

    Integer hexists(byte[] key, byte[] field);

    Integer hdel(byte[] key, byte[] field);

    Integer hlen(byte[] key);

    List<String> hkeys(byte[] key);

    List<String> hvals(byte[] key);

    Map<String, String> hgetAll(byte[] key);

    Integer rpush(byte[] key, byte[] string);

    Integer lpush(byte[] key, byte[] string);

    Integer llen(byte[] key);

    List<String> lrange(byte[] key, int start, int end);

    String ltrim(byte[] key, int start, int end);

    String lindex(byte[] key, int index);

    String lset(byte[] key, int index, byte[] value);

    Integer lrem(byte[] key, int count, byte[] value);

    String lpop(byte[] key);

    String rpop(byte[] key);

    Integer sadd(byte[] key, byte[] member);

    Set<String> smembers(byte[] key);

    Integer srem(byte[] key, byte[] member);

    String spop(byte[] key);

    Integer scard(byte[] key);

    Integer sismember(byte[] key, byte[] member);

    String srandmember(byte[] key);

    Integer zadd(byte[] key, double score, byte[] member);

    Set<String> zrange(byte[] key, int start, int end);

    Integer zrem(byte[] key, byte[] member);

    Double zincrby(byte[] key, double score, byte[] member);

    Integer zrank(byte[] key, byte[] member);

    Integer zrevrank(byte[] key, byte[] member);

    Set<String> zrevrange(byte[] key, int start, int end);

    Set<Tuple> zrangeWithScores(byte[] key, int start, int end);

    Set<Tuple> zrevrangeWithScores(byte[] key, int start, int end);

    Integer zcard(byte[] key);

    Double zscore(byte[] key, byte[] member);

    List<String> sort(byte[] key);

    List<String> sort(byte[] key, SortingParams sortingParameters);

    Integer zcount(byte[] key, double min, double max);

    Set<String> zrangeByScore(byte[] key, double min, double max);

    Set<String> zrangeByScore(
    		byte[] key,
    		double min,
    		double max,
    		int offset,
    		int count);

    Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max);

    Set<Tuple> zrangeByScoreWithScores(
    		byte[] key,
    		double min,
            double max,
            int offset,
            int count);

    Integer zremrangeByRank(byte[] key, int start, int end);

    Integer zremrangeByScore(byte[] key, double start, double end);

    Integer linsert(
    		byte[] key,
    		LIST_POSITION where,
    		byte[] pivot,
    		byte[] value);
}
