package redis.clients.jedis.tests;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import redis.clients.jedis.ConsistentJedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.util.Hashing;

public class ConsistentJedisPoolTest extends Assert {

	int maxActive = 20;
	int MaxIdle = 5;
	int MaxWait = 1000;
	int expireTime = 86400;
	List<String> addressArr = Arrays.asList("127.0.0.1:6379", "127.0.0.1:6380");
	int numberOfReplicas = 500;

	@Test
	public void checkConnections() throws Throwable {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxActive(maxActive);
		config.setMaxIdle(MaxIdle);
		config.setMaxWait(MaxWait);
		config.setTestOnBorrow(true);

		ConsistentJedisPool consistentJedisPool = new ConsistentJedisPool(
				addressArr, numberOfReplicas, config, Hashing.MD5);

		RedisClient redisClient = new RedisClient(consistentJedisPool);

		redisClient.set("foo", "bar");
		assertEquals("bar", redisClient.get("foo"));

		consistentJedisPool.destroy();
	}

	@Test
	public void checkMURMUR_HASH() throws Throwable {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxActive(maxActive);
		config.setMaxIdle(MaxIdle);
		config.setMaxWait(MaxWait);
		config.setTestOnBorrow(true);

		ConsistentJedisPool consistentJedisPool = new ConsistentJedisPool(
				addressArr, numberOfReplicas, config, Hashing.MURMUR_HASH);

		RedisClient redisClient = new RedisClient(consistentJedisPool);

		redisClient.set("foo", "bar");
		assertEquals("bar", redisClient.get("foo"));

		consistentJedisPool.destroy();
	}

}