package redis.clients.jedis;

import java.util.List;
import java.util.Map;

public class BinaryTransaction {
	protected Client client = null;

	public BinaryTransaction() {
	}

	public BinaryTransaction(final Client client) {
		this.client = client;
	}

	public String ping() {
		client.ping();
		return client.getStatusCodeReply();
	}

	public String set(final byte[] key, final byte[] value) {
		client.set(key, value);
		return client.getStatusCodeReply();
	}

	public String get(final byte[] key) {
		client.get(key);
		return client.getStatusCodeReply();
	}

	public String exists(final byte[] key) {
		client.exists(key);
		return client.getStatusCodeReply();
	}

	public String del(final byte[]... keys) {
		client.del(keys);
		return client.getStatusCodeReply();
	}

	public String type(final byte[] key) {
		client.type(key);
		return client.getStatusCodeReply();
	}

	public String flushDB() {
		client.flushDB();
		return client.getStatusCodeReply();
	}

	public String keys(final byte[] pattern) {
		client.keys(pattern);
		return client.getStatusCodeReply();
	}

	public byte[] randomBinaryKey() {
		client.randomKey();
		return client.getBinaryBulkReply();
	}

	public String rename(final byte[] oldkey, final byte[] newkey) {
		client.rename(oldkey, newkey);
		return client.getStatusCodeReply();
	}

	public String renamenx(final byte[] oldkey, final byte[] newkey) {
		client.renamenx(oldkey, newkey);
		return client.getStatusCodeReply();
	}

	public String dbSize() {
		client.dbSize();
		return client.getStatusCodeReply();
	}

	public String expire(final byte[] key, final int seconds) {
		client.expire(key, seconds);
		return client.getStatusCodeReply();
	}

	public String expireAt(final byte[] key, final long unixTime) {
		client.expireAt(key, unixTime);
		return client.getStatusCodeReply();
	}

	public String ttl(final byte[] key) {
		client.ttl(key);
		return client.getStatusCodeReply();
	}

	public String select(final int index) {
		client.select(index);
		return client.getStatusCodeReply();
	}

	public String move(final byte[] key, final int dbIndex) {
		client.move(key, dbIndex);
		return client.getStatusCodeReply();
	}

	public String flushAll() {
		client.flushAll();
		return client.getStatusCodeReply();
	}

	public String getSet(final byte[] key, final byte[] value) {
		client.getSet(key, value);
		return client.getStatusCodeReply();
	}

	public String mget(final byte[]... keys) {
		client.mget(keys);
		return client.getStatusCodeReply();
	}

	public String setnx(final byte[] key, final byte[] value) {
		client.setnx(key, value);
		return client.getStatusCodeReply();
	}

	public String setex(final byte[] key, final int seconds, final byte[] value) {
		client.setex(key, seconds, value);
		return client.getStatusCodeReply();
	}

	public String mset(final byte[]... keysvalues) {
		client.mset(keysvalues);
		return client.getStatusCodeReply();
	}

	public String msetnx(final byte[]... keysvalues) {
		client.msetnx(keysvalues);
		return client.getStatusCodeReply();
	}

	public String decrBy(final byte[] key, final int integer) {
		client.decrBy(key, integer);
		return client.getStatusCodeReply();
	}

	public String decr(final byte[] key) {
		client.decr(key);
		return client.getStatusCodeReply();
	}

	public String incrBy(final byte[] key, final int integer) {
		client.incrBy(key, integer);
		return client.getStatusCodeReply();
	}

	public String incr(final byte[] key) {
		client.incr(key);
		return client.getStatusCodeReply();
	}

	public String append(final byte[] key, final byte[] value) {
		client.append(key, value);
		return client.getStatusCodeReply();
	}

	public String substr(final byte[] key, final int start, final int end) {
		client.substr(key, start, end);
		return client.getStatusCodeReply();
	}

	public String hset(final byte[] key, final byte[] field, final byte[] value) {
		client.hset(key, field, value);
		return client.getStatusCodeReply();
	}

	public String hget(final byte[] key, final byte[] field) {
		client.hget(key, field);
		return client.getStatusCodeReply();
	}

	public String hsetnx(final byte[] key, final byte[] field,
			final byte[] value) {
		client.hsetnx(key, field, value);
		return client.getStatusCodeReply();
	}

	public String hmset(final byte[] key, final Map<byte[], byte[]> hash) {
		client.hmset(key, hash);
		return client.getStatusCodeReply();
	}

	public String hmget(final byte[] key, final byte[]... fields) {
		client.hmget(key, fields);
		return client.getStatusCodeReply();
	}

	public String hincrBy(final byte[] key, final byte[] field, final int value) {
		client.hincrBy(key, field, value);
		return client.getStatusCodeReply();
	}

	public String hexists(final byte[] key, final byte[] field) {
		client.hexists(key, field);
		return client.getStatusCodeReply();
	}

	public String hdel(final byte[] key, final byte[] field) {
		client.hdel(key, field);
		return client.getStatusCodeReply();
	}

	public String hlen(final byte[] key) {
		client.hlen(key);
		return client.getStatusCodeReply();
	}

	public String hkeys(final byte[] key) {
		client.hkeys(key);
		return client.getStatusCodeReply();
	}

	public String hvals(final byte[] key) {
		client.hvals(key);
		return client.getStatusCodeReply();
	}

	public String hgetAll(final byte[] key) {
		client.hgetAll(key);
		return client.getStatusCodeReply();
	}

