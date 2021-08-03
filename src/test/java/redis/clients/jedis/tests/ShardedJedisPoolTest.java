package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisExhaustedPoolException;
import redis.clients.jedis.util.Hashing;

public class ShardedJedisPoolTest {
  private static HostAndPort redis1 = HostAndPortUtil.getRedisServers().get(0);
  private static HostAndPort redis2 = HostAndPortUtil.getRedisServers().get(1);

  private List<JedisShardInfo> shards;

  @Before
  public void startUp() {
    shards = new ArrayList<>();
    shards.add(new JedisShardInfo(redis1));
    shards.add(new JedisShardInfo(redis2));
    shards.get(0).setPassword("foobared");
    shards.get(1).setPassword("foobared");

    for (JedisShardInfo shard : shards) {
      try (Jedis j = new Jedis(shard)) {
        j.flushAll();
      }
    }
  }

  @Test
  public void checkConnections() {
    ShardedJedisPool pool = new ShardedJedisPool(new GenericObjectPoolConfig<ShardedJedis>(),
        shards);
    ShardedJedis jedis = pool.getResource();
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.destroy();
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    ShardedJedisPool pool = new ShardedJedisPool(new GenericObjectPoolConfig<ShardedJedis>(),
        shards);
    ShardedJedis jedis = pool.getResource();
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void checkConnectionWithDefaultPort() {
    ShardedJedisPool pool = new ShardedJedisPool(new GenericObjectPoolConfig<ShardedJedis>(),
        shards);
    ShardedJedis jedis = pool.getResource();
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.destroy();
  }

  @Test
  public void checkJedisIsReusedWhenReturned() {
    ShardedJedisPool pool = new ShardedJedisPool(new GenericObjectPoolConfig<ShardedJedis>(),
        shards);
    ShardedJedis jedis = pool.getResource();
    jedis.set("foo", "0");
    jedis.close();

    jedis = pool.getResource();
    jedis.incr("foo");
    jedis.close();
    pool.destroy();
  }

  @Test
  public void checkPoolRepairedWhenJedisIsBroken() {
    ShardedJedisPool pool = new ShardedJedisPool(new GenericObjectPoolConfig<ShardedJedis>(),
        shards);
    ShardedJedis jedis = pool.getResource();
    jedis.disconnect();
    jedis.close();

    jedis = pool.getResource();
    jedis.incr("foo");
    jedis.close();
    pool.destroy();
  }

  @Test(expected = JedisExhaustedPoolException.class)
  public void checkPoolOverflow() {
    GenericObjectPoolConfig<ShardedJedis> config = new GenericObjectPoolConfig<ShardedJedis>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);

    ShardedJedisPool pool = new ShardedJedisPool(config, shards);

    ShardedJedis jedis = pool.getResource();
    jedis.set("foo", "0");

    ShardedJedis newJedis = pool.getResource();
    newJedis.incr("foo");
  }

  @Test
  public void shouldNotShareInstances() {
    GenericObjectPoolConfig<ShardedJedis> config = new GenericObjectPoolConfig<ShardedJedis>();
    config.setMaxTotal(2);

    ShardedJedisPool pool = new ShardedJedisPool(config, shards);

    ShardedJedis j1 = pool.getResource();
    ShardedJedis j2 = pool.getResource();

    assertNotSame(j1.getShard("foo"), j2.getShard("foo"));
  }

  @Test
  public void checkFailedJedisServer() {
    ShardedJedisPool pool = new ShardedJedisPool(new GenericObjectPoolConfig<ShardedJedis>(),
        shards);
    ShardedJedis jedis = pool.getResource();
    jedis.incr("foo");
    jedis.close();
    pool.destroy();
  }

