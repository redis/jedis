package redis.clients.jedis;

import java.util.List;
import java.util.Map;

public class Transaction {
    protected Client client = null;

    public Transaction() {
    }

    public Transaction(Client client) {
	this.client = client;
    }

    public String ping() {
	client.ping();
	return client.getStatusCodeReply();
    }

    public String set(String key, String value) {
	client.set(key, value);
	return client.getStatusCodeReply();
    }

    public String get(String key) {
	client.sendCommand("GET", key);
	return client.getStatusCodeReply();
    }

    public String exists(String key) {
	client.exists(key);
	return client.getStatusCodeReply();
    }

    public String del(String... keys) {
	client.del(keys);
	return client.getStatusCodeReply();
    }

    public String type(String key) {
	client.type(key);
	return client.getStatusCodeReply();
    }

    public String flushDB() {
	client.flushDB();
	return client.getStatusCodeReply();
    }

    public String keys(String pattern) {
	client.keys(pattern);
	return client.getStatusCodeReply();
    }

    public String randomKey() {
	client.randomKey();
	return client.getStatusCodeReply();
    }

    public String rename(String oldkey, String newkey) {
	client.rename(oldkey, newkey);
	return client.getStatusCodeReply();
    }

    public String renamenx(String oldkey, String newkey) {
	client.renamenx(oldkey, newkey);
	return client.getStatusCodeReply();
    }

    public String dbSize() {
	client.dbSize();
	return client.getStatusCodeReply();
    }

    public String expire(String key, int seconds) {
	client.expire(key, seconds);
	return client.getStatusCodeReply();
    }

    public String expireAt(String key, long unixTime) {
	client.expireAt(key, unixTime);
	return client.getStatusCodeReply();
    }

    public String ttl(String key) {
	client.ttl(key);
	return client.getStatusCodeReply();
    }

    public String select(int index) {
	client.select(index);
	return client.getStatusCodeReply();
    }

    public String move(String key, int dbIndex) {
	client.move(key, dbIndex);
	return client.getStatusCodeReply();
    }

    public String flushAll() {
	client.flushAll();
	return client.getStatusCodeReply();
    }

    public String getSet(String key, String value) {
	client.getSet(key, value);
	return client.getStatusCodeReply();
    }

    public String mget(String... keys) {
	client.mget(keys);
	return client.getStatusCodeReply();
    }

    public String setnx(String key, String value) {
	client.setnx(key, value);
	return client.getStatusCodeReply();
    }

    public String setex(String key, int seconds, String value) {
	client.setex(key, seconds, value);
	return client.getStatusCodeReply();
    }

    public String mset(String... keysvalues) {
	client.mset(keysvalues);
	return client.getStatusCodeReply();
    }

    public String msetnx(String... keysvalues) {
	client.msetnx(keysvalues);
	return client.getStatusCodeReply();
    }

    public String decrBy(String key, int integer) {
	client.decrBy(key, integer);
	return client.getStatusCodeReply();
    }

    public String decr(String key) {
	client.decr(key);
	return client.getStatusCodeReply();
    }

    public String incrBy(String key, int integer) {
	client.incrBy(key, integer);
	return client.getStatusCodeReply();
    }

    public String incr(String key) {
	client.incr(key);
	return client.getStatusCodeReply();
    }

    public String append(String key, String value) {
	client.append(key, value);
	return client.getStatusCodeReply();
    }

    public String substr(String key, int start, int end) {
	client.substr(key, start, end);
	return client.getStatusCodeReply();
    }

    public String hset(String key, String field, String value) {
	client.hset(key, field, value);
	return client.getStatusCodeReply();
    }

    public String hget(String key, String field) {
	client.hget(key, field);
	return client.getStatusCodeReply();
    }

    public String hsetnx(String key, String field, String value) {
	client.hsetnx(key, field, value);
	return client.getStatusCodeReply();
    }

    public String hmset(String key, Map<String, String> hash) {
	client.hmset(key, hash);
	return client.getStatusCodeReply();
    }

    public String hmget(String key, String... fields) {
	client.hmget(key, fields);
	return client.getStatusCodeReply();
    }

    public String hincrBy(String key, String field, int value) {
	client.hincrBy(key, field, value);
	return client.getStatusCodeReply();
    }

    public String hexists(String key, String field) {
	client.hexists(key, field);
	return client.getStatusCodeReply();
    }

    public String hdel(String key, String field) {
	client.hdel(key, field);
	return client.getStatusCodeReply();
    }

    public String hlen(String key) {
	client.hlen(key);
	return client.getStatusCodeReply();
    }

    public String hkeys(String key) {
	client.hkeys(key);
	return client.getStatusCodeReply();
    }

