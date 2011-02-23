package redis.clients.jedis;

import java.util.Map;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

public class Transaction extends BinaryTransaction {
    public Transaction() {
    }

    public Transaction(final Client client) {
        super(client);
    }

    public String set(final String key, final String value) {
        client.set(key, value);
        return client.getStatusCodeReply();
    }

    public String get(final String key) {
        client.get(key);
        return client.getStatusCodeReply();
    }

    public String exists(final String key) {
        client.exists(key);
        return client.getStatusCodeReply();
    }

    public String del(final String... keys) {
        client.del(keys);
        return client.getStatusCodeReply();
    }

    public String type(final String key) {
        client.type(key);
        return client.getStatusCodeReply();
    }

    public String keys(final String pattern) {
        client.keys(pattern);
        return client.getStatusCodeReply();
    }

    public String randomKey() {
        client.randomKey();
        return client.getBulkReply();
    }

    public String rename(final String oldkey, final String newkey) {
        client.rename(oldkey, newkey);
        return client.getStatusCodeReply();
    }

    public String renamenx(final String oldkey, final String newkey) {
        client.renamenx(oldkey, newkey);
        return client.getStatusCodeReply();
    }

    public String expire(final String key, final int seconds) {
        client.expire(key, seconds);
        return client.getStatusCodeReply();
    }

    public String expireAt(final String key, final long unixTime) {
        client.expireAt(key, unixTime);
        return client.getStatusCodeReply();
    }

    public String ttl(final String key) {
        client.ttl(key);
        return client.getStatusCodeReply();
    }

    public String move(final String key, final int dbIndex) {
        client.move(key, dbIndex);
        return client.getStatusCodeReply();
    }

    public String getSet(final String key, final String value) {
        client.getSet(key, value);
        return client.getStatusCodeReply();
    }

    public String mget(final String... keys) {
        client.mget(keys);
        return client.getStatusCodeReply();
    }

    public String setnx(final String key, final String value) {
        client.setnx(key, value);
        return client.getStatusCodeReply();
    }

    public String setex(final String key, final int seconds, final String value) {
        client.setex(key, seconds, value);
        return client.getStatusCodeReply();
    }

    public String mset(final String... keysvalues) {
        client.mset(keysvalues);
        return client.getStatusCodeReply();
    }

    public String msetnx(final String... keysvalues) {
        client.msetnx(keysvalues);
        return client.getStatusCodeReply();
    }

    public String decrBy(final String key, final int integer) {
        client.decrBy(key, integer);
        return client.getStatusCodeReply();
    }

    public String decr(final String key) {
        client.decr(key);
        return client.getStatusCodeReply();
    }

    public String incrBy(final String key, final int integer) {
        client.incrBy(key, integer);
        return client.getStatusCodeReply();
    }

    public String incr(final String key) {
        client.incr(key);
        return client.getStatusCodeReply();
    }

    public String append(final String key, final String value) {
        client.append(key, value);
        return client.getStatusCodeReply();
    }

    public String substr(final String key, final int start, final int end) {
        client.substr(key, start, end);
        return client.getStatusCodeReply();
    }

    public String hset(final String key, final String field, final String value) {
        client.hset(key, field, value);
        return client.getStatusCodeReply();
    }

    public String hget(final String key, final String field) {
        client.hget(key, field);
        return client.getStatusCodeReply();
    }

    public String hsetnx(final String key, final String field,
            final String value) {
        client.hsetnx(key, field, value);
        return client.getStatusCodeReply();
    }

    public String hmset(final String key, final Map<String, String> hash) {
        client.hmset(key, hash);
        return client.getStatusCodeReply();
    }

    public String hmget(final String key, final String... fields) {
        client.hmget(key, fields);
        return client.getStatusCodeReply();
    }

    public String hincrBy(final String key, final String field, final int value) {
        client.hincrBy(key, field, value);
        return client.getStatusCodeReply();
    }

    public String hexists(final String key, final String field) {
        client.hexists(key, field);
        return client.getStatusCodeReply();
    }

    public String hdel(final String key, final String field) {
        client.hdel(key, field);
        return client.getStatusCodeReply();
    }

    public String hlen(final String key) {
        client.hlen(key);
        return client.getStatusCodeReply();
    }

    public String hkeys(final String key) {
        client.hkeys(key);
        return client.getStatusCodeReply();
    }

    public String hvals(final String key) {
        client.hvals(key);
        return client.getStatusCodeReply();
    }

    public String hgetAll(final String key) {
        client.hgetAll(key);
        return client.getStatusCodeReply();
    }

    public String rpush(final String key, final String string) {
        client.rpush(key, string);
        return client.getStatusCodeReply();
    }

    public String lpush(final String key, final String string) {
        client.lpush(key, string);
        return client.getStatusCodeReply();
    }

    public String llen(final String key) {
        client.llen(key);
        return client.getStatusCodeReply();
    }

    public String lrange(final String key, final int start, final int end) {
        client.lrange(key, start, end);
        return client.getStatusCodeReply();
    }

    public String ltrim(String key, final int start, final int end) {
        client.ltrim(key, start, end);
        return client.getStatusCodeReply();
    }

