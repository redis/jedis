package com.googlecode.jedis;

import static com.googlecode.jedis.Protocol.DEFAULT_CHARSET;
import static com.googlecode.jedis.Protocol.toByteArray;
import static com.googlecode.jedis.Protocol.Command.*;
import static com.googlecode.jedis.Protocol.Keyword.LIMIT;
import static com.googlecode.jedis.Protocol.Keyword.NO;
import static com.googlecode.jedis.Protocol.Keyword.ONE;
import static com.googlecode.jedis.Protocol.Keyword.STORE;
import static com.googlecode.jedis.Protocol.Keyword.WITHSCORES;
import static java.lang.System.arraycopy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.googlecode.jedis.Protocol.Keyword;

class RawClient extends Connection {
    private enum LIST_POSITION {
	BEFORE, AFTER;
	public final byte[] raw;

	private LIST_POSITION() {
	    raw = name().getBytes(DEFAULT_CHARSET);
	}
    }

    private boolean isInMulti;

    protected RawClient() {
    }

    public void append(final byte[] key, final byte[] value) {
	sendCommand(APPEND, key, value);
    }

    // public void auth(final String password) {
    // sendCommand(AUTH, password);
    // }

    public void auth(final byte[] password) {
	sendCommand(AUTH, password);
    }

    public void bgrewriteaof() {
	sendCommand(BGREWRITEAOF);
    }

    public void bgsave() {
	sendCommand(BGSAVE);
    }

    public void blpop(final byte[][] args) {
	sendCommand(BLPOP, args);
    }

    public void brpop(final byte[][] args) {
	sendCommand(BRPOP, args);
    }

    public void configGet(final String pattern) {
	sendCommand(CONFIG, Keyword.GET.name(), pattern);
    }

    public void configSet(final String parameter, final String value) {
	sendCommand(CONFIG, Keyword.SET.name(), parameter, value);
    }

    public void dbSize() {
	sendCommand(DBSIZE);
    }

    public void debug(final DebugParams params) {
	sendCommand(DEBUG, params.getCommand());
    }

    public void decr(final byte[] key) {
	sendCommand(DECR, key);
    }

    public void decrBy(final byte[] key, final long integer) {
	sendCommand(DECRBY, key, toByteArray(integer));
    }

    public void del(final byte[] key1, final byte[]... keyN) {
	byte[][] keys = new byte[keyN.length + 1][];
	keys[0] = key1;
	System.arraycopy(keyN, 0, keys, 1, keyN.length);
	sendCommand(DEL, keys);
    }

    public void discard() {
	sendCommand(DISCARD);
	isInMulti = false;
    }

    public void echo(final byte[] string) {
	sendCommand(ECHO, string);
    }

    public void exec() {
	sendCommand(EXEC);
	isInMulti = false;
    }

    public void exists(final byte[] key) {
	sendCommand(EXISTS, key);
    }

    public void expire(final byte[] key, final int seconds) {
	sendCommand(EXPIRE, key, toByteArray(seconds));
    }

    public void expireAt(final byte[] key, final long unixTime) {
	sendCommand(EXPIREAT, key, toByteArray(unixTime));
    }

    public void flushAll() {
	sendCommand(FLUSHALL);
    }

    public void flushDB() {
	sendCommand(FLUSHDB);
    }

    public void get(final byte[] key) {
	sendCommand(GET, key);
    }

    public void getSet(final byte[] key, final byte[] value) {
	sendCommand(GETSET, key, value);
    }

    public void hdel(final byte[] key, final byte[] field) {
	sendCommand(HDEL, key, field);
    }

    public void hexists(final byte[] key, final byte[] field) {
	sendCommand(HEXISTS, key, field);
    }

    public void hget(final byte[] key, final byte[] field) {
	sendCommand(HGET, key, field);
    }

    public void hgetAll(final byte[] key) {
	sendCommand(HGETALL, key);
    }

    public void hincrBy(final byte[] key, final byte[] field, final long value) {
	sendCommand(HINCRBY, key, field, toByteArray(value));
    }

    public void hkeys(final byte[] key) {
	sendCommand(HKEYS, key);
    }

    public void hlen(final byte[] key) {
	sendCommand(HLEN, key);
    }

    public void hmget(final byte[] key, final byte[]... fields) {
	final byte[][] params = new byte[fields.length + 1][];
	params[0] = key;
	System.arraycopy(fields, 0, params, 1, fields.length);
	sendCommand(HMGET, params);
    }

