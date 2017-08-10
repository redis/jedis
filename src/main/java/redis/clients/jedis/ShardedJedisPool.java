package redis.clients.jedis;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.util.Hashing;
import redis.clients.util.Pool;

public class ShardedJedisPool extends Pool<ShardedJedis> {
  public ShardedJedisPool(final GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards) {
    this(poolConfig, shards, Hashing.MURMUR_HASH);
  }

  public ShardedJedisPool(final GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards,
      Hashing algo) {
    this(poolConfig, shards, algo, null);
  }

  public ShardedJedisPool(final GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards,
      Pattern keyTagPattern) {
    this(poolConfig, shards, Hashing.MURMUR_HASH, keyTagPattern);
  }

  public ShardedJedisPool(final GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards,
      Hashing algo, Pattern keyTagPattern) {
    super(poolConfig, new ShardedJedisFactory(shards, algo, keyTagPattern));
  }

  @Override
  public ShardedJedis getResource() {
    ShardedJedis jedis = super.getResource();
    jedis.setDataSource(this);
    return jedis;
  }

  @Override
  protected void returnBrokenResource(final ShardedJedis resource) {
    if (resource != null) {
      returnBrokenResourceObject(resource);
    }
  }

  @Override
  protected void returnResource(final ShardedJedis resource) {
    if (resource != null) {
      resource.resetState();
      returnResourceObject(resource);
    }
  }

  /**
   * PoolableObjectFactory custom impl.
   */
  private static class ShardedJedisFactory implements PooledObjectFactory<ShardedJedis> {
    private List<JedisShardInfo> shards;
    private Hashing algo;
    private Pattern keyTagPattern;

    public ShardedJedisFactory(List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern) {
      this.shards = shards;
      this.algo = algo;
      this.keyTagPattern = keyTagPattern;
    }

    @Override
    public PooledObject<ShardedJedis> makeObject() throws Exception {
      ShardedJedis jedis = new ShardedJedis(shards, algo, keyTagPattern);
      return new DefaultPooledObject<ShardedJedis>(jedis);
    }

    @Override
    public void destroyObject(PooledObject<ShardedJedis> pooledShardedJedis) throws Exception {
      final ShardedJedis shardedJedis = pooledShardedJedis.getObject();
      for (Jedis jedis : shardedJedis.getAllShards()) {
        Jedis.destroy(jedis);
      }
    }

    @Override
    public boolean validateObject(PooledObject<ShardedJedis> pooledShardedJedis) {
      ShardedJedis jedis = pooledShardedJedis.getObject();
      for (JedisShardInfo shardInfo : jedis.getDistinctShardInfo()) {
        Jedis shard = jedis.getShard(shardInfo);
        boolean valid = true;
        try {
          valid = shard.ping().equals("PONG");
        } catch(Exception ex) {
          valid = false;
        }
        if (!valid) {
          jedis.replaceShard(shardInfo);
          Jedis.destroy(shard);
        }
      }
      return true;
    }

    @Override
    public void activateObject(PooledObject<ShardedJedis> p) throws Exception {

    }

    @Override
    public void passivateObject(PooledObject<ShardedJedis> p) throws Exception {

    }
  }
}