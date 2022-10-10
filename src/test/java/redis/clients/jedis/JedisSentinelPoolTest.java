package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

public class JedisSentinelPoolTest {

  private static final String MASTER_NAME = "mymaster";

  protected static HostAndPort master = HostAndPorts.getRedisServers().get(2);
  protected static HostAndPort slave1 = HostAndPorts.getRedisServers().get(3);

  protected static HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(1);
  protected static HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(3);

  protected static Jedis sentinelJedis1;
  protected static Jedis sentinelJedis2;

  protected Set<String> sentinels = new HashSet<>();

  @Before
  public void setUp() throws Exception {
    sentinels.clear();

    sentinels.add(sentinel1.toString());
    sentinels.add(sentinel2.toString());

    sentinelJedis1 = new Jedis(sentinel1);
    sentinelJedis2 = new Jedis(sentinel2);
  }

  @After
  public void tearDown() throws Exception {
    sentinelJedis1.close();
    sentinelJedis2.close();
  }

  @Test
  public void repeatedSentinelPoolInitialization() {

    for (int i = 0; i < 20; ++i) {
      GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();

      JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
          "foobared", 2);
      pool.getResource().close();
      pool.destroy();
    }
  }

  @Test(expected = JedisConnectionException.class)
  public void initializeWithNotAvailableSentinelsShouldThrowException() {
    Set<String> wrongSentinels = new HashSet<String>();
    wrongSentinels.add(new HostAndPort("localhost", 65432).toString());
    wrongSentinels.add(new HostAndPort("localhost", 65431).toString());

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, wrongSentinels);
    pool.destroy();
  }

  @Test(expected = JedisException.class)
  public void initializeWithNotMonitoredMasterNameShouldThrowException() {
    final String wrongMasterName = "wrongMasterName";
    JedisSentinelPool pool = new JedisSentinelPool(wrongMasterName, sentinels);
    pool.destroy();
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "foobared", 2);
    Jedis jedis = pool.getResource();
    jedis.auth("foobared");
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void returnResourceShouldResetState() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "foobared", 2)) {

      Jedis jedis = null;
      try (Jedis jedis1 = pool.getResource()) {
        jedis = jedis1;
        jedis1.set("hello", "jedis");
        Transaction t = jedis1.multi();
        t.set("hello", "world");
      }

      try (Jedis jedis2 = pool.getResource()) {
        assertSame(jedis, jedis2);
        assertEquals("jedis", jedis2.get("hello"));
      }
    }
  }

  @Test
  public void checkResourceIsCloseable() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "foobared", 2);

    Jedis jedis = pool.getResource();
    try {
      jedis.set("hello", "jedis");
    } finally {
      jedis.close();
    }

    Jedis jedis2 = pool.getResource();
    try {
      assertEquals(jedis, jedis2);
    } finally {
      jedis2.close();
    }
  }

  @Test
  public void customClientName() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "foobared", 0, "my_shiny_client_name");

    Jedis jedis = pool.getResource();

    try {
      assertEquals("my_shiny_client_name", jedis.clientGetname());
    } finally {
      jedis.close();
      pool.destroy();
    }

    assertTrue(pool.isClosed());
  }

  @Test
  public void testResetInvalidPassword() {
    JedisFactory factory = new JedisFactory(null, 0, 2000, 2000, "foobared", 0, "my_shiny_client_name") { };

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, new JedisPoolConfig(), factory)) {
      Jedis obj1_ref;
      try (Jedis obj1_1 = pool.getResource()) {
        obj1_ref = obj1_1;
        obj1_1.set("foo", "bar");
        assertEquals("bar", obj1_1.get("foo"));
      }
      try (Jedis obj1_2 = pool.getResource()) {
        assertSame(obj1_ref, obj1_2);
        factory.setPassword("wrong password");
        try (Jedis obj2 = pool.getResource()) {
          fail("Should not get resource from pool");
        } catch (JedisException e) { }
      }
    }
  }

  @Test
  public void testResetValidPassword() {
    JedisFactory factory = new JedisFactory(null, 0, 2000, 2000, "wrong password", 0, "my_shiny_client_name") { };

    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, new JedisPoolConfig(), factory)) {
      try (Jedis obj1 = pool.getResource()) {
        fail("Should not get resource from pool");
      } catch (JedisException e) { }

      factory.setPassword("foobared");
      try (Jedis obj2 = pool.getResource()) {
        obj2.set("foo", "bar");
        assertEquals("bar", obj2.get("foo"));
      }
    }
  }
//
//  @Test
//  public void ensureSafeTwiceFailover() throws InterruptedException {
//    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels,
//        new GenericObjectPoolConfig<Jedis>(), 1000, "foobared", 2, "twice-failover-client");
//
//    forceFailover(pool);
//    // after failover sentinel needs a bit of time to stabilize before a new failover
//    Thread.sleep(1000);
//    forceFailover(pool);
//
//    // you can test failover as much as possible
//  }
//
//  private void forceFailover(JedisSentinelPool pool) throws InterruptedException {
//    HostAndPort oldMaster = pool.getCurrentHostMaster();
//
//    // jedis connection should be master
//    Jedis beforeFailoverJedis = pool.getResource();
//    assertEquals("PONG", beforeFailoverJedis.ping());
//
//    waitForFailover(pool, oldMaster);
//
//    Jedis afterFailoverJedis = pool.getResource();
//    assertEquals("PONG", afterFailoverJedis.ping());
//    assertEquals(2, afterFailoverJedis.getDB());
//    assertEquals("twice-failover-client", afterFailoverJedis.clientGetname());
//
//    // returning both connections to the pool should not throw
//    beforeFailoverJedis.close();
//    afterFailoverJedis.close();
//  }
//
//  private void waitForFailover(JedisSentinelPool pool, HostAndPort oldMaster)
//      throws InterruptedException {
//    HostAndPort newMaster = JedisSentinelTestUtil.waitForNewPromotedMaster(MASTER_NAME,
//      sentinelJedis1, sentinelJedis2);
//
//    waitForJedisSentinelPoolRecognizeNewMaster(pool, newMaster);
//  }
//
//  private void waitForJedisSentinelPoolRecognizeNewMaster(JedisSentinelPool pool,
//      HostAndPort newMaster) throws InterruptedException {
//
//    while (true) {
//      HostAndPort currentHostMaster = pool.getCurrentHostMaster();
//
//      if (newMaster.equals(currentHostMaster)) break;
//
//      // System.out.println("JedisSentinelPool's master is not yet changed, sleep...");
//      Thread.sleep(100);
//    }
//  }

}
