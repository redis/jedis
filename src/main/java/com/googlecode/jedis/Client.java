package com.googlecode.jedis;

import static com.googlecode.jedis.Protocol.DEFAULT_CHARSET;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.googlecode.jedis.Protocol.Command;

class Client extends RawClient {

    Charset charset;

    public Client() {
	super();
    }

    public void append(final String key, final String value) {
	append(SafeEncoder.encode(key), SafeEncoder.encode(value));
    }

    public void blpop(final String[] args) {
	final byte[][] bargs = new byte[args.length][];
	for (int i = 0; i < bargs.length; i++) {
	    bargs[i] = SafeEncoder.encode(args[i]);
	}
	blpop(bargs);
    }

    public void brpop(final String[] args) {
	final byte[][] bargs = new byte[args.length][];
	for (int i = 0; i < bargs.length; i++) {
	    bargs[i] = SafeEncoder.encode(args[i]);
	}
	brpop(bargs);
    }

    public void decr(final String key) {
	decr(SafeEncoder.encode(key));
    }

    public void decrBy(final String key, final long integer) {
	decrBy(SafeEncoder.encode(key), integer);
    }

    public void del(final String key1, final String... keyN) {
	final byte[][] bkeys = new byte[keyN.length][];
	for (int i = 0; i < keyN.length; i++) {
	    bkeys[i] = SafeEncoder.encode(keyN[i]);
	}
	del(SafeEncoder.encode(key1), bkeys);
    }

    public void echo(final String string) {
	echo(SafeEncoder.encode(string));
    }

    public void exists(final String key) {
	exists(SafeEncoder.encode(key));
    }

    public void expire(final String key, final int seconds) {
	expire(SafeEncoder.encode(key), seconds);
    }

    public void expireAt(final String key, final long unixTime) {
	expireAt(SafeEncoder.encode(key), unixTime);
    }

    public void get(final String key) {
	sendCommand(Command.GET, SafeEncoder.encode(key));
    }

    public void getSet(final String key, final String value) {
	getSet(SafeEncoder.encode(key), SafeEncoder.encode(value));
    }

    public void hdel(final String key, final String field) {
	hdel(SafeEncoder.encode(key), SafeEncoder.encode(field));
    }

    public void hexists(final String key, final String field) {
	hexists(SafeEncoder.encode(key), SafeEncoder.encode(field));
    }

    public void hget(final String key, final String field) {
	hget(SafeEncoder.encode(key), SafeEncoder.encode(field));
    }

    public void hgetAll(final String key) {
	hgetAll(SafeEncoder.encode(key));
    }

    public void hincrBy(final String key, final String field, final long value) {
	hincrBy(SafeEncoder.encode(key), SafeEncoder.encode(field), value);
    }

    public void hkeys(final String key) {
	hkeys(SafeEncoder.encode(key));
    }

    public void hlen(final String key) {
	hlen(SafeEncoder.encode(key));
    }

    public void hmget(final String key, final String... fields) {
	final byte[][] bfields = new byte[fields.length][];
	for (int i = 0; i < bfields.length; i++) {
	    bfields[i] = SafeEncoder.encode(fields[i]);
	}
	hmget(SafeEncoder.encode(key), bfields);
    }

    public void hmset(final String key, final Map<String, String> hash) {
	final Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>(
		hash.size());
	for (final Entry<String, String> entry : hash.entrySet()) {
	    bhash.put(SafeEncoder.encode(entry.getKey()),
		    SafeEncoder.encode(entry.getValue()));
	}
	hmset(SafeEncoder.encode(key), bhash);
    }

    public void hset(final String key, final String field, final String value) {
	hset(SafeEncoder.encode(key), SafeEncoder.encode(field),
		SafeEncoder.encode(value));
    }

    public void hsetnx(final String key, final String field, final String value) {
	hsetnx(SafeEncoder.encode(key), SafeEncoder.encode(field),
		SafeEncoder.encode(value));
    }

