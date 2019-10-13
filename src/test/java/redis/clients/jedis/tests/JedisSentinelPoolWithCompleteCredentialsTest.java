package redis.clients.jedis.tests;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.tests.utils.JedisSentinelTestUtil;
import redis.clients.jedis.tests.utils.RedisVersionUtil;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * This test class is a copy of @JedisSentinelPoolTest where all authentications are made with
 * default:foobared credentialsinformation
 *
 * This test is only executed when the server/cluster is Redis 6. or more.
 */
public class JedisSentinelPoolWithCompleteCredentialsTest {
  private static final String MASTER_NAME = "mymaster";

  private static HostAndPort hnp = HostAndPortUtil.getRedisServers().get(0);

  protected static HostAndPort master = HostAndPortUtil.getRedisServers().get(2);
  protected static HostAndPort slave1 = HostAndPortUtil.getRedisServers().get(3);

  protected static HostAndPort sentinel1 = HostAndPortUtil.getSentinelServers().get(1);
  protected static HostAndPort sentinel2 = HostAndPortUtil.getSentinelServers().get(3);

  protected static Jedis sentinelJedis1;
  protected static Jedis sentinelJedis2;

  protected Set<String> sentinels = new HashSet<String>();

  @Before
  public void setUp() throws Exception {
    Jedis jedis = new Jedis(hnp.getHost(), hnp.getPort(), 500);
    jedis.connect();
    jedis.auth("foobared");
    // run the test only if the verison support ACL (6 or later)
    boolean shouldNotRun = ((new RedisVersionUtil(jedis)).getRedisMajorVersionNumber() < 6);
    if ( shouldNotRun ) {
      org.junit.Assume.assumeFalse("Not running ACL tests on this version of Redis", shouldNotRun);
    }

    sentinels.add(sentinel1.toString());
    sentinels.add(sentinel2.toString());

    sentinelJedis1 = new Jedis(sentinel1);
    sentinelJedis2 = new Jedis(sentinel2);
  }
  
  @Test
  public void repeatedSentinelPoolInitialization() {

    for(int i=0; i<20 ; ++i) {
      GenericObjectPoolConfig config = new GenericObjectPoolConfig();

      JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
          "default","foobared", 2);
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
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "default","foobared", 2);
    Jedis jedis = pool.getResource();
    jedis.auth("default", "foobared");
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void ensureSafeTwiceFailover() throws InterruptedException {
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels,
        new GenericObjectPoolConfig(), 1000, "default", "foobared", 2);

    forceFailover(pool);
    // after failover sentinel needs a bit of time to stabilize before a new
    // failover
    Thread.sleep(100);
    forceFailover(pool);

    // you can test failover as much as possible
  }

  @Test
  public void returnResourceShouldResetState() {
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "default", "foobared", 2);

    Jedis jedis = pool.getResource();
    Jedis jedis2 = null;

    try {
      jedis.set("hello", "jedis");
      Transaction t = jedis.multi();
      t.set("hello", "world");
      jedis.close();

      jedis2 = pool.getResource();

      assertTrue(jedis == jedis2);
      assertEquals("jedis", jedis2.get("hello"));
    } catch (JedisConnectionException e) {
      if (jedis2 != null) {
        jedis2 = null;
      }
    } finally {
      jedis2.close();

      pool.destroy();
    }
  }

  @Test
  public void checkResourceIsCloseable() {
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "default", "foobared", 2);

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
    GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "default", "foobared", 0, "my_shiny_client_name");

    Jedis jedis = pool.getResource();

    try {
      assertEquals("my_shiny_client_name", jedis.clientGetname());
    } finally {
      jedis.close();
      pool.destroy();
    }

    assertTrue(pool.isClosed());
  }

  private void forceFailover(JedisSentinelPool pool) throws InterruptedException {
    HostAndPort oldMaster = pool.getCurrentHostMaster();

    // jedis connection should be master
    Jedis beforeFailoverJedis = pool.getResource();
    assertEquals("PONG", beforeFailoverJedis.ping());

    waitForFailover(pool, oldMaster);

    Jedis afterFailoverJedis = pool.getResource();
    assertEquals("PONG", afterFailoverJedis.ping());
    assertNotNull(afterFailoverJedis.configGet("requirepass").get(1));
    assertEquals(2, afterFailoverJedis.getDB());

    // returning both connections to the pool should not throw
    beforeFailoverJedis.close();
    afterFailoverJedis.close();
  }

  private void waitForFailover(JedisSentinelPool pool, HostAndPort oldMaster)
      throws InterruptedException {
    HostAndPort newMaster = JedisSentinelTestUtil.waitForNewPromotedMaster(MASTER_NAME,
      sentinelJedis1, sentinelJedis2);

    waitForJedisSentinelPoolRecognizeNewMaster(pool, newMaster);
  }

  private void waitForJedisSentinelPoolRecognizeNewMaster(JedisSentinelPool pool,
      HostAndPort newMaster) throws InterruptedException {

    while (true) {
      HostAndPort currentHostMaster = pool.getCurrentHostMaster();

      if (newMaster.equals(currentHostMaster)) break;

      System.out.println("JedisSentinelPool's master is not yet changed, sleep...");

      Thread.sleep(100);
    }
  }

}