    public String hvals(String key) {
	client.hvals(key);
	return client.getStatusCodeReply();
    }

    public String hgetAll(String key) {
	client.hgetAll(key);
	return client.getStatusCodeReply();
    }

    public String rpush(String key, String string) {
	client.rpush(key, string);
	return client.getStatusCodeReply();
    }

    public String lpush(String key, String string) {
	client.lpush(key, string);
	return client.getStatusCodeReply();
    }

    public String llen(String key) {
	client.llen(key);
	return client.getStatusCodeReply();
    }

    public String lrange(String key, int start, int end) {
	client.lrange(key, start, end);
	return client.getStatusCodeReply();
    }

    public String ltrim(String key, int start, int end) {
	client.ltrim(key, start, end);
	return client.getStatusCodeReply();
    }

    public String lindex(String key, int index) {
	client.lindex(key, index);
	return client.getStatusCodeReply();
    }

    public String lset(String key, int index, String value) {
	client.lset(key, index, value);
	return client.getStatusCodeReply();
    }

    public String lrem(String key, int count, String value) {
	client.lrem(key, count, value);
	return client.getStatusCodeReply();
    }

    public String lpop(String key) {
	client.lpop(key);
	return client.getStatusCodeReply();
    }

    public String rpop(String key) {
	client.rpop(key);
	return client.getStatusCodeReply();
    }

    public String rpoplpush(String srckey, String dstkey) {
	client.rpoplpush(srckey, dstkey);
	return client.getStatusCodeReply();
    }

    public String sadd(String key, String member) {
	client.sadd(key, member);
	return client.getStatusCodeReply();
    }

    public String smembers(String key) {
	client.smembers(key);
	return client.getStatusCodeReply();
    }

    public String srem(String key, String member) {
	client.srem(key, member);
	return client.getStatusCodeReply();
    }

    public String spop(String key) {
	client.spop(key);
	return client.getStatusCodeReply();
    }

    public String smove(String srckey, String dstkey, String member) {
	client.smove(srckey, dstkey, member);
	return client.getStatusCodeReply();
    }

    public String scard(String key) {
	client.scard(key);
	return client.getStatusCodeReply();
    }

    public String sismember(String key, String member) {
	client.sismember(key, member);
	return client.getStatusCodeReply();
    }

    public String sinter(String... keys) {
	client.sinter(keys);
	return client.getStatusCodeReply();
    }

    public String sinterstore(String dstkey, String... keys) {
	client.sinterstore(dstkey, keys);
	return client.getStatusCodeReply();
    }

    public String sunion(String... keys) {
	client.sunion(keys);
	return client.getStatusCodeReply();
    }

    public String sunionstore(String dstkey, String... keys) {
	client.sunionstore(dstkey, keys);
	return client.getStatusCodeReply();
    }

    public String sdiff(String... keys) {
	client.sdiff(keys);
	return client.getStatusCodeReply();
    }

    public String sdiffstore(String dstkey, String... keys) {
	client.sdiffstore(dstkey, keys);
	return client.getStatusCodeReply();
    }

    public String srandmember(String key) {
	client.srandmember(key);
	return client.getStatusCodeReply();
    }

    public String zadd(String key, double score, String member) {
	client.zadd(key, score, member);
	return client.getStatusCodeReply();
    }

    public String zrange(String key, int start, int end) {
	client.zrange(key, start, end);
	return client.getStatusCodeReply();
    }

    public String zrem(String key, String member) {
	client.zrem(key, member);
	return client.getStatusCodeReply();
    }

    public String zincrby(String key, double score, String member) {
	client.zincrby(key, score, member);
	return client.getStatusCodeReply();
    }

    public String zrank(String key, String member) {
	client.zrank(key, member);
	return client.getStatusCodeReply();
    }

    public String zrevrank(String key, String member) {
	client.zrevrank(key, member);
	return client.getStatusCodeReply();
    }

    public String zrevrange(String key, int start, int end) {
	client.zrevrange(key, start, end);
	return client.getStatusCodeReply();
    }

    public String zrangeWithScores(String key, int start, int end) {
	client.zrangeWithScores(key, start, end);
	return client.getStatusCodeReply();
    }

    public String zrevrangeWithScores(String key, int start, int end) {
	client.zrevrangeWithScores(key, start, end);
	return client.getStatusCodeReply();
    }

    public String zcard(String key) {
	client.zcard(key);
	return client.getStatusCodeReply();
    }

    public String zscore(String key, String member) {
	client.zscore(key, member);
	return client.getStatusCodeReply();
    }

    public List<Object> exec() {
	client.exec();
	return client.getObjectMultiBulkReply();
    }

	public void discard() {
		client.discard();
	}
}