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

  /**
   * @deprecated starting from Jedis 3.0 this method will not be exposed. Resource cleanup should be
   *             done using @see {@link redis.clients.jedis.Jedis#close()}
   */
  @Override
  @Deprecated
  public void returnBrokenResource(final ShardedJedis resource) {
    if (resource != null) {
      returnBrokenResourceObject(resource);
    }
  }

  /**
   * @deprecated starting from Jedis 3.0 this method will not be exposed. Resource cleanup should be
   *             done using @see {@link redis.clients.jedis.Jedis#close()}
   */
  @Override
  @Deprecated
  public void returnResource(final ShardedJedis resource) {
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

    @Override
    public boolean validateObject(PooledObject<ShardedJedis> pooledShardedJedis) {
      try {
        ShardedJedis jedis = pooledShardedJedis.getObject();
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

    @Override
    public void activateObject(PooledObject<ShardedJedis> p) throws Exception {

    }

    @Override
    public void passivateObject(PooledObject<ShardedJedis> p) throws Exception {

    }
  }
}