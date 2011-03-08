package redis.clients.jedis;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

import java.util.List;
import java.util.Map;

public class Pipeline implements Commands {
    private Client client;

    public Pipeline(Client client) {
        this.client = client;
    }

    public void append(String key, String value) {
        client.append(key, value);
    }

    public void append(byte[] key, byte[] value) {
        client.append(key, value);
    }

    public void blpop(String[] args) {
        client.blpop(args);
    }

    public void blpop(byte[][] args) {
        client.blpop(args);
    }

    public void brpop(String[] args) {
        client.brpop(args);
    }

    public void brpop(byte[][] args) {
        client.brpop(args);
    }

    public void decr(String key) {
        client.decr(key);
    }

    public void decr(byte[] key) {
        client.decr(key);
    }

    public void decrBy(String key, long integer) {
        client.decrBy(key, integer);
    }

    public void decrBy(byte[] key, long integer) {
        client.decrBy(key, integer);
    }

    public void del(String... keys) {
        client.del(keys);
    }

    public void del(byte[]... keys) {
        client.del(keys);
    }

    public void echo(String string) {
        client.echo(string);
    }

    public void echo(byte[] string) {
        client.echo(string);
    }

    public void exists(String key) {
        client.exists(key);
    }

    public void exists(byte[] key) {
        client.exists(key);
    }

    public void expire(String key, int seconds) {
        client.expire(key, seconds);
    }

    public void expire(byte[] key, int seconds) {
        client.expire(key, seconds);
    }

    public void expireAt(String key, long unixTime) {
        client.expireAt(key, unixTime);
    }

    public void expireAt(byte[] key, long unixTime) {
        client.expireAt(key, unixTime);
    }

    public void get(String key) {
        client.get(key);
    }

    public void get(byte[] key) {
        client.get(key);
    }

    public void getbit(String key, long offset) {
        client.getbit(key, offset);
    }

    public void getrange(String key, long startOffset, long endOffset) {
        client.getrange(key, startOffset, endOffset);
    }

    public void getSet(String key, String value) {
        client.getSet(key, value);
    }

    public void getSet(byte[] key, byte[] value) {
        client.getSet(key, value);
    }

    public void hdel(String key, String field) {
        client.hdel(key, field);
    }

    public void hdel(byte[] key, byte[] field) {
        client.hdel(key, field);
    }

    public void hexists(String key, String field) {
        client.hexists(key, field);
    }

    public void hexists(byte[] key, byte[] field) {
        client.hexists(key, field);
    }

    public void hget(String key, String field) {
        client.hget(key, field);
    }

    public void hget(byte[] key, byte[] field) {
        client.hget(key, field);
    }

    public void hgetAll(String key) {
        client.hgetAll(key);
    }

    public void hgetAll(byte[] key) {
        client.hgetAll(key);
    }

    public void hincrBy(String key, String field, long value) {
        client.hincrBy(key, field, value);
    }

    public void hincrBy(byte[] key, byte[] field, long value) {
        client.hincrBy(key, field, value);
    }

    public void hkeys(String key) {
        client.hkeys(key);
    }

    public void hkeys(byte[] key) {
        client.hkeys(key);
    }

    public void hlen(String key) {
        client.hlen(key);
    }

    public void hlen(byte[] key) {
        client.hlen(key);
    }

    public void hmget(String key, String... fields) {
        client.hmget(key, fields);
    }

    public void hmget(byte[] key, byte[]... fields) {
        client.hmget(key, fields);
    }

    public void hmset(String key, Map<String, String> hash) {
        client.hmset(key, hash);
    }

    public void hmset(byte[] key, Map<byte[], byte[]> hash) {
        client.hmset(key, hash);
    }

    public void hset(String key, String field, String value) {
        client.hset(key, field, value);
    }

    public void hset(byte[] key, byte[] field, byte[] value) {
        client.hset(key, field, value);
    }

    public void hsetnx(String key, String field, String value) {
        client.hsetnx(key, field, value);
    }

    public void hsetnx(byte[] key, byte[] field, byte[] value) {
        client.hsetnx(key, field, value);
    }

