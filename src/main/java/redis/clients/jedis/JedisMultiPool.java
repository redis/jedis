package redis.clients.jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.pool.impl.GenericObjectPool;

import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Hashing;
import redis.clients.util.Pool;
import redis.clients.util.ShardInfo;
import redis.clients.util.Sharded;

/**
 * Thread safe, holds one pool per each Redis instance. 
 */
public class JedisMultiPool { 
	
	private final ShardedJedisPools shardedJedisPools;
	private final List<JedisShardInfo> shardsInfo;

    public JedisMultiPool(final GenericObjectPool.Config poolConfig,
            List<JedisShardInfo> shards) {
        this(poolConfig, shards, Hashing.MURMUR_HASH);
    }

    public JedisMultiPool(final GenericObjectPool.Config poolConfig,
            List<JedisShardInfo> shards, Hashing algo) {
        this(poolConfig, shards, algo, null);
    }

    public JedisMultiPool(final GenericObjectPool.Config poolConfig,
            List<JedisShardInfo> shards, Pattern keyTagPattern) {
        this(poolConfig, shards, Hashing.MURMUR_HASH, keyTagPattern);
    }

    public JedisMultiPool(final GenericObjectPool.Config poolConfig,
            List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern) {
    	this.shardsInfo = new ArrayList<JedisShardInfo>(shards);
    	
    	List<JedisPoolShardInfo> pooledShards = new ArrayList<JedisMultiPool.JedisPoolShardInfo>(shards.size()); 
    	for(JedisShardInfo shardInfo : shards){
    		pooledShards.add(new JedisPoolShardInfo(shardInfo, poolConfig));
    	}
    	
    	shardedJedisPools = new ShardedJedisPools(pooledShards, algo, keyTagPattern);
    }
    
    public Pool<Jedis> getPool(byte[] key) {
    	return shardedJedisPools.getShard(key);
    }
    
    public Pool<Jedis> getPool(String key) {
    	return shardedJedisPools.getShard(key);
    }
    
    public JedisShardInfo getShardInfo(byte[] key) {
    	return shardedJedisPools.getShardInfo(key).getShardInfo();
    }

    public JedisShardInfo getShardInfo(String key) {
        return shardedJedisPools.getShardInfo(key).getShardInfo();
    }
    
    public String getKeyTag(String key){
    	return shardedJedisPools.getKeyTag(key);
    }

    public Collection<JedisShardInfo> getShardsInfo() {
        return Collections.unmodifiableCollection(shardsInfo);
    }
    
    public Collection<JedisPool> getAllPools() {
        return shardedJedisPools.getAllShards();
    }

    public void destroy() {
        Exception lastException = null;
    	for(JedisPool pool: shardedJedisPools.getAllShards()){
    		try {
    			pool.destroy();
    		} catch (Exception e) {
    			lastException = e;
    		}
    	}
    	
    	if(lastException != null){
            throw new JedisException("Could not destroy some pools", lastException);
    	}
    }

	private static class JedisPoolShardInfo extends ShardInfo<JedisPool>{

		private final JedisShardInfo shardInfo;
		private final GenericObjectPool.Config poolConfig;
		
		public JedisPoolShardInfo(JedisShardInfo shardInfo, GenericObjectPool.Config poolConfig){
			super(shardInfo.getWeight());
			this.shardInfo = shardInfo;
			this.poolConfig = poolConfig;
		}
		
		public JedisShardInfo getShardInfo() {
			return shardInfo;
		}

		@Override
		protected JedisPool createResource() {
			return new JedisPool(poolConfig, shardInfo);
		}
		
	}
	
	private static class ShardedJedisPools extends Sharded<JedisPool, JedisPoolShardInfo>{

		public ShardedJedisPools(List<JedisPoolShardInfo> shards, Hashing algo, Pattern tagPattern) {
			super(shards, algo, tagPattern);
		}
		
	}
    
}