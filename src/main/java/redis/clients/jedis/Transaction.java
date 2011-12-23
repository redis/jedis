package redis.clients.jedis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

public class Transaction extends BinaryTransaction {
    public Transaction() {
    }

    public Transaction(final Client client) {
        super(client);
    }

    public Response<Long> append(String key, String value) {
        client.append(key, value);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<List<String>> blpop(String... args) {
        client.blpop(args);
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<List<String>> brpop(String... args) {
        client.brpop(args);
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<Long> decr(String key) {
        client.decr(key);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> decrBy(String key, long integer) {
        client.decrBy(key, integer);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> del(String... keys) {
        client.del(keys);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> echo(String string) {
        client.echo(string);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Boolean> exists(String key) {
        client.exists(key);
        return getResponse(BuilderFactory.BOOLEAN);
    }

    public Response<Long> expire(String key, int seconds) {
        client.expire(key, seconds);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> expireAt(String key, long unixTime) {
        client.expireAt(key, unixTime);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> get(String key) {
        client.get(key);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Boolean> getbit(String key, long offset) {
        client.getbit(key, offset);
        return getResponse(BuilderFactory.BOOLEAN);
    }

    public Response<String> getrange(String key, long startOffset,
            long endOffset) {
        client.getrange(key, startOffset, endOffset);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> getSet(String key, String value) {
        client.getSet(key, value);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> hdel(String key, String field) {
        client.hdel(key, field);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Boolean> hexists(String key, String field) {
        client.hexists(key, field);
        return getResponse(BuilderFactory.BOOLEAN);
    }

    public Response<String> hget(String key, String field) {
        client.hget(key, field);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Map<String, String>> hgetAll(String key) {
        client.hgetAll(key);
        return getResponse(BuilderFactory.STRING_MAP);
    }

    public Response<Long> hincrBy(String key, String field, long value) {
        client.hincrBy(key, field, value);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Set<String>> hkeys(String key) {
        client.hkeys(key);
        return getResponse(BuilderFactory.STRING_SET);
    }

    public Response<Long> hlen(String key) {
        client.hlen(key);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<List<String>> hmget(String key, String... fields) {
        client.hmget(key, fields);
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<String> hmset(String key, Map<String, String> hash) {
        client.hmset(key, hash);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> hset(String key, String field, String value) {
        client.hset(key, field, value);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> hsetnx(String key, String field, String value) {
        client.hsetnx(key, field, value);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<List<String>> hvals(String key) {
        client.hvals(key);
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<Long> incr(String key) {
        client.incr(key);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> incrBy(String key, long integer) {
        client.incrBy(key, integer);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Set<String>> keys(String pattern) {
        client.keys(pattern);
        return getResponse(BuilderFactory.STRING_SET);
    }

    public Response<String> lindex(String key, int index) {
        client.lindex(key, index);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> linsert(String key, LIST_POSITION where,
            String pivot, String value) {
        client.linsert(key, where, pivot, value);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> llen(String key) {
        client.llen(key);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> lpop(String key) {
        client.lpop(key);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> lpush(String key, String string) {
        client.lpush(key, string);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> lpushx(String key, String string) {
        client.lpushx(key, string);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<List<String>> lrange(String key, long start, long end) {
        client.lrange(key, start, end);
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<Long> lrem(String key, long count, String value) {
        client.lrem(key, count, value);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> lset(String key, long index, String value) {
        client.lset(key, index, value);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> ltrim(String key, long start, long end) {
        client.ltrim(key, start, end);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<List<String>> mget(String... keys) {
        client.mget(keys);
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<Long> move(String key, int dbIndex) {
        client.move(key, dbIndex);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> mset(String... keysvalues) {
        client.mset(keysvalues);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> msetnx(String... keysvalues) {
        client.msetnx(keysvalues);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> persist(String key) {
        client.persist(key);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> rename(String oldkey, String newkey) {
        client.rename(oldkey, newkey);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> renamenx(String oldkey, String newkey) {
        client.renamenx(oldkey, newkey);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> rpop(String key) {
        client.rpop(key);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> rpoplpush(String srckey, String dstkey) {
        client.rpoplpush(srckey, dstkey);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> rpush(String key, String string) {
        client.rpush(key, string);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> rpushx(String key, String string) {
        client.rpushx(key, string);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> sadd(String key, String member) {
        client.sadd(key, member);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> scard(String key) {
        client.scard(key);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Set<String>> sdiff(String... keys) {
        client.sdiff(keys);
        return getResponse(BuilderFactory.STRING_SET);
    }

    public Response<Long> sdiffstore(String dstkey, String... keys) {
        client.sdiffstore(dstkey, keys);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> set(String key, String value) {
        client.set(key, value);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Boolean> setbit(String key, long offset, boolean value) {
        client.setbit(key, offset, value);
        return getResponse(BuilderFactory.BOOLEAN);
    }

    public Response<String> setex(String key, int seconds, String value) {
        client.setex(key, seconds, value);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> setnx(String key, String value) {
        client.setnx(key, value);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> setrange(String key, long offset, String value) {
        client.setrange(key, offset, value);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Set<String>> sinter(String... keys) {
        client.sinter(keys);
        return getResponse(BuilderFactory.STRING_SET);
    }

    public Response<Long> sinterstore(String dstkey, String... keys) {
        client.sinterstore(dstkey, keys);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Boolean> sismember(String key, String member) {
        client.sismember(key, member);
        return getResponse(BuilderFactory.BOOLEAN);
    }

    public Response<Set<String>> smembers(String key) {
        client.smembers(key);
        return getResponse(BuilderFactory.STRING_SET);
    }

    public Response<Long> smove(String srckey, String dstkey, String member) {
        client.smove(srckey, dstkey, member);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<List<String>> sort(String key) {
        client.sort(key);
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<List<String>> sort(String key,
            SortingParams sortingParameters) {
        client.sort(key, sortingParameters);
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<List<String>> sort(String key,
            SortingParams sortingParameters, String dstkey) {
        client.sort(key, sortingParameters, dstkey);
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<List<String>> sort(String key, String dstkey) {
        client.sort(key, dstkey);
        return getResponse(BuilderFactory.STRING_LIST);
    }

    public Response<String> spop(String key) {
        client.spop(key);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> srandmember(String key) {
        client.srandmember(key);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> srem(String key, String member) {
        client.srem(key, member);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> strlen(String key) {
        client.strlen(key);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> substr(String key, int start, int end) {
        client.substr(key, start, end);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Set<String>> sunion(String... keys) {
        client.sunion(keys);
        return getResponse(BuilderFactory.STRING_SET);
    }

    public Response<Long> sunionstore(String dstkey, String... keys) {
        client.sunionstore(dstkey, keys);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> ttl(String key) {
        client.ttl(key);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> type(String key) {
        client.type(key);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> zadd(String key, double score, String member) {
        client.zadd(key, score, member);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> zcard(String key) {
        client.zcard(key);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> zcount(String key, double min, double max) {
        client.zcount(key, min, max);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Double> zincrby(String key, double score, String member) {
        client.zincrby(key, score, member);
        return getResponse(BuilderFactory.DOUBLE);
    }

    public Response<Long> zinterstore(String dstkey, String... sets) {
        client.zinterstore(dstkey, sets);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> zinterstore(String dstkey, ZParams params,
            String... sets) {
        client.zinterstore(dstkey, params, sets);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Set<String>> zrange(String key, int start, int end) {
        client.zrange(key, start, end);
        return getResponse(BuilderFactory.STRING_ZSET);
    }

    public Response<Set<String>> zrangeByScore(String key, double min,
            double max) {
        client.zrangeByScore(key, min, max);
        return getResponse(BuilderFactory.STRING_ZSET);
    }

    public Response<Set<String>> zrangeByScore(String key, String min,
            String max) {
        client.zrangeByScore(key, min, max);
        return getResponse(BuilderFactory.STRING_ZSET);
    }

    public Response<Set<String>> zrangeByScore(String key, double min,
            double max, int offset, int count) {
        client.zrangeByScore(key, min, max, offset, count);
        return getResponse(BuilderFactory.STRING_ZSET);
    }

    public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min,
            double max) {
        client.zrangeByScoreWithScores(key, min, max);
        return getResponse(BuilderFactory.TUPLE_ZSET);
    }

    public Response<Set<Tuple>> zrangeByScoreWithScores(String key, double min,
            double max, int offset, int count) {
        client.zrangeByScoreWithScores(key, min, max, offset, count);
        return getResponse(BuilderFactory.TUPLE_ZSET);
    }

    public Response<Set<Tuple>> zrangeWithScores(String key, int start, int end) {
        client.zrangeWithScores(key, start, end);
        return getResponse(BuilderFactory.TUPLE_ZSET);
    }

    public Response<Long> zrank(String key, String member) {
        client.zrank(key, member);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> zrem(String key, String member) {
        client.zrem(key, member);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> zremrangeByRank(String key, int start, int end) {
        client.zremrangeByRank(key, start, end);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> zremrangeByScore(String key, double start, double end) {
        client.zremrangeByScore(key, start, end);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Set<String>> zrevrange(String key, int start, int end) {
        client.zrevrange(key, start, end);
        return getResponse(BuilderFactory.STRING_ZSET);
    }

    public Response<Set<Tuple>> zrevrangeWithScores(String key, int start,
            int end) {
        client.zrevrangeWithScores(key, start, end);
        return getResponse(BuilderFactory.TUPLE_ZSET);
    }

    public Response<Long> zrevrank(String key, String member) {
        client.zrevrank(key, member);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Double> zscore(String key, String member) {
        client.zscore(key, member);
        return getResponse(BuilderFactory.DOUBLE);
    }

    public Response<Long> zunionstore(String dstkey, String... sets) {
        client.zunionstore(dstkey, sets);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> zunionstore(String dstkey, ZParams params,
            String... sets) {
        client.zunionstore(dstkey, params, sets);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<String> bgrewriteaof() {
        client.bgrewriteaof();
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> bgsave() {
        client.bgsave();
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> configGet(String pattern) {
        client.configGet(pattern);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> configSet(String parameter, String value) {
        client.configSet(parameter, value);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> brpoplpush(String source, String destination,
            int timeout) {
        client.brpoplpush(source, destination, timeout);
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> configResetStat() {
        client.configResetStat();
        return getResponse(BuilderFactory.STRING);
    }

    public Response<String> save() {
        client.save();
        return getResponse(BuilderFactory.STRING);
    }

    public Response<Long> lastsave() {
        client.lastsave();
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> publish(String channel, String message) {
        client.publish(channel, message);
        return getResponse(BuilderFactory.LONG);
    }

    public Response<Long> publish(byte[] channel, byte[] message) {
        client.publish(channel, message);
        return getResponse(BuilderFactory.LONG);
    }
}