    public void hvals(String key) {
        client.hvals(key);
    }

    public void hvals(byte[] key) {
        client.hvals(key);
    }

    public void incr(String key) {
        client.incr(key);
    }

    public void incr(byte[] key) {
        client.incr(key);
    }

    public void incrBy(String key, long integer) {
        client.incrBy(key, integer);
    }

    public void incrBy(byte[] key, long integer) {
        client.incrBy(key, integer);
    }

    public void keys(String pattern) {
        client.keys(pattern);
    }

    public void keys(byte[] pattern) {
        client.keys(pattern);
    }

    public void lindex(String key, int index) {
        client.lindex(key, index);
    }

    public void lindex(byte[] key, int index) {
        client.lindex(key, index);
    }

    public void linsert(String key, LIST_POSITION where, String pivot,
            String value) {
        client.linsert(key, where, pivot, value);
    }

    public void linsert(byte[] key, LIST_POSITION where, byte[] pivot,
            byte[] value) {
        client.linsert(key, where, pivot, value);
    }

    public void llen(String key) {
        client.llen(key);
    }

    public void llen(byte[] key) {
        client.llen(key);
    }

    public void lpop(String key) {
        client.lpop(key);
    }

    public void lpop(byte[] key) {
        client.lpop(key);
    }

    public void lpush(String key, String string) {
        client.lpush(key, string);
    }

    public void lpush(byte[] key, byte[] string) {
        client.lpush(key, string);
    }

    public void lpushx(String key, String string) {
        client.lpushx(key, string);
    }

    public void lpushx(byte[] key, byte[] bytes) {
        client.lpushx(key, bytes);
    }

    public void lrange(String key, int start, int end) {
        client.lrange(key, start, end);
    }

    public void lrange(byte[] key, int start, int end) {
        client.lrange(key, start, end);
    }

    public void lrem(String key, int count, String value) {
        client.lrem(key, count, value);
    }

    public void lrem(byte[] key, int count, byte[] value) {
        client.lrem(key, count, value);
    }

    public void lset(String key, int index, String value) {
        client.lset(key, index, value);
    }

    public void lset(byte[] key, int index, byte[] value) {
        client.lset(key, index, value);
    }

    public void ltrim(String key, int start, int end) {
        client.ltrim(key, start, end);
    }

    public void ltrim(byte[] key, int start, int end) {
        client.ltrim(key, start, end);
    }

    public void mget(String... keys) {
        client.mget(keys);
    }

    public void mget(byte[]... keys) {
        client.mget(keys);
    }

    public void move(String key, int dbIndex) {
        client.move(key, dbIndex);
    }

    public void move(byte[] key, int dbIndex) {
        client.move(key, dbIndex);
    }

    public void mset(String... keysvalues) {
        client.mset(keysvalues);
    }

    public void mset(byte[]... keysvalues) {
        client.mset(keysvalues);
    }

    public void msetnx(String... keysvalues) {
        client.msetnx(keysvalues);
    }

    public void msetnx(byte[]... keysvalues) {
        client.msetnx(keysvalues);
    }

    public void persist(String key) {
        client.persist(key);
    }

    public void persist(byte[] key) {
        client.persist(key);
    }

    public void rename(String oldkey, String newkey) {
        client.rename(oldkey, newkey);
    }

    public void rename(byte[] oldkey, byte[] newkey) {
        client.rename(oldkey, newkey);
    }

    public void renamenx(String oldkey, String newkey) {
        client.renamenx(oldkey, newkey);
    }

    public void renamenx(byte[] oldkey, byte[] newkey) {
        client.renamenx(oldkey, newkey);
    }

    public void rpop(String key) {
        client.rpop(key);
    }

    public void rpop(byte[] key) {
        client.rpop(key);
    }

    public void rpoplpush(String srckey, String dstkey) {
        client.rpoplpush(srckey, dstkey);
    }

    public void rpoplpush(byte[] srckey, byte[] dstkey) {
        client.rpoplpush(srckey, dstkey);
    }

    public void rpush(String key, String string) {
        client.rpush(key, string);
    }

