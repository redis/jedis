package redis.clients.jedis;

import java.util.List;
import java.util.Map;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

public class BinaryTransaction {
    protected Client client = null;
    protected boolean inTransaction = true;

    public BinaryTransaction() {
    }

    public BinaryTransaction(final Client client) {
        this.client = client;
    }

    public void ping() {
        client.ping();
    }

    public void set(final byte[] key, final byte[] value) {
        client.set(key, value);
    }

    public void get(final byte[] key) {
        client.get(key);
    }

    public void exists(final byte[] key) {
        client.exists(key);
    }

    public void del(final byte[]... keys) {
        client.del(keys);
    }

    public void type(final byte[] key) {
        client.type(key);
    }

    public void flushDB() {
        client.flushDB();
    }

    public void keys(final byte[] pattern) {
        client.keys(pattern);
    }

    public void randomBinaryKey() {
        client.randomKey();
    }

    public void rename(final byte[] oldkey, final byte[] newkey) {
        client.rename(oldkey, newkey);
    }

    public void renamenx(final byte[] oldkey, final byte[] newkey) {
        client.renamenx(oldkey, newkey);
    }

    public void dbSize() {
        client.dbSize();
    }

    public void expire(final byte[] key, final int seconds) {
        client.expire(key, seconds);
    }

    public void expireAt(final byte[] key, final long unixTime) {
        client.expireAt(key, unixTime);
    }

    public void ttl(final byte[] key) {
        client.ttl(key);
    }

    public void select(final int index) {
        client.select(index);
    }

    public void move(final byte[] key, final int dbIndex) {
        client.move(key, dbIndex);
    }

    public void flushAll() {
        client.flushAll();
    }

    public void getSet(final byte[] key, final byte[] value) {
        client.getSet(key, value);
    }

    public void mget(final byte[]... keys) {
        client.mget(keys);
    }

    public void setnx(final byte[] key, final byte[] value) {
        client.setnx(key, value);
    }

    public void setex(final byte[] key, final int seconds, final byte[] value) {
        client.setex(key, seconds, value);
    }

    public void mset(final byte[]... keysvalues) {
        client.mset(keysvalues);
    }

    public void msetnx(final byte[]... keysvalues) {
        client.msetnx(keysvalues);
    }

    public void decrBy(final byte[] key, final int integer) {
        client.decrBy(key, integer);
    }

    public void decr(final byte[] key) {
        client.decr(key);
    }

    public void incrBy(final byte[] key, final int integer) {
        client.incrBy(key, integer);
    }

    public void incr(final byte[] key) {
        client.incr(key);
    }

    public void append(final byte[] key, final byte[] value) {
        client.append(key, value);
    }

    public void substr(final byte[] key, final int start, final int end) {
        client.substr(key, start, end);
    }

    public void hset(final byte[] key, final byte[] field, final byte[] value) {
        client.hset(key, field, value);
    }

    public void hget(final byte[] key, final byte[] field) {
        client.hget(key, field);
    }

    public void hsetnx(final byte[] key, final byte[] field,
            final byte[] value) {
        client.hsetnx(key, field, value);
    }

    public void hmset(final byte[] key, final Map<byte[], byte[]> hash) {
        client.hmset(key, hash);
    }

    public void hmget(final byte[] key, final byte[]... fields) {
        client.hmget(key, fields);
    }

    public void hincrBy(final byte[] key, final byte[] field, final int value) {
        client.hincrBy(key, field, value);
    }

    public void hexists(final byte[] key, final byte[] field) {
        client.hexists(key, field);
    }

    public void hdel(final byte[] key, final byte[] field) {
        client.hdel(key, field);
    }

    public void hlen(final byte[] key) {
        client.hlen(key);
    }

    public void hkeys(final byte[] key) {
        client.hkeys(key);
    }

    public void hvals(final byte[] key) {
        client.hvals(key);
    }

    public void hgetAll(final byte[] key) {
        client.hgetAll(key);
    }

    public void rpush(final byte[] key, final byte[] string) {
        client.rpush(key, string);
    }

    public void lpush(final byte[] key, final byte[] string) {
        client.lpush(key, string);
    }

    public void llen(final byte[] key) {
        client.llen(key);
    }

    public void lrange(final byte[] key, final int start, final int end) {
        client.lrange(key, start, end);
    }

