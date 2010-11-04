package redis.clients.jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Client extends Connection {
	public enum LIST_POSITION {
		BEFORE, AFTER
	}

	private boolean isInMulti;

	public boolean isInMulti() {
		return isInMulti;
	}

	public Client(final String host) {
		super(host);
	}

	public Client(final String host, final int port) {
		super(host, port);
	}

	public void ping() {
		sendCommand("PING");
	}

	public void set(final String key, final String value) {
		sendCommand("SET", key, value);
	}

	public void get(final String key) {
		sendCommand("GET", key);
	}

	public void quit() {
		sendCommand("QUIT");
	}

	public void exists(final String key) {
		sendCommand("EXISTS", key);
	}

	public void del(final String... keys) {
		sendCommand("DEL", keys);
	}

	public void type(final String key) {
		sendCommand("TYPE", key);
	}

	public void flushDB() {
		sendCommand("FLUSHDB");
	}

	public void keys(final String pattern) {
		sendCommand("KEYS", pattern);
	}

	public void randomKey() {
		sendCommand("RANDOMKEY");
	}

	public void rename(final String oldkey, final String newkey) {
		sendCommand("RENAME", oldkey, newkey);
	}

	public void renamenx(final String oldkey, final String newkey) {
		sendCommand("RENAMENX", oldkey, newkey);
	}

	public void dbSize() {
		sendCommand("DBSIZE");
	}

	public void expire(final String key, final int seconds) {
		sendCommand("EXPIRE", key, String.valueOf(seconds));
	}

	public void expireAt(final String key, final long unixTime) {
		sendCommand("EXPIREAT", key, String.valueOf(unixTime));
	}

	public void ttl(final String key) {
		sendCommand("TTL", key);
	}

	public void select(final int index) {
		sendCommand("SELECT", String.valueOf(index));
	}

	public void move(final String key, final int dbIndex) {
		sendCommand("MOVE", key, String.valueOf(dbIndex));
	}

	public void flushAll() {
		sendCommand("FLUSHALL");
	}

	public void getSet(final String key, final String value) {
		sendCommand("GETSET", key, value);
	}

	public void mget(final String... keys) {
		sendCommand("MGET", keys);
	}

	public void setnx(final String key, final String value) {
		sendCommand("SETNX", key, value);
	}

	public void setex(
			final String key,
			final int seconds,
			final String value) {
		sendCommand("SETEX", key, String.valueOf(seconds), value);
	}

	public void mset(final String... keysvalues) {
		sendCommand("MSET", keysvalues);
	}

	public void msetnx(final String... keysvalues) {
		sendCommand("MSETNX", keysvalues);
	}

	public void decrBy(final String key, final int integer) {
		sendCommand("DECRBY", key, String.valueOf(integer));
	}

	public void decr(final String key) {
		sendCommand("DECR", key);
	}

	public void incrBy(final String key, final int integer) {
		sendCommand("INCRBY", key, String.valueOf(integer));
	}

	public void incr(final String key) {
		sendCommand("INCR", key);
	}

	public void append(final String key, final String value) {
		sendCommand("APPEND", key, value);
	}

	public void substr(final String key, final int start, final int end) {
		sendCommand("SUBSTR", key, String.valueOf(start), String.valueOf(end));
	}

	public void hset(
			final String key,
			final String field,
			final String value) {
		sendCommand("HSET", key, field, value);
	}

	public void hget(final String key, final String field) {
		sendCommand("HGET", key, field);
	}

	public void hsetnx(
			final String key,
			final String field,
			final String value) {
		sendCommand("HSETNX", key, field, value);
	}

	public void hmset(final String key, final Map<String, String> hash) {
		final List<String> params = new ArrayList<String>();
		params.add(key);

		for (final String field : hash.keySet()) {
			params.add(field);
			params.add(hash.get(field));
		}
		sendCommand("HMSET", params.toArray(new String[params.size()]));
	}

	public void hmget(final String key, final String... fields) {
		final String[] params = new String[fields.length + 1];
		params[0] = key;
		System.arraycopy(fields, 0, params, 1, fields.length);
		sendCommand("HMGET", params);
	}

	public void hincrBy(final String key, final String field, final int value) {
		sendCommand("HINCRBY", key, field, String.valueOf(value));
	}

	public void hexists(final String key, final String field) {
		sendCommand("HEXISTS", key, field);
	}

	public void hdel(final String key, final String field) {
		sendCommand("HDEL", key, field);
	}

	public void hlen(final String key) {
		sendCommand("HLEN", key);
	}

	public void hkeys(final String key) {
		sendCommand("HKEYS", key);
	}

	public void hvals(final String key) {
		sendCommand("HVALS", key);
	}

	public void hgetAll(final String key) {
		sendCommand("HGETALL", key);
	}

	public void rpush(final String key, final String string) {
		sendCommand("RPUSH", key, string);
	}

	public void lpush(final String key, final String string) {
		sendCommand("LPUSH", key, string);
	}

	public void llen(final String key) {
		sendCommand("LLEN", key);
	}

	public void lrange(final String key, final int start, final int end) {
		sendCommand("LRANGE", key, String.valueOf(start), String.valueOf(end));
	}

	public void ltrim(final String key, final int start, final int end) {
		sendCommand("LTRIM", key, String.valueOf(start), String.valueOf(end));
	}

	public void lindex(final String key, final int index) {
		sendCommand("LINDEX", key, String.valueOf(index));
	}

	public void lset(final String key, final int index, final String value) {
		sendCommand("LSET", key, String.valueOf(index), value);
	}

	public void lrem(final String key, int count, final String value) {
		sendCommand("LREM", key, String.valueOf(count), value);
	}

	public void lpop(final String key) {
		sendCommand("LPOP", key);
	}

	public void rpop(final String key) {
		sendCommand("RPOP", key);
	}

	public void rpoplpush(final String srckey, final String dstkey) {
		sendCommand("RPOPLPUSH", srckey, dstkey);
	}

	public void sadd(final String key, final String member) {
		sendCommand("SADD", key, member);
	}

	public void smembers(final String key) {
		sendCommand("SMEMBERS", key);
	}

	public void srem(final String key, final String member) {
		sendCommand("SREM", key, member);
	}

	public void spop(final String key) {
		sendCommand("SPOP", key);
	}

	public void smove(
			final String srckey,
			final String dstkey,
			final String member) {
		sendCommand("SMOVE", srckey, dstkey, member);
	}

	public void scard(final String key) {
		sendCommand("SCARD", key);
	}

	public void sismember(final String key, final String member) {
		sendCommand("SISMEMBER", key, member);
	}

	public void sinter(final String... keys) {
		sendCommand("SINTER", keys);
	}

	public void sinterstore(final String dstkey, final String... keys) {
		String[] params = new String[keys.length + 1];
		params[0] = dstkey;
		System.arraycopy(keys, 0, params, 1, keys.length);
		sendCommand("SINTERSTORE", params);
	}

	public void sunion(final String... keys) {
		sendCommand("SUNION", keys);
	}

	public void sunionstore(final String dstkey, final String... keys) {
		String[] params = new String[keys.length + 1];
		params[0] = dstkey;
		System.arraycopy(keys, 0, params, 1, keys.length);
		sendCommand("SUNIONSTORE", params);
	}

	public void sdiff(final String... keys) {
		sendCommand("SDIFF", keys);
	}

	public void sdiffstore(final String dstkey, final String... keys) {
		String[] params = new String[keys.length + 1];
		params[0] = dstkey;
		System.arraycopy(keys, 0, params, 1, keys.length);
		sendCommand("SDIFFSTORE", params);
	}

	public void srandmember(final String key) {
		sendCommand("SRANDMEMBER", key);
	}

	public void zadd(
			final String key,
			final double score,
			final String member) {
		sendCommand("ZADD", key, String.valueOf(score), member);
	}

	public void zrange(final String key, final int start, final int end) {
		sendCommand("ZRANGE", key, String.valueOf(start), String.valueOf(end));
	}

	public void zrem(final String key, final String member) {
		sendCommand("ZREM", key, member);
	}

	public void zincrby(
			final String key,
			final double score,
			final String member) {
		sendCommand("ZINCRBY", key, String.valueOf(score), member);
	}

	public void zrank(final String key, final String member) {
		sendCommand("ZRANK", key, member);
	}

	public void zrevrank(final String key, final String member) {
		sendCommand("ZREVRANK", key, member);
	}

	public void zrevrange(
			final String key,
			final int start,
			final int end) {
		sendCommand(
				"ZREVRANGE",
				key,
				String.valueOf(start),
				String.valueOf(end));
	}

	public void zrangeWithScores(
			final String key,
			final int start,
			final int end) {
		sendCommand(
				"ZRANGE",
				key,
				String.valueOf(start),
				String.valueOf(end),
				"WITHSCORES");
	}

	public void zrevrangeWithScores(
			final String key,
			final int start,
			final int end) {
		sendCommand(
				"ZREVRANGE",
				key,
				String.valueOf(start),
				String.valueOf(end),
				"WITHSCORES");
	}

	public void zcard(final String key) {
		sendCommand("ZCARD", key);
	}

	public void zscore(final String key, final String member) {
		sendCommand("ZSCORE", key, member);
	}

	public void multi() {
		sendCommand("MULTI");
		isInMulti = true;
	}

	public void discard() {
		sendCommand("DISCARD");
		isInMulti = false;
	}

	public void exec() {
		sendCommand("EXEC");
		isInMulti = false;
	}

	public void watch(final String key) {
		sendCommand("WATCH", key);
	}

	public void unwatch() {
		sendCommand("UNWATCH");
	}

	public void sort(final String key) {
		sendCommand("SORT", key);
	}

	public void sort(final String key, final SortingParams sortingParameters) {
		List<String> args = new ArrayList<String>();
		args.add(key);
		args.addAll(sortingParameters.getParams());
		sendCommand("SORT", args.toArray(new String[args.size()]));
	}

	public void blpop(final String[] args) {
		sendCommand("BLPOP", args);
	}

	public void sort(
			final String key,
			final SortingParams sortingParameters,
			final String dstkey) {
		List<String> args = new ArrayList<String>();
		args.add(key);
		args.addAll(sortingParameters.getParams());
		args.add("STORE");
		args.add(dstkey);
		sendCommand("SORT", args.toArray(new String[args.size()]));
	}

	public void sort(final String key, final String dstkey) {
		sendCommand("SORT", key, "STORE", dstkey);
	}

	public void brpop(final String[] args) {
		sendCommand("BRPOP", args);
	}

	public void auth(final String password) {
		sendCommand("AUTH", password);
	}

	public void subscribe(final String... channels) {
		sendCommand("SUBSCRIBE", channels);
	}

	public void publish(final String channel, final String message) {
		sendCommand("PUBLISH", channel, message);
	}

	public void unsubscribe() {
		sendCommand("UNSUBSCRIBE");
	}

	public void unsubscribe(final String... channels) {
		sendCommand("UNSUBSCRIBE", channels);
	}

	public void psubscribe(final String[] patterns) {
		sendCommand("PSUBSCRIBE", patterns);
	}

	public void punsubscribe() {
		sendCommand("PUNSUBSCRIBE");
	}

	public void punsubscribe(final String... patterns) {
		sendCommand("PUNSUBSCRIBE", patterns);
	}

	public void zcount(final String key, final double min, final double max) {
		sendCommand("ZCOUNT", key, String.valueOf(min), String.valueOf(max));
	}

	public void zrangeByScore(
			final String key,
			final double min,
			final double max) {
		sendCommand(
				"ZRANGEBYSCORE",
				key,
				String.valueOf(min),
				String.valueOf(max));
	}

	public void zrangeByScore(
			final String key,
			final String min,
			final String max) {
		sendCommand("ZRANGEBYSCORE", key, min, max);
	}

	public void zrangeByScore(
			final String key,
			final double min,
			final double max,
			final int offset,
			int count) {
		sendCommand(
				"ZRANGEBYSCORE",
				key, String.valueOf(min),
				String.valueOf(max),
				"LIMIT",
				String.valueOf(offset),
				String.valueOf(count));
	}

	public void zrangeByScoreWithScores(
			final String key,
			final double min,
			final double max) {
		sendCommand(
			"ZRANGEBYSCORE",
			key,
			String.valueOf(min),
			String.valueOf(max),
			"WITHSCORES");
	}

	public void zrangeByScoreWithScores(
			final String key,
			final double min,
			final double max,
			final int offset,
			final int count) {
		sendCommand(
				"ZRANGEBYSCORE",
				key, String.valueOf(min),
				String.valueOf(max),
				"LIMIT",
				String.valueOf(offset),
				String.valueOf(count),
				"WITHSCORES");
	}

	public void zremrangeByRank(
			final String key,
			final int start,
			final int end) {
		sendCommand(
				"ZREMRANGEBYRANK",
				key,
				String.valueOf(start),
				String.valueOf(end));
	}

	public void zremrangeByScore(
			final String key,
			final double start,
			final double end) {
		sendCommand(
				"ZREMRANGEBYSCORE",
				key,
				String.valueOf(start),
				String.valueOf(end));
	}

	public void zunionstore(final String dstkey, final String... sets) {
		final String[] params = new String[sets.length + 2];
		params[0] = dstkey;
		params[1] = String.valueOf(sets.length);
		System.arraycopy(sets, 0, params, 2, sets.length);
		sendCommand("ZUNIONSTORE", params);
	}

	public void zunionstore(
			final String dstkey,
			final ZParams params,
			final String... sets) {
		final List<String> args = new ArrayList<String>();
		args.add(dstkey);
		args.add(String.valueOf(sets.length));
		for (final String set : sets) {
			args.add(set);
		}
		args.addAll(params.getParams());
		sendCommand("ZUNIONSTORE", args.toArray(new String[args.size()]));
	}

	public void zinterstore(final String dstkey, final String... sets) {
		final String[] params = new String[sets.length + 2];
		params[0] = dstkey;
		params[1] = String.valueOf(sets.length);
		System.arraycopy(sets, 0, params, 2, sets.length);
		sendCommand("ZINTERSTORE", params);
	}

	public void zinterstore(
			final String dstkey,
			final ZParams params,
			final String... sets) {
		final List<String> args = new ArrayList<String>();
		args.add(dstkey);
		args.add(String.valueOf(sets.length));
		for (final String set : sets) {
			args.add(set);
		}
		args.addAll(params.getParams());
		sendCommand("ZINTERSTORE", args.toArray(new String[args.size()]));
	}

	public void save() {
		sendCommand("SAVE");
	}

	public void bgsave() {
		sendCommand("BGSAVE");
	}

	public void bgrewriteaof() {
		sendCommand("BGREWRITEAOF");
	}

	public void lastsave() {
		sendCommand("LASTSAVE");
	}

	public void shutdown() {
		sendCommand("SHUTDOWN");
	}

	public void info() {
		sendCommand("INFO");
	}

	public void monitor() {
		sendCommand("MONITOR");
	}

	public void slaveof(final String host, final int port) {
		sendCommand("SLAVEOF", host, String.valueOf(port));
	}

	public void slaveofNoOne() {
		sendCommand("SLAVEOF", "no", "one");
	}

	public void configGet(final String pattern) {
		sendCommand("CONFIG", "GET", pattern);
	}

	public void configSet(final String parameter, final String value) {
		sendCommand("CONFIG", "SET", parameter, value);
	}

	public void strlen(final String key) {
		sendCommand("STRLEN", key);
	}

	public void sync() {
		sendCommand("SYNC");
	}

	public void lpushx(final String key, final String string) {
		sendCommand("LPUSHX", key, string);
	}

	public void persist(final String key) {
		sendCommand("PERSIST", key);
	}

	public void rpushx(final String key, final String string) {
		sendCommand("RPUSHX", key, string);
	}

	public void echo(final String string) {
		sendCommand("ECHO", string);
	}

	public void linsert(
			final String key,
			final LIST_POSITION where,
			final String pivot,
			final String value) {
		sendCommand("LINSERT", key, where.toString(), pivot, value);
	}

	public void debug(final DebugParams params) {
		sendCommand("DEBUG", params.getCommand());
	}
}