    public void rpush(byte[] key, byte[] string) {
        client.rpush(key, string);
    }

    public void rpushx(String key, String string) {
        client.rpushx(key, string);
    }

    public void rpushx(byte[] key, byte[] string) {
        client.rpushx(key, string);
    }

    public void sadd(String key, String member) {
        client.sadd(key, member);
    }

    public void sadd(byte[] key, byte[] member) {
        client.sadd(key, member);
    }

    public void scard(String key) {
        client.scard(key);
    }

    public void scard(byte[] key) {
        client.scard(key);
    }

    public void sdiff(String... keys) {
        client.sdiff(keys);
    }

    public void sdiff(byte[]... keys) {
        client.sdiff(keys);
    }

    public void sdiffstore(String dstkey, String... keys) {
        client.sdiffstore(dstkey, keys);
    }

    public void sdiffstore(byte[] dstkey, byte[]... keys) {
        client.sdiffstore(dstkey, keys);
    }

    public void set(String key, String value) {
        client.set(key, value);
    }

    public void set(byte[] key, byte[] value) {
        client.set(key, value);
    }

    public void setbit(String key, long offset, boolean value) {
        client.setbit(key, offset, value);
    }

    public void setex(String key, int seconds, String value) {
        client.setex(key, seconds, value);
    }

    public void setex(byte[] key, int seconds, byte[] value) {
        client.setex(key, seconds, value);
    }

    public void setnx(String key, String value) {
        client.setnx(key, value);
    }

    public void setnx(byte[] key, byte[] value) {
        client.setnx(key, value);
    }

    public void setrange(String key, long offset, String value) {
        client.setrange(key, offset, value);
    }

    public void sinter(String... keys) {
        client.sinter(keys);
    }

    public void sinter(byte[]... keys) {
        client.sinter(keys);
    }

    public void sinterstore(String dstkey, String... keys) {
        client.sinterstore(dstkey, keys);
    }

    public void sinterstore(byte[] dstkey, byte[]... keys) {
        client.sinterstore(dstkey, keys);
    }

    public void sismember(String key, String member) {
        client.sismember(key, member);
    }

    public void sismember(byte[] key, byte[] member) {
        client.sismember(key, member);
    }

    public void smembers(String key) {
        client.smembers(key);
    }

    public void smembers(byte[] key) {
        client.smembers(key);
    }

    public void smove(String srckey, String dstkey, String member) {
        client.smove(srckey, dstkey, member);
    }

    public void smove(byte[] srckey, byte[] dstkey, byte[] member) {
        client.smove(srckey, dstkey, member);
    }

    public void sort(String key) {
        client.sort(key);
    }

    public void sort(byte[] key) {
        client.sort(key);
    }

    public void sort(String key, SortingParams sortingParameters) {
        client.sort(key, sortingParameters);
    }

    public void sort(byte[] key, SortingParams sortingParameters) {
        client.sort(key, sortingParameters);
    }

    public void sort(String key, SortingParams sortingParameters, String dstkey) {
        client.sort(key, sortingParameters, dstkey);
    }

    public void sort(byte[] key, SortingParams sortingParameters, byte[] dstkey) {
        client.sort(key, sortingParameters, dstkey);
    }

    public void sort(String key, String dstkey) {
        client.sort(key, dstkey);
    }

    public void sort(byte[] key, byte[] dstkey) {
        client.sort(key, dstkey);
    }

    public void spop(String key) {
        client.spop(key);
    }

    public void spop(byte[] key) {
        client.spop(key);
    }

    public void srandmember(String key) {
        client.srandmember(key);
    }

    public void srandmember(byte[] key) {
        client.srandmember(key);
    }

    public void srem(String key, String member) {
        client.srem(key, member);
    }

    public void srem(byte[] key, byte[] member) {
        client.srem(key, member);
    }

    public void strlen(String key) {
        client.strlen(key);
    }

    public void strlen(byte[] key) {
        client.strlen(key);
    }

    public void substr(String key, int start, int end) {
        client.substr(key, start, end);
    }

    public void substr(byte[] key, int start, int end) {
        client.substr(key, start, end);
    }

