package redis.clients.jedis;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Jedis {
    private Client client = null;

    public Jedis(String host) {
	client = new Client(host);
    }

    public Jedis(String host, int port) {
	client = new Client(host, port);
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
	return client.getBulkReply();
    }

    public void quit() throws JedisException {
	client.quit();
    }

    public int exists(String key) throws JedisException {
	client.exists(key);
	return client.getIntegerReply();
    }

    public int del(String... keys) throws JedisException {
	client.del(keys);
	return client.getIntegerReply();
    }

    public String type(String key) throws JedisException {
	client.type(key);
	return client.getStatusCodeReply();
    }

    public String flushDB() throws JedisException {
	client.flushDB();
	return client.getStatusCodeReply();
    }

    public List<String> keys(String pattern) throws JedisException {
	client.keys(pattern);
	return client.getMultiBulkReply();
    }

    public String randomKey() throws JedisException {
	client.randomKey();
	return client.getBulkReply();
    }

    public String rename(String oldkey, String newkey) throws JedisException {
	client.rename(oldkey, newkey);
	return client.getStatusCodeReply();
    }

    public int renamenx(String oldkey, String newkey) throws JedisException {
	client.renamenx(oldkey, newkey);
	return client.getIntegerReply();
    }

    public int dbSize() throws JedisException {
	client.dbSize();
	return client.getIntegerReply();
    }

    public int expire(String key, int seconds) throws JedisException {
	client.expire(key, seconds);
	return client.getIntegerReply();
    }

    public int expireAt(String key, long unixTime) throws JedisException {
	client.expireAt(key, unixTime);
	return client.getIntegerReply();
    }

    public int ttl(String key) throws JedisException {
	client.ttl(key);
	return client.getIntegerReply();
    }

    public String select(int index) throws JedisException {
	client.select(index);
	return client.getStatusCodeReply();
    }

    public int move(String key, int dbIndex) throws JedisException {
	client.move(key, dbIndex);
	return client.getIntegerReply();
    }

    public String flushAll() throws JedisException {
	client.flushAll();
	return client.getStatusCodeReply();
    }

    public String getSet(String key, String value) throws JedisException {
	client.getSet(key, value);
	return client.getBulkReply();
    }

    public List<String> mget(String... keys) throws JedisException {
	client.mget(keys);
	return client.getMultiBulkReply();
    }

    public int setnx(String key, String value) throws JedisException {
	client.setnx(key, value);
	return client.getIntegerReply();
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

    public int msetnx(String... keysvalues) throws JedisException {
	client.msetnx(keysvalues);
	return client.getIntegerReply();
    }

    public int decrBy(String key, int integer) throws JedisException {
	client.decrBy(key, integer);
	return client.getIntegerReply();
    }

    public int decr(String key) throws JedisException {
	client.decr(key);
	return client.getIntegerReply();
    }

    public int incrBy(String key, int integer) throws JedisException {
	client.incrBy(key, integer);
	return client.getIntegerReply();
    }

    public int incr(String key) throws JedisException {
	client.incr(key);
	return client.getIntegerReply();
    }

    public int append(String key, String value) throws JedisException {
	client.append(key, value);
	return client.getIntegerReply();
    }

    public String substr(String key, int start, int end) throws JedisException {
	client.substr(key, start, end);
	return client.getBulkReply();
    }

    public int hset(String key, String field, String value)
	    throws JedisException {
	client.hset(key, field, value);
	return client.getIntegerReply();
    }

    public String hget(String key, String field) throws JedisException {
	client.hget(key, field);
	return client.getBulkReply();
    }

    public int hsetnx(String key, String field, String value)
	    throws JedisException {
	client.hsetnx(key, field, value);
	return client.getIntegerReply();
    }

    public String hmset(String key, Map<String, String> hash)
	    throws JedisException {
	client.hmset(key, hash);
	return client.getStatusCodeReply();
    }

    public List<String> hmget(String key, String... fields)
	    throws JedisException {
	client.hmget(key, fields);
	return client.getMultiBulkReply();
    }

    public int hincrBy(String key, String field, int value)
	    throws JedisException {
	client.hincrBy(key, field, value);
	return client.getIntegerReply();
    }

    public int hexists(String key, String field) throws JedisException {
	client.hexists(key, field);
	return client.getIntegerReply();
    }

    public int hdel(String key, String field) throws JedisException {
	client.hdel(key, field);
	return client.getIntegerReply();
    }

    public int hlen(String key) throws JedisException {
	client.hlen(key);
	return client.getIntegerReply();
    }

    public List<String> hkeys(String key) throws JedisException {
	client.hkeys(key);
	return client.getMultiBulkReply();
    }

    public List<String> hvals(String key) throws JedisException {
	client.hvals(key);
	return client.getMultiBulkReply();
    }

    public Map<String, String> hgetAll(String key) throws JedisException {
	client.hgetAll(key);
	List<String> flatHash = client.getMultiBulkReply();
	Map<String, String> hash = new HashMap<String, String>();
	Iterator<String> iterator = flatHash.iterator();
	while (iterator.hasNext()) {
	    hash.put(iterator.next(), iterator.next());
	}

	return hash;
    }

    public int rpush(String key, String string) throws JedisException {
	client.rpush(key, string);
	return client.getIntegerReply();
    }

    public int lpush(String key, String string) throws JedisException {
	client.lpush(key, string);
	return client.getIntegerReply();
    }

    public int llen(String key) throws JedisException {
	client.llen(key);
	return client.getIntegerReply();
    }

    public List<String> lrange(String key, int start, int end)
	    throws JedisException {
	client.lrange(key, start, end);
	return client.getMultiBulkReply();
    }

    public String ltrim(String key, int start, int end) throws JedisException {
	client.ltrim(key, start, end);
	return client.getStatusCodeReply();
    }

    public String lindex(String key, int index) throws JedisException {
	client.lindex(key, index);
	return client.getBulkReply();
    }

    public String lset(String key, int index, String value)
	    throws JedisException {
	client.lset(key, index, value);
	return client.getStatusCodeReply();
    }

    public int lrem(String key, int count, String value) throws JedisException {
	client.lrem(key, count, value);
	return client.getIntegerReply();
    }

    public String lpop(String key) throws JedisException {
	client.lpop(key);
	return client.getBulkReply();
    }

    public String rpop(String key) throws JedisException {
	client.rpop(key);
	return client.getBulkReply();
    }

    public String rpoplpush(String srckey, String dstkey) throws JedisException {
	client.rpoplpush(srckey, dstkey);
	return client.getBulkReply();
    }

    public int sadd(String key, String member) throws JedisException {
	client.sadd(key, member);
	return client.getIntegerReply();
    }

    public Set<String> smembers(String key) throws JedisException {
	client.smembers(key);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int srem(String key, String member) throws JedisException {
	client.srem(key, member);
	return client.getIntegerReply();
    }

    public String spop(String key) throws JedisException {
	client.spop(key);
	return client.getBulkReply();
    }

    public int smove(String srckey, String dstkey, String member)
	    throws JedisException {
	client.smove(srckey, dstkey, member);
	return client.getIntegerReply();
    }

    public int scard(String key) throws JedisException {
	client.scard(key);
	return client.getIntegerReply();
    }

    public int sismember(String key, String member) throws JedisException {
	client.sismember(key, member);
	return client.getIntegerReply();
    }

    public Set<String> sinter(String... keys) throws JedisException {
	client.sinter(keys);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int sinterstore(String dstkey, String... keys) throws JedisException {
	client.sinterstore(dstkey, keys);
	return client.getIntegerReply();
    }

    public Set<String> sunion(String... keys) throws JedisException {
	client.sunion(keys);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int sunionstore(String dstkey, String... keys) throws JedisException {
	client.sunionstore(dstkey, keys);
	return client.getIntegerReply();
    }

    public Set<String> sdiff(String... keys) throws JedisException {
	client.sdiff(keys);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int sdiffstore(String dstkey, String... keys) throws JedisException {
	client.sdiffstore(dstkey, keys);
	return client.getIntegerReply();
    }

    public String srandmember(String key) throws JedisException {
	client.srandmember(key);
	return client.getBulkReply();
    }

    public int zadd(String key, double score, String member)
	    throws JedisException {
	client.zadd(key, score, member);
	return client.getIntegerReply();
    }

    public Set<String> zrange(String key, int start, int end)
	    throws JedisException {
	client.zrange(key, start, end);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int zrem(String key, String member) throws JedisException {
	client.zrem(key, member);
	return client.getIntegerReply();
    }

    public double zincrby(String key, double score, String member)
	    throws JedisException {
	client.zincrby(key, score, member);
	String newscore = client.getBulkReply();
	return Double.valueOf(newscore);
    }

    public int zrank(String key, String member) throws JedisException {
	client.zrank(key, member);
	return client.getIntegerReply();
    }

    public int zrevrank(String key, String member) throws JedisException {
	client.zrevrank(key, member);
	return client.getIntegerReply();
    }

    public Set<String> zrevrange(String key, int start, int end)
	    throws JedisException {
	client.zrevrange(key, start, end);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public Set<Tuple> zrangeWithScores(String key, int start, int end)
	    throws JedisException {
	client.zrangeWithScores(key, start, end);
	List<String> membersWithScores = client.getMultiBulkReply();
	Set<Tuple> set = new LinkedHashSet<Tuple>();
	Iterator<String> iterator = membersWithScores.iterator();
	while (iterator.hasNext()) {
	    set
		    .add(new Tuple(iterator.next(), Double.valueOf(iterator
			    .next())));
	}
	return set;
    }

    public Set<Tuple> zrevrangeWithScores(String key, int start, int end)
	    throws JedisException {
	client.zrevrangeWithScores(key, start, end);
	List<String> membersWithScores = client.getMultiBulkReply();
	Set<Tuple> set = new LinkedHashSet<Tuple>();
	Iterator<String> iterator = membersWithScores.iterator();
	while (iterator.hasNext()) {
	    set
		    .add(new Tuple(iterator.next(), Double.valueOf(iterator
			    .next())));
	}
	return set;
    }

    public int zcard(String key) throws JedisException {
	client.zcard(key);
	return client.getIntegerReply();
    }

    public double zscore(String key, String member) throws JedisException {
	client.zscore(key, member);
	String score = client.getBulkReply();
	return Double.valueOf(score);
    }

    public Transaction multi() throws JedisException {
	client.multi();
	client.getStatusCodeReply();
	return new Transaction(client);
    }

    public List<Object> multi(TransactionBlock jedisTransaction)
	    throws JedisException {
	try {
	    jedisTransaction.setClient(client);
	    multi();
	    jedisTransaction.execute();
	} catch (Exception ex) {
	    client.discard();
	}
	return jedisTransaction.exec();
    }

    public void connect() throws UnknownHostException, IOException {
	client.connect();
    }

    public void disconnect() throws IOException {
	client.disconnect();
    }

    public String watch(String key) throws JedisException {
	client.watch(key);
	return client.getStatusCodeReply();
    }

}
