package redis.clients.jedis;

import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Hashing;
import redis.clients.util.Pool;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ShardedJedisPool extends Pool<ShardedJedis> {
  
  private static Logger log = Logger.getLogger(ShardedJedisPool.class.getName());
  
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
    this(poolConfig, shards, algo, keyTagPattern, null);
  }
  
  public ShardedJedisPool(final GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards,
      Hashing algo, Pattern keyTagPattern, String clientName) {
    super(poolConfig, new ShardedJedisFactory(shards, algo, keyTagPattern, clientName));
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
    private String clientName;

    public ShardedJedisFactory(List<JedisShardInfo> shards, Hashing algo, Pattern keyTagPattern, String clientName) {
      this.shards = shards;
      this.algo = algo;
      this.keyTagPattern = keyTagPattern;
      this.clientName = clientName;
    }

    @Override
    public PooledObject<ShardedJedis> makeObject() throws Exception {
      ShardedJedis shardedJedis = new ShardedJedis(shards, algo, keyTagPattern);
      for (Jedis jedis : shardedJedis.getAllShards()) {
        try{
          jedis.connect();
          if (clientName != null) {
            jedis.clientSetname(clientName);
          }
        } catch (JedisException je) {
          log.log(Level.SEVERE, "Can't connect to redis at " + jedis.getClient().getHost(), je);
          jedis.close();
        }
      }

      return new DefaultPooledObject<ShardedJedis>(shardedJedis);
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
