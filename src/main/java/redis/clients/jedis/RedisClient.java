package redis.clients.jedis;

import java.util.List;
import java.util.Map;
import java.util.Set;

import redis.clients.jedis.BinaryClient.LIST_POSITION;

/**
 * RedisClient Class Using the ConsistenJedisPool to make the redis-servers easy
 * distributing and clustering, RedisCallback is to ease the connection resource
 * management
 * 
 * @author briangxchen@gmail.com
 * 
 * 
 */

public class RedisClient implements JedisCommands {

	private ConsistentJedisPool consistentPool;

	public RedisClient(ConsistentJedisPool consistentPool) {
		this.consistentPool = consistentPool;
	}

	public String setex(final String key, final int seconds, final String value) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.setex(key, seconds, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String get(final String key) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.get(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String set(final String key, final String value) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.set(key, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Boolean exists(final String key) {
		return consistentPool.redisCall(new RedisCallback<Boolean>() {
			public Boolean doInRedis(Jedis jedis) {
				return jedis.exists(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long expire(final String key, final int seconds) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.expire(key, seconds);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long expireAt(final String key, final long unixTime) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.expireAt(key, unixTime);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long hset(final String key, final String field, final String value) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.hset(key, field, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String hget(final String key, final String field) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.hget(key, field);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long hsetnx(final String key, final String field, final String value) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.hsetnx(key, field, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String hmset(final String key, final Map<String, String> hash) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.hmset(key, hash);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public List<String> hmget(final String key, final String... fields) {
		return consistentPool.redisCall(new RedisCallback<List<String>>() {
			public List<String> doInRedis(Jedis jedis) {
				return jedis.hmget(key, fields);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String type(final String key) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.type(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long ttl(final String key) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.ttl(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Boolean setbit(final String key, final long offset,
			final boolean value) {
		return consistentPool.redisCall(new RedisCallback<Boolean>() {
			public Boolean doInRedis(Jedis jedis) {
				return jedis.setbit(key, offset, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Boolean getbit(final String key, final long offset) {
		return consistentPool.redisCall(new RedisCallback<Boolean>() {
			public Boolean doInRedis(Jedis jedis) {
				return jedis.getbit(key, offset);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long setrange(final String key, final long offset, final String value) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.setrange(key, offset, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String getrange(final String key, final long startOffset,
			final long endOffset) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.getrange(key, startOffset, endOffset);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String getSet(final String key, final String value) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.getSet(key, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long setnx(final String key, final String value) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.setnx(key, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long decrBy(final String key, final long integer) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.decrBy(key, integer);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long decr(final String key) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.decr(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long incrBy(final String key, final long integer) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.incrBy(key, integer);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long incr(final String key) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.incr(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long append(final String key, final String value) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.append(key, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String substr(final String key, final int start, final int end) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.substr(key, start, end);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long hincrBy(final String key, final String field, final long value) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.hincrBy(key, field, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Boolean hexists(final String key, final String field) {
		return consistentPool.redisCall(new RedisCallback<Boolean>() {
			public Boolean doInRedis(Jedis jedis) {
				return jedis.hexists(key, field);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long hdel(final String key, final String... fields) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.hdel(key, fields);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long hlen(final String key) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.hlen(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<String> hkeys(final String key) {
		return consistentPool.redisCall(new RedisCallback<Set<String>>() {
			public Set<String> doInRedis(Jedis jedis) {
				return jedis.hkeys(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public List<String> hvals(final String key) {
		return consistentPool.redisCall(new RedisCallback<List<String>>() {
			public List<String> doInRedis(Jedis jedis) {
				return jedis.hvals(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Map<String, String> hgetAll(final String key) {
		return consistentPool.redisCall(new RedisCallback<Map<String, String>>() {
			public Map<String, String> doInRedis(Jedis jedis) {
				return jedis.hgetAll(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long rpush(final String key, final String... strings) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.rpush(key, strings);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long lpush(final String key, final String... strings) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.lpush(key, strings);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long llen(final String key) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.llen(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public List<String> lrange(final String key, final long start,
			final long end) {
		return consistentPool.redisCall(new RedisCallback<List<String>>() {
			public List<String> doInRedis(Jedis jedis) {
				return jedis.lrange(key, start, end);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String ltrim(final String key, final long start, final long end) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.ltrim(key, start, end);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String lindex(final String key, final long index) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.lindex(key, index);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String lset(final String key, final long index, final String value) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.lset(key, index, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long lrem(final String key, final long count, final String value) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.lrem(key, count, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String lpop(final String key) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.lpop(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String rpop(final String key) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.rpop(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long sadd(final String key, final String... members) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.sadd(key, members);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<String> smembers(final String key) {
		return consistentPool.redisCall(new RedisCallback<Set<String>>() {
			public Set<String> doInRedis(Jedis jedis) {
				return jedis.smembers(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long srem(final String key, final String... members) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.srem(key, members);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String spop(final String key) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.spop(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long scard(final String key) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.scard(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Boolean sismember(final String key, final String member) {
		return consistentPool.redisCall(new RedisCallback<Boolean>() {
			public Boolean doInRedis(Jedis jedis) {
				return jedis.sismember(key, member);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public String srandmember(final String key) {
		return consistentPool.redisCall(new RedisCallback<String>() {
			public String doInRedis(Jedis jedis) {
				return jedis.srandmember(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long zadd(final String key, final double score, final String member) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.zadd(key, score, member);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long zadd(final String key, final Map<Double, String> scoreMembers) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.zadd(key, scoreMembers);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<String> zrange(final String key, final long start, final long end) {
		return consistentPool.redisCall(new RedisCallback<Set<String>>() {
			public Set<String> doInRedis(Jedis jedis) {
				return jedis.zrange(key, start, end);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long zrem(final String key, final String... members) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.zrem(key, members);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Double zincrby(final String key, final double score,
			final String member) {
		return consistentPool.redisCall(new RedisCallback<Double>() {
			public Double doInRedis(Jedis jedis) {
				return jedis.zincrby(key, score, member);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long zrank(final String key, final String member) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.zrank(key, member);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long zrevrank(final String key, final String member) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.zrevrank(key, member);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<String> zrevrange(final String key, final long start,
			final long end) {
		return consistentPool.redisCall(new RedisCallback<Set<String>>() {
			public Set<String> doInRedis(final Jedis jedis) {
				return jedis.zrevrange(key, start, end);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<Tuple> zrangeWithScores(final String key, final long start,
			final long end) {
		return consistentPool.redisCall(new RedisCallback<Set<Tuple>>() {
			public Set<Tuple> doInRedis(Jedis jedis) {
				return jedis.zrangeWithScores(key, start, end);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<Tuple> zrevrangeWithScores(final String key, final long start,
			final long end) {
		return consistentPool.redisCall(new RedisCallback<Set<Tuple>>() {
			public Set<Tuple> doInRedis(Jedis jedis) {
				return jedis.zrevrangeWithScores(key, start, end);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long zcard(final String key) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.zcard(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Double zscore(final String key, final String member) {
		return consistentPool.redisCall(new RedisCallback<Double>() {
			public Double doInRedis(Jedis jedis) {
				return jedis.zscore(key, member);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public List<String> sort(final String key) {
		return consistentPool.redisCall(new RedisCallback<List<String>>() {
			public List<String> doInRedis(Jedis jedis) {
				return jedis.sort(key);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public List<String> sort(final String key,
			final SortingParams sortingParameters) {
		return consistentPool.redisCall(new RedisCallback<List<String>>() {
			public List<String> doInRedis(Jedis jedis) {
				return jedis.sort(key, sortingParameters);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long zcount(final String key, final double min, final double max) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.zcount(key, min, max);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long zcount(final String key, final String min, final String max) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.zcount(key, min, max);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<String> zrangeByScore(final String key, final double min,
			final double max) {
		return consistentPool.redisCall(new RedisCallback<Set<String>>() {
			public Set<String> doInRedis(Jedis jedis) {
				return jedis.zrangeByScore(key, min, max);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<String> zrangeByScore(final String key, final String min,
			final String max) {
		return consistentPool.redisCall(new RedisCallback<Set<String>>() {
			public Set<String> doInRedis(Jedis jedis) {
				return jedis.zrangeByScore(key, min, max);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<String> zrevrangeByScore(final String key, final double max,
			final double min) {
		return consistentPool.redisCall(new RedisCallback<Set<String>>() {
			public Set<String> doInRedis(Jedis jedis) {
				return jedis.zrevrangeByScore(key, max, min);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<String> zrangeByScore(final String key, final double min,
			final double max, final int offset, final int count) {
		return consistentPool.redisCall(new RedisCallback<Set<String>>() {
			public Set<String> doInRedis(Jedis jedis) {
				return jedis.zrangeByScore(key, min, max, offset, count);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<String> zrevrangeByScore(final String key, final String max,
			final String min) {
		return consistentPool.redisCall(new RedisCallback<Set<String>>() {
			public Set<String> doInRedis(Jedis jedis) {
				return jedis.zrevrangeByScore(key, max, min);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<String> zrangeByScore(final String key, final String min,
			final String max, final int offset, final int count) {
		return consistentPool.redisCall(new RedisCallback<Set<String>>() {
			public Set<String> doInRedis(Jedis jedis) {
				return jedis.zrangeByScore(key, min, max, offset, count);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<String> zrevrangeByScore(final String key, final double max,
			final double min, final int offset, final int count) {
		return consistentPool.redisCall(new RedisCallback<Set<String>>() {
			public Set<String> doInRedis(Jedis jedis) {
				return jedis.zrevrangeByScore(key, max, min, offset, count);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<Tuple> zrangeByScoreWithScores(final String key,
			final double min, final double max) {
		return consistentPool.redisCall(new RedisCallback<Set<Tuple>>() {
			public Set<Tuple> doInRedis(Jedis jedis) {
				return jedis.zrangeByScoreWithScores(key, min, max);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<Tuple> zrevrangeByScoreWithScores(final String key,
			final double max, final double min) {
		return consistentPool.redisCall(new RedisCallback<Set<Tuple>>() {
			public Set<Tuple> doInRedis(Jedis jedis) {
				return jedis.zrevrangeByScoreWithScores(key, max, min);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<Tuple> zrangeByScoreWithScores(final String key,
			final double min, final double max, final int offset,
			final int count) {
		return consistentPool.redisCall(new RedisCallback<Set<Tuple>>() {
			public Set<Tuple> doInRedis(Jedis jedis) {
				return jedis.zrangeByScoreWithScores(key, min, max, offset,
						count);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<String> zrevrangeByScore(final String key, final String max,
			final String min, final int offset, final int count) {
		return consistentPool.redisCall(new RedisCallback<Set<String>>() {
			public Set<String> doInRedis(Jedis jedis) {
				return jedis.zrevrangeByScore(key, max, min, offset, count);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<Tuple> zrangeByScoreWithScores(final String key,
			final String min, final String max) {
		return consistentPool.redisCall(new RedisCallback<Set<Tuple>>() {
			public Set<Tuple> doInRedis(Jedis jedis) {
				return jedis.zrangeByScoreWithScores(key, min, max);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<Tuple> zrevrangeByScoreWithScores(final String key,
			final String max, final String min) {
		return consistentPool.redisCall(new RedisCallback<Set<Tuple>>() {
			public Set<Tuple> doInRedis(Jedis jedis) {
				return jedis.zrevrangeByScoreWithScores(key, max, min);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<Tuple> zrangeByScoreWithScores(final String key,
			final String min, final String max, final int offset,
			final int count) {
		return consistentPool.redisCall(new RedisCallback<Set<Tuple>>() {
			public Set<Tuple> doInRedis(Jedis jedis) {
				return jedis.zrangeByScoreWithScores(key, min, max, offset,
						count);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<Tuple> zrevrangeByScoreWithScores(final String key,
			final double max, final double min, final int offset,
			final int count) {
		return consistentPool.redisCall(new RedisCallback<Set<Tuple>>() {
			public Set<Tuple> doInRedis(Jedis jedis) {
				return jedis.zrevrangeByScoreWithScores(key, max, min, offset,
						count);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Set<Tuple> zrevrangeByScoreWithScores(final String key,
			final String max, final String min, final int offset,
			final int count) {
		return consistentPool.redisCall(new RedisCallback<Set<Tuple>>() {
			public Set<Tuple> doInRedis(Jedis jedis) {
				return jedis.zrevrangeByScoreWithScores(key, max, min, offset,
						count);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long zremrangeByRank(final String key, final long start,
			final long end) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.zremrangeByRank(key, start, end);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long zremrangeByScore(final String key, final double start,
			final double end) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.zremrangeByScore(key, start, end);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long zremrangeByScore(final String key, final String start,
			final String end) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.zremrangeByScore(key, start, end);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long linsert(final String key, final LIST_POSITION where,
			final String pivot, final String value) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.linsert(key, where, pivot, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long lpushx(final String key, final String string) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.lpushx(key, string);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Long rpushx(final String key, final String string) {
		return consistentPool.redisCall(new RedisCallback<Long>() {
			public Long doInRedis(Jedis jedis) {
				return jedis.rpushx(key, string);
			}

			public String getKey() {
				return key;
			}
		});
	}
}
