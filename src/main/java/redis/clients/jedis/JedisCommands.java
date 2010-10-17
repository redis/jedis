package redis.clients.jedis;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Common interface for sharded and non-sharded Jedis
 */
public interface JedisCommands {
    String set(String key, String value);

    String get(String key);

    Integer exists(String key);

    String type(String key);

    Integer expire(String key, int seconds);

    Integer expireAt(String key, long unixTime);

    Integer ttl(String key);

    String getSet(String key, String value);

    Integer setnx(String key, String value);

    String setex(String key, int seconds, String value);

    Integer decrBy(String key, int integer);

    Integer decr(String key);

    Integer incrBy(String key, int integer);

    Integer incr(String key);

    Integer append(String key, String value);

    String substr(String key, int start, int end);

    Integer hset(String key, String field, String value);

    String hget(String key, String field);

    Integer hsetnx(String key, String field, String value);

    String hmset(String key, Map<String, String> hash);

    List<String> hmget(String key, String... fields);

    Integer hincrBy(String key, String field, int value);

    Integer hexists(String key, String field);

    Integer hdel(String key, String field);

    Integer hlen(String key);

    List<String> hkeys(String key);

    List<String> hvals(String key);

    Map<String, String> hgetAll(String key);

    Integer rpush(String key, String string);

    Integer lpush(String key, String string);

    Integer llen(String key);

    List<String> lrange(String key, int start, int end);

    String ltrim(String key, int start, int end);

    String lindex(String key, int index);

    String lset(String key, int index, String value);

    Integer lrem(String key, int count, String value);

    String lpop(String key);

    String rpop(String key);

    Integer sadd(String key, String member);

    Set<String> smembers(String key);

    Integer srem(String key, String member);

    String spop(String key);

    Integer scard(String key);

    Integer sismember(String key, String member);

    String srandmember(String key);

    Integer zadd(String key, double score, String member);

    Set<String> zrange(String key, int start, int end);

    Integer zrem(String key, String member);

    Double zincrby(String key, double score, String member);

    Integer zrank(String key, String member);

    Integer zrevrank(String key, String member);

    Set<String> zrevrange(String key, int start, int end);

    Set<Tuple> zrangeWithScores(String key, int start, int end);

    Set<Tuple> zrevrangeWithScores(String key, int start, int end);

    Integer zcard(String key);

    Double zscore(String key, String member);

    List<String> sort(String key);

    List<String> sort(String key, SortingParams sortingParameters);

    Integer zcount(String key, double min, double max);

    Set<String> zrangeByScore(String key, double min, double max);

    Set<String> zrangeByScore(String key, double min, double max,
	    int offset, int count);

    Set<Tuple> zrangeByScoreWithScores(String key, double min, double max);

    Set<Tuple> zrangeByScoreWithScores(String key, double min,
            double max, int offset, int count);

    Integer zremrangeByRank(String key, int start, int end);

    Integer zremrangeByScore(String key, double start, double end);

    Integer linsert(String key, Client.LIST_POSITION where, String pivot,
                String value);
}
