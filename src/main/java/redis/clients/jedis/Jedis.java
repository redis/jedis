package redis.clients.jedis;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
	return client.getBulkReply();
    }

    public void quit() {
	client.quit();
    }

    public int exists(String key) {
	client.exists(key);
	return client.getIntegerReply();
    }

    public int del(String... keys) {
	client.del(keys);
	return client.getIntegerReply();
    }

    public String type(String key) {
	client.type(key);
	return client.getStatusCodeReply();
    }

    public String flushDB() {
	client.flushDB();
	return client.getStatusCodeReply();
    }

    public List<String> keys(String pattern) {
	client.keys(pattern);
	return client.getMultiBulkReply();
    }

    public String randomKey() {
	client.randomKey();
	return client.getBulkReply();
    }

    public String rename(String oldkey, String newkey) {
	client.rename(oldkey, newkey);
	return client.getStatusCodeReply();
    }

    public int renamenx(String oldkey, String newkey) {
	client.renamenx(oldkey, newkey);
	return client.getIntegerReply();
    }

    public int dbSize() {
	client.dbSize();
	return client.getIntegerReply();
    }

    public int expire(String key, int seconds) {
	client.expire(key, seconds);
	return client.getIntegerReply();
    }

    public int expireAt(String key, long unixTime) {
	client.expireAt(key, unixTime);
	return client.getIntegerReply();
    }

    public int ttl(String key) {
	client.ttl(key);
	return client.getIntegerReply();
    }

    public String select(int index) {
	client.select(index);
	return client.getStatusCodeReply();
    }

    public int move(String key, int dbIndex) {
	client.move(key, dbIndex);
	return client.getIntegerReply();
    }

    public String flushAll() {
	client.flushAll();
	return client.getStatusCodeReply();
    }

    public String getSet(String key, String value) {
	client.getSet(key, value);
	return client.getBulkReply();
    }

    public List<String> mget(String... keys) {
	client.mget(keys);
	return client.getMultiBulkReply();
    }

    public int setnx(String key, String value) {
	client.setnx(key, value);
	return client.getIntegerReply();
    }

    public String setex(String key, int seconds, String value) {
	client.setex(key, seconds, value);
	return client.getStatusCodeReply();
    }

    public String mset(String... keysvalues) {
	client.mset(keysvalues);
	return client.getStatusCodeReply();
    }

    public int msetnx(String... keysvalues) {
	client.msetnx(keysvalues);
	return client.getIntegerReply();
    }

    public int decrBy(String key, int integer) {
	client.decrBy(key, integer);
	return client.getIntegerReply();
    }

    public int decr(String key) {
	client.decr(key);
	return client.getIntegerReply();
    }

    public int incrBy(String key, int integer) {
	client.incrBy(key, integer);
	return client.getIntegerReply();
    }

    public int incr(String key) {
	client.incr(key);
	return client.getIntegerReply();
    }

    public int append(String key, String value) {
	client.append(key, value);
	return client.getIntegerReply();
    }

    public String substr(String key, int start, int end) {
	client.substr(key, start, end);
	return client.getBulkReply();
    }

    public int hset(String key, String field, String value) {
	client.hset(key, field, value);
	return client.getIntegerReply();
    }

    public String hget(String key, String field) {
	client.hget(key, field);
	return client.getBulkReply();
    }

    public int hsetnx(String key, String field, String value) {
	client.hsetnx(key, field, value);
	return client.getIntegerReply();
    }

    public String hmset(String key, Map<String, String> hash) {
	client.hmset(key, hash);
	return client.getStatusCodeReply();
    }

    public List<String> hmget(String key, String... fields) {
	client.hmget(key, fields);
	return client.getMultiBulkReply();
    }

    public int hincrBy(String key, String field, int value) {
	client.hincrBy(key, field, value);
	return client.getIntegerReply();
    }

    public int hexists(String key, String field) {
	client.hexists(key, field);
	return client.getIntegerReply();
    }

    public int hdel(String key, String field) {
	client.hdel(key, field);
	return client.getIntegerReply();
    }

    public int hlen(String key) {
	client.hlen(key);
	return client.getIntegerReply();
    }

    public List<String> hkeys(String key) {
	client.hkeys(key);
	return client.getMultiBulkReply();
    }

    public List<String> hvals(String key) {
	client.hvals(key);
	return client.getMultiBulkReply();
    }

    public Map<String, String> hgetAll(String key) {
	client.hgetAll(key);
	List<String> flatHash = client.getMultiBulkReply();
	Map<String, String> hash = new HashMap<String, String>();
	Iterator<String> iterator = flatHash.iterator();
	while (iterator.hasNext()) {
	    hash.put(iterator.next(), iterator.next());
	}

	return hash;
    }

    public int rpush(String key, String string) {
	client.rpush(key, string);
	return client.getIntegerReply();
    }

    public int lpush(String key, String string) {
	client.lpush(key, string);
	return client.getIntegerReply();
    }

    public int llen(String key) {
	client.llen(key);
	return client.getIntegerReply();
    }

    public List<String> lrange(String key, int start, int end) {
	client.lrange(key, start, end);
	return client.getMultiBulkReply();
    }

    public String ltrim(String key, int start, int end) {
	client.ltrim(key, start, end);
	return client.getStatusCodeReply();
    }

    public String lindex(String key, int index) {
	client.lindex(key, index);
	return client.getBulkReply();
    }

    public String lset(String key, int index, String value) {
	client.lset(key, index, value);
	return client.getStatusCodeReply();
    }

    public int lrem(String key, int count, String value) {
	client.lrem(key, count, value);
	return client.getIntegerReply();
    }

    public String lpop(String key) {
	client.lpop(key);
	return client.getBulkReply();
    }

    public String rpop(String key) {
	client.rpop(key);
	return client.getBulkReply();
    }

    public String rpoplpush(String srckey, String dstkey) {
	client.rpoplpush(srckey, dstkey);
	return client.getBulkReply();
    }

    public int sadd(String key, String member) {
	client.sadd(key, member);
	return client.getIntegerReply();
    }

    public Set<String> smembers(String key) {
	client.smembers(key);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int srem(String key, String member) {
	client.srem(key, member);
	return client.getIntegerReply();
    }

    public String spop(String key) {
	client.spop(key);
	return client.getBulkReply();
    }

    public int smove(String srckey, String dstkey, String member) {
	client.smove(srckey, dstkey, member);
	return client.getIntegerReply();
    }

    public int scard(String key) {
	client.scard(key);
	return client.getIntegerReply();
    }

    public int sismember(String key, String member) {
	client.sismember(key, member);
	return client.getIntegerReply();
    }

    public Set<String> sinter(String... keys) {
	client.sinter(keys);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int sinterstore(String dstkey, String... keys) {
	client.sinterstore(dstkey, keys);
	return client.getIntegerReply();
    }

    public Set<String> sunion(String... keys) {
	client.sunion(keys);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int sunionstore(String dstkey, String... keys) {
	client.sunionstore(dstkey, keys);
	return client.getIntegerReply();
    }

    public Set<String> sdiff(String... keys) {
	client.sdiff(keys);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int sdiffstore(String dstkey, String... keys) {
	client.sdiffstore(dstkey, keys);
	return client.getIntegerReply();
    }

    public String srandmember(String key) {
	client.srandmember(key);
	return client.getBulkReply();
    }

    public int zadd(String key, double score, String member) {
	client.zadd(key, score, member);
	return client.getIntegerReply();
    }

    public Set<String> zrange(String key, int start, int end) {
	client.zrange(key, start, end);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int zrem(String key, String member) {
	client.zrem(key, member);
	return client.getIntegerReply();
    }

    public double zincrby(String key, double score, String member) {
	client.zincrby(key, score, member);
	String newscore = client.getBulkReply();
	return Double.valueOf(newscore);
    }

    public int zrank(String key, String member) {
	client.zrank(key, member);
	return client.getIntegerReply();
    }

    public int zrevrank(String key, String member) {
	client.zrevrank(key, member);
	return client.getIntegerReply();
    }

    public Set<String> zrevrange(String key, int start, int end) {
	client.zrevrange(key, start, end);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public Set<Tuple> zrangeWithScores(String key, int start, int end) {
	client.zrangeWithScores(key, start, end);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    public Set<Tuple> zrevrangeWithScores(String key, int start, int end) {
	client.zrevrangeWithScores(key, start, end);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    public int zcard(String key) {
	client.zcard(key);
	return client.getIntegerReply();
    }

    public double zscore(String key, String member) {
	client.zscore(key, member);
	String score = client.getBulkReply();
	return Double.valueOf(score);
    }

    public Transaction multi() {
	client.multi();
	client.getStatusCodeReply();
	return new Transaction(client);
    }

    public List<Object> multi(TransactionBlock jedisTransaction) {
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

    public String watch(String key) {
	client.watch(key);
	return client.getStatusCodeReply();
    }

    public String unwatch() {
	client.unwatch();
	return client.getStatusCodeReply();
    }

    public List<String> sort(String key) {
	client.sort(key);
	return client.getMultiBulkReply();
    }

    public List<String> sort(String key, SortingParams sortingParameters) {
	client.sort(key, sortingParameters);
	return client.getMultiBulkReply();
    }

    public List<String> blpop(int timeout, String... keys) {
	List<String> args = new ArrayList<String>();
	for (String arg : keys) {
	    args.add(arg);
	}
	args.add(String.valueOf(timeout));

	client.blpop(args.toArray(new String[args.size()]));
	return client.getMultiBulkReply();
    }

    public int sort(String key, SortingParams sortingParameters, String dstkey) {
	client.sort(key, sortingParameters, dstkey);
	return client.getIntegerReply();
    }

    public int sort(String key, String dstkey) {
	client.sort(key, dstkey);
	return client.getIntegerReply();
    }

    public List<String> brpop(int timeout, String... keys) {
	List<String> args = new ArrayList<String>();
	for (String arg : keys) {
	    args.add(arg);
	}
	args.add(String.valueOf(timeout));

	client.brpop(args.toArray(new String[args.size()]));
	return client.getMultiBulkReply();
    }

    public String auth(String password) {
	client.auth(password);
	return client.getStatusCodeReply();
    }

    public List<Object> pipelined(JedisPipeline jedisPipeline) {
	jedisPipeline.setClient(client);
	jedisPipeline.execute();
	return client.getAll();
    }

    public void subscribe(JedisPubSub jedisPubSub, String... channels) {
	jedisPubSub.proceed(client, channels);
    }

    public void publish(String channel, String message) {
	client.publish(channel, message);
    }

    public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
	jedisPubSub.proceedWithPatterns(client, patterns);
    }

    public int zcount(String key, double min, double max) {
	client.zcount(key, min, max);
	return client.getIntegerReply();
    }

    public Set<String> zrangeByScore(String key, double min, double max) {
	client.zrangeByScore(key, min, max);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    public Set<String> zrangeByScore(String key, double min, double max,
	    int offset, int count) {
	client.zrangeByScore(key, min, max, offset, count);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
	client.zrangeByScoreWithScores(key, min, max);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    public Set<Tuple> zrangeByScoreWithScores(String key, double min,
	    double max, int offset, int count) {
	client.zrangeByScoreWithScores(key, min, max, offset, count);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    private Set<Tuple> getTupledSet() {
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

    public int zremrangeByRank(String key, int start, int end) {
	client.zremrangeByRank(key, start, end);
	return client.getIntegerReply();
    }

    public int zremrangeByScore(String key, int start, int end) {
	client.zremrangeByScore(key, start, end);
	return client.getIntegerReply();
    }

    public int zunionstore(String dstkey, String... sets) {
	client.zunionstore(dstkey, sets);
	return client.getIntegerReply();
    }

    public int zunionstore(String dstkey, ZParams params, String... sets) {
	client.zunionstore(dstkey, params, sets);
	return client.getIntegerReply();
    }

    public int zinterstore(String dstkey, String... sets) {
	client.zinterstore(dstkey, sets);
	return client.getIntegerReply();
    }

    public int zinterstore(String dstkey, ZParams params, String... sets) {
	client.zinterstore(dstkey, params, sets);
	return client.getIntegerReply();
    }

    public String save() {
	client.save();
	return client.getStatusCodeReply();
    }

    public String bgsave() {
	client.bgsave();
	return client.getStatusCodeReply();
    }

    public String bgrewriteaof() {
	client.bgrewriteaof();
	return client.getStatusCodeReply();
    }

    public int lastsave() {
	client.lastsave();
	return client.getIntegerReply();
    }

    public String shutdown() {
	client.shutdown();
	String status = null;
	try {
	    status = client.getStatusCodeReply();
	} catch (JedisException ex) {
	    status = null;
	}
	return status;
    }

    public String info() {
	client.info();
	return client.getBulkReply();
    }
}