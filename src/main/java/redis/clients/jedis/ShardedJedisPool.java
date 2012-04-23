package redis.clients.jedis;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

import redis.clients.util.Hashing;
import redis.clients.util.JedisDynamicShardsProvider;
import redis.clients.util.Pool;

public class ShardedJedisPool extends Pool<ShardedJedis> {
	public ShardedJedisPool (final GenericObjectPool.Config poolConfig, final JedisDynamicShardsProvider shardProvider) {
		this(poolConfig, shardProvider, Hashing.MURMUR_HASH, null);
	}

	public ShardedJedisPool (final GenericObjectPool.Config poolConfig, final JedisDynamicShardsProvider shardProvider, final Pattern keyTagPattern) {
		this(poolConfig, shardProvider, Hashing.MURMUR_HASH, keyTagPattern);
	}

	public ShardedJedisPool (final GenericObjectPool.Config poolConfig, final JedisDynamicShardsProvider shardProvider, final Hashing algo, final Pattern keyTagPattern) {
		super(poolConfig, new ShardedJedisFactory(shardProvider, algo, keyTagPattern));
	}

	public ShardedJedisPool(final GenericObjectPool.Config poolConfig,
            List<JedisShardInfo> shards) {
        this(poolConfig, shards, Hashing.MURMUR_HASH);
    }

    public ShardedJedisPool(final GenericObjectPool.Config poolConfig,
            List<JedisShardInfo> shards, Hashing algo) {
        this(poolConfig, shards, algo, null);
    }

    public ShardedJedisPool(final GenericObjectPool.Config poolConfig,
            List<JedisShardInfo> shards, Pattern keyTagPattern) {
        this(poolConfig, shards, Hashing.MURMUR_HASH, keyTagPattern);
    }

    public ShardedJedisPool(final GenericObjectPool.Config poolConfig,
            List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern) {
        super(poolConfig, new ShardedJedisFactory(shards, algo, keyTagPattern));
    }

    /**
     * PoolableObjectFactory custom impl.
     */
    private static class ShardedJedisFactory extends BasePoolableObjectFactory {
        private List<JedisShardInfo> shards;
        private Hashing algo;
        private Pattern keyTagPattern;
        
        private final JedisDynamicShardsProvider provider;
        private final boolean isDynamic;
        
        public ShardedJedisFactory(final JedisDynamicShardsProvider provider, final Hashing algo, final Pattern keyTagPattern) {
        	this.isDynamic = true;
        	this.algo = algo;
        	this.provider = provider;
        	this.shards = null;
        	this.keyTagPattern = keyTagPattern;
        }

        public ShardedJedisFactory(List<JedisShardInfo> shards, Hashing algo,
                Pattern keyTagPattern) {
        	this.isDynamic = false;
        	this.provider = null;
            this.shards = shards;
            this.algo = algo;
            this.keyTagPattern = keyTagPattern;
        }

        public Object makeObject() throws Exception {
        	if(isDynamic) {
	            ShardedJedis jedis = new ShardedJedis(provider, algo, keyTagPattern);
	            return jedis;
        	} else {
	            ShardedJedis jedis = new ShardedJedis(shards, algo, keyTagPattern);
	            return jedis;
        	}
        }

        public void destroyObject(final Object obj) throws Exception {
            if ((obj != null) && (obj instanceof ShardedJedis)) {
                ShardedJedis shardedJedis = (ShardedJedis) obj;
                if(isDynamic) {
                	provider.unregister(shardedJedis);
                }
                for (Jedis jedis : shardedJedis.getAllShards()) {
                    try {
                   		try {
                   			jedis.quit();
                        } catch (Exception e) {

                        }
                        jedis.disconnect();
                    } catch (Exception e) {

                    }
                }
            }
        }

        public boolean validateObject(final Object obj) {
        	try {
                ShardedJedis jedis = (ShardedJedis) obj;
                for (Jedis shard : jedis.getAllShards()) {
                    if (!shard.ping().equals("PONG")) {
                        return false;
                    }
                }
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }
}