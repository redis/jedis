package redis.clients.jedis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

public class JedisCluster implements JedisCommands, BasicCommands {
	
	public static final short HASHSLOTS = 16384;
	private static final int DEFAULT_TIMEOUT = 1;
	
	
	private JedisClusterConnectionHandler connectionHandler;

	public JedisCluster(Set<HostAndPort> nodes, int timeout) {
		connectionHandler = new JedisSlotBasedConnectionHandler(nodes);
		
	}

	public JedisCluster(Set<HostAndPort> nodes) {
		this(nodes, DEFAULT_TIMEOUT);
	}
	

	@Override
	public String set(final String key, final String value) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).set(key, value);
			}
		}.run();
	}

	@Override
	public String get(final String key) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).get(key);
			}
		}.run();
	}

	@Override
	public Boolean exists(final String key) {
		return new JedisClusterCommand<Boolean>(connectionHandler) {
			@Override
			public Boolean execute() {
				return connectionHandler.getConnection(key).exists(key);
			}
		}.run();
	}

	@Override
	public Long persist(final String key) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).persist(key);
			}
		}.run();
	}

	@Override
	public String type(final String key) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).type(key);
			}
		}.run();
	}

	@Override
	public Long expire(final String key, final int seconds) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).expire(key, seconds);
			}
		}.run();
	}

	@Override
	public Long expireAt(final String key, final long unixTime) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).expireAt(key, unixTime);
			}
		}.run();
	}

	@Override
	public Long ttl(final String key) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).ttl(key);
			}
		}.run();
	}

	@Override
	public Boolean setbit(final String key, final long offset, final boolean value) {
		return new JedisClusterCommand<Boolean>(connectionHandler) {
			@Override
			public Boolean execute() {
				return connectionHandler.getConnection(key).setbit(key, offset, value);
			}
		}.run();
	}

	@Override
	public Boolean setbit(final String key, final long offset, final String value) {
		return new JedisClusterCommand<Boolean>(connectionHandler) {
			@Override
			public Boolean execute() {
				return connectionHandler.getConnection(key).setbit(key, offset, value);
			}
		}.run();
	}

	@Override
	public Boolean getbit(final String key, final long offset) {
		return new JedisClusterCommand<Boolean>(connectionHandler) {
			@Override
			public Boolean execute() {
				return connectionHandler.getConnection(key).getbit(key, offset);
			}
		}.run();
	}

	@Override
	public Long setrange(final String key, final long offset, final String value) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).setrange(key, offset, value);
			}
		}.run();
	}

	@Override
	public String getrange(final String key, final long startOffset, final long endOffset) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).getrange(key, startOffset, endOffset);
			}
		}.run();
	}

	@Override
	public String getSet(final String key, final String value) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).getSet(key, value);
			}
		}.run();
	}

	@Override
	public Long setnx(final String key, final String value) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).setnx(key, value);
			}
		}.run();
	}

	@Override
	public String setex(final String key, final int seconds, final String value) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).setex(key, seconds, value);
			}
		}.run();
	}

	@Override
	public Long decrBy(final String key, final long integer) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).decrBy(key, integer);
			}
		}.run();
	}

	@Override
	public Long decr(final String key) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).decr(key);
			}
		}.run();
	}

	@Override
	public Long incrBy(final String key, final long integer) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).incrBy(key, integer);
			}
		}.run();
	}

	@Override
	public Long incr(final String key) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).incr(key);
			}
		}.run();
	}

	@Override
	public Long append(final String key, final String value) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).append(key, value);
			}
		}.run();
	}

	@Override
	public String substr(final String key, final int start, final int end) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).substr(key, start, end);
			}
		}.run();
	}

	@Override
	public Long hset(final String key, final String field, final String value) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).hset(key, field, value);
			}
		}.run();
	}

	@Override
	public String hget(final String key, final String field) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).hget(key, field);
			}
		}.run();
	}

	@Override
	public Long hsetnx(final String key, final String field, final String value) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).hsetnx(key, field, value);
			}
		}.run();
	}

	@Override
	public String hmset(final String key, final Map<String, String> hash) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).hmset(key, hash);
			}
		}.run();
	}

	@Override
	public List<String> hmget(final String key, final String... fields) {
		return new JedisClusterCommand<List<String>>(connectionHandler) {
			@Override
			public List<String> execute() {
				return connectionHandler.getConnection(key).hmget(key, fields);
			}
		}.run();
	}

	@Override
	public Long hincrBy(final String key, final String field, final long value) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).hincrBy(key, field, value);
			}
		}.run();
	}

	@Override
	public Boolean hexists(final String key, final String field) {
		return new JedisClusterCommand<Boolean>(connectionHandler) {
			@Override
			public Boolean execute() {
				return connectionHandler.getConnection(key).hexists(key, field);
			}
		}.run();
	}

	@Override
	public Long hdel(final String key, final String... field) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).hdel(key, field);
			}
		}.run();
	}

	@Override
	public Long hlen(final String key) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).hdel(key);
			}
		}.run();
	}

	@Override
	public Set<String> hkeys(final String key) {
		return new JedisClusterCommand<Set<String>>(connectionHandler) {
			@Override
			public Set<String> execute() {
				return connectionHandler.getConnection(key).hkeys(key);
			}
		}.run();
	}

	@Override
	public List<String> hvals(final String key) {
		return new JedisClusterCommand<List<String>>(connectionHandler) {
			@Override
			public List<String> execute() {
				return connectionHandler.getConnection(key).hvals(key);
			}
		}.run();
	}

	@Override
	public Map<String, String> hgetAll(final String key) {
		return new JedisClusterCommand<Map<String, String>>(connectionHandler) {
			@Override
			public Map<String, String> execute() {
				return connectionHandler.getConnection(key).hgetAll(key);
			}
		}.run();
	}

	@Override
	public Long rpush(final String key, final String... string) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).rpush(key, string);
			}
		}.run();
	}

	@Override
	public Long lpush(final String key, final String... string) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).lpush(key, string);
			}
		}.run();
	}

	@Override
	public Long llen(final String key) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).llen(key);
			}
		}.run();
	}

	@Override
	public List<String> lrange(final String key, final long start, final long end) {
		return new JedisClusterCommand<List<String>>(connectionHandler) {
			@Override
			public List<String> execute() {
				return connectionHandler.getConnection(key).lrange(key, start, end);
			}
		}.run();
	}

	@Override
	public String ltrim(final String key, final long start, final long end) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).ltrim(key, start, end);
			}
		}.run();
	}

	@Override
	public String lindex(final String key, final long index) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).lindex(key, index);
			}
		}.run();
	}

	@Override
	public String lset(final String key, final long index, final String value) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).lset(key, index, value);
			}
		}.run();
	}

	@Override
	public Long lrem(final String key, final long count, final String value) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).lrem(key, count, value);
			}
		}.run();
	}

	@Override
	public String lpop(final String key) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).lpop(key);
			}
		}.run();
	}

	@Override
	public String rpop(final String key) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).rpop(key);
			}
		}.run();
	}

	@Override
	public Long sadd(final String key, final String... member) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).sadd(key, member);
			}
		}.run();
	}

	@Override
	public Set<String> smembers(final String key) {
		return new JedisClusterCommand<Set<String>>(connectionHandler) {
			@Override
			public Set<String> execute() {
				return connectionHandler.getConnection(key).smembers(key);
			}
		}.run();
	}

	@Override
	public Long srem(final String key, final String... member) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).srem(key, member);
			}
		}.run();
	}

	@Override
	public String spop(final String key) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).spop(key);
			}
		}.run();
	}

	@Override
	public Long scard(final String key) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).scard(key);
			}
		}.run();
	}

	@Override
	public Boolean sismember(final String key, final String member) {
		return new JedisClusterCommand<Boolean>(connectionHandler) {
			@Override
			public Boolean execute() {
				return connectionHandler.getConnection(key).sismember(key, member);
			}
		}.run();
	}

	@Override
	public String srandmember(final String key) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(key).srandmember(key);
			}
		}.run();
	}

	@Override
	public Long strlen(final String key) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).strlen(key);
			}
		}.run();
	}

	@Override
	public Long zadd(final String key, final double score, final String member) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).zadd(key, score, member);
			}
		}.run();
	}

	@Override
	public Long zadd(final String key, final Map<Double, String> scoreMembers) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).zadd(key, scoreMembers);
			}
		}.run();
	}

	@Override
	public Set<String> zrange(final String key, final long start, final long end) {
		return new JedisClusterCommand<Set<String>>(connectionHandler) {
			@Override
			public Set<String> execute() {
				return connectionHandler.getConnection(key).zrange(key, start, end);
			}
		}.run();
	}

	@Override
	public Long zrem(final String key, final String... member) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).zrem(key, member);
			}
		}.run();
	}

	@Override
	public Double zincrby(final String key, final double score, final String member) {
		return new JedisClusterCommand<Double>(connectionHandler) {
			@Override
			public Double execute() {
				return connectionHandler.getConnection(key).zincrby(key, score, member);
			}
		}.run();
	}

	@Override
	public Long zrank(final String key, final String member) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).zrank(key, member);
			}
		}.run();
	}

	@Override
	public Long zrevrank(final String key, final String member) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).zrevrank(key, member);
			}
		}.run();
	}

	@Override
	public Set<String> zrevrange(final String key, final long start, final long end) {
		return new JedisClusterCommand<Set<String>>(connectionHandler) {
			@Override
			public Set<String> execute() {
				return connectionHandler.getConnection(key).zrevrange(key, start, end);
			}
		}.run();
	}

	@Override
	public Set<Tuple> zrangeWithScores(final String key, final long start, final long end) {
		return new JedisClusterCommand<Set<Tuple>>(connectionHandler) {
			@Override
			public Set<Tuple> execute() {
				return connectionHandler.getConnection(key).zrangeWithScores(key, start, end);
			}
		}.run();
	}

	@Override
	public Set<Tuple> zrevrangeWithScores(final String key, final long start, final long end) {
		return new JedisClusterCommand<Set<Tuple>>(connectionHandler) {
			@Override
			public Set<Tuple> execute() {
				return connectionHandler.getConnection(key).zrevrangeWithScores(key, start, end);
			}
		}.run();
	}

	@Override
	public Long zcard(final String key) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).zcard(key);
			}
		}.run();
	}

	@Override
	public Double zscore(final String key, final String member) {
		return new JedisClusterCommand<Double>(connectionHandler) {
			@Override
			public Double execute() {
				return connectionHandler.getConnection(key).zscore(key, member);
			}
		}.run();
	}

	@Override
	public List<String> sort(final String key) {
		return new JedisClusterCommand<List<String>>(connectionHandler) {
			@Override
			public List<String> execute() {
				return connectionHandler.getConnection(key).sort(key);
			}
		}.run();
	}

	@Override
	public List<String> sort(final String key, final SortingParams sortingParameters) {
		return new JedisClusterCommand<List<String>>(connectionHandler) {
			@Override
			public List<String> execute() {
				return connectionHandler.getConnection(key).sort(key, sortingParameters);
			}
		}.run();
	}

	@Override
	public Long zcount(final String key, final double min, final double max) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).zcount(key, min, max);
			}
		}.run();
	}

	@Override
	public Long zcount(final String key, final String min, final String max) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).zcount(key, min, max);
			}
		}.run();
	}

	@Override
	public Set<String> zrangeByScore(final String key, final double min, final double max) {
		return new JedisClusterCommand<Set<String>>(connectionHandler) {
			@Override
			public Set<String> execute() {
				return connectionHandler.getConnection(key).zrangeByScore(key, min, max);
			}
		}.run();
	}

	@Override
	public Set<String> zrangeByScore(final String key, final String min, final String max) {
		return new JedisClusterCommand<Set<String>>(connectionHandler) {
			@Override
			public Set<String> execute() {
				return connectionHandler.getConnection(key).zrangeByScore(key, min, max);
			}
		}.run();
	}

	@Override
	public Set<String> zrevrangeByScore(final String key, final double max, final double min) {
		return new JedisClusterCommand<Set<String>>(connectionHandler) {
			@Override
			public Set<String> execute() {
				return connectionHandler.getConnection(key).zrevrangeByScore(key, min, max);
			}
		}.run();
	}

	@Override
	public Set<String> zrangeByScore(final String key, final double min, final double max,
			final int offset, final int count) {
		return new JedisClusterCommand<Set<String>>(connectionHandler) {
			@Override
			public Set<String> execute() {
				return connectionHandler.getConnection(key).zrangeByScore(key, min, max, offset, count);
			}
		}.run();
	}

	@Override
	public Set<String> zrevrangeByScore(final String key, final String max, final String min) {
		return new JedisClusterCommand<Set<String>>(connectionHandler) {
			@Override
			public Set<String> execute() {
				return connectionHandler.getConnection(key).zrevrangeByScore(key, min, max);
			}
		}.run();
	}

	@Override
	public Set<String> zrangeByScore(final String key, final String min, final String max,
			final int offset, final int count) {
		return new JedisClusterCommand<Set<String>>(connectionHandler) {
			@Override
			public Set<String> execute() {
				return connectionHandler.getConnection(key).zrangeByScore(key, min, max, offset, count);
			}
		}.run();
	}

	@Override
	public Set<String> zrevrangeByScore(final String key, final double max, final double min,
			final int offset, final int count) {
		return new JedisClusterCommand<Set<String>>(connectionHandler) {
			@Override
			public Set<String> execute() {
				return connectionHandler.getConnection(key).zrevrangeByScore(key, min, max, offset, count);
			}
		}.run();
	}

	@Override
	public Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max) {
		return new JedisClusterCommand<Set<Tuple>>(connectionHandler) {
			@Override
			public Set<Tuple> execute() {
				return connectionHandler.getConnection(key).zrangeByScoreWithScores(key, min, max);
			}
		}.run();
	}

	@Override
	public Set<Tuple> zrevrangeByScoreWithScores(final String key, final double max, final double min) {
		return new JedisClusterCommand<Set<Tuple>>(connectionHandler) {
			@Override
			public Set<Tuple> execute() {
				return connectionHandler.getConnection(key).zrevrangeByScoreWithScores(key, min, max);
			}
		}.run();
	}

	@Override
	public Set<Tuple> zrangeByScoreWithScores(final String key, final double min, final double max, final int offset, final int count) {
		return new JedisClusterCommand<Set<Tuple>>(connectionHandler) {
			@Override
			public Set<Tuple> execute() {
				return connectionHandler.getConnection(key).zrangeByScoreWithScores(key, min, max, offset, count);
			}
		}.run();
	}

	@Override
	public Set<String> zrevrangeByScore(final String key, final String max, final String min,
			final int offset, final int count) {
		return new JedisClusterCommand<Set<String>>(connectionHandler) {
			@Override
			public Set<String> execute() {
				return connectionHandler.getConnection(key).zrevrangeByScore(key, min, max, offset, count);
			}
		}.run();
	}

	@Override
	public Set<Tuple> zrangeByScoreWithScores(final String key, final String min, final String max) {
		return new JedisClusterCommand<Set<Tuple>>(connectionHandler) {
			@Override
			public Set<Tuple> execute() {
				return connectionHandler.getConnection(key).zrangeByScoreWithScores(key, min, max);
			}
		}.run();
	}

	@Override
	public Set<Tuple> zrevrangeByScoreWithScores(final String key, final String max,
			final String min) {
		return new JedisClusterCommand<Set<Tuple>>(connectionHandler) {
			@Override
			public Set<Tuple> execute() {
				return connectionHandler.getConnection(key).zrevrangeByScoreWithScores(key, min, max);
			}
		}.run();
	}

	@Override
	public Set<Tuple> zrangeByScoreWithScores(final String key, final String min,
			final String max, final int offset, final int count) {
		return new JedisClusterCommand<Set<Tuple>>(connectionHandler) {
			@Override
			public Set<Tuple> execute() {
				return connectionHandler.getConnection(key).zrangeByScoreWithScores(key, min, max, offset, count);
			}
		}.run();
	}

	@Override
	public Set<Tuple> zrevrangeByScoreWithScores(final String key, final double max,
			final double min, final int offset, final int count) {
		return new JedisClusterCommand<Set<Tuple>>(connectionHandler) {
			@Override
			public Set<Tuple> execute() {
				return connectionHandler.getConnection(key).zrevrangeByScoreWithScores(key, max, min, offset, count);
			}
		}.run();
	}

	@Override
	public Set<Tuple> zrevrangeByScoreWithScores(final String key, final String max,
			final String min, final int offset, final int count) {
		return new JedisClusterCommand<Set<Tuple>>(connectionHandler) {
			@Override
			public Set<Tuple> execute() {
				return connectionHandler.getConnection(key).zrevrangeByScoreWithScores(key, max, min, offset, count);
			}
		}.run();
	}

	@Override
	public Long zremrangeByRank(final String key, final long start, final long end) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).zremrangeByRank(key, start, end);
			}
		}.run();
	}

	@Override
	public Long zremrangeByScore(final String key, final double start, final double end) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).zremrangeByScore(key, start, end);
			}
		}.run();
	}

	@Override
	public Long zremrangeByScore(final String key, final String start, final String end) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).zremrangeByScore(key, start, end);
			}
		}.run();
	}

	@Override
	public Long linsert(final String key, final LIST_POSITION where, final String pivot,
			final String value) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).linsert(key, where, pivot, value);
			}
		}.run();
	}

	@Override
	public Long lpushx(final String key, final String... string) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).lpushx(key, string);
			}
		}.run();
	}

	@Override
	public Long rpushx(final String key, final String... string) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).rpushx(key, string);
			}
		}.run();
	}

	@Override
	public List<String> blpop(final String arg) {
		return new JedisClusterCommand<List<String>>(connectionHandler) {
			@Override
			public List<String> execute() {
				return connectionHandler.getConnection(null).blpop(arg);
			}
		}.run();
	}

	@Override
	public List<String> brpop(final String arg) {
		return new JedisClusterCommand<List<String>>(connectionHandler) {
			@Override
			public List<String> execute() {
				return connectionHandler.getConnection(null).brpop(arg);
			}
		}.run();
	}

	@Override
	public Long del(final String key) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).del(key);
			}
		}.run();
	}

	@Override
	public String echo(final String string) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).echo(string);
			}
		}.run();
	}

	@Override
	public Long move(final String key, final int dbIndex) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).move(key, dbIndex);
			}
		}.run();
	}

	@Override
	public Long bitcount(final String key) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).bitcount(key);
			}
		}.run();
	}

	@Override
	public Long bitcount(final String key, final long start, final long end) {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(key).bitcount(key, start, end);
			}
		}.run();
	}

	@Override
	public String ping() {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).ping();
			}
		}.run();
	}

	@Override
	public String quit() {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).quit();
			}
		}.run();
	}

	@Override
	public String flushDB() {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).flushDB();
			}
		}.run();
	}

	@Override
	public Long dbSize() {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(null).dbSize();
			}
		}.run();
	}

	@Override
	public String select(final int index) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).select(index);
			}
		}.run();
	}

	@Override
	public String flushAll() {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).flushAll();
			}
		}.run();
	}

	@Override
	public String auth(final String password) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).auth(password);
			}
		}.run();
	}

	@Override
	public String save() {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).save();
			}
		}.run();
	}

	@Override
	public String bgsave() {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).bgsave();
			}
		}.run();
	}

	@Override
	public String bgrewriteaof() {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).bgrewriteaof();
			}
		}.run();
	}

	@Override
	public Long lastsave() {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(null).lastsave();
			}
		}.run();
	}

	@Override
	public String shutdown() {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).shutdown();
			}
		}.run();
	}

	@Override
	public String info() {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).info();
			}
		}.run();
	}

	@Override
	public String info(final String section) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).info(section);
			}
		}.run();
	}

	@Override
	public String slaveof(final String host, final int port) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).slaveof(host, port);
			}
		}.run();
	}

	@Override
	public String slaveofNoOne() {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).slaveofNoOne();
			}
		}.run();
	}

	@Override
	public Long getDB() {
		return new JedisClusterCommand<Long>(connectionHandler) {
			@Override
			public Long execute() {
				return connectionHandler.getConnection(null).getDB();
			}
		}.run();
	}

	@Override
	public String debug(final DebugParams params) {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).debug(params);
			}
		}.run();
	}

	@Override
	public String configResetStat() {
		return new JedisClusterCommand<String>(connectionHandler) {
			@Override
			public String execute() {
				return connectionHandler.getConnection(null).configResetStat();
			}
		}.run();
	}

	public Map<String, JedisPool> getClusterNodes() {
		return connectionHandler.getNodes();
	}
}