    public void hvals(final String key) {
	hvals(SafeEncoder.encode(key));
    }

    public void incr(final String key) {
	incr(SafeEncoder.encode(key));
    }

    public void incrBy(final String key, final long integer) {
	incrBy(SafeEncoder.encode(key), integer);
    }

    public void keys(final String pattern) {
	keys(SafeEncoder.encode(pattern));
    }

    public void lindex(final String key, final int index) {
	lindex(SafeEncoder.encode(key), index);
    }

    public void linsertAfter(String key, String element, String value) {
	linsertAfter(key.getBytes(DEFAULT_CHARSET),
		element.getBytes(DEFAULT_CHARSET),
		value.getBytes(DEFAULT_CHARSET));
    }

    public void linsertBefore(String key, String element, String value) {
	linsertBefore(key.getBytes(DEFAULT_CHARSET),
		element.getBytes(DEFAULT_CHARSET),
		value.getBytes(DEFAULT_CHARSET));
    }

    public void llen(final String key) {
	llen(SafeEncoder.encode(key));
    }

    public void lpop(final String key) {
	lpop(SafeEncoder.encode(key));
    }

    public void lpush(final String key, final String string) {
	lpush(SafeEncoder.encode(key), SafeEncoder.encode(string));
    }

    public void lpushx(final String key, final String string) {
	lpushx(SafeEncoder.encode(key), SafeEncoder.encode(string));
    }

    public void lrange(final String key, final long start, final long end) {
	lrange(SafeEncoder.encode(key), start, end);
    }

    public void lrem(final String key, int count, final String value) {
	lrem(SafeEncoder.encode(key), count, SafeEncoder.encode(value));
    }

    public void lset(final String key, final int index, final String value) {
	lset(SafeEncoder.encode(key), index, SafeEncoder.encode(value));
    }

    public void ltrim(final String key, final int start, final int end) {
	ltrim(SafeEncoder.encode(key), start, end);
    }

    public void mget(final String... keys) {
	final byte[][] bkeys = new byte[keys.length][];
	for (int i = 0; i < bkeys.length; i++) {
	    bkeys[i] = SafeEncoder.encode(keys[i]);
	}
	mget(bkeys);
    }

    public void move(final String key, final int dbIndex) {
	move(SafeEncoder.encode(key), dbIndex);
    }

    public void mset(final String... keysvalues) {
	final byte[][] bkeysvalues = new byte[keysvalues.length][];
	for (int i = 0; i < keysvalues.length; i++) {
	    bkeysvalues[i] = SafeEncoder.encode(keysvalues[i]);
	}
	mset(bkeysvalues);
    }

    public void msetnx(final String... keysvalues) {
	final byte[][] bkeysvalues = new byte[keysvalues.length][];
	for (int i = 0; i < keysvalues.length; i++) {
	    bkeysvalues[i] = SafeEncoder.encode(keysvalues[i]);
	}
	msetnx(bkeysvalues);
    }

    public void persist(final String key) {
	persist(SafeEncoder.encode(key));
    }

    public void rename(final String oldkey, final String newkey) {
	rename(SafeEncoder.encode(oldkey), SafeEncoder.encode(newkey));
    }

    public void renamenx(final String oldkey, final String newkey) {
	renamenx(SafeEncoder.encode(oldkey), SafeEncoder.encode(newkey));
    }

    public void rpop(final String key) {
	rpop(SafeEncoder.encode(key));
    }

    public void rpoplpush(final String srckey, final String dstkey) {
	rpoplpush(SafeEncoder.encode(srckey), SafeEncoder.encode(dstkey));
    }

    public void rpush(final String key, final String string) {
	rpush(SafeEncoder.encode(key), SafeEncoder.encode(string));
    }

    public void rpushx(final String key, final String string) {
	rpushx(SafeEncoder.encode(key), SafeEncoder.encode(string));
    }

    public void sadd(final String key, final String member) {
	sadd(SafeEncoder.encode(key), SafeEncoder.encode(member));
    }