  @Test
  public void startWithUrlString() {
    Jedis j = new Jedis("localhost", 6380);
    j.auth("foobared");
    j.set("foo", "bar");
    j.disconnect();

    j = new Jedis("localhost", 6379);
    j.auth("foobared");
    j.set("foo", "bar");
    j.disconnect();

    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    shards.add(new JedisShardInfo("redis://:foobared@localhost:6380"));
    shards.add(new JedisShardInfo("redis://:foobared@localhost:6379"));

    GenericObjectPoolConfig<ShardedJedis> redisConfig = new GenericObjectPoolConfig<ShardedJedis>();
    ShardedJedisPool pool = new ShardedJedisPool(redisConfig, shards);

    Jedis[] jedises = pool.getResource().getAllShards().toArray(new Jedis[2]);

    Jedis jedis = jedises[0];
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));

    jedis = jedises[1];
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));
  }

  @Test
  public void startWithUrl() throws URISyntaxException {
    Jedis j = new Jedis("localhost", 6380);
    j.auth("foobared");
    j.set("foo", "bar");
    j.disconnect();

    j = new Jedis("localhost", 6379);
    j.auth("foobared");
    j.set("foo", "bar");
    j.disconnect();

    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    shards.add(new JedisShardInfo(new URI("redis://:foobared@localhost:6380")));
    shards.add(new JedisShardInfo(new URI("redis://:foobared@localhost:6379")));

    GenericObjectPoolConfig<ShardedJedis> redisConfig = new GenericObjectPoolConfig<ShardedJedis>();
    ShardedJedisPool pool = new ShardedJedisPool(redisConfig, shards);

    Jedis[] jedises = pool.getResource().getAllShards().toArray(new Jedis[2]);

    Jedis jedis = jedises[0];
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));

    jedis = jedises[1];
    assertEquals("PONG", jedis.ping());
    assertEquals("bar", jedis.get("foo"));
  }

  @Test
  public void returnResourceShouldResetState() throws URISyntaxException {
    GenericObjectPoolConfig<ShardedJedis> config = new GenericObjectPoolConfig<ShardedJedis>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);

    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    shards.add(new JedisShardInfo(new URI("redis://:foobared@localhost:6380")));
    shards.add(new JedisShardInfo(new URI("redis://:foobared@localhost:6379")));

    ShardedJedisPool pool = new ShardedJedisPool(config, shards);

    ShardedJedis jedis = pool.getResource();
    jedis.set("pipelined", String.valueOf(0));
    jedis.set("pipelined2", String.valueOf(0));

    ShardedJedisPipeline pipeline = jedis.pipelined();

    pipeline.incr("pipelined");
    pipeline.incr("pipelined2");

    jedis.resetState();

    pipeline = jedis.pipelined();
    pipeline.incr("pipelined");
    pipeline.incr("pipelined2");
    List<Object> results = pipeline.syncAndReturnAll();

    assertEquals(2, results.size());
    jedis.close();
    pool.destroy();
  }

  @Test
  public void checkResourceIsCloseable() throws URISyntaxException {
    GenericObjectPoolConfig<ShardedJedis> config = new GenericObjectPoolConfig<ShardedJedis>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);

    List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
    shards.add(new JedisShardInfo(new URI("redis://:foobared@localhost:6380")));
    shards.add(new JedisShardInfo(new URI("redis://:foobared@localhost:6379")));

    ShardedJedisPool pool = new ShardedJedisPool(config, shards);

    ShardedJedis jedis = pool.getResource();
    try {
      jedis.set("hello", "jedis");
    } finally {
      jedis.close();
    }

    ShardedJedis jedis2 = pool.getResource();
    try {
      assertEquals(jedis, jedis2);
    } finally {
      jedis2.close();
    }
  }

  @Test
  public void checkOverrideFactoryValidateMethod() {
    AtomicInteger counter = new AtomicInteger(0);

    ShardedJedisPool.ShardedJedisFactory overriddenFactory = new ShardedJedisPool.ShardedJedisFactory(
        shards, Hashing.MURMUR_HASH, null) {
      @Override public boolean validateObject(PooledObject<ShardedJedis> pooledShardedJedis) {
        counter.incrementAndGet();
        return super.validateObject(pooledShardedJedis);
      }
    };

    GenericObjectPoolConfig<ShardedJedis> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setTestOnReturn(true);
    ShardedJedisPool pool = new ShardedJedisPool(poolConfig, overriddenFactory);
    try (ShardedJedis jedis = pool.getResource();) {
      jedis.set("foo", "bar");
      assertEquals(0, counter.get());
    }
    assertEquals(1, counter.get());

    pool.destroy();
  }

}
