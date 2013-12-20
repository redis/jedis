package redis.clients.jedis.shard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.Client;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.util.Hashing;
import redis.clients.util.Sharded;

public class ShardedPool extends Sharded<JedisPool, JedisPoolShardInfo> {

  private Map<HostAndPort,JedisPoolShardInfo> shardInfoLookup = new HashMap<HostAndPort, JedisPoolShardInfo>();
  
  public ShardedPool(GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards, int database) {
    super(createPooledShards(poolConfig, shards, database));
    initLookup();
  }

  public ShardedPool(GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards, int database, Hashing algo) {
    super(createPooledShards(poolConfig, shards, database), algo);
    initLookup();
  }

  public ShardedPool(GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards, int database, Pattern tagPattern) {
    super(createPooledShards(poolConfig, shards, database), tagPattern);
    initLookup();
  }

  public ShardedPool(GenericObjectPoolConfig poolConfig, List<JedisShardInfo> shards, int database, Hashing algo, Pattern tagPattern) {
    super(createPooledShards(poolConfig, shards, database), algo, tagPattern);
    initLookup();
  }

  private static List<JedisPoolShardInfo> createPooledShards(GenericObjectPoolConfig poolConfig,  List<JedisShardInfo> shards, int database) {
    List<JedisPoolShardInfo> pooledShards = new ArrayList<JedisPoolShardInfo>(shards.size()); 
    for(JedisShardInfo shardInfo : shards){
      pooledShards.add(new JedisPoolShardInfo(shardInfo, database, poolConfig));
    }
    return pooledShards;
  }

  private void initLookup() {
    for (JedisPoolShardInfo shardInfo : getAllShardInfo()) {
      shardInfoLookup.put(new HostAndPort(shardInfo.getHost(), shardInfo.getPort()), shardInfo);
    }
  }

  public Jedis getResource(String key) {
    try {
      JedisPool pool = getShard(key);
      return pool.getResource();
    } catch (Exception e) {
      throw new JedisConnectionException("Could not get a resource from the pool", e);
    }
  }

  public Jedis getResource(byte[] key) {
    try {
      JedisPool pool = getShard(key);
      return pool.getResource();
    } catch (Exception e) {
      throw new JedisConnectionException("Could not get a resource from the pool", e);
    }
  }

  public void returnResource(final Jedis resource) {
    try {
      JedisPool pool = findPool(resource);
      pool.returnResource(resource);
    } catch (Exception e) {
      throw new JedisException("Could not return the resource to the pool", e);
    }
  }

  public void returnBrokenResource(final Jedis resource) {
    try {
      JedisPool pool = findPool(resource);
      pool.returnBrokenResource(resource);
    } catch (Exception e) {
      throw new JedisException("Could not return the resource to the pool", e);
    }
  }

  protected JedisPool findPool(Jedis resource) {
    Client client = resource.getClient();
    JedisPoolShardInfo shardInfo = shardInfoLookup.get(new HostAndPort(client.getHost(), client.getPort()));
    if (shardInfo == null) {
      throw new JedisException("Could not find pool. ");
    }
    return getShard(shardInfo);
  }

  public void destroy() {
    Exception lastException = null;
    for (JedisPool pool : this.getAllShards()) {
      try {
        pool.destroy();
      } catch (Exception e) {
        lastException = e;
      }
    }

    if (lastException != null) {
      throw new JedisException("Could not destroy some pools", lastException);
    }
  }

}
