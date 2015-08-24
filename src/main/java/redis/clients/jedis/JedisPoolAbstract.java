package redis.clients.jedis;

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import redis.clients.util.Pool;

public class JedisPoolAbstract extends Pool<Jedis> {
  public ConnectionBrokenDeterminer connBrokenDeterminer;

  public JedisPoolAbstract() {
    super();
  }

  public JedisPoolAbstract(GenericObjectPoolConfig poolConfig, PooledObjectFactory<Jedis> factory) {
    super(poolConfig, factory);
  }

  public void setConnectionBrokenDeterminer(final ConnectionBrokenDeterminer determiner) {
    this.connBrokenDeterminer = determiner;
  }

  @Override
  public Jedis getResource() {
    Jedis jedis = super.getResource();
    jedis.setDataSource(this);
    if (connBrokenDeterminer != null) {
      jedis.setConnectionBrokenDeterminer(connBrokenDeterminer);
    }
    return jedis;
  }

  @Override
  protected void returnBrokenResource(Jedis resource) {
    super.returnBrokenResource(resource);
  }

  @Override
  protected void returnResource(Jedis resource) {
    super.returnResource(resource);
  }
}
