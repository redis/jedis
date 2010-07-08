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

    public String ping() throws JedisException {
	client.ping();
	return client.getStatusCodeReply();
    }

    public String set(String key, String value) throws JedisException {
	client.set(key, value);
	return client.getStatusCodeReply();
    }

    public String get(String key) throws JedisException {
	client.sendCommand("GET", key);
	return client.getStatusCodeReply();
    }

    public String exists(String key) throws JedisException {
	client.exists(key);
	return client.getStatusCodeReply();
    }

    public String del(String... keys) throws JedisException {
	client.del(keys);
	return client.getStatusCodeReply();
    }

    public String type(String key) throws JedisException {
	client.type(key);
	return client.getStatusCodeReply();
    }

    public String flushDB() throws JedisException {
	client.flushDB();
	return client.getStatusCodeReply();
    }

    public String keys(String pattern) throws JedisException {
	client.keys(pattern);
	return client.getStatusCodeReply();
    }

    public String randomKey() throws JedisException {
	client.randomKey();
	return client.getStatusCodeReply();
    }

    public String rename(String oldkey, String newkey) throws JedisException {
	client.rename(oldkey, newkey);
	return client.getStatusCodeReply();
    }

    public String renamenx(String oldkey, String newkey) throws JedisException {
	client.renamenx(oldkey, newkey);
	return client.getStatusCodeReply();
    }

    public String dbSize() throws JedisException {
	client.dbSize();
	return client.getStatusCodeReply();
    }

    public String expire(String key, int seconds) throws JedisException {
	client.expire(key, seconds);
	return client.getStatusCodeReply();
    }

    public String expireAt(String key, long unixTime) throws JedisException {
	client.expireAt(key, unixTime);
	return client.getStatusCodeReply();
    }

    public String ttl(String key) throws JedisException {
	client.ttl(key);
	return client.getStatusCodeReply();
    }

    public String select(int index) throws JedisException {
	client.select(index);
	return client.getStatusCodeReply();
    }

    public String move(String key, int dbIndex) throws JedisException {
	client.move(key, dbIndex);
	return client.getStatusCodeReply();
    }

    public String flushAll() throws JedisException {
	client.flushAll();
	return client.getStatusCodeReply();
    }

    public String getSet(String key, String value) throws JedisException {
	client.getSet(key, value);
	return client.getStatusCodeReply();
    }

    public String mget(String... keys) throws JedisException {
	client.mget(keys);
	return client.getStatusCodeReply();
    }

    public String setnx(String key, String value) throws JedisException {
	client.setnx(key, value);
	return client.getStatusCodeReply();
    }

    public String setex(String key, int seconds, String value)
	    throws JedisException {
	client.setex(key, seconds, value);
	return client.getStatusCodeReply();
    }

    public String mset(String... keysvalues) throws JedisException {
	client.mset(keysvalues);
	return client.getStatusCodeReply();
    }

    public String msetnx(String... keysvalues) throws JedisException {
	client.msetnx(keysvalues);
	return client.getStatusCodeReply();
    }

    public String decrBy(String key, int integer) throws JedisException {
	client.decrBy(key, integer);
	return client.getStatusCodeReply();
    }

    public String decr(String key) throws JedisException {
	client.decr(key);
	return client.getStatusCodeReply();
    }

    public String incrBy(String key, int integer) throws JedisException {
	client.incrBy(key, integer);
	return client.getStatusCodeReply();
    }

    public String incr(String key) throws JedisException {
	client.incr(key);
	return client.getStatusCodeReply();
    }

    public String append(String key, String value) throws JedisException {
	client.append(key, value);
	return client.getStatusCodeReply();
    }

    public String substr(String key, int start, int end) throws JedisException {
	client.substr(key, start, end);
	return client.getStatusCodeReply();
    }

    public String hset(String key, String field, String value)
	    throws JedisException {
	client.hset(key, field, value);
	return client.getStatusCodeReply();
    }

    public String hget(String key, String field) throws JedisException {
	client.hget(key, field);
	return client.getStatusCodeReply();
    }

    public String hsetnx(String key, String field, String value)
	    throws JedisException {
	client.hsetnx(key, field, value);
	return client.getStatusCodeReply();
    }

    public String hmset(String key, Map<String, String> hash)
	    throws JedisException {
	client.hmset(key, hash);
	return client.getStatusCodeReply();
    }

    public String hmget(String key, String... fields) throws JedisException {
	client.hmget(key, fields);
	return client.getStatusCodeReply();
    }

    public String hincrBy(String key, String field, int value)
	    throws JedisException {
	client.hincrBy(key, field, value);
	return client.getStatusCodeReply();
    }

    public String hexists(String key, String field) throws JedisException {
	client.hexists(key, field);
	return client.getStatusCodeReply();
    }

    public String hdel(String key, String field) throws JedisException {
	client.hdel(key, field);
	return client.getStatusCodeReply();
    }

    public String hlen(String key) throws JedisException {
	client.hlen(key);
	return client.getStatusCodeReply();
    }

    public String hkeys(String key) throws JedisException {
	client.hkeys(key);
	return client.getStatusCodeReply();
    }

    public String hvals(String key) throws JedisException {
	client.hvals(key);
	return client.getStatusCodeReply();
    }

    public String hgetAll(String key) throws JedisException {
	client.hgetAll(key);
	return client.getStatusCodeReply();
    }

    public String rpush(String key, String string) throws JedisException {
	client.rpush(key, string);
	return client.getStatusCodeReply();
    }

    public String lpush(String key, String string) throws JedisException {
	client.lpush(key, string);
	return client.getStatusCodeReply();
    }

    public String llen(String key) throws JedisException {
	client.llen(key);
	return client.getStatusCodeReply();
    }

    public String lrange(String key, int start, int end) throws JedisException {
	client.lrange(key, start, end);
	return client.getStatusCodeReply();
    }

    public String ltrim(String key, int start, int end) throws JedisException {
	client.ltrim(key, start, end);
	return client.getStatusCodeReply();
    }

    public String lindex(String key, int index) throws JedisException {
	client.lindex(key, index);
	return client.getStatusCodeReply();
    }

    public String lset(String key, int index, String value)
	    throws JedisException {
	client.lset(key, index, value);
	return client.getStatusCodeReply();
    }

    public String lrem(String key, int count, String value)
	    throws JedisException {
	client.lrem(key, count, value);
	return client.getStatusCodeReply();
    }

    public String lpop(String key) throws JedisException {
	client.lpop(key);
	return client.getStatusCodeReply();
    }

    public String rpop(String key) throws JedisException {
	client.rpop(key);
	return client.getStatusCodeReply();
    }

    public String rpoplpush(String srckey, String dstkey) throws JedisException {
	client.rpoplpush(srckey, dstkey);
	return client.getStatusCodeReply();
    }

    public String sadd(String key, String member) throws JedisException {
	client.sadd(key, member);
	return client.getStatusCodeReply();
    }

    public String smembers(String key) throws JedisException {
	client.smembers(key);
	return client.getStatusCodeReply();
    }

    public String srem(String key, String member) throws JedisException {
	client.srem(key, member);
	return client.getStatusCodeReply();
    }

    public String spop(String key) throws JedisException {
	client.spop(key);
	return client.getStatusCodeReply();
    }

    public String smove(String srckey, String dstkey, String member)
	    throws JedisException {
	client.smove(srckey, dstkey, member);
	return client.getStatusCodeReply();
    }

    public String scard(String key) throws JedisException {
	client.scard(key);
	return client.getStatusCodeReply();
    }

    public String sismember(String key, String member) throws JedisException {
	client.sismember(key, member);
	return client.getStatusCodeReply();
    }

    public String sinter(String... keys) throws JedisException {
	client.sinter(keys);
	return client.getStatusCodeReply();
    }

    public String sinterstore(String dstkey, String... keys)
	    throws JedisException {
	client.sinterstore(dstkey, keys);
	return client.getStatusCodeReply();
    }

    public String sunion(String... keys) throws JedisException {
	client.sunion(keys);
	return client.getStatusCodeReply();
    }

    public String sunionstore(String dstkey, String... keys)
	    throws JedisException {
	client.sunionstore(dstkey, keys);
	return client.getStatusCodeReply();
    }

    public String sdiff(String... keys) throws JedisException {
	client.sdiff(keys);
	return client.getStatusCodeReply();
    }

    public String sdiffstore(String dstkey, String... keys)
	    throws JedisException {
	client.sdiffstore(dstkey, keys);
	return client.getStatusCodeReply();
    }

    public String srandmember(String key) throws JedisException {
	client.srandmember(key);
	return client.getStatusCodeReply();
    }

    public String zadd(String key, double score, String member)
	    throws JedisException {
	client.zadd(key, score, member);
	return client.getStatusCodeReply();
    }

    public String zrange(String key, int start, int end) throws JedisException {
	client.zrange(key, start, end);
	return client.getStatusCodeReply();
    }

    public String zrem(String key, String member) throws JedisException {
	client.zrem(key, member);
	return client.getStatusCodeReply();
    }

    public String zincrby(String key, double score, String member)
	    throws JedisException {
	client.zincrby(key, score, member);
	return client.getStatusCodeReply();
    }

    public String zrank(String key, String member) throws JedisException {
	client.zrank(key, member);
	return client.getStatusCodeReply();
    }

    public String zrevrank(String key, String member) throws JedisException {
	client.zrevrank(key, member);
	return client.getStatusCodeReply();
    }

    public String zrevrange(String key, int start, int end)
	    throws JedisException {
	client.zrevrange(key, start, end);
	return client.getStatusCodeReply();
    }

    public String zrangeWithScores(String key, int start, int end)
	    throws JedisException {
	client.zrangeWithScores(key, start, end);
	return client.getStatusCodeReply();
    }

    public String zrevrangeWithScores(String key, int start, int end)
	    throws JedisException {
	client.zrevrangeWithScores(key, start, end);
	return client.getStatusCodeReply();
    }

    public String zcard(String key) throws JedisException {
	client.zcard(key);
	return client.getStatusCodeReply();
    }

    public String zscore(String key, String member) throws JedisException {
	client.zscore(key, member);
	return client.getStatusCodeReply();
    }

    public List<Object> exec() throws JedisException {
	client.exec();
	return client.getObjectMultiBulkReply();
    }
}