    public String lindex(final String key, final int index) {
        client.lindex(key, index);
        return client.getStatusCodeReply();
    }

    public String lset(final String key, final int index, final String value) {
        client.lset(key, index, value);
        return client.getStatusCodeReply();
    }

    public String lrem(final String key, final int count, final String value) {
        client.lrem(key, count, value);
        return client.getStatusCodeReply();
    }

    public String lpop(final String key) {
        client.lpop(key);
        return client.getStatusCodeReply();
    }

    public String rpop(final String key) {
        client.rpop(key);
        return client.getStatusCodeReply();
    }

    public String rpoplpush(final String srckey, final String dstkey) {
        client.rpoplpush(srckey, dstkey);
        return client.getStatusCodeReply();
    }

    public String sadd(final String key, final String member) {
        client.sadd(key, member);
        return client.getStatusCodeReply();
    }

    public String smembers(final String key) {
        client.smembers(key);
        return client.getStatusCodeReply();
    }

    public String srem(final String key, final String member) {
        client.srem(key, member);
        return client.getStatusCodeReply();
    }

    public String spop(final String key) {
        client.spop(key);
        return client.getStatusCodeReply();
    }

    public String smove(final String srckey, final String dstkey,
            final String member) {
        client.smove(srckey, dstkey, member);
        return client.getStatusCodeReply();
    }

    public String scard(final String key) {
        client.scard(key);
        return client.getStatusCodeReply();
    }

    public String sismember(final String key, final String member) {
        client.sismember(key, member);
        return client.getStatusCodeReply();
    }

    public String sinter(final String... keys) {
        client.sinter(keys);
        return client.getStatusCodeReply();
    }

    public String sinterstore(final String dstkey, final String... keys) {
        client.sinterstore(dstkey, keys);
        return client.getStatusCodeReply();
    }

    public String sunion(final String... keys) {
        client.sunion(keys);
        return client.getStatusCodeReply();
    }

    public String sunionstore(final String dstkey, final String... keys) {
        client.sunionstore(dstkey, keys);
        return client.getStatusCodeReply();
    }

    public String sdiff(final String... keys) {
        client.sdiff(keys);
        return client.getStatusCodeReply();
    }

    public String sdiffstore(final String dstkey, final String... keys) {
        client.sdiffstore(dstkey, keys);
        return client.getStatusCodeReply();
    }

    public String srandmember(final String key) {
        client.srandmember(key);
        return client.getStatusCodeReply();
    }

    public String zadd(final String key, final double score, final String member) {
        client.zadd(key, score, member);
        return client.getStatusCodeReply();
    }

    public String zrange(final String key, final int start, final int end) {
        client.zrange(key, start, end);
        return client.getStatusCodeReply();
    }

    public String zrem(final String key, final String member) {
        client.zrem(key, member);
        return client.getStatusCodeReply();
    }

    public String zincrby(final String key, final double score,
            final String member) {
        client.zincrby(key, score, member);
        return client.getStatusCodeReply();
    }

    public String zrank(final String key, final String member) {
        client.zrank(key, member);
        return client.getStatusCodeReply();
    }

    public String zrevrank(final String key, final String member) {
        client.zrevrank(key, member);
        return client.getStatusCodeReply();
    }

    public String zrevrange(final String key, final int start, final int end) {
        client.zrevrange(key, start, end);
        return client.getStatusCodeReply();
    }

    public String zrangeWithScores(final String key, final int start,
            final int end) {
        client.zrangeWithScores(key, start, end);
        return client.getStatusCodeReply();
    }

    public String zrevrangeWithScores(final String key, final int start,
            final int end) {
        client.zrevrangeWithScores(key, start, end);
        return client.getStatusCodeReply();
    }

    public String zcard(final String key) {
        client.zcard(key);
        return client.getStatusCodeReply();
    }

    public String zscore(final String key, final String member) {
        client.zscore(key, member);
        return client.getStatusCodeReply();
    }

    public String sort(final String key) {
        client.sort(key);
        return client.getStatusCodeReply();
    }

    public String sort(final String key, final SortingParams sortingParameters) {
        client.sort(key, sortingParameters);
        return client.getStatusCodeReply();
    }

    public String sort(final String key, final String dstkey) {
        client.sort(key, dstkey);
        return client.getStatusCodeReply();
    }

    public String sort(final String key, final SortingParams sortingParameters,
            final String dstkey) {
        client.sort(key, sortingParameters, dstkey);
        return client.getStatusCodeReply();
    }

    public String setbit(String key, long offset, boolean value) {
        client.setbit(key, offset, value);
        return client.getStatusCodeReply();
    }

    public boolean getbit(String key, long offset) {
        client.getbit(key, offset);
        return client.getStatusCodeReply() == "1";
    }

    public long setrange(String key, long offset, String value) {
        client.setrange(key, offset, value);
        return client.getIntegerReply();
    }

    public String getrange(String key, long startOffset, long endOffset) {
        client.getrange(key, startOffset, endOffset);
        return client.getBulkReply();
    }

    public String linsert(final String key, final LIST_POSITION where,
            final String pivot, final String value) {
        client.linsert(key, where, pivot, value);
        return client.getStatusCodeReply();
    }
}