    public void ltrim(final byte[] key, final int start, final int end) {
        client.ltrim(key, start, end);
    }

    public void lindex(final byte[] key, final int index) {
        client.lindex(key, index);
    }

    public void lset(final byte[] key, final int index, final byte[] value) {
        client.lset(key, index, value);
    }

    public void lrem(final byte[] key, final int count, final byte[] value) {
        client.lrem(key, count, value);
    }

    public void lpop(final byte[] key) {
        client.lpop(key);
    }

    public void rpop(final byte[] key) {
        client.rpop(key);
    }

    public void rpoplpush(final byte[] srckey, final byte[] dstkey) {
        client.rpoplpush(srckey, dstkey);
    }

    public void sadd(final byte[] key, final byte[] member) {
        client.sadd(key, member);
    }

    public void smembers(final byte[] key) {
        client.smembers(key);
    }

    public void srem(final byte[] key, final byte[] member) {
        client.srem(key, member);
    }

    public void spop(final byte[] key) {
        client.spop(key);
    }

    public void smove(final byte[] srckey, final byte[] dstkey,
            final byte[] member) {
        client.smove(srckey, dstkey, member);
    }

    public void scard(final byte[] key) {
        client.scard(key);
    }

    public void sismember(final byte[] key, final byte[] member) {
        client.sismember(key, member);
    }

    public void sinter(final byte[]... keys) {
        client.sinter(keys);
    }

    public void sinterstore(final byte[] dstkey, final byte[]... keys) {
        client.sinterstore(dstkey, keys);
    }

    public void sunion(final byte[]... keys) {
        client.sunion(keys);
    }

    public void sunionstore(final byte[] dstkey, final byte[]... keys) {
        client.sunionstore(dstkey, keys);
    }

    public void sdiff(final byte[]... keys) {
        client.sdiff(keys);
    }

    public void sdiffstore(final byte[] dstkey, final byte[]... keys) {
        client.sdiffstore(dstkey, keys);
    }

    public void srandmember(final byte[] key) {
        client.srandmember(key);
    }

    public void zadd(final byte[] key, final double score, final byte[] member) {
        client.zadd(key, score, member);
    }

    public void zrange(final byte[] key, final int start, final int end) {
        client.zrange(key, start, end);
    }

    public void zrem(final byte[] key, final byte[] member) {
        client.zrem(key, member);
    }

    public void zincrby(final byte[] key, final double score,
            final byte[] member) {
        client.zincrby(key, score, member);
    }

    public void zrank(final byte[] key, final byte[] member) {
        client.zrank(key, member);
    }

    public void zrevrank(final byte[] key, final byte[] member) {
        client.zrevrank(key, member);
    }

    public void zrevrange(final byte[] key, final int start, final int end) {
        client.zrevrange(key, start, end);
    }

    public void zrangeWithScores(final byte[] key, final int start,
            final int end) {
        client.zrangeWithScores(key, start, end);
    }

    public void zrevrangeWithScores(final byte[] key, final int start,
            final int end) {
        client.zrevrangeWithScores(key, start, end);
    }

    public void zcard(final byte[] key) {
        client.zcard(key);
    }

    public void zscore(final byte[] key, final byte[] member) {
        client.zscore(key, member);
    }

    public List<Object> exec() {
        client.exec();
        client.getAll(1); // Discard all but the last reply
        return client.getObjectMultiBulkReply();
    }

    public String discard() {
        client.discard();
        client.getAll(1); // Discard all but the last reply
        inTransaction = false;
        return client.getStatusCodeReply();
    }

    public void sort(final byte[] key) {
        client.sort(key);
    }

    public void sort(final byte[] key, final SortingParams sortingParameters) {
        client.sort(key, sortingParameters);
    }

    public void sort(final byte[] key, final SortingParams sortingParameters,
            final byte[] dstkey) {
        client.sort(key, sortingParameters, dstkey);
    }

    public void sort(final byte[] key, final byte[] dstkey) {
        client.sort(key, dstkey);
    }

    public void setbit(byte[] key, long offset, byte[] value) {
        client.setbit(key, offset, value);
    }

    public void getbit(byte[] key, long offset) {
        client.getbit(key, offset);
    }

    public void linsert(final byte[] key, final LIST_POSITION where,
            final byte[] pivot, final byte[] value) {
        client.linsert(key, where, pivot, value);
    }
}