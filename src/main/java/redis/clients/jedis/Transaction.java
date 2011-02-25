package redis.clients.jedis;

import java.util.Map;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

public class Transaction extends BinaryTransaction {
    public Transaction() {
    }

    public Transaction(final Client client) {
        super(client);
    }

    public void set(final String key, final String value) {
        client.set(key, value);
    }

    public void get(final String key) {
        client.get(key);
    }

    public void exists(final String key) {
        client.exists(key);
    }

    public void del(final String... keys) {
        client.del(keys);
    }

    public void type(final String key) {
        client.type(key);
    }

    public void keys(final String pattern) {
        client.keys(pattern);
    }

    public void randomKey() {
        client.randomKey();
    }

    public void rename(final String oldkey, final String newkey) {
        client.rename(oldkey, newkey);
    }

    public void renamenx(final String oldkey, final String newkey) {
        client.renamenx(oldkey, newkey);
    }

    public void expire(final String key, final int seconds) {
        client.expire(key, seconds);
    }

    public void expireAt(final String key, final long unixTime) {
        client.expireAt(key, unixTime);
    }

    public void ttl(final String key) {
        client.ttl(key);
    }

    public void move(final String key, final int dbIndex) {
        client.move(key, dbIndex);
    }

    public void getSet(final String key, final String value) {
        client.getSet(key, value);
    }

    public void mget(final String... keys) {
        client.mget(keys);
    }

    public void setnx(final String key, final String value) {
        client.setnx(key, value);
    }

    public void setex(final String key, final int seconds, final String value) {
        client.setex(key, seconds, value);
    }

    public void mset(final String... keysvalues) {
        client.mset(keysvalues);
    }

    public void msetnx(final String... keysvalues) {
        client.msetnx(keysvalues);
    }

    public void decrBy(final String key, final int integer) {
        client.decrBy(key, integer);
    }

    public void decr(final String key) {
        client.decr(key);
    }

    public void incrBy(final String key, final int integer) {
        client.incrBy(key, integer);
    }

    public void incr(final String key) {
        client.incr(key);
    }

    public void append(final String key, final String value) {
        client.append(key, value);
    }

    public void substr(final String key, final int start, final int end) {
        client.substr(key, start, end);
    }

    public void hset(final String key, final String field, final String value) {
        client.hset(key, field, value);
    }

    public void hget(final String key, final String field) {
        client.hget(key, field);
    }

    public void hsetnx(final String key, final String field,
            final String value) {
        client.hsetnx(key, field, value);
    }

    public void hmset(final String key, final Map<String, String> hash) {
        client.hmset(key, hash);
    }

    public void hmget(final String key, final String... fields) {
        client.hmget(key, fields);
    }

    public void hincrBy(final String key, final String field, final int value) {
        client.hincrBy(key, field, value);
    }

    public void hexists(final String key, final String field) {
        client.hexists(key, field);
    }

    public void hdel(final String key, final String field) {
        client.hdel(key, field);
    }

    public void hlen(final String key) {
        client.hlen(key);
    }

    public void hkeys(final String key) {
        client.hkeys(key);
    }

    public void hvals(final String key) {
        client.hvals(key);
    }

    public void hgetAll(final String key) {
        client.hgetAll(key);
    }

    public void rpush(final String key, final String string) {
        client.rpush(key, string);
    }

    public void lpush(final String key, final String string) {
        client.lpush(key, string);
    }

    public void llen(final String key) {
        client.llen(key);
    }

    public void lrange(final String key, final int start, final int end) {
        client.lrange(key, start, end);
    }

    public void ltrim(String key, final int start, final int end) {
        client.ltrim(key, start, end);
    }

    public void lindex(final String key, final int index) {
        client.lindex(key, index);
    }

    public void lset(final String key, final int index, final String value) {
        client.lset(key, index, value);
    }

    public void lrem(final String key, final int count, final String value) {
        client.lrem(key, count, value);
    }

    public void lpop(final String key) {
        client.lpop(key);
    }

    public void rpop(final String key) {
        client.rpop(key);
    }

    public void rpoplpush(final String srckey, final String dstkey) {
        client.rpoplpush(srckey, dstkey);
    }

    public void sadd(final String key, final String member) {
        client.sadd(key, member);
    }

    public void smembers(final String key) {
        client.smembers(key);
    }

    public void srem(final String key, final String member) {
        client.srem(key, member);
    }

    public void spop(final String key) {
        client.spop(key);
    }

    public void smove(final String srckey, final String dstkey,
            final String member) {
        client.smove(srckey, dstkey, member);
    }

    public void scard(final String key) {
        client.scard(key);
    }

    public void sismember(final String key, final String member) {
        client.sismember(key, member);
    }

    public void sinter(final String... keys) {
        client.sinter(keys);
    }

    public void sinterstore(final String dstkey, final String... keys) {
        client.sinterstore(dstkey, keys);
    }

    public void sunion(final String... keys) {
        client.sunion(keys);
    }

    public void sunionstore(final String dstkey, final String... keys) {
        client.sunionstore(dstkey, keys);
    }

    public void sdiff(final String... keys) {
        client.sdiff(keys);
    }

    public void sdiffstore(final String dstkey, final String... keys) {
        client.sdiffstore(dstkey, keys);
    }

    public void srandmember(final String key) {
        client.srandmember(key);
    }

    public void zadd(final String key, final double score, final String member) {
        client.zadd(key, score, member);
    }

    public void zrange(final String key, final int start, final int end) {
        client.zrange(key, start, end);
    }

    public void zrem(final String key, final String member) {
        client.zrem(key, member);
    }

    public void zincrby(final String key, final double score,
            final String member) {
        client.zincrby(key, score, member);
    }

    public void zrank(final String key, final String member) {
        client.zrank(key, member);
    }

    public void zrevrank(final String key, final String member) {
        client.zrevrank(key, member);
    }

    public void zrevrange(final String key, final int start, final int end) {
        client.zrevrange(key, start, end);
    }

    public void zrangeWithScores(final String key, final int start,
            final int end) {
        client.zrangeWithScores(key, start, end);
    }

    public void zrevrangeWithScores(final String key, final int start,
            final int end) {
        client.zrevrangeWithScores(key, start, end);
    }

    public void zcard(final String key) {
        client.zcard(key);
    }

    public void zscore(final String key, final String member) {
        client.zscore(key, member);
    }

    public void sort(final String key) {
        client.sort(key);
    }

    public void sort(final String key, final SortingParams sortingParameters) {
        client.sort(key, sortingParameters);
    }

    public void sort(final String key, final String dstkey) {
        client.sort(key, dstkey);
    }

    public void sort(final String key, final SortingParams sortingParameters,
            final String dstkey) {
        client.sort(key, sortingParameters, dstkey);
    }

    public void setbit(String key, long offset, String value) {
        client.setbit(key, offset, value);
    }

    public void getbit(String key, long offset) {
        client.getbit(key, offset);
    }

    public void linsert(final String key, final LIST_POSITION where,
            final String pivot, final String value) {
        client.linsert(key, where, pivot, value);
    }
}