    public void hmset(final byte[] key, final Map<byte[], byte[]> hash) {
	final List<byte[]> params = Lists.newArrayList();
	params.add(key);

	for (Entry<byte[], byte[]> e : hash.entrySet()) {
	    params.add(e.getKey());
	    params.add(e.getValue());
	}
	sendCommand(HMSET, params.toArray(new byte[params.size()][]));
    }

    public void hset(final byte[] key, final byte[] field, final byte[] value) {
	sendCommand(HSET, key, field, value);
    }

    public void hsetnx(final byte[] key, final byte[] field, final byte[] value) {
	sendCommand(HSETNX, key, field, value);
    }

    public void hvals(final byte[] key) {
	sendCommand(HVALS, key);
    }

    public void incr(final byte[] key) {
	sendCommand(INCR, key);
    }

    public void incrBy(final byte[] key, final long integer) {
	sendCommand(INCRBY, key, toByteArray(integer));
    }

    public void info() {
	sendCommand(INFO);
    }

    public boolean isInMulti() {
	return isInMulti;
    }

    public void keys(final byte[] pattern) {
	sendCommand(KEYS, pattern);
    }

    public void lastsave() {
	sendCommand(LASTSAVE);
    }

    public void lindex(final byte[] key, final int index) {
	sendCommand(LINDEX, key, toByteArray(index));
    }

    public void linsert(final byte[] key, final LIST_POSITION where,
	    final byte[] pivot, final byte[] value) {
	sendCommand(LINSERT, key, where.raw, pivot, value);
    }

    public void linsertAfter(byte[] key, byte[] element, byte[] value) {
	sendCommand(LINSERT, key, LIST_POSITION.AFTER.raw, element, value);
    }

    public void linsertBefore(byte[] key, byte[] element, byte[] value) {
	sendCommand(LINSERT, key, LIST_POSITION.BEFORE.raw, element, value);
    }

    public void llen(final byte[] key) {
	sendCommand(LLEN, key);
    }

    public void lpop(final byte[] key) {
	sendCommand(LPOP, key);
    }

    public void lpush(final byte[] key, final byte[] string) {
	sendCommand(LPUSH, key, string);
    }

    public void lpushx(final byte[] key, final byte[] string) {
	sendCommand(LPUSHX, key, string);
    }

    public void lrange(final byte[] key, final long start, final long end) {
	sendCommand(LRANGE, key, toByteArray(start), toByteArray(end));
    }

    public void lrem(final byte[] key, int count, final byte[] value) {
	sendCommand(LREM, key, toByteArray(count), value);
    }

    public void lset(final byte[] key, final int index, final byte[] value) {
	sendCommand(LSET, key, toByteArray(index), value);
    }

    public void ltrim(final byte[] key, final int start, final int end) {
	sendCommand(LTRIM, key, toByteArray(start), toByteArray(end));
    }

    public void mget(final byte[]... keys) {
	sendCommand(MGET, keys);
    }

    public void monitor() {
	sendCommand(MONITOR);
    }

    public void move(final byte[] key, final int dbIndex) {
	sendCommand(MOVE, key, toByteArray(dbIndex));
    }

    public void mset(final byte[]... keysvalues) {
	sendCommand(MSET, keysvalues);
    }

    public void msetnx(final byte[]... keysvalues) {
	sendCommand(MSETNX, keysvalues);
    }

    public void multi() {
	sendCommand(MULTI);
	isInMulti = true;
    }

    public void persist(final byte[] key) {
	sendCommand(PERSIST, key);
    }

    public void ping() {
	sendCommand(PING);
    }

    public void psubscribe(final String[] patterns) {
	sendCommand(PSUBSCRIBE, patterns);
    }

    public void publish(final String channel, final String message) {
	sendCommand(PUBLISH, channel, message);
    }

    public void punsubscribe() {
	sendCommand(PUNSUBSCRIBE);
    }

    public void punsubscribe(final String... patterns) {
	sendCommand(PUNSUBSCRIBE, patterns);
    }

    public void quit() {
	sendCommand(QUIT);
    }

    public void randomKey() {
	sendCommand(RANDOMKEY);
    }

    public void rename(final byte[] oldkey, final byte[] newkey) {
	sendCommand(RENAME, oldkey, newkey);
    }

    public void renamenx(final byte[] oldkey, final byte[] newkey) {
	sendCommand(RENAMENX, oldkey, newkey);
    }

    public void rpop(final byte[] key) {
	sendCommand(RPOP, key);
    }

    public void rpoplpush(final byte[] srckey, final byte[] dstkey) {
	sendCommand(RPOPLPUSH, srckey, dstkey);
    }

    public void rpush(final byte[] key, final byte[] string) {
	sendCommand(RPUSH, key, string);
    }