    public void sunion(String... keys) {
        client.sunion(keys);
    }

    public void sunion(byte[]... keys) {
        client.sunion(keys);
    }

    public void sunionstore(String dstkey, String... keys) {
        client.sunionstore(dstkey, keys);
    }

    public void sunionstore(byte[] dstkey, byte[]... keys) {
        client.sunionstore(dstkey, keys);
    }

    public void ttl(String key) {
        client.ttl(key);
    }

    public void ttl(byte[] key) {
        client.ttl(key);
    }

    public void type(String key) {
        client.type(key);
    }

    public void type(byte[] key) {
        client.type(key);
    }

    public void watch(String... keys) {
        client.watch(keys);
    }

    public void watch(byte[]... keys) {
        client.watch(keys);
    }

    public void zadd(String key, double score, String member) {
        client.zadd(key, score, member);
    }

    public void zadd(byte[] key, double score, byte[] member) {
        client.zadd(key, score, member);
    }

    public void zcard(String key) {
        client.zcard(key);
    }

    public void zcard(byte[] key) {
        client.zcard(key);
    }

    public void zcount(String key, double min, double max) {
        client.zcount(key, min, max);
    }

    public void zcount(byte[] key, double min, double max) {
        client.zcount(key, min, max);
    }

    public void zincrby(String key, double score, String member) {
        client.zincrby(key, score, member);
    }

    public void zincrby(byte[] key, double score, byte[] member) {
        client.zincrby(key, score, member);
    }

    public void zinterstore(String dstkey, String... sets) {
        client.zinterstore(dstkey, sets);
    }

    public void zinterstore(byte[] dstkey, byte[]... sets) {
        client.zinterstore(dstkey, sets);
    }

    public void zinterstore(String dstkey, ZParams params, String... sets) {
        client.zinterstore(dstkey, params, sets);
    }

    public void zinterstore(byte[] dstkey, ZParams params, byte[]... sets) {
        client.zinterstore(dstkey, params, sets);
    }

    public void zrange(String key, int start, int end) {
        client.zrange(key, start, end);
    }

    public void zrange(byte[] key, int start, int end) {
        client.zrange(key, start, end);
    }

    public void zrangeByScore(String key, double min, double max) {
        client.zrangeByScore(key, min, max);
    }

    public void zrangeByScore(byte[] key, double min, double max) {
        client.zrangeByScore(key, min, max);
    }

    public void zrangeByScore(String key, String min, String max) {
        client.zrangeByScore(key, min, max);
    }

    public void zrangeByScore(byte[] key, byte[] min, byte[] max) {
        client.zrangeByScore(key, min, max);
    }

    public void zrangeByScore(String key, double min, double max, int offset,
            int count) {
        client.zrangeByScore(key, min, max, offset, count);
    }

    public void zrangeByScore(byte[] key, double min, double max, int offset,
            int count) {
        client.zrangeByScore(key, min, max, offset, count);
    }

    public void zrangeByScoreWithScores(String key, double min, double max) {
        client.zrangeByScoreWithScores(key, min, max);
    }

    public void zrangeByScoreWithScores(byte[] key, double min, double max) {
        client.zrangeByScoreWithScores(key, min, max);
    }

