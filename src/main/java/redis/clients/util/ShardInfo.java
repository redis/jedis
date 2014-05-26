package redis.clients.util;

/**
 * Decided to deprecate ShardedJedis because it's somewhat duplicate to Redis Cluster.
 * Will be removed at Jedis 3.0.0
 * @see https://groups.google.com/d/msg/jedis_redis/avphfQld81Y/X_uouHp_lCIJ
 */
@Deprecated
public abstract class ShardInfo<T> {
    private int weight;

    public ShardInfo() {
    }

    public ShardInfo(int weight) {
	this.weight = weight;
    }

    public int getWeight() {
	return this.weight;
    }

    protected abstract T createResource();

    public abstract String getName();
}
