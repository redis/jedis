package redis.clients.jedis.shard;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.util.ShardInfo;

public class JedisPoolShardInfo extends ShardInfo<JedisPool>{

  private final JedisShardInfo shardInfo;
  private final GenericObjectPoolConfig poolConfig;
  private int database;
  
  public JedisPoolShardInfo(JedisShardInfo shardInfo, int database, GenericObjectPoolConfig poolConfig){
    super(shardInfo.getWeight());
    
    this.shardInfo = shardInfo;
    this.database = database;
    this.poolConfig = poolConfig;
  }
  
  public JedisShardInfo getShardInfo() {
    return shardInfo;
  }

  public GenericObjectPoolConfig getPoolConfig() {
    return poolConfig;
  }
  
  public String getHost() {
    return shardInfo.getHost();
  }

  public int getPort() {
    return shardInfo.getPort();
  }

  @Override
  public String getName() {
      return shardInfo.getName();
  }
  
  @Override
  protected JedisPool createResource() {
    return new JedisPool(poolConfig, shardInfo.getHost(), shardInfo.getPort(), shardInfo.getTimeout(),
                    shardInfo.getPassword(), database);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + shardInfo.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    JedisPoolShardInfo other = (JedisPoolShardInfo) obj;
    if (shardInfo != other.shardInfo) {
      return false;
    }
    return true;
  }
  
}