    public void zrangeByScoreWithScores(String key, double min, double max,
            int offset, int count) {
        client.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    public void zrangeByScoreWithScores(byte[] key, double min, double max,
            int offset, int count) {
        client.zrangeByScoreWithScores(key, min, max, offset, count);
    }

    //--
    public void zrevrangeByScore(String key, double max, double min) {
        client.zrevrangeByScore(key, max, min);
    }

    public void zrevrangeByScore(byte[] key, double max, double min) {
        client.zrevrangeByScore(key, max, min);
    }

    public void zrevrangeByScore(String key, String max, String min) {
        client.zrevrangeByScore(key, max, min);
    }

    public void zrevrangeByScore(byte[] key, byte[] max, byte[] min) {
        client.zrevrangeByScore(key, max, min);
    }

    public void zrevrangeByScore(String key, double max, double min, int offset,
            int count) {
        client.zrevrangeByScore(key, max, min, offset, count);
    }

    public void zrevrangeByScore(byte[] key, double max, double min, int offset,
            int count) {
        client.zrevrangeByScore(key, max, min, offset, count);
    }

    public void zrevrangeByScoreWithScores(String key, double max, double min) {
        client.zrevrangeByScoreWithScores(key, max, min);
    }

    public void zrevrangeByScoreWithScores(byte[] key, double max, double min) {
        client.zrevrangeByScoreWithScores(key, max, min);
    }

    public void zrevrangeByScoreWithScores(String key, double max, double min,
            int offset, int count) {
        client.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    public void zrevrangeByScoreWithScores(byte[] key, double max, double min,
            int offset, int count) {
        client.zrevrangeByScoreWithScores(key, max, min, offset, count);
    }

    public void zrangeWithScores(String key, int start, int end) {
        client.zrangeWithScores(key, start, end);
    }

    public void zrangeWithScores(byte[] key, int start, int end) {
        client.zrangeWithScores(key, start, end);
    }

    public void zrank(String key, String member) {
        client.zrank(key, member);
    }

    public void zrank(byte[] key, byte[] member) {
        client.zrank(key, member);
    }

    public void zrem(String key, String member) {
        client.zrem(key, member);
    }

    public void zrem(byte[] key, byte[] member) {
        client.zrem(key, member);
    }

    public void zremrangeByRank(String key, int start, int end) {
        client.zremrangeByRank(key, start, end);
    }

    public void zremrangeByRank(byte[] key, int start, int end) {
        client.zremrangeByRank(key, start, end);
    }

    public void zremrangeByScore(String key, double start, double end) {
        client.zremrangeByScore(key, start, end);
    }

    public void zremrangeByScore(byte[] key, double start, double end) {
        client.zremrangeByScore(key, start, end);
    }

    public void zrevrange(String key, int start, int end) {
        client.zrevrange(key, start, end);
    }

    public void zrevrange(byte[] key, int start, int end) {
        client.zrevrange(key, start, end);
    }

    public void zrevrangeWithScores(String key, int start, int end) {
        client.zrevrangeWithScores(key, start, end);
    }

    public void zrevrangeWithScores(byte[] key, int start, int end) {
        client.zrevrangeWithScores(key, start, end);
    }

    public void zrevrank(String key, String member) {
        client.zrevrank(key, member);
    }

    public void zrevrank(byte[] key, byte[] member) {
        client.zrevrank(key, member);
    }

    public void zscore(String key, String member) {
        client.zscore(key, member);
    }

    public void zscore(byte[] key, byte[] member) {
        client.zscore(key, member);
    }

    public void zunionstore(String dstkey, String... sets) {
        client.zunionstore(dstkey, sets);
    }

    public void zunionstore(byte[] dstkey, byte[]... sets) {
        client.zunionstore(dstkey, sets);
    }

    public void zunionstore(String dstkey, ZParams params, String... sets) {
        client.zunionstore(dstkey, params, sets);
    }

    public void zunionstore(byte[] dstkey, ZParams params, byte[]... sets) {
        client.zunionstore(dstkey, params, sets);
    }

    public List<Object> execute() {
        return client.getAll();
    }

    public void bgrewriteaof() {
        client.bgrewriteaof();
    }

    public void bgsave() {
        client.bgsave();
    }

    public void configGet(String pattern) {
        client.configGet(pattern);
    }

    public void configSet(String parameter, String value) {
        client.configSet(parameter, value);
    }

    public void brpoplpush(String source, String destination, int timeout) {
        client.brpoplpush(source, destination, timeout);
    }

    public void brpoplpush(byte[] source, byte[] destination, int timeout) {
        client.brpoplpush(source, destination, timeout);
    }

    public void configResetStat() {
        client.configResetStat();
    }

    public void save() {
        client.save();
    }

    public void lastsave() {
        client.lastsave();
    }

    public void discard() {
        client.discard();
    }

    public void exec() {
        client.exec();
    }

    public void multi() {
        client.multi();
    }
}
