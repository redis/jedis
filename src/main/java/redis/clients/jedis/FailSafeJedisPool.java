package redis.clients.jedis;

import org.apache.commons.pool.impl.GenericObjectPool;

import redis.clients.util.FailSafeJedisCluster;
import redis.clients.util.FailSafeJedisFactory;
import redis.clients.util.Pool;

public class FailSafeJedisPool extends Pool<JedisFacade> {

	public FailSafeJedisPool(final GenericObjectPool.Config poolConfig,
			FailSafeJedisCluster cluster) {
		super(poolConfig, new FailSafeJedisFactory(cluster));
	}
}
