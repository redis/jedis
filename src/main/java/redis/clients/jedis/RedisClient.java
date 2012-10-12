package redis.clients.jedis;

/**
 * RedisClient Class Using the ConsistenJedisPool to make the redis-servers easy
 * distributing and clustering, RedisCallback is to ease the connection resource
 * management
 * 
 * @author briangxchen@gmail.com
 * 
 * 
 */

public class RedisClient {

	private ConsistentJedisPool consistentPool;

	public RedisClient(ConsistentJedisPool consistentPool) {
		this.consistentPool = consistentPool;
	}

	public void setex(final String key, final int seconds,
			final String value) throws Throwable {
		consistentPool.redisCall(new RedisCallback() {
			public Object doInRedis(Jedis jedis) {
				return jedis.setex(key, seconds, value);
			}

			public String getKey() {
				return key;
			}
		});
	}

	public Object get(final String key) throws Throwable {
		return consistentPool.redisCall(new RedisCallback() {
			public Object doInRedis(Jedis jedis) {
				return jedis.get(key);
			}

			public String getKey() {
				return key;
			}
		});

	}

	public void set(final String key, final String value) throws Throwable {
		consistentPool.redisCall(new RedisCallback() {
			public Object doInRedis(Jedis jedis) {
				return jedis.set(key, value);
			}

			public String getKey() {
				return key;
			}
		});

	}

}
