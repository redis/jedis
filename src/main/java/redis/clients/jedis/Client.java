package redis.clients.jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Client extends Connection {

    public Client(String host) {
	super(host);
    }

    public Client(String host, int port) {
	super(host, port);
    }

    public void ping() throws JedisException {
	sendCommand("PING");
    }

    public void set(String key, String value) throws JedisException {
	sendCommand("SET", key, value);
    }

    public void get(String key) throws JedisException {
	sendCommand("GET", key);
    }

    public void quit() throws JedisException {
	sendCommand("QUIT");
    }

    public void exists(String key) throws JedisException {
	sendCommand("EXISTS", key);
    }

    public void del(String... keys) throws JedisException {
	sendCommand("DEL", keys);
    }

    public void type(String key) throws JedisException {
	sendCommand("TYPE", key);
    }

    public void flushDB() throws JedisException {
	sendCommand("FLUSHDB");
    }

    public void keys(String pattern) throws JedisException {
	sendCommand("KEYS", pattern);
    }

    public void randomKey() throws JedisException {
	sendCommand("RANDOMKEY");
    }

    public void rename(String oldkey, String newkey) throws JedisException {
	sendCommand("RENAME", oldkey, newkey);
    }

    public void renamenx(String oldkey, String newkey) throws JedisException {
	sendCommand("RENAMENX", oldkey, newkey);
    }

    public void dbSize() throws JedisException {
	sendCommand("DBSIZE");
    }

    public void expire(String key, int seconds) throws JedisException {
	sendCommand("EXPIRE", key, String.valueOf(seconds));
    }

    public void expireAt(String key, long unixTime) throws JedisException {
	sendCommand("EXPIREAT", key, String.valueOf(unixTime));
    }

    public void ttl(String key) throws JedisException {
	sendCommand("TTL", key);
    }

    public void select(int index) throws JedisException {
	sendCommand("SELECT", String.valueOf(index));
    }

    public void move(String key, int dbIndex) throws JedisException {
	sendCommand("MOVE", key, String.valueOf(dbIndex));
    }

    public void flushAll() throws JedisException {
	sendCommand("FLUSHALL");
    }

    public void getSet(String key, String value) throws JedisException {
	sendCommand("GETSET", key, value);
    }

    public void mget(String... keys) throws JedisException {
	sendCommand("MGET", keys);
    }

    public void setnx(String key, String value) throws JedisException {
	sendCommand("SETNX", key, value);
    }

    public void setex(String key, int seconds, String value)
	    throws JedisException {
	sendCommand("SETEX", key, String.valueOf(seconds), value);
    }

    public void mset(String... keysvalues) throws JedisException {
	sendCommand("MSET", keysvalues);
    }

    public void msetnx(String... keysvalues) throws JedisException {
	sendCommand("MSETNX", keysvalues);
    }

    public void decrBy(String key, int integer) throws JedisException {
	sendCommand("DECRBY", key, String.valueOf(integer));
    }

    public void decr(String key) throws JedisException {
	sendCommand("DECR", key);
    }

    public void incrBy(String key, int integer) throws JedisException {
	sendCommand("INCRBY", key, String.valueOf(integer));
    }

    public void incr(String key) throws JedisException {
	sendCommand("INCR", key);
    }

    public void append(String key, String value) throws JedisException {
	sendCommand("APPEND", key, value);
    }

    public void substr(String key, int start, int end) throws JedisException {
	sendCommand("SUBSTR", key, String.valueOf(start), String.valueOf(end));
    }

    public void hset(String key, String field, String value)
	    throws JedisException {
	sendCommand("HSET", key, field, value);
    }

    public void hget(String key, String field) throws JedisException {
	sendCommand("HGET", key, field);
    }

    public void hsetnx(String key, String field, String value)
	    throws JedisException {
	sendCommand("HSETNX", key, field, value);
    }

    public void hmset(String key, Map<String, String> hash)
	    throws JedisException {
	List<String> params = new ArrayList<String>();
	params.add(key);

	for (String field : hash.keySet()) {
	    params.add(field);
	    params.add(hash.get(field));
	}
	sendCommand("HMSET", params.toArray(new String[params.size()]));
    }

    public void hmget(String key, String... fields) throws JedisException {
	String[] params = new String[fields.length + 1];
	params[0] = key;
	System.arraycopy(fields, 0, params, 1, fields.length);
	sendCommand("HMGET", params);
    }

    public void hincrBy(String key, String field, int value)
	    throws JedisException {
	sendCommand("HINCRBY", key, field, String.valueOf(value));
    }

    public void hexists(String key, String field) throws JedisException {
	sendCommand("HEXISTS", key, field);
    }

    public void hdel(String key, String field) throws JedisException {
	sendCommand("HDEL", key, field);
    }

    public void hlen(String key) throws JedisException {
	sendCommand("HLEN", key);
    }

    public void hkeys(String key) throws JedisException {
	sendCommand("HKEYS", key);
    }

    public void hvals(String key) throws JedisException {
	sendCommand("HVALS", key);
    }

    public void hgetAll(String key) throws JedisException {
	sendCommand("HGETALL", key);
    }

    public void rpush(String key, String string) throws JedisException {
	sendCommand("RPUSH", key, string);
    }

    public void lpush(String key, String string) throws JedisException {
	sendCommand("LPUSH", key, string);
    }

    public void llen(String key) throws JedisException {
	sendCommand("LLEN", key);
    }

    public void lrange(String key, int start, int end) throws JedisException {
	sendCommand("LRANGE", key, String.valueOf(start), String.valueOf(end));
    }

    public void ltrim(String key, int start, int end) throws JedisException {
	sendCommand("LTRIM", key, String.valueOf(start), String.valueOf(end));
    }

    public void lindex(String key, int index) throws JedisException {
	sendCommand("LINDEX", key, String.valueOf(index));
    }

    public void lset(String key, int index, String value) throws JedisException {
	sendCommand("LSET", key, String.valueOf(index), value);
    }

    public void lrem(String key, int count, String value) throws JedisException {
	sendCommand("LREM", key, String.valueOf(count), value);
    }

    public void lpop(String key) throws JedisException {
	sendCommand("LPOP", key);
    }

    public void rpop(String key) throws JedisException {
	sendCommand("RPOP", key);
    }

    public void rpoplpush(String srckey, String dstkey) throws JedisException {
	sendCommand("RPOPLPUSH", srckey, dstkey);
    }

    public void sadd(String key, String member) throws JedisException {
	sendCommand("SADD", key, member);
    }

    public void smembers(String key) throws JedisException {
	sendCommand("SMEMBERS", key);
    }

    public void srem(String key, String member) throws JedisException {
	sendCommand("SREM", key, member);
    }

    public void spop(String key) throws JedisException {
	sendCommand("SPOP", key);
    }

    public void smove(String srckey, String dstkey, String member)
	    throws JedisException {
	sendCommand("SMOVE", srckey, dstkey, member);
    }

    public void scard(String key) throws JedisException {
	sendCommand("SCARD", key);
    }

    public void sismember(String key, String member) throws JedisException {
	sendCommand("SISMEMBER", key, member);
    }

    public void sinter(String... keys) throws JedisException {
	sendCommand("SINTER", keys);
    }

    public void sinterstore(String dstkey, String... keys)
	    throws JedisException {
	String[] params = new String[keys.length + 1];
	params[0] = dstkey;
	System.arraycopy(keys, 0, params, 1, keys.length);
	sendCommand("SINTERSTORE", params);
    }

    public void sunion(String... keys) throws JedisException {
	sendCommand("SUNION", keys);
    }

    public void sunionstore(String dstkey, String... keys)
	    throws JedisException {
	String[] params = new String[keys.length + 1];
	params[0] = dstkey;
	System.arraycopy(keys, 0, params, 1, keys.length);
	sendCommand("SUNIONSTORE", params);
    }

    public void sdiff(String... keys) throws JedisException {
	sendCommand("SDIFF", keys);
    }

    public void sdiffstore(String dstkey, String... keys) throws JedisException {
	String[] params = new String[keys.length + 1];
	params[0] = dstkey;
	System.arraycopy(keys, 0, params, 1, keys.length);
	sendCommand("SDIFFSTORE", params);
    }

    public void srandmember(String key) throws JedisException {
	sendCommand("SRANDMEMBER", key);
    }

    public void zadd(String key, double score, String member)
	    throws JedisException {
	sendCommand("ZADD", key, String.valueOf(score), member);
    }

    public void zrange(String key, int start, int end) throws JedisException {
	sendCommand("ZRANGE", key, String.valueOf(start), String.valueOf(end));
    }

    public void zrem(String key, String member) throws JedisException {
	sendCommand("ZREM", key, member);
    }

    public void zincrby(String key, double score, String member)
	    throws JedisException {
	sendCommand("ZINCRBY", key, String.valueOf(score), member);
    }

    public void zrank(String key, String member) throws JedisException {
	sendCommand("ZRANK", key, member);
    }

    public void zrevrank(String key, String member) throws JedisException {
	sendCommand("ZREVRANK", key, member);
    }

    public void zrevrange(String key, int start, int end) throws JedisException {
	sendCommand("ZREVRANGE", key, String.valueOf(start), String
		.valueOf(end));
    }

    public void zrangeWithScores(String key, int start, int end)
	    throws JedisException {
	sendCommand("ZRANGE", key, String.valueOf(start), String.valueOf(end),
		"WITHSCORES");
    }

    public void zrevrangeWithScores(String key, int start, int end)
	    throws JedisException {
	sendCommand("ZREVRANGE", key, String.valueOf(start), String
		.valueOf(end), "WITHSCORES");
    }

    public void zcard(String key) throws JedisException {
	sendCommand("ZCARD", key);
    }

    public void zscore(String key, String member) throws JedisException {
	sendCommand("ZSCORE", key, member);
    }

    public void multi() throws JedisException {
	sendCommand("MULTI");
    }

    public void discard() throws JedisException {
	sendCommand("MULTI");
    }

    public void exec() throws JedisException {
	sendCommand("EXEC");
    }

    public void watch(String key) throws JedisException {
	sendCommand("WATCH", key);
    }

    public void unwatch() throws JedisException {
	sendCommand("UNWATCH");
    }

    public void sort(String key) throws JedisException {
	sendCommand("SORT", key);
    }

    public void sort(String key, SortingParams sortingParameters)
	    throws JedisException {
	List<String> args = new ArrayList<String>();
	args.add(key);
	args.addAll(sortingParameters.getParams());
	sendCommand("SORT", args.toArray(new String[args.size()]));
    }
}
