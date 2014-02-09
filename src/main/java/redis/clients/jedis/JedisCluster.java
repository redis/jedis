package redis.clients.jedis;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

public class JedisCluster implements JedisCommands, BasicCommands {
    public static final short HASHSLOTS = 16384;
    private static final int DEFAULT_TIMEOUT = 1;
    private static final int DEFAULT_MAX_REDIRECTIONS = 5;

    private int timeout;
    private int maxRedirections;

    private JedisClusterConnectionHandler connectionHandler;

    public JedisCluster(Set<HostAndPort> nodes, int timeout) {
	this(nodes, timeout, DEFAULT_MAX_REDIRECTIONS);
    }

    public JedisCluster(Set<HostAndPort> nodes) {
	this(nodes, DEFAULT_TIMEOUT);
    }

    public JedisCluster(Set<HostAndPort> jedisClusterNode, int timeout,
	    int maxRedirections) {
	this.connectionHandler = new JedisSlotBasedConnectionHandler(
		jedisClusterNode);
	this.timeout = timeout;
	this.maxRedirections = maxRedirections;
    }

    @Override
    public String set(final String key, final String value) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().set(key, value);
	    }
	}.run(key);
    }

    @Override
    public String get(final String key) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().get(key);
	    }
	}.run(key);
    }

    @Override
    public Boolean exists(final String key) {
	return new JedisClusterCommand<Boolean>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Boolean execute() {
		return connectionHandler.getConnection().exists(key);
	    }
	}.run(key);
    }

    @Override
    public Long persist(final String key) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().persist(key);
	    }
	}.run(key);
    }

    @Override
    public String type(final String key) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().type(key);
	    }
	}.run(key);
    }

    @Override
    public Long expire(final String key, final int seconds) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().expire(key, seconds);
	    }
	}.run(key);
    }

    @Override
    public Long expireAt(final String key, final long unixTime) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection()
			.expireAt(key, unixTime);
	    }
	}.run(key);
    }

    @Override
    public Long ttl(final String key) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().ttl(key);
	    }
	}.run(key);
    }

    @Override
    public Boolean setbit(final String key, final long offset,
	    final boolean value) {
	return new JedisClusterCommand<Boolean>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Boolean execute() {
		return connectionHandler.getConnection().setbit(key, offset,
			value);
	    }
	}.run(key);
    }

    @Override
    public Boolean setbit(final String key, final long offset,
	    final String value) {
	return new JedisClusterCommand<Boolean>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Boolean execute() {
		return connectionHandler.getConnection().setbit(key, offset,
			value);
	    }
	}.run(key);
    }

    @Override
    public Boolean getbit(final String key, final long offset) {
	return new JedisClusterCommand<Boolean>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Boolean execute() {
		return connectionHandler.getConnection().getbit(key, offset);
	    }
	}.run(key);
    }

    @Override
    public Long setrange(final String key, final long offset, final String value) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().setrange(key, offset,
			value);
	    }
	}.run(key);
    }

    @Override
    public String getrange(final String key, final long startOffset,
	    final long endOffset) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().getrange(key,
			startOffset, endOffset);
	    }
	}.run(key);
    }

    @Override
    public String getSet(final String key, final String value) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().getSet(key, value);
	    }
	}.run(key);
    }

    @Override
    public Long setnx(final String key, final String value) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().setnx(key, value);
	    }
	}.run(key);
    }

    @Override
    public String setex(final String key, final int seconds, final String value) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().setex(key, seconds,
			value);
	    }
	}.run(key);
    }

    @Override
    public Long decrBy(final String key, final long integer) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().decrBy(key, integer);
	    }
	}.run(key);
    }

    @Override
    public Long decr(final String key) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().decr(key);
	    }
	}.run(key);
    }

    @Override
    public Long incrBy(final String key, final long integer) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().incrBy(key, integer);
	    }
	}.run(key);
    }

    @Override
    public Long incr(final String key) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().incr(key);
	    }
	}.run(key);
    }

    @Override
    public Long append(final String key, final String value) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().append(key, value);
	    }
	}.run(key);
    }

    @Override
    public String substr(final String key, final int start, final int end) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection()
			.substr(key, start, end);
	    }
	}.run(key);
    }

    @Override
    public Long hset(final String key, final String field, final String value) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection()
			.hset(key, field, value);
	    }
	}.run(key);
    }

    @Override
    public String hget(final String key, final String field) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().hget(key, field);
	    }
	}.run(key);
    }

    @Override
    public Long hsetnx(final String key, final String field, final String value) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().hsetnx(key, field,
			value);
	    }
	}.run(key);
    }

    @Override
    public String hmset(final String key, final Map<String, String> hash) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().hmset(key, hash);
	    }
	}.run(key);
    }

    @Override
    public List<String> hmget(final String key, final String... fields) {
	return new JedisClusterCommand<List<String>>(connectionHandler,
		timeout, maxRedirections) {
	    @Override
	    public List<String> execute() {
		return connectionHandler.getConnection().hmget(key, fields);
	    }
	}.run(key);
    }

    @Override
    public Long hincrBy(final String key, final String field, final long value) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().hincrBy(key, field,
			value);
	    }
	}.run(key);
    }

    @Override
    public Boolean hexists(final String key, final String field) {
	return new JedisClusterCommand<Boolean>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Boolean execute() {
		return connectionHandler.getConnection().hexists(key, field);
	    }
	}.run(key);
    }

    @Override
    public Long hdel(final String key, final String... field) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().hdel(key, field);
	    }
	}.run(key);
    }

    @Override
    public Long hlen(final String key) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().hdel(key);
	    }
	}.run(key);
    }

    @Override
    public Set<String> hkeys(final String key) {
	return new JedisClusterCommand<Set<String>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<String> execute() {
		return connectionHandler.getConnection().hkeys(key);
	    }
	}.run(key);
    }

    @Override
    public List<String> hvals(final String key) {
	return new JedisClusterCommand<List<String>>(connectionHandler,
		timeout, maxRedirections) {
	    @Override
	    public List<String> execute() {
		return connectionHandler.getConnection().hvals(key);
	    }
	}.run(key);
    }

    @Override
    public Map<String, String> hgetAll(final String key) {
	return new JedisClusterCommand<Map<String, String>>(connectionHandler,
		timeout, maxRedirections) {
	    @Override
	    public Map<String, String> execute() {
		return connectionHandler.getConnection().hgetAll(key);
	    }
	}.run(key);
    }

    @Override
    public Long rpush(final String key, final String... string) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().rpush(key, string);
	    }
	}.run(key);
    }

    @Override
    public Long lpush(final String key, final String... string) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().lpush(key, string);
	    }
	}.run(key);
    }

    @Override
    public Long llen(final String key) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().llen(key);
	    }
	}.run(key);
    }

    @Override
    public List<String> lrange(final String key, final long start,
	    final long end) {
	return new JedisClusterCommand<List<String>>(connectionHandler,
		timeout, maxRedirections) {
	    @Override
	    public List<String> execute() {
		return connectionHandler.getConnection()
			.lrange(key, start, end);
	    }
	}.run(key);
    }

    @Override
    public String ltrim(final String key, final long start, final long end) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().ltrim(key, start, end);
	    }
	}.run(key);
    }

    @Override
    public String lindex(final String key, final long index) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().lindex(key, index);
	    }
	}.run(key);
    }

    @Override
    public String lset(final String key, final long index, final String value) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection()
			.lset(key, index, value);
	    }
	}.run(key);
    }

    @Override
    public Long lrem(final String key, final long count, final String value) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection()
			.lrem(key, count, value);
	    }
	}.run(key);
    }

    @Override
    public String lpop(final String key) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().lpop(key);
	    }
	}.run(key);
    }

    @Override
    public String rpop(final String key) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().rpop(key);
	    }
	}.run(key);
    }

    @Override
    public Long sadd(final String key, final String... member) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().sadd(key, member);
	    }
	}.run(key);
    }

    @Override
    public Set<String> smembers(final String key) {
	return new JedisClusterCommand<Set<String>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<String> execute() {
		return connectionHandler.getConnection().smembers(key);
	    }
	}.run(key);
    }

    @Override
    public Long srem(final String key, final String... member) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().srem(key, member);
	    }
	}.run(key);
    }

    @Override
    public String spop(final String key) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().spop(key);
	    }
	}.run(key);
    }

    @Override
    public Long scard(final String key) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().scard(key);
	    }
	}.run(key);
    }

    @Override
    public Boolean sismember(final String key, final String member) {
	return new JedisClusterCommand<Boolean>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Boolean execute() {
		return connectionHandler.getConnection().sismember(key, member);
	    }
	}.run(key);
    }

    @Override
    public String srandmember(final String key) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().srandmember(key);
	    }
	}.run(key);
    }

    @Override
    public Long strlen(final String key) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().strlen(key);
	    }
	}.run(key);
    }

    @Override
    public Long zadd(final String key, final double score, final String member) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().zadd(key, score,
			member);
	    }
	}.run(key);
    }

    @Override
    public Long zadd(final String key, final Map<String, Double> scoreMembers) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection()
			.zadd(key, scoreMembers);
	    }
	}.run(key);
    }

    @Override
    public Set<String> zrange(final String key, final long start, final long end) {
	return new JedisClusterCommand<Set<String>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<String> execute() {
		return connectionHandler.getConnection()
			.zrange(key, start, end);
	    }
	}.run(key);
    }

    @Override
    public Long zrem(final String key, final String... member) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().zrem(key, member);
	    }
	}.run(key);
    }

    @Override
    public Double zincrby(final String key, final double score,
	    final String member) {
	return new JedisClusterCommand<Double>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Double execute() {
		return connectionHandler.getConnection().zincrby(key, score,
			member);
	    }
	}.run(key);
    }

    @Override
    public Long zrank(final String key, final String member) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().zrank(key, member);
	    }
	}.run(key);
    }

    @Override
    public Long zrevrank(final String key, final String member) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().zrevrank(key, member);
	    }
	}.run(key);
    }

    @Override
    public Set<String> zrevrange(final String key, final long start,
	    final long end) {
	return new JedisClusterCommand<Set<String>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<String> execute() {
		return connectionHandler.getConnection().zrevrange(key, start,
			end);
	    }
	}.run(key);
    }

    @Override
    public Set<Tuple> zrangeWithScores(final String key, final long start,
	    final long end) {
	return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<Tuple> execute() {
		return connectionHandler.getConnection().zrangeWithScores(key,
			start, end);
	    }
	}.run(key);
    }

    @Override
    public Set<Tuple> zrevrangeWithScores(final String key, final long start,
	    final long end) {
	return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<Tuple> execute() {
		return connectionHandler.getConnection().zrevrangeWithScores(
			key, start, end);
	    }
	}.run(key);
    }

    @Override
    public Long zcard(final String key) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().zcard(key);
	    }
	}.run(key);
    }

    @Override
    public Double zscore(final String key, final String member) {
	return new JedisClusterCommand<Double>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Double execute() {
		return connectionHandler.getConnection().zscore(key, member);
	    }
	}.run(key);
    }

    @Override
    public List<String> sort(final String key) {
	return new JedisClusterCommand<List<String>>(connectionHandler,
		timeout, maxRedirections) {
	    @Override
	    public List<String> execute() {
		return connectionHandler.getConnection().sort(key);
	    }
	}.run(key);
    }

    @Override
    public List<String> sort(final String key,
	    final SortingParams sortingParameters) {
	return new JedisClusterCommand<List<String>>(connectionHandler,
		timeout, maxRedirections) {
	    @Override
	    public List<String> execute() {
		return connectionHandler.getConnection().sort(key,
			sortingParameters);
	    }
	}.run(key);
    }

    @Override
    public Long zcount(final String key, final double min, final double max) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().zcount(key, min, max);
	    }
	}.run(key);
    }

    @Override
    public Long zcount(final String key, final String min, final String max) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().zcount(key, min, max);
	    }
	}.run(key);
    }

    @Override
    public Set<String> zrangeByScore(final String key, final double min,
	    final double max) {
	return new JedisClusterCommand<Set<String>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<String> execute() {
		return connectionHandler.getConnection().zrangeByScore(key,
			min, max);
	    }
	}.run(key);
    }

    @Override
    public Set<String> zrangeByScore(final String key, final String min,
	    final String max) {
	return new JedisClusterCommand<Set<String>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<String> execute() {
		return connectionHandler.getConnection().zrangeByScore(key,
			min, max);
	    }
	}.run(key);
    }

    @Override
    public Set<String> zrevrangeByScore(final String key, final double max,
	    final double min) {
	return new JedisClusterCommand<Set<String>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<String> execute() {
		return connectionHandler.getConnection().zrevrangeByScore(key,
			min, max);
	    }
	}.run(key);
    }

    @Override
    public Set<String> zrangeByScore(final String key, final double min,
	    final double max, final int offset, final int count) {
	return new JedisClusterCommand<Set<String>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<String> execute() {
		return connectionHandler.getConnection().zrangeByScore(key,
			min, max, offset, count);
	    }
	}.run(key);
    }

    @Override
    public Set<String> zrevrangeByScore(final String key, final String max,
	    final String min) {
	return new JedisClusterCommand<Set<String>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<String> execute() {
		return connectionHandler.getConnection().zrevrangeByScore(key,
			min, max);
	    }
	}.run(key);
    }

    @Override
    public Set<String> zrangeByScore(final String key, final String min,
	    final String max, final int offset, final int count) {
	return new JedisClusterCommand<Set<String>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<String> execute() {
		return connectionHandler.getConnection().zrangeByScore(key,
			min, max, offset, count);
	    }
	}.run(key);
    }

    @Override
    public Set<String> zrevrangeByScore(final String key, final double max,
	    final double min, final int offset, final int count) {
	return new JedisClusterCommand<Set<String>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<String> execute() {
		return connectionHandler.getConnection().zrevrangeByScore(key,
			min, max, offset, count);
	    }
	}.run(key);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final String key,
	    final double min, final double max) {
	return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<Tuple> execute() {
		return connectionHandler.getConnection()
			.zrangeByScoreWithScores(key, min, max);
	    }
	}.run(key);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final String key,
	    final double max, final double min) {
	return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<Tuple> execute() {
		return connectionHandler.getConnection()
			.zrevrangeByScoreWithScores(key, min, max);
	    }
	}.run(key);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final String key,
	    final double min, final double max, final int offset,
	    final int count) {
	return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<Tuple> execute() {
		return connectionHandler.getConnection()
			.zrangeByScoreWithScores(key, min, max, offset, count);
	    }
	}.run(key);
    }

    @Override
    public Set<String> zrevrangeByScore(final String key, final String max,
	    final String min, final int offset, final int count) {
	return new JedisClusterCommand<Set<String>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<String> execute() {
		return connectionHandler.getConnection().zrevrangeByScore(key,
			min, max, offset, count);
	    }
	}.run(key);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final String key,
	    final String min, final String max) {
	return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<Tuple> execute() {
		return connectionHandler.getConnection()
			.zrangeByScoreWithScores(key, min, max);
	    }
	}.run(key);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final String key,
	    final String max, final String min) {
	return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<Tuple> execute() {
		return connectionHandler.getConnection()
			.zrevrangeByScoreWithScores(key, min, max);
	    }
	}.run(key);
    }

    @Override
    public Set<Tuple> zrangeByScoreWithScores(final String key,
	    final String min, final String max, final int offset,
	    final int count) {
	return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<Tuple> execute() {
		return connectionHandler.getConnection()
			.zrangeByScoreWithScores(key, min, max, offset, count);
	    }
	}.run(key);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final String key,
	    final double max, final double min, final int offset,
	    final int count) {
	return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<Tuple> execute() {
		return connectionHandler.getConnection()
			.zrevrangeByScoreWithScores(key, max, min, offset,
				count);
	    }
	}.run(key);
    }

    @Override
    public Set<Tuple> zrevrangeByScoreWithScores(final String key,
	    final String max, final String min, final int offset,
	    final int count) {
	return new JedisClusterCommand<Set<Tuple>>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Set<Tuple> execute() {
		return connectionHandler.getConnection()
			.zrevrangeByScoreWithScores(key, max, min, offset,
				count);
	    }
	}.run(key);
    }

    @Override
    public Long zremrangeByRank(final String key, final long start,
	    final long end) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().zremrangeByRank(key,
			start, end);
	    }
	}.run(key);
    }

    @Override
    public Long zremrangeByScore(final String key, final double start,
	    final double end) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().zremrangeByScore(key,
			start, end);
	    }
	}.run(key);
    }

    @Override
    public Long zremrangeByScore(final String key, final String start,
	    final String end) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().zremrangeByScore(key,
			start, end);
	    }
	}.run(key);
    }

    @Override
    public Long linsert(final String key, final LIST_POSITION where,
	    final String pivot, final String value) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().linsert(key, where,
			pivot, value);
	    }
	}.run(key);
    }

    @Override
    public Long lpushx(final String key, final String... string) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().lpushx(key, string);
	    }
	}.run(key);
    }

    @Override
    public Long rpushx(final String key, final String... string) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().rpushx(key, string);
	    }
	}.run(key);
    }

    @Override
    public List<String> blpop(final String arg) {
	return new JedisClusterCommand<List<String>>(connectionHandler,
		timeout, maxRedirections) {
	    @Override
	    public List<String> execute() {
		return connectionHandler.getConnection().blpop(arg);
	    }
	}.run(null);
    }

    @Override
    public List<String> brpop(final String arg) {
	return new JedisClusterCommand<List<String>>(connectionHandler,
		timeout, maxRedirections) {
	    @Override
	    public List<String> execute() {
		return connectionHandler.getConnection().brpop(arg);
	    }
	}.run(null);
    }

    @Override
    public Long del(final String key) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().del(key);
	    }
	}.run(null);
    }

    @Override
    public String echo(final String string) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().echo(string);
	    }
	}.run(null);
    }

    @Override
    public Long move(final String key, final int dbIndex) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().move(key, dbIndex);
	    }
	}.run(key);
    }

    @Override
    public Long bitcount(final String key) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().bitcount(key);
	    }
	}.run(key);
    }

    @Override
    public Long bitcount(final String key, final long start, final long end) {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().bitcount(key, start,
			end);
	    }
	}.run(key);
    }

    @Override
    public String ping() {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().ping();
	    }
	}.run(null);
    }

    @Override
    public String quit() {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().quit();
	    }
	}.run(null);
    }

    @Override
    public String flushDB() {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().flushDB();
	    }
	}.run(null);
    }

    @Override
    public Long dbSize() {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().dbSize();
	    }
	}.run(null);
    }

    @Override
    public String select(final int index) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().select(index);
	    }
	}.run(null);
    }

    @Override
    public String flushAll() {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().flushAll();
	    }
	}.run(null);
    }

    @Override
    public String auth(final String password) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().auth(password);
	    }
	}.run(null);
    }

    @Override
    public String save() {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().save();
	    }
	}.run(null);
    }

    @Override
    public String bgsave() {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().bgsave();
	    }
	}.run(null);
    }

    @Override
    public String bgrewriteaof() {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().bgrewriteaof();
	    }
	}.run(null);
    }

    @Override
    public Long lastsave() {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().lastsave();
	    }
	}.run(null);
    }

    @Override
    public String shutdown() {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().shutdown();
	    }
	}.run(null);
    }

    @Override
    public String info() {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().info();
	    }
	}.run(null);
    }

    @Override
    public String info(final String section) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().info(section);
	    }
	}.run(null);
    }

    @Override
    public String slaveof(final String host, final int port) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().slaveof(host, port);
	    }
	}.run(null);
    }

    @Override
    public String slaveofNoOne() {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().slaveofNoOne();
	    }
	}.run(null);
    }

    @Override
    public Long getDB() {
	return new JedisClusterCommand<Long>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public Long execute() {
		return connectionHandler.getConnection().getDB();
	    }
	}.run(null);
    }

    @Override
    public String debug(final DebugParams params) {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().debug(params);
	    }
	}.run(null);
    }

    @Override
    public String configResetStat() {
	return new JedisClusterCommand<String>(connectionHandler, timeout,
		maxRedirections) {
	    @Override
	    public String execute() {
		return connectionHandler.getConnection().configResetStat();
	    }
	}.run(null);
    }

    public Map<String, JedisPool> getClusterNodes() {
	return connectionHandler.getNodes();
    }

    @Override
    public Long waitReplicas(int replicas, long timeout) {
	// TODO Auto-generated method stub
	return null;
    }

    @Deprecated
    /**
     * This method is deprecated due to bug (scan cursor should be unsigned long)
     * And will be removed on next major release
     * @see https://github.com/xetorthio/jedis/issues/531 
     */
    @Override
    public ScanResult<Entry<String, String>> hscan(final String key,
	    final int cursor) {
	return new JedisClusterCommand<ScanResult<Entry<String, String>>>(
		connectionHandler, timeout, maxRedirections) {
	    @Override
	    public ScanResult<Entry<String, String>> execute() {
		return connectionHandler.getConnection().hscan(key, cursor);
	    }
	}.run(null);
    }

    @Deprecated
    /**
     * This method is deprecated due to bug (scan cursor should be unsigned long)
     * And will be removed on next major release
     * @see https://github.com/xetorthio/jedis/issues/531 
     */
    @Override
    public ScanResult<String> sscan(final String key, final int cursor) {
	return new JedisClusterCommand<ScanResult<String>>(connectionHandler,
		timeout, maxRedirections) {
	    @Override
	    public ScanResult<String> execute() {
		return connectionHandler.getConnection().sscan(key, cursor);
	    }
	}.run(null);
    }

    @Deprecated
    /**
     * This method is deprecated due to bug (scan cursor should be unsigned long)
     * And will be removed on next major release
     * @see https://github.com/xetorthio/jedis/issues/531 
     */
    @Override
    public ScanResult<Tuple> zscan(final String key, final int cursor) {
	return new JedisClusterCommand<ScanResult<Tuple>>(connectionHandler,
		timeout, maxRedirections) {
	    @Override
	    public ScanResult<Tuple> execute() {
		return connectionHandler.getConnection().zscan(key, cursor);
	    }
	}.run(null);
    }
    
    @Override
    public ScanResult<Entry<String, String>> hscan(final String key,
	    final String cursor) {
	return new JedisClusterCommand<ScanResult<Entry<String, String>>>(
		connectionHandler, timeout, maxRedirections) {
	    @Override
	    public ScanResult<Entry<String, String>> execute() {
		return connectionHandler.getConnection().hscan(key, cursor);
	    }
	}.run(null);
    }
    
    @Override
    public ScanResult<String> sscan(final String key, final String cursor) {
	return new JedisClusterCommand<ScanResult<String>>(connectionHandler,
		timeout, maxRedirections) {
	    @Override
	    public ScanResult<String> execute() {
		return connectionHandler.getConnection().sscan(key, cursor);
	    }
	}.run(null);
    }
    
    @Override
    public ScanResult<Tuple> zscan(final String key, final String cursor) {
	return new JedisClusterCommand<ScanResult<Tuple>>(connectionHandler,
		timeout, maxRedirections) {
	    @Override
	    public ScanResult<Tuple> execute() {
		return connectionHandler.getConnection().zscan(key, cursor);
	    }
	}.run(null);
    }
}