    public void rpushx(final byte[] key, final byte[] string) {
	sendCommand(RPUSHX, key, string);
    }

    public void sadd(final byte[] key, final byte[] member) {
	sendCommand(SADD, key, member);
    }

    public void save() {
	sendCommand(SAVE);
    }

    public void scard(final byte[] key) {
	sendCommand(SCARD, key);
    }

    public void sdiff(final byte[]... keys) {
	sendCommand(SDIFF, keys);
    }

    public void sdiffstore(final byte[] dstkey, final byte[]... keys) {
	byte[][] params = new byte[keys.length + 1][];
	params[0] = dstkey;
	System.arraycopy(keys, 0, params, 1, keys.length);
	sendCommand(SDIFFSTORE, params);
    }

    public void select(final int index) {
	sendCommand(SELECT, toByteArray(index));
    }

    public void set(final byte[] key, final byte[] value) {
	sendCommand(SET, key, value);
    }

    public void setex(final byte[] key, final int seconds, final byte[] value) {
	sendCommand(SETEX, key, toByteArray(seconds), value);
    }

    public void setnx(final byte[] key, final byte[] value) {
	sendCommand(SETNX, key, value);
    }

    public void shutdown() {
	sendCommand(SHUTDOWN);
    }

    public void sinter(final byte[] key1, final byte[]... keyN) {
	byte[][] args = new byte[keyN.length + 1][];
	args[0] = key1;
	System.arraycopy(keyN, 0, args, 1, keyN.length);
	sendCommand(SINTER, args);
    }

    public void sinterstore(final byte[] dstkey, final byte[] key1,
	    final byte[]... keyN) {
	final byte[][] params = new byte[keyN.length + 2][];
	params[0] = dstkey;
	params[1] = key1;
	System.arraycopy(keyN, 0, params, 2, keyN.length);
	sendCommand(SINTERSTORE, params);
    }

    public void sismember(final byte[] key, final byte[] member) {
	sendCommand(SISMEMBER, key, member);
    }

    public void slaveof(final String host, final int port) {
	sendCommand(SLAVEOF, host, String.valueOf(port));
    }

    public void slaveofNoOne() {
	sendCommand(SLAVEOF, NO.raw, ONE.raw);
    }

    public void smembers(final byte[] key) {
	sendCommand(SMEMBERS, key);
    }

    public void smove(final byte[] srckey, final byte[] dstkey,
	    final byte[] member) {
	sendCommand(SMOVE, srckey, dstkey, member);
    }

    public void sort(final byte[] key) {
	sendCommand(SORT, key);
    }

    public void sort(final byte[] key, final byte[] dstkey) {
	sendCommand(SORT, key, STORE.raw, dstkey);
    }

    public void sort(final byte[] key, final SortParams sortingParameters) {
	final List<byte[]> args = new ArrayList<byte[]>();
	args.add(key);
	args.addAll(sortingParameters.getParams());
	sendCommand(SORT, args.toArray(new byte[args.size()][]));
    }

    public void sort(final byte[] key, final SortParams sortingParameters,
	    final byte[] dstkey) {
	final List<byte[]> args = new ArrayList<byte[]>();
	args.add(key);
	args.addAll(sortingParameters.getParams());
	args.add(STORE.raw);
	args.add(dstkey);
	sendCommand(SORT, args.toArray(new byte[args.size()][]));
    }

    public void spop(final byte[] key) {
	sendCommand(SPOP, key);
    }

    public void srandmember(final byte[] key) {
	sendCommand(SRANDMEMBER, key);
    }

    public void srem(final byte[] key, final byte[] member) {
	sendCommand(SREM, key, member);
    }

    public void strlen(final byte[] key) {
	sendCommand(STRLEN, key);
    }

    public void subscribe(final String... channels) {
	sendCommand(SUBSCRIBE, channels);
    }

    public void substr(final byte[] key, final int start, final int end) {
	sendCommand(SUBSTR, key, toByteArray(start), toByteArray(end));
    }

    public void sunion(final byte[] key1, final byte[]... keyN) {
	if (key1 == null) {
	    throw new NullPointerException();
	}
	byte[][] args = new byte[1 + keyN.length][];
	args[0] = key1;
	arraycopy(keyN, 0, args, 1, keyN.length);

	sendCommand(SUNION, args);
    }

    public void sunionstore(final byte[] dstkey, final byte[]... keys) {
	byte[][] params = new byte[keys.length + 1][];
	params[0] = dstkey;
	System.arraycopy(keys, 0, params, 1, keys.length);
	sendCommand(SUNIONSTORE, params);
    }

