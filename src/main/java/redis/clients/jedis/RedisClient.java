package redis.clients.jedis;

/**
 * Using the ConsistenJedisPool to make the redis-servers easy distribute The
 * RedisCallback is for the connection resource management much more easy and
 * safe
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

	public void redisSetEx(final String key, final int seconds,
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

	public Object redisGet(final String key) throws Throwable {
		return consistentPool.redisCall(new RedisCallback() {
			public Object doInRedis(Jedis jedis) {
				return jedis.get(key);
			}

			public String getKey() {
				return key;
			}
		});

	}

	public void redisSet(final String key, final String value) throws Throwable {
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