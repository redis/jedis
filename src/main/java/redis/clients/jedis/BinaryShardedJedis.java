package redis.clients.jedis;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import redis.clients.jedis.BinaryClient.LIST_POSITION;
import redis.clients.util.Hashing;
import redis.clients.util.Sharded;


public class BinaryShardedJedis extends Sharded<Jedis, JedisShardInfo> implements BinaryJedisCommands {
    public BinaryShardedJedis(List<JedisShardInfo> shards) {
        super(shards);
    }

    public BinaryShardedJedis(List<JedisShardInfo> shards, Hashing algo) {
        super(shards, algo);
    }

    public BinaryShardedJedis(List<JedisShardInfo> shards, Pattern keyTagPattern) {
        super(shards, keyTagPattern);
    }

    public BinaryShardedJedis(List<JedisShardInfo> shards, Hashing algo,
            Pattern keyTagPattern) {
        super(shards, algo, keyTagPattern);
    }

    public void disconnect() throws IOException {
        for (JedisShardInfo jedis : getAllShards()) {
            jedis.getResource().disconnect();
        }
    }

    protected Jedis create(JedisShardInfo shard) {
        return new Jedis(shard);
    }

    public String set(byte[] key, byte[] value) {
        Jedis j = getShard(key);
        return j.set(key, value);
    }

    public byte[] get(byte[] key) {
        Jedis j = getShard(key);
        return j.get(key);
    }

    public Integer exists(byte[] key) {
    	Jedis j = getShard(key);
        return j.exists(key);
    }

    public String type(byte[] key) {
    	Jedis j = getShard(key);
        return j.type(key);
    }

    public Integer expire(byte[] key, int seconds) {
        Jedis j = getShard(key);
        return j.expire(key, seconds);
    }

    public Integer expireAt(byte[] key, long unixTime) {
        Jedis j = getShard(key);
        return j.expireAt(key, unixTime);
    }

    public Integer ttl(byte[] key) {
        Jedis j = getShard(key);
        return j.ttl(key);
    }

    public byte[] getSet(byte[] key, byte[] value) {
        Jedis j = getShard(key);
        return j.getSet(key, value);
    }

    public Integer setnx(byte[] key, byte[] value) {
        Jedis j = getShard(key);
        return j.setnx(key, value);
    }

    public String setex(byte[] key, int seconds, byte[] value) {
        Jedis j = getShard(key);
        return j.setex(key, seconds, value);
    }

    public Integer decrBy(byte[] key, int integer) {
        Jedis j = getShard(key);
        return j.decrBy(key, integer);
    }

    public Integer decr(byte[] key) {
        Jedis j = getShard(key);
        return j.decr(key);
    }

    public Integer incrBy(byte[] key, int integer) {
        Jedis j = getShard(key);
        return j.incrBy(key, integer);
    }

    public Integer incr(byte[] key) {
        Jedis j = getShard(key);
        return j.incr(key);
    }

    public Integer append(byte[] key, byte[] value) {
        Jedis j = getShard(key);
        return j.append(key, value);
    }

    public byte[] substr(byte[] key, int start, int end) {
        Jedis j = getShard(key);
        return j.substr(key, start, end);
    }

    public Integer hset(byte[] key, byte[] field, byte[] value) {
        Jedis j = getShard(key);
        return j.hset(key, field, value);
    }

    public byte[] hget(byte[] key, byte[] field) {
        Jedis j = getShard(key);
        return j.hget(key, field);
    }

    public Integer hsetnx(byte[] key, byte[] field, byte[] value) {
        Jedis j = getShard(key);
        return j.hsetnx(key, field, value);
    }

    public String hmset(byte[] key, Map<byte[], byte[]> hash) {
        Jedis j = getShard(key);
        return j.hmset(key, hash);
    }

    public List<byte[]> hmget(byte[] key, byte[]... fields) {
        Jedis j = getShard(key);
        return j.hmget(key, fields);
    }

    public Integer hincrBy(byte[] key, byte[] field, int value) {
        Jedis j = getShard(key);
        return j.hincrBy(key, field, value);
    }

    public Integer hexists(byte[] key, byte[] field) {
        Jedis j = getShard(key);
        return j.hexists(key, field);
    }

    public Integer hdel(byte[] key, byte[] field) {
        Jedis j = getShard(key);
        return j.hdel(key, field);
    }

    public Integer hlen(byte[] key) {
        Jedis j = getShard(key);
        return j.hlen(key);
    }

    public Set<byte[]> hkeys(byte[] key) {
        Jedis j = getShard(key);
        return j.hkeys(key);
    }

    public Set<byte[]> hvals(byte[] key) {
        Jedis j = getShard(key);
        return j.hvals(key);
    }

    public Map<byte[], byte[]> hgetAll(byte[] key) {
        Jedis j = getShard(key);
        return j.hgetAll(key);
    }

    public Integer rpush(byte[] key, byte[] string) {
        Jedis j = getShard(key);
        return j.rpush(key, string);
    }

    public Integer lpush(byte[] key, byte[] string) {
        Jedis j = getShard(key);
        return j.lpush(key, string);
    }

    public Integer llen(byte[] key) {
        Jedis j = getShard(key);
        return j.llen(key);
    }