    public void scard(final String key) {
	scard(SafeEncoder.encode(key));
    }

    public void sdiff(final String... keys) {
	final byte[][] bkeys = new byte[keys.length][];
	for (int i = 0; i < bkeys.length; i++) {
	    bkeys[i] = SafeEncoder.encode(keys[i]);
	}
	sdiff(bkeys);
    }

    public void sdiffstore(final String dstkey, final String... keys) {
	final byte[][] bkeys = new byte[keys.length][];
	for (int i = 0; i < bkeys.length; i++) {
	    bkeys[i] = SafeEncoder.encode(keys[i]);
	}
	sdiffstore(SafeEncoder.encode(dstkey), bkeys);
    }

    public void set(final String key, final String value) {
	set(SafeEncoder.encode(key), SafeEncoder.encode(value));
    }

    public void setex(final String key, final int seconds, final String value) {
	setex(SafeEncoder.encode(key), seconds, SafeEncoder.encode(value));
    }

    public void setnx(final String key, final String value) {
	setnx(SafeEncoder.encode(key), SafeEncoder.encode(value));
    }

    public void sinter(final String key1, final String... keyN) {
	final byte[][] bkeys = new byte[keyN.length][];

	for (int i = 0; i < bkeys.length; i++) {
	    bkeys[i] = SafeEncoder.encode(keyN[i]);
	}

	sinter(SafeEncoder.encode(key1), bkeys);
    }

    public void sinterstore(final String dstkey, final String key1,
	    final String... keyN) {
	byte[][] bkeys = new byte[keyN.length][];

	for (int i = 0; i < keyN.length; i++) {
	    bkeys[i] = SafeEncoder.encode(keyN[i]);
	}

	sinterstore(SafeEncoder.encode(dstkey), SafeEncoder.encode(key1), bkeys);
    }

    public void sismember(final String key, final String member) {
	sismember(SafeEncoder.encode(key), SafeEncoder.encode(member));
    }

    public void smembers(final String key) {
	smembers(SafeEncoder.encode(key));
    }

    public void smove(final String srckey, final String dstkey,
	    final String member) {
	smove(SafeEncoder.encode(srckey), SafeEncoder.encode(dstkey),
		SafeEncoder.encode(member));
    }

    public void sort(final String key) {
	sort(SafeEncoder.encode(key));
    }

    public void sort(final String key, final SortParams sortingParameters) {
	sort(SafeEncoder.encode(key), sortingParameters);
    }

    public void sort(final String key, final SortParams sortingParameters,
	    final String dstkey) {
	sort(SafeEncoder.encode(key), sortingParameters,
		SafeEncoder.encode(dstkey));
    }

    public void sort(final String key, final String dstkey) {
	sort(SafeEncoder.encode(key), SafeEncoder.encode(dstkey));
    }

    public void spop(final String key) {
	spop(SafeEncoder.encode(key));
    }

    public void srandmember(final String key) {
	srandmember(SafeEncoder.encode(key));
    }

    public void srem(final String key, final String member) {
	srem(SafeEncoder.encode(key), SafeEncoder.encode(member));
    }

    public void strlen(final String key) {
	strlen(SafeEncoder.encode(key));
    }

    public void substr(final String key, final int start, final int end) {
	substr(SafeEncoder.encode(key), start, end);
    }

    public void sunion(final String key1, final String... keyN) {
	final byte[][] bkeys = new byte[keyN.length][];
	for (int i = 0; i < bkeys.length; i++) {
	    bkeys[i] = SafeEncoder.encode(keyN[i]);
	}
	sunion(SafeEncoder.encode(key1), bkeys);
    }

    public void sunionstore(final String dstkey, final String... keys) {
	final byte[][] bkeys = new byte[keys.length][];
	for (int i = 0; i < bkeys.length; i++) {
	    bkeys[i] = SafeEncoder.encode(keys[i]);
	}
	sunionstore(SafeEncoder.encode(dstkey), bkeys);
    }

