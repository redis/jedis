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

    public Jedis(String host, int port, int timeout) {
	client = new Client(host, port);
	client.setTimeout(timeout);
    }

    public String ping() {
	checkIsInMulti();
	client.ping();
	return client.getStatusCodeReply();
    }

    public String set(String key, String value) {
	checkIsInMulti();
	client.set(key, value);
	return client.getStatusCodeReply();
    }

    public String get(String key) {
	checkIsInMulti();
	client.sendCommand("GET", key);
	return client.getBulkReply();
    }

    public void quit() {
	checkIsInMulti();
	client.quit();
    }

    public int exists(String key) {
	checkIsInMulti();
	client.exists(key);
	return client.getIntegerReply();
    }

    public int del(String... keys) {
	checkIsInMulti();
	client.del(keys);
	return client.getIntegerReply();
    }

    public String type(String key) {
	checkIsInMulti();
	client.type(key);
	return client.getStatusCodeReply();
    }

    public String flushDB() {
	checkIsInMulti();
	client.flushDB();
	return client.getStatusCodeReply();
    }

    public List<String> keys(String pattern) {
	checkIsInMulti();
	client.keys(pattern);
	return client.getMultiBulkReply();
    }

    public String randomKey() {
	checkIsInMulti();
	client.randomKey();
	return client.getBulkReply();
    }

    public String rename(String oldkey, String newkey) {
	checkIsInMulti();
	client.rename(oldkey, newkey);
	return client.getStatusCodeReply();
    }

    public int renamenx(String oldkey, String newkey) {
	checkIsInMulti();
	client.renamenx(oldkey, newkey);
	return client.getIntegerReply();
    }

    public int dbSize() {
	checkIsInMulti();
	client.dbSize();
	return client.getIntegerReply();
    }

    public int expire(String key, int seconds) {
	checkIsInMulti();
	client.expire(key, seconds);
	return client.getIntegerReply();
    }

    public int expireAt(String key, long unixTime) {
	checkIsInMulti();
	client.expireAt(key, unixTime);
	return client.getIntegerReply();
    }

    public int ttl(String key) {
	checkIsInMulti();
	client.ttl(key);
	return client.getIntegerReply();
    }

    public String select(int index) {
	checkIsInMulti();
	client.select(index);
	return client.getStatusCodeReply();
    }

    public int move(String key, int dbIndex) {
	checkIsInMulti();
	client.move(key, dbIndex);
	return client.getIntegerReply();
    }

    public String flushAll() {
	checkIsInMulti();
	client.flushAll();
	return client.getStatusCodeReply();
    }

    public String getSet(String key, String value) {
	checkIsInMulti();
	client.getSet(key, value);
	return client.getBulkReply();
    }

    public List<String> mget(String... keys) {
	checkIsInMulti();
	client.mget(keys);
	return client.getMultiBulkReply();
    }

    public int setnx(String key, String value) {
	checkIsInMulti();
	client.setnx(key, value);
	return client.getIntegerReply();
    }

    public String setex(String key, int seconds, String value) {
	checkIsInMulti();
	client.setex(key, seconds, value);
	return client.getStatusCodeReply();
    }

    public String mset(String... keysvalues) {
	checkIsInMulti();
	client.mset(keysvalues);
	return client.getStatusCodeReply();
    }

    public int msetnx(String... keysvalues) {
	checkIsInMulti();
	client.msetnx(keysvalues);
	return client.getIntegerReply();
    }

    public int decrBy(String key, int integer) {
	checkIsInMulti();
	client.decrBy(key, integer);
	return client.getIntegerReply();
    }

    public int decr(String key) {
	checkIsInMulti();
	client.decr(key);
	return client.getIntegerReply();
    }

    public int incrBy(String key, int integer) {
	checkIsInMulti();
	client.incrBy(key, integer);
	return client.getIntegerReply();
    }

    public int incr(String key) {
	checkIsInMulti();
	client.incr(key);
	return client.getIntegerReply();
    }

    public int append(String key, String value) {
	checkIsInMulti();
	client.append(key, value);
	return client.getIntegerReply();
    }

    public String substr(String key, int start, int end) {
	checkIsInMulti();
	client.substr(key, start, end);
	return client.getBulkReply();
    }

    public int hset(String key, String field, String value) {
	checkIsInMulti();
	client.hset(key, field, value);
	return client.getIntegerReply();
    }

    public String hget(String key, String field) {
	checkIsInMulti();
	client.hget(key, field);
	return client.getBulkReply();
    }

    public int hsetnx(String key, String field, String value) {
	checkIsInMulti();
	client.hsetnx(key, field, value);
	return client.getIntegerReply();
    }

    public String hmset(String key, Map<String, String> hash) {
	checkIsInMulti();
	client.hmset(key, hash);
	return client.getStatusCodeReply();
    }

    public List<String> hmget(String key, String... fields) {
	checkIsInMulti();
	client.hmget(key, fields);
	return client.getMultiBulkReply();
    }

    public int hincrBy(String key, String field, int value) {
	checkIsInMulti();
	client.hincrBy(key, field, value);
	return client.getIntegerReply();
    }

    public int hexists(String key, String field) {
	checkIsInMulti();
	client.hexists(key, field);
	return client.getIntegerReply();
    }

    public int hdel(String key, String field) {
	checkIsInMulti();
	client.hdel(key, field);
	return client.getIntegerReply();
    }

    public int hlen(String key) {
	checkIsInMulti();
	client.hlen(key);
	return client.getIntegerReply();
    }

    public List<String> hkeys(String key) {
	checkIsInMulti();
	client.hkeys(key);
	return client.getMultiBulkReply();
    }

    public List<String> hvals(String key) {
	checkIsInMulti();
	client.hvals(key);
	return client.getMultiBulkReply();
    }

    public Map<String, String> hgetAll(String key) {
	checkIsInMulti();
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
	checkIsInMulti();
	client.rpush(key, string);
	return client.getIntegerReply();
    }

    public int lpush(String key, String string) {
	checkIsInMulti();
	client.lpush(key, string);
	return client.getIntegerReply();
    }

    public int llen(String key) {
	checkIsInMulti();
	client.llen(key);
	return client.getIntegerReply();
    }

    public List<String> lrange(String key, int start, int end) {
	checkIsInMulti();
	client.lrange(key, start, end);
	return client.getMultiBulkReply();
    }

    public String ltrim(String key, int start, int end) {
	checkIsInMulti();
	client.ltrim(key, start, end);
	return client.getStatusCodeReply();
    }

    public String lindex(String key, int index) {
	checkIsInMulti();
	client.lindex(key, index);
	return client.getBulkReply();
    }

    public String lset(String key, int index, String value) {
	checkIsInMulti();
	client.lset(key, index, value);
	return client.getStatusCodeReply();
    }

    public int lrem(String key, int count, String value) {
	checkIsInMulti();
	client.lrem(key, count, value);
	return client.getIntegerReply();
    }

    public String lpop(String key) {
	checkIsInMulti();
	client.lpop(key);
	return client.getBulkReply();
    }

    public String rpop(String key) {
	checkIsInMulti();
	client.rpop(key);
	return client.getBulkReply();
    }

    public String rpoplpush(String srckey, String dstkey) {
	checkIsInMulti();
	client.rpoplpush(srckey, dstkey);
	return client.getBulkReply();
    }

    public int sadd(String key, String member) {
	checkIsInMulti();
	client.sadd(key, member);
	return client.getIntegerReply();
    }

    public Set<String> smembers(String key) {
	checkIsInMulti();
	client.smembers(key);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int srem(String key, String member) {
	checkIsInMulti();
	client.srem(key, member);
	return client.getIntegerReply();
    }

    public String spop(String key) {
	checkIsInMulti();
	client.spop(key);
	return client.getBulkReply();
    }

    public int smove(String srckey, String dstkey, String member) {
	checkIsInMulti();
	client.smove(srckey, dstkey, member);
	return client.getIntegerReply();
    }

    public int scard(String key) {
	checkIsInMulti();
	client.scard(key);
	return client.getIntegerReply();
    }

    public int sismember(String key, String member) {
	checkIsInMulti();
	client.sismember(key, member);
	return client.getIntegerReply();
    }

    public Set<String> sinter(String... keys) {
	checkIsInMulti();
	client.sinter(keys);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int sinterstore(String dstkey, String... keys) {
	checkIsInMulti();
	client.sinterstore(dstkey, keys);
	return client.getIntegerReply();
    }

    public Set<String> sunion(String... keys) {
	checkIsInMulti();
	client.sunion(keys);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int sunionstore(String dstkey, String... keys) {
	checkIsInMulti();
	client.sunionstore(dstkey, keys);
	return client.getIntegerReply();
    }

    public Set<String> sdiff(String... keys) {
	checkIsInMulti();
	client.sdiff(keys);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int sdiffstore(String dstkey, String... keys) {
	checkIsInMulti();
	client.sdiffstore(dstkey, keys);
	return client.getIntegerReply();
    }

    public String srandmember(String key) {
	checkIsInMulti();
	client.srandmember(key);
	return client.getBulkReply();
    }

    public int zadd(String key, double score, String member) {
	checkIsInMulti();
	client.zadd(key, score, member);
	return client.getIntegerReply();
    }

    public Set<String> zrange(String key, int start, int end) {
	checkIsInMulti();
	client.zrange(key, start, end);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public int zrem(String key, String member) {
	checkIsInMulti();
	client.zrem(key, member);
	return client.getIntegerReply();
    }

    public double zincrby(String key, double score, String member) {
	checkIsInMulti();
	client.zincrby(key, score, member);
	String newscore = client.getBulkReply();
	return Double.valueOf(newscore);
    }

    public int zrank(String key, String member) {
	checkIsInMulti();
	client.zrank(key, member);
	return client.getIntegerReply();
    }

    public int zrevrank(String key, String member) {
	checkIsInMulti();
	client.zrevrank(key, member);
	return client.getIntegerReply();
    }

    public Set<String> zrevrange(String key, int start, int end) {
	checkIsInMulti();
	client.zrevrange(key, start, end);
	List<String> members = client.getMultiBulkReply();
	return new LinkedHashSet<String>(members);
    }

    public Set<Tuple> zrangeWithScores(String key, int start, int end) {
	checkIsInMulti();
	client.zrangeWithScores(key, start, end);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    public Set<Tuple> zrevrangeWithScores(String key, int start, int end) {
	checkIsInMulti();
	client.zrevrangeWithScores(key, start, end);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    public int zcard(String key) {
	checkIsInMulti();
	client.zcard(key);
	return client.getIntegerReply();
    }

    public double zscore(String key, String member) {
	checkIsInMulti();
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
	List<Object> results = null;
	try {
	    jedisTransaction.setClient(client);
	    multi();
	    jedisTransaction.execute();
	    results = jedisTransaction.exec();
	} catch (Exception ex) {
	    client.discard();
	}
	return results;
    }

    private void checkIsInMulti() {
	if (client.isInMulti()) {
	    throw new JedisException(
		    "Cannot use Jedis when in Multi. Please use JedisTransaction instead.");
	}
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
	checkIsInMulti();
	client.sort(key);
	return client.getMultiBulkReply();
    }

    public List<String> sort(String key, SortingParams sortingParameters) {
	checkIsInMulti();
	client.sort(key, sortingParameters);
	return client.getMultiBulkReply();
    }

    public List<String> blpop(int timeout, String... keys) {
	checkIsInMulti();
	List<String> args = new ArrayList<String>();
	for (String arg : keys) {
	    args.add(arg);
	}
	args.add(String.valueOf(timeout));

	client.blpop(args.toArray(new String[args.size()]));
	client.setTimeoutInfinite();
	List<String> multiBulkReply = client.getMultiBulkReply();
	client.rollbackTimeout();
	return multiBulkReply;
    }

    public int sort(String key, SortingParams sortingParameters, String dstkey) {
	checkIsInMulti();
	client.sort(key, sortingParameters, dstkey);
	return client.getIntegerReply();
    }

    public int sort(String key, String dstkey) {
	checkIsInMulti();
	client.sort(key, dstkey);
	return client.getIntegerReply();
    }

    public List<String> brpop(int timeout, String... keys) {
	checkIsInMulti();
	List<String> args = new ArrayList<String>();
	for (String arg : keys) {
	    args.add(arg);
	}
	args.add(String.valueOf(timeout));

	client.brpop(args.toArray(new String[args.size()]));
	client.setTimeoutInfinite();
	List<String> multiBulkReply = client.getMultiBulkReply();
	client.rollbackTimeout();

	return multiBulkReply;
    }

    public String auth(String password) {
	checkIsInMulti();
	client.auth(password);
	return client.getStatusCodeReply();
    }

    public List<Object> pipelined(JedisPipeline jedisPipeline) {
	jedisPipeline.setClient(client);
	jedisPipeline.execute();
	return client.getAll();
    }

    public void subscribe(JedisPubSub jedisPubSub, String... channels) {
	client.setTimeoutInfinite();
	jedisPubSub.proceed(client, channels);
	client.rollbackTimeout();
    }

    public int publish(String channel, String message) {
	client.publish(channel, message);
	return client.getIntegerReply();
    }

    public void psubscribe(JedisPubSub jedisPubSub, String... patterns) {
	client.setTimeoutInfinite();
	jedisPubSub.proceedWithPatterns(client, patterns);
	client.rollbackTimeout();
    }

    public int zcount(String key, double min, double max) {
	checkIsInMulti();
	client.zcount(key, min, max);
	return client.getIntegerReply();
    }

    public Set<String> zrangeByScore(String key, double min, double max) {
	checkIsInMulti();
	client.zrangeByScore(key, min, max);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    public Set<String> zrangeByScore(String key, double min, double max,
	    int offset, int count) {
	checkIsInMulti();
	client.zrangeByScore(key, min, max, offset, count);
	return new LinkedHashSet<String>(client.getMultiBulkReply());
    }

    public Set<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
	checkIsInMulti();
	client.zrangeByScoreWithScores(key, min, max);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    public Set<Tuple> zrangeByScoreWithScores(String key, double min,
	    double max, int offset, int count) {
	checkIsInMulti();
	client.zrangeByScoreWithScores(key, min, max, offset, count);
	Set<Tuple> set = getTupledSet();
	return set;
    }

    private Set<Tuple> getTupledSet() {
	checkIsInMulti();
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
	checkIsInMulti();
	client.zremrangeByRank(key, start, end);
	return client.getIntegerReply();
    }

    public int zremrangeByScore(String key, double start, double end) {
	checkIsInMulti();
	client.zremrangeByScore(key, start, end);
	return client.getIntegerReply();
    }

    public int zunionstore(String dstkey, String... sets) {
	checkIsInMulti();
	client.zunionstore(dstkey, sets);
	return client.getIntegerReply();
    }

    public int zunionstore(String dstkey, ZParams params, String... sets) {
	checkIsInMulti();
	client.zunionstore(dstkey, params, sets);
	return client.getIntegerReply();
    }

    public int zinterstore(String dstkey, String... sets) {
	checkIsInMulti();
	client.zinterstore(dstkey, sets);
	return client.getIntegerReply();
    }

    public int zinterstore(String dstkey, ZParams params, String... sets) {
	checkIsInMulti();
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

    public void monitor(JedisMonitor jedisMonitor) {
	client.monitor();
	jedisMonitor.proceed(client);
    }

    public String slaveof(String host, int port) {
	client.slaveof(host, port);
	return client.getStatusCodeReply();
    }

    public String slaveofNoOne() {
	client.slaveofNoOne();
	return client.getStatusCodeReply();
    }

    public List<String> configGet(String pattern) {
	client.configGet(pattern);
	return client.getMultiBulkReply();
    }

    public String configSet(String parameter, String value) {
	client.configSet(parameter, value);
	return client.getStatusCodeReply();
    }

    public boolean isConnected() {
	return client.isConnected();
    }
}