    public List<byte[]> lrange(byte[] key, int start, int end) {
        Jedis j = getShard(key);
        return j.lrange(key, start, end);
    }

    public String ltrim(byte[] key, int start, int end) {
        Jedis j = getShard(key);
        return j.ltrim(key, start, end);
    }

    public byte[] lindex(byte[] key, int index) {
        Jedis j = getShard(key);
        return j.lindex(key, index);
    }

    public String lset(byte[] key, int index, byte[] value) {
        Jedis j = getShard(key);
        return j.lset(key, index, value);
    }

    public Integer lrem(byte[] key, int count, byte[] value) {
        Jedis j = getShard(key);
        return j.lrem(key, count, value);
    }

    public byte[] lpop(byte[] key) {
        Jedis j = getShard(key);
        return j.lpop(key);
    }

    public byte[] rpop(byte[] key) {
        Jedis j = getShard(key);
        return j.rpop(key);
    }

    public Integer sadd(byte[] key, byte[] member) {
        Jedis j = getShard(key);
        return j.sadd(key, member);
    }

    public Set<byte[]> smembers(byte[] key) {
        Jedis j = getShard(key);
        return j.smembers(key);
    }

    public Integer srem(byte[] key, byte[] member) {
        Jedis j = getShard(key);
        return j.srem(key, member);
    }

    public byte[] spop(byte[] key) {
        Jedis j = getShard(key);
        return j.spop(key);
    }

    public Integer scard(byte[] key) {
        Jedis j = getShard(key);
        return j.scard(key);
    }

    public Integer sismember(byte[] key, byte[] member) {
        Jedis j = getShard(key);
        return j.sismember(key, member);
    }

    public byte[] srandmember(byte[] key) {
        Jedis j = getShard(key);
        return j.srandmember(key);
    }

    public Integer zadd(byte[] key, double score, byte[] member) {
        Jedis j = getShard(key);
        return j.zadd(key, score, member);
    }

    public Set<byte[]> zrange(byte[] key, int start, int end) {
        Jedis j = getShard(key);
        return j.zrange(key, start, end);
    }

    public Integer zrem(byte[] key, byte[] member) {
        Jedis j = getShard(key);
        return j.zrem(key, member);
    }

    public Double zincrby(byte[] key, double score, byte[] member) {
        Jedis j = getShard(key);
        return j.zincrby(key, score, member);
    }

    public Integer zrank(byte[] key, byte[] member) {
        Jedis j = getShard(key);
        return j.zrank(key, member);
    }

    public Integer zrevrank(byte[] key, byte[] member) {
        Jedis j = getShard(key);
        return j.zrevrank(key, member);
    }

    public Set<byte[]> zrevrange(byte[] key, int start, int end) {
        Jedis j = getShard(key);
        return j.zrevrange(key, start, end);
    }

    public Set<Tuple> zrangeWithScores(byte[] key, int start, int end) {
        Jedis j = getShard(key);
        return j.zrangeWithScores(key, start, end);
    }

    public Set<Tuple> zrevrangeWithScores(byte[] key, int start, int end) {
        Jedis j = getShard(key);
        return j.zrevrangeWithScores(key, start, end);
    }

    public Integer zcard(byte[] key) {
        Jedis j = getShard(key);
        return j.zcard(key);
    }

    public Double zscore(byte[] key, byte[] member) {
        Jedis j = getShard(key);
        return j.zscore(key, member);
    }

    public List<byte[]> sort(byte[] key) {
        Jedis j = getShard(key);
        return j.sort(key);
    }

    public List<byte[]> sort(byte[] key, SortingParams sortingParameters) {
        Jedis j = getShard(key);
        return j.sort(key, sortingParameters);
    }

    public Integer zcount(byte[] key, double min, double max) {
        Jedis j = getShard(key);
        return j.zcount(key, min, max);
    }

    public Set<byte[]> zrangeByScore(byte[] key, double min, double max) {
        Jedis j = getShard(key);
        return j.zrangeByScore(key, min, max);
    }

    public Set<byte[]> zrangeByScore(byte[] key, double min, double max,
            int offset, int count) {
        Jedis j = getShard(key);
        return j.zrangeByScore(key, min, max, offset, count);
    }

    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min, double max) {
        Jedis j = getShard(key);
        return j.zrangeByScoreWithScores(key, min, max);
    }

    public Set<Tuple> zrangeByScoreWithScores(byte[] key, double min,
            double max, int offset, int count) {
        Jedis j = getShard(key);
        return j.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    public Integer zremrangeByRank(byte[] key, int start, int end) {
        Jedis j = getShard(key);
        return j.zremrangeByRank(key, start, end);
    }

    public Integer zremrangeByScore(byte[] key, double start, double end) {
        Jedis j = getShard(key);
        return j.zremrangeByScore(key, start, end);
    }

    public Integer linsert(byte[] key, LIST_POSITION where, byte[] pivot,
            byte[] value) {
        Jedis j = getShard(key);
        return j.linsert(key, where, pivot, value);
    }

    public List<Object> pipelined(ShardedJedisPipeline shardedJedisPipeline) {
        shardedJedisPipeline.setShardedJedis(this);
        shardedJedisPipeline.execute();
        return shardedJedisPipeline.getResults();
    }
}