	public String rpush(final byte[] key, final byte[] string) {
		client.rpush(key, string);
		return client.getStatusCodeReply();
	}

	public String lpush(final byte[] key, final byte[] string) {
		client.lpush(key, string);
		return client.getStatusCodeReply();
	}

	public String llen(final byte[] key) {
		client.llen(key);
		return client.getStatusCodeReply();
	}

	public String lrange(final byte[] key, final int start, final int end) {
		client.lrange(key, start, end);
		return client.getStatusCodeReply();
	}

	public String ltrim(final byte[] key, final int start, final int end) {
		client.ltrim(key, start, end);
		return client.getStatusCodeReply();
	}

	public String lindex(final byte[] key, final int index) {
		client.lindex(key, index);
		return client.getStatusCodeReply();
	}

	public String lset(final byte[] key, final int index, final byte[] value) {
		client.lset(key, index, value);
		return client.getStatusCodeReply();
	}

	public String lrem(final byte[] key, final int count, final byte[] value) {
		client.lrem(key, count, value);
		return client.getStatusCodeReply();
	}

	public String lpop(final byte[] key) {
		client.lpop(key);
		return client.getStatusCodeReply();
	}

	public String rpop(final byte[] key) {
		client.rpop(key);
		return client.getStatusCodeReply();
	}

	public String rpoplpush(final byte[] srckey, final byte[] dstkey) {
		client.rpoplpush(srckey, dstkey);
		return client.getStatusCodeReply();
	}

	public String sadd(final byte[] key, final byte[] member) {
		client.sadd(key, member);
		return client.getStatusCodeReply();
	}

	public String smembers(final byte[] key) {
		client.smembers(key);
		return client.getStatusCodeReply();
	}

	public String srem(final byte[] key, final byte[] member) {
		client.srem(key, member);
		return client.getStatusCodeReply();
	}

	public String spop(final byte[] key) {
		client.spop(key);
		return client.getStatusCodeReply();
	}

	public String smove(final byte[] srckey, final byte[] dstkey,
			final byte[] member) {
		client.smove(srckey, dstkey, member);
		return client.getStatusCodeReply();
	}

	public String scard(final byte[] key) {
		client.scard(key);
		return client.getStatusCodeReply();
	}

	public String sismember(final byte[] key, final byte[] member) {
		client.sismember(key, member);
		return client.getStatusCodeReply();
	}

	public String sinter(final byte[]... keys) {
		client.sinter(keys);
		return client.getStatusCodeReply();
	}

	public String sinterstore(final byte[] dstkey, final byte[]... keys) {
		client.sinterstore(dstkey, keys);
		return client.getStatusCodeReply();
	}

	public String sunion(final byte[]... keys) {
		client.sunion(keys);
		return client.getStatusCodeReply();
	}

	public String sunionstore(final byte[] dstkey, final byte[]... keys) {
		client.sunionstore(dstkey, keys);
		return client.getStatusCodeReply();
	}

	public String sdiff(final byte[]... keys) {
		client.sdiff(keys);
		return client.getStatusCodeReply();
	}

	public String sdiffstore(final byte[] dstkey, final byte[]... keys) {
		client.sdiffstore(dstkey, keys);
		return client.getStatusCodeReply();
	}

	public String srandmember(final byte[] key) {
		client.srandmember(key);
		return client.getStatusCodeReply();
	}

	public String zadd(final byte[] key, final double score, final byte[] member) {
		client.zadd(key, score, member);
		return client.getStatusCodeReply();
	}

	public String zrange(final byte[] key, final int start, final int end) {
		client.zrange(key, start, end);
		return client.getStatusCodeReply();
	}

	public String zrem(final byte[] key, final byte[] member) {
		client.zrem(key, member);
		return client.getStatusCodeReply();
	}

	public String zincrby(final byte[] key, final double score,
			final byte[] member) {
		client.zincrby(key, score, member);
		return client.getStatusCodeReply();
	}

	public String zrank(final byte[] key, final byte[] member) {
		client.zrank(key, member);
		return client.getStatusCodeReply();
	}

	public String zrevrank(final byte[] key, final byte[] member) {
		client.zrevrank(key, member);
		return client.getStatusCodeReply();
	}

	public String zrevrange(final byte[] key, final int start, final int end) {
		client.zrevrange(key, start, end);
		return client.getStatusCodeReply();
	}

	public String zrangeWithScores(final byte[] key, final int start,
			final int end) {
		client.zrangeWithScores(key, start, end);
		return client.getStatusCodeReply();
	}

	public String zrevrangeWithScores(final byte[] key, final int start,
			final int end) {
		client.zrevrangeWithScores(key, start, end);
		return client.getStatusCodeReply();
	}

	public String zcard(final byte[] key) {
		client.zcard(key);
		return client.getStatusCodeReply();
	}

	public String zscore(final byte[] key, final byte[] member) {
		client.zscore(key, member);
		return client.getStatusCodeReply();
	}

	public List<Object> exec() {
		client.exec();

		return client.getObjectMultiBulkReply();
	}

	public String sort(final byte[] key) {
		client.sort(key);
		return client.getStatusCodeReply();
	}

	public String sort(final byte[] key, final SortingParams sortingParameters) {
		client.sort(key, sortingParameters);
		return client.getStatusCodeReply();
	}

	public void discard() {
		client.discard();
	}
}