    public void ttl(final String key) {
	ttl(SafeEncoder.encode(key));
    }

    public void type(final String key) {
	type(SafeEncoder.encode(key));
    }

    public void watch(final String... keys) {
	final byte[][] bargs = new byte[keys.length][];
	for (int i = 0; i < bargs.length; i++) {
	    bargs[i] = SafeEncoder.encode(keys[i]);
	}
	watch(bargs);
    }

    public void zadd(final String key, final double score, final String member) {
	zadd(SafeEncoder.encode(key), score, SafeEncoder.encode(member));
    }

    public void zcard(final String key) {
	zcard(SafeEncoder.encode(key));
    }

    public void zcount(final String key, final double min, final double max) {
	zcount(SafeEncoder.encode(key), min, max);
    }

    public void zincrby(final String key, final double score,
	    final String member) {
	zincrby(SafeEncoder.encode(key), score, SafeEncoder.encode(member));
    }

    public void zinterstore(final String dstkey, final String... sets) {
	final byte[][] bsets = new byte[sets.length][];
	for (int i = 0; i < bsets.length; i++) {
	    bsets[i] = SafeEncoder.encode(sets[i]);
	}
	zinterstore(SafeEncoder.encode(dstkey), bsets);
    }

    public void zrange(final String key, final int start, final int end) {
	zrange(SafeEncoder.encode(key), start, end);
    }

    public void zrangeByScore(final String key, final double min,
	    final double max) {
	zrangeByScore(SafeEncoder.encode(key), min, max);
    }

    public void zrangeByScore(final String key, final double min,
	    final double max, final int offset, int count) {
	zrangeByScore(SafeEncoder.encode(key), min, max, offset, count);
    }

    public void zrangeByScore(final String key, final String min,
	    final String max) {
	zrangeByScore(SafeEncoder.encode(key), SafeEncoder.encode(min),
		SafeEncoder.encode(max));
    }

    public void zrangeByScoreWithScores(final String key, final double min,
	    final double max) {
	zrangeByScoreWithScores(SafeEncoder.encode(key), min, max);
    }

    public void zrangeByScoreWithScores(final String key, final double min,
	    final double max, final int offset, final int count) {
	zrangeByScoreWithScores(SafeEncoder.encode(key), min, max, offset,
		count);
    }

    public void zrangeWithScores(final String key, final int start,
	    final int end) {
	zrangeWithScores(SafeEncoder.encode(key), start, end);
    }

    public void zrank(final String key, final String member) {
	zrank(SafeEncoder.encode(key), SafeEncoder.encode(member));
    }

    public void zrem(final String key, final String member) {
	zrem(SafeEncoder.encode(key), SafeEncoder.encode(member));
    }

    public void zremrangeByRank(final String key, final int start, final int end) {
	zremrangeByRank(SafeEncoder.encode(key), start, end);
    }

    public void zremrangeByScore(final String key, final double start,
	    final double end) {
	zremrangeByScore(SafeEncoder.encode(key), start, end);
    }

    public void zrevrange(final String key, final int start, final int end) {
	zrevrange(SafeEncoder.encode(key), start, end);
    }

    public void zrevrangeWithScores(final String key, final int start,
	    final int end) {
	zrevrangeWithScores(SafeEncoder.encode(key), start, end);
    }

    public void zrevrank(final String key, final String member) {
	zrevrank(SafeEncoder.encode(key), SafeEncoder.encode(member));
    }

    public void zscore(final String key, final String member) {
	zscore(SafeEncoder.encode(key), SafeEncoder.encode(member));
    }

    public void zunionstore(final String dstkey, final String... sets) {
	final byte[][] bsets = new byte[sets.length][];
	for (int i = 0; i < bsets.length; i++) {
	    bsets[i] = SafeEncoder.encode(sets[i]);
	}
	zunionstore(SafeEncoder.encode(dstkey), bsets);
    }

}