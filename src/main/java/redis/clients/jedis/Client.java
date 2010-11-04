package redis.clients.jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Client extends BinaryClient {
	public Client(final String host) {
		super(host);
	}

	public Client(final String host, final int port) {
		super(host, port);
	}

	public void set(final String key, final String value) {
		set(key.getBytes(Protocol.UTF8), value.getBytes(Protocol.UTF8));
	}

	public void get(final String key) {
		get(key.getBytes(Protocol.UTF8));
	}

	public void exists(final String key) {
		exists(key.getBytes(Protocol.UTF8));
	}

	public void del(final String... keys) {
		final byte[][] bkeys = new byte[keys.length][];
		for(int i=0; i < keys.length; i++) {
			bkeys[i] = keys[i].getBytes(Protocol.UTF8);
		}
		del(bkeys);
	}

	public void type(final String key) {
		type(key.getBytes(Protocol.UTF8));
	}

	public void keys(final String pattern) {
		keys(pattern.getBytes(Protocol.UTF8));
	}


	public void rename(final String oldkey, final String newkey) {
		rename(oldkey.getBytes(Protocol.UTF8), newkey.getBytes(Protocol.UTF8));
	}

	public void renamenx(final String oldkey, final String newkey) {
		renamenx(
				oldkey.getBytes(Protocol.UTF8),
				newkey.getBytes(Protocol.UTF8));
	}

	public void expire(final String key, final int seconds) {
		expire(key.getBytes(Protocol.UTF8), seconds);
	}

	public void expireAt(final String key, final long unixTime) {
		expireAt(key.getBytes(Protocol.UTF8), unixTime);
	}

	public void ttl(final String key) {
		ttl(key.getBytes(Protocol.UTF8));
	}

	public void move(final String key, final int dbIndex) {
		move(key.getBytes(Protocol.UTF8), dbIndex);
	}

	public void getSet(final String key, final String value) {
		getSet(key.getBytes(Protocol.UTF8), value.getBytes(Protocol.UTF8));
	}

	public void mget(final String... keys) {
		final byte[][] bkeys = new byte[keys.length][];
		for (int i = 0; i < bkeys.length; i++) {
			bkeys[i] = keys[i].getBytes(Protocol.UTF8);
		}
		mget(bkeys);
	}

	public void setnx(final String key, final String value) {
		setnx(key.getBytes(Protocol.UTF8), value.getBytes(Protocol.UTF8));
	}

	public void setex(
			final String key,
			final int seconds,
			final String value) {
		setex(
				key.getBytes(Protocol.UTF8),
				seconds,
				value.getBytes(Protocol.UTF8));
	}

	public void mset(final String... keysvalues) {
		final byte[][] bkeysvalues = new byte[keysvalues.length][];
		for (int i = 0; i < keysvalues.length; i++) {
			bkeysvalues[i] = keysvalues[i].getBytes(Protocol.UTF8);
		}
		mset(bkeysvalues);
	}

	public void msetnx(final String... keysvalues) {
		final byte[][] bkeysvalues = new byte[keysvalues.length][];
		for (int i = 0; i < keysvalues.length; i++) {
			bkeysvalues[i] = keysvalues[i].getBytes(Protocol.UTF8);
		}
		msetnx(bkeysvalues);
	}

	public void decrBy(final String key, final int integer) {
		decrBy(key.getBytes(Protocol.UTF8), integer);
	}

	public void decr(final String key) {
		decr(key.getBytes(Protocol.UTF8));
	}

	public void incrBy(final String key, final int integer) {
		incrBy(key.getBytes(Protocol.UTF8), integer);
	}

	public void incr(final String key) {
		incr(key.getBytes(Protocol.UTF8));
	}

	public void append(final String key, final String value) {
		append(key.getBytes(Protocol.UTF8), value.getBytes(Protocol.UTF8));
	}

	public void substr(final String key, final int start, final int end) {
		substr(key.getBytes(Protocol.UTF8), start, end);
	}

	public void hset(
			final String key,
			final String field,
			final String value) {
		hset(
				key.getBytes(Protocol.UTF8),
				field.getBytes(Protocol.UTF8),
				value.getBytes(Protocol.UTF8));
	}

	public void hget(final String key, final String field) {
		hget(key.getBytes(Protocol.UTF8), field.getBytes(Protocol.UTF8));
	}

	public void hsetnx(
			final String key,
			final String field,
			final String value) {
		hsetnx(
				key.getBytes(Protocol.UTF8),
				field.getBytes(Protocol.UTF8),
				value.getBytes(Protocol.UTF8));
	}

	public void hmset(final String key, final Map<String, String> hash) {
		final Map<byte[], byte[]> bhash = new HashMap<byte[], byte[]>(hash.size());
		for (final Entry<String,String> entry : hash.entrySet()) {
			bhash.put(
					entry.getKey().getBytes(Protocol.UTF8),
					entry.getValue().getBytes(Protocol.UTF8));
		}
		hmset(key.getBytes(Protocol.UTF8), bhash);
	}

	public void hmget(final String key, final String... fields) {
		final byte[][] bfields = new byte[fields.length][];
		for (int i = 0; i < bfields.length; i++) {
			bfields[i] = fields[i].getBytes(Protocol.UTF8);
		}
		hmget(key.getBytes(Protocol.UTF8), bfields);
	}

	public void hincrBy(final String key, final String field, final int value) {
		hincrBy(
				key.getBytes(Protocol.UTF8),
				field.getBytes(Protocol.UTF8),
				value);
	}

	public void hexists(final String key, final String field) {
		hexists(key.getBytes(Protocol.UTF8), field.getBytes(Protocol.UTF8));
	}

	public void hdel(final String key, final String field) {
		hdel(key.getBytes(Protocol.UTF8), field.getBytes(Protocol.UTF8));
	}

	public void hlen(final String key) {
		hlen(key.getBytes(Protocol.UTF8));
	}

	public void hkeys(final String key) {
		hkeys(key.getBytes(Protocol.UTF8));
	}

	public void hvals(final String key) {
		hvals(key.getBytes(Protocol.UTF8));
	}

	public void hgetAll(final String key) {
		hgetAll(key.getBytes(Protocol.UTF8));
	}

	public void rpush(final String key, final String string) {
		rpush(key.getBytes(Protocol.UTF8), string.getBytes(Protocol.UTF8));
	}

	public void lpush(final String key, final String string) {
		lpush(key.getBytes(Protocol.UTF8), string.getBytes(Protocol.UTF8));
	}

	public void llen(final String key) {
		llen(key.getBytes(Protocol.UTF8));
	}

	public void lrange(final String key, final int start, final int end) {
		lrange(key.getBytes(Protocol.UTF8), start, end);
	}

	public void ltrim(final String key, final int start, final int end) {
		ltrim(key.getBytes(Protocol.UTF8), start, end);
	}

	public void lindex(final String key, final int index) {
		lindex(key.getBytes(Protocol.UTF8), index);
	}

	public void lset(final String key, final int index, final String value) {
		lset(key.getBytes(Protocol.UTF8), index, value.getBytes(Protocol.UTF8));
	}

	public void lrem(final String key, int count, final String value) {
		lrem(key.getBytes(Protocol.UTF8), count, value.getBytes(Protocol.UTF8));
	}

	public void lpop(final String key) {
		lpop(key.getBytes(Protocol.UTF8));
	}

	public void rpop(final String key) {
		rpop(key.getBytes(Protocol.UTF8));
	}

	public void rpoplpush(final String srckey, final String dstkey) {
		rpoplpush(
				srckey.getBytes(Protocol.UTF8),
				dstkey.getBytes(Protocol.UTF8));
	}

	public void sadd(final String key, final String member) {
		sadd(key.getBytes(Protocol.UTF8), member.getBytes(Protocol.UTF8));
	}

	public void smembers(final String key) {
		smembers(key.getBytes(Protocol.UTF8));
	}

	public void srem(final String key, final String member) {
		srem(key.getBytes(Protocol.UTF8), member.getBytes(Protocol.UTF8));
	}

	public void spop(final String key) {
		spop(key.getBytes(Protocol.UTF8));
	}

	public void smove(
			final String srckey,
			final String dstkey,
			final String member) {
		smove(srckey.getBytes(Protocol.UTF8), dstkey.getBytes(Protocol.UTF8), member.getBytes(Protocol.UTF8));
	}

	public void scard(final String key) {
		scard(key.getBytes(Protocol.UTF8));
	}

	public void sismember(final String key, final String member) {
		sismember(key.getBytes(Protocol.UTF8), member.getBytes(Protocol.UTF8));
	}

	public void sinter(final String... keys) {
		final byte[][] bkeys = new byte[keys.length][];
		for (int i = 0; i < bkeys.length; i++) {
			bkeys[i] = keys[i].getBytes(Protocol.UTF8);
		}
		sinter(bkeys);
	}

	public void sinterstore(final String dstkey, final String... keys) {
		final byte[][] bkeys = new byte[keys.length][];
		for (int i = 0; i < bkeys.length; i++) {
			bkeys[i] = keys[i].getBytes(Protocol.UTF8);
		}
		sinterstore(dstkey.getBytes(Protocol.UTF8), bkeys);
	}

	public void sunion(final String... keys) {
		final byte[][] bkeys = new byte[keys.length][];
		for (int i = 0; i < bkeys.length; i++) {
			bkeys[i] = keys[i].getBytes(Protocol.UTF8);
		}
		sunion(bkeys);
	}

	public void sunionstore(final String dstkey, final String... keys) {
		final byte[][] bkeys = new byte[keys.length][];
		for (int i = 0; i < bkeys.length; i++) {
			bkeys[i] = keys[i].getBytes(Protocol.UTF8);
		}
		sunionstore(dstkey.getBytes(Protocol.UTF8), bkeys);
	}

	public void sdiff(final String... keys) {
		final byte[][] bkeys = new byte[keys.length][];
		for (int i = 0; i < bkeys.length; i++) {
			bkeys[i] = keys[i].getBytes(Protocol.UTF8);
		}
		sdiff(bkeys);
	}

	public void sdiffstore(final String dstkey, final String... keys) {
		final byte[][] bkeys = new byte[keys.length][];
		for (int i = 0; i < bkeys.length; i++) {
			bkeys[i] = keys[i].getBytes(Protocol.UTF8);
		}
		sdiffstore(dstkey.getBytes(Protocol.UTF8), bkeys);
	}

	public void srandmember(final String key) {
		srandmember(key.getBytes(Protocol.UTF8));
	}

	public void zadd(
			final String key,
			final double score,
			final String member) {
		zadd(key.getBytes(Protocol.UTF8), score, member.getBytes(Protocol.UTF8));
	}

	public void zrange(final String key, final int start, final int end) {
		zrange(key.getBytes(Protocol.UTF8), start, end);
	}

	public void zrem(final String key, final String member) {
		zrem(key.getBytes(Protocol.UTF8), member.getBytes(Protocol.UTF8));
	}

	public void zincrby(
			final String key,
			final double score,
			final String member) {
		zincrby(key.getBytes(Protocol.UTF8), score, member.getBytes(Protocol.UTF8));
	}

	public void zrank(final String key, final String member) {
		zrank(key.getBytes(Protocol.UTF8), member.getBytes(Protocol.UTF8));
	}

	public void zrevrank(final String key, final String member) {
		zrevrank(key.getBytes(Protocol.UTF8), member.getBytes(Protocol.UTF8));
	}

	public void zrevrange(
			final String key,
			final int start,
			final int end) {
		zrevrange(key.getBytes(Protocol.UTF8), start, end);
	}

	public void zrangeWithScores(
			final String key,
			final int start,
			final int end) {
		zrangeWithScores(key.getBytes(Protocol.UTF8), start, end);
	}

	public void zrevrangeWithScores(
			final String key,
			final int start,
			final int end) {
		zrevrangeWithScores(key.getBytes(Protocol.UTF8), start, end);
	}

	public void zcard(final String key) {
		zcard(key.getBytes(Protocol.UTF8));
	}

	public void zscore(final String key, final String member) {
		zscore(key.getBytes(Protocol.UTF8), member.getBytes(Protocol.UTF8));
	}


	public void watch(final String key) {
		watch(key.getBytes(Protocol.UTF8));
	}

	public void sort(final String key) {
		sort(key.getBytes(Protocol.UTF8));
	}

	public void sort(final String key, final SortingParams sortingParameters) {
		sort(key.getBytes(Protocol.UTF8),sortingParameters);
	}

	public void blpop(final String[] args) {
		final byte[][] bargs = new byte[args.length][];
		for (int i = 0; i < bargs.length; i++) {
			bargs[i] = args[i].getBytes(Protocol.UTF8);
		}
		blpop(bargs);
	}

	public void sort(
			final String key,
			final SortingParams sortingParameters,
			final String dstkey) {
		sort(
				key.getBytes(Protocol.UTF8),
				sortingParameters,
				dstkey.getBytes(Protocol.UTF8));
	}

	public void sort(final String key, final String dstkey) {
		sort(key.getBytes(Protocol.UTF8), dstkey.getBytes(Protocol.UTF8));
	}

	public void brpop(final String[] args) {
		final byte[][] bargs = new byte[args.length][];
		for (int i = 0; i < bargs.length; i++) {
			bargs[i] = args[i].getBytes(Protocol.UTF8);
		}
		brpop(bargs);
	}

	public void zcount(final String key, final double min, final double max) {
		zcount(key.getBytes(Protocol.UTF8), min, max);
	}

	public void zrangeByScore(
			final String key,
			final double min,
			final double max) {
		zrangeByScore(key.getBytes(Protocol.UTF8), min, max);
	}

	public void zrangeByScore(
			final String key,
			final String min,
			final String max) {
		zrangeByScore(
				key.getBytes(Protocol.UTF8),
				min.getBytes(Protocol.UTF8),
				max.getBytes(Protocol.UTF8));
	}

	public void zrangeByScore(
			final String key,
			final double min,
			final double max,
			final int offset,
			int count) {
		zrangeByScore(key.getBytes(Protocol.UTF8), min, max, offset, count);
	}

	public void zrangeByScoreWithScores(
			final String key,
			final double min,
			final double max) {
		zrangeByScoreWithScores(key.getBytes(Protocol.UTF8), min, max);
	}

	public void zrangeByScoreWithScores(
			final String key,
			final double min,
			final double max,
			final int offset,
			final int count) {
		zrangeByScoreWithScores(key.getBytes(Protocol.UTF8), min, max, offset, count);
	}

	public void zremrangeByRank(
			final String key,
			final int start,
			final int end) {
		zremrangeByRank(key.getBytes(Protocol.UTF8), start, end);
	}

	public void zremrangeByScore(
			final String key,
			final double start,
			final double end) {
		zremrangeByScore(key.getBytes(Protocol.UTF8), start, end);
	}

	public void zunionstore(final String dstkey, final String... sets) {
		final byte[][] bsets = new byte[sets.length][];
		for (int i = 0; i < bsets.length; i++) {
			bsets[i] = sets[i].getBytes(Protocol.UTF8);
		}
		zunionstore(dstkey.getBytes(Protocol.UTF8), bsets);
	}

	public void zunionstore(
			final String dstkey,
			final ZParams params,
			final String... sets) {
		final byte[][] bsets = new byte[sets.length][];
		for (int i = 0; i < bsets.length; i++) {
			bsets[i] = sets[i].getBytes(Protocol.UTF8);
		}
		zunionstore(dstkey.getBytes(Protocol.UTF8), params, bsets);
	}

	public void zinterstore(final String dstkey, final String... sets) {
		final byte[][] bsets = new byte[sets.length][];
		for (int i = 0; i < bsets.length; i++) {
			bsets[i] = sets[i].getBytes(Protocol.UTF8);
		}
		zinterstore(dstkey.getBytes(Protocol.UTF8), bsets);
	}

	public void zinterstore(
			final String dstkey,
			final ZParams params,
			final String... sets) {
		final byte[][] bsets = new byte[sets.length][];
		for (int i = 0; i < bsets.length; i++) {
			bsets[i] = sets[i].getBytes(Protocol.UTF8);
		}
		zinterstore(dstkey.getBytes(Protocol.UTF8), params, bsets);
	}

	public void strlen(final String key) {
		strlen(key.getBytes(Protocol.UTF8));
	}

	public void lpushx(final String key, final String string) {
		lpushx(key.getBytes(Protocol.UTF8), string.getBytes(Protocol.UTF8));
	}

	public void persist(final String key) {
		persist(key.getBytes(Protocol.UTF8));
	}

	public void rpushx(final String key, final String string) {
		rpushx(key.getBytes(Protocol.UTF8), string.getBytes(Protocol.UTF8));
	}

	public void echo(final String string) {
		echo(string.getBytes(Protocol.UTF8));
	}

	public void linsert(
			final String key,
			final LIST_POSITION where,
			final String pivot,
			final String value) {
		linsert(
				key.getBytes(Protocol.UTF8),
				where,
				pivot.getBytes(Protocol.UTF8),
				value.getBytes(Protocol.UTF8));
	}
}