    public void sync() {
	sendCommand(SYNC);
    }

    public void ttl(final byte[] key) {
	sendCommand(TTL, key);
    }

    public void type(final byte[] key) {
	sendCommand(TYPE, key);
    }

    public void unsubscribe() {
	sendCommand(UNSUBSCRIBE);
    }

    public void unsubscribe(final String... channels) {
	sendCommand(UNSUBSCRIBE, channels);
    }

    public void unwatch() {
	sendCommand(UNWATCH);
    }

    public void watch(final byte[]... keys) {
	sendCommand(WATCH, keys);
    }

    public void zadd(final byte[] key, final double score, final byte[] member) {
	sendCommand(ZADD, key, toByteArray(score), member);
    }

    public void zcard(final byte[] key) {
	sendCommand(ZCARD, key);
    }

    public void zcount(final byte[] key, final double min, final double max) {
	sendCommand(ZCOUNT, key, toByteArray(min), toByteArray(max));
    }

    public void zincrby(final byte[] key, final double score,
	    final byte[] member) {
	sendCommand(ZINCRBY, key, toByteArray(score), member);
    }

    public void zinterstore(final byte[] dstkey, final byte[]... sets) {
	final byte[][] params = new byte[sets.length + 2][];
	params[0] = dstkey;
	params[1] = Protocol.toByteArray(sets.length);
	System.arraycopy(sets, 0, params, 2, sets.length);
	sendCommand(ZINTERSTORE, params);
    }

    public void zrange(final byte[] key, final int start, final int end) {
	sendCommand(ZRANGE, key, toByteArray(start), toByteArray(end));
    }

    public void zrangeByScore(final byte[] key, final byte[] min,
	    final byte[] max) {
	sendCommand(ZRANGEBYSCORE, key, min, max);
    }

    public void zrangeByScore(final byte[] key, final double min,
	    final double max) {
	sendCommand(ZRANGEBYSCORE, key, toByteArray(min), toByteArray(max));
    }

    public void zrangeByScore(final byte[] key, final double min,
	    final double max, final int offset, int count) {
	sendCommand(ZRANGEBYSCORE, key, toByteArray(min), toByteArray(max),
		LIMIT.raw, toByteArray(offset), toByteArray(count));
    }

    public void zrangeByScoreWithScores(final byte[] key, final double min,
	    final double max) {
	sendCommand(ZRANGEBYSCORE, key, toByteArray(min), toByteArray(max),
		WITHSCORES.raw);
    }

    public void zrangeByScoreWithScores(final byte[] key, final double min,
	    final double max, final int offset, final int count) {
	sendCommand(ZRANGEBYSCORE, key, toByteArray(min), toByteArray(max),
		LIMIT.raw, toByteArray(offset), toByteArray(count),
		WITHSCORES.raw);
    }

    public void zrangeWithScores(final byte[] key, final int start,
	    final int end) {
	sendCommand(ZRANGE, key, toByteArray(start), toByteArray(end),
		WITHSCORES.raw);
    }

    public void zrank(final byte[] key, final byte[] member) {
	sendCommand(ZRANK, key, member);
    }

    public void zrem(final byte[] key, final byte[] member) {
	sendCommand(ZREM, key, member);
    }

    public void zremrangeByRank(final byte[] key, final int start, final int end) {
	sendCommand(ZREMRANGEBYRANK, key, toByteArray(start), toByteArray(end));
    }

    public void zremrangeByScore(final byte[] key, final double start,
	    final double end) {
	sendCommand(ZREMRANGEBYSCORE, key, toByteArray(start), toByteArray(end));
    }

    public void zrevrange(final byte[] key, final int start, final int end) {
	sendCommand(ZREVRANGE, key, toByteArray(start), toByteArray(end));
    }

    public void zrevrangeWithScores(final byte[] key, final int start,
	    final int end) {
	sendCommand(ZREVRANGE, key, toByteArray(start), toByteArray(end),
		WITHSCORES.raw);
    }

    public void zrevrank(final byte[] key, final byte[] member) {
	sendCommand(ZREVRANK, key, member);
    }

    public void zscore(final byte[] key, final byte[] member) {
	sendCommand(ZSCORE, key, member);
    }

    public void zunionstore(final byte[] dstkey, final byte[]... sets) {
	final byte[][] params = new byte[sets.length + 2][];
	params[0] = dstkey;
	params[1] = toByteArray(sets.length);
	System.arraycopy(sets, 0, params, 2, sets.length);
	sendCommand(ZUNIONSTORE, params);
    }

}