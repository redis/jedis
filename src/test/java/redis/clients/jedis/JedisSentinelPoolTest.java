package redis.clients.jedis;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;

import org.junit.runners.MethodSorters;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;

import static org.junit.Assert.*;

@FixMethodOrder(MethodSorters.JVM)
public class JedisSentinelPoolTest {

  private static final String MASTER_NAME = "mymaster";

  //protected static HostAndPort master = HostAndPorts.getRedisServers().get(2);
  //protected static HostAndPort slave1 = HostAndPorts.getRedisServers().get(3);

  protected static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(1);
  protected static final HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(3);

  protected final Set<String> sentinels = new HashSet<>();

  @Before
  public void setUp() throws Exception {
    sentinels.clear();

    sentinels.add(sentinel1.toString());
    sentinels.add(sentinel2.toString());
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


  @Test
  public void testSentinelMasterSubscribeListener() {
    // case 1: default : subscribe on ,active off
    SentinelPoolConfig config = new SentinelPoolConfig();

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
            "foobared", 2);
    HostAndPort hostPort1 = pool.getResource().connection.getHostAndPort();

    Jedis sentinel = new Jedis(sentinel1);
    sentinel.sendCommand(Protocol.Command.SENTINEL, "failover", MASTER_NAME);
    try {
      Thread.sleep(20000); // sleep. let the failover finish
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    HostAndPort hostPort2 = pool.getResource().connection.getHostAndPort();

    pool.destroy();
    assertNotEquals(hostPort1, hostPort2);
  }

  @Test
  public void testSentinelMasterActiveDetectListener() {
    // case 2: subscribe off ,active on
    SentinelPoolConfig config = new SentinelPoolConfig();
    config.setEnableActiveDetectListener(true);
    config.setEnableDefaultSubscribeListener(false);

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
            "foobared", 2);
    HostAndPort hostPort1 = pool.getResource().connection.getHostAndPort();

    Jedis sentinel = new Jedis(sentinel1);
    sentinel.sendCommand(Protocol.Command.SENTINEL, "failover", MASTER_NAME);
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    HostAndPort hostPort2 = pool.getResource().connection.getHostAndPort();

    pool.destroy();
    assertNotEquals(hostPort1, hostPort2);
  }

  @Test
  public void testALLSentinelMasterListener() {
    // case 2: subscribe on ,active on
    SentinelPoolConfig config = new SentinelPoolConfig();
    config.setEnableActiveDetectListener(true);
    config.setEnableDefaultSubscribeListener(true);
    config.setActiveDetectIntervalTimeMillis(5*1000);
    config.setSubscribeRetryWaitTimeMillis(5*1000);

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
            "foobared", 2);
    HostAndPort hostPort1 = pool.getResource().connection.getHostAndPort();

    Jedis sentinel = new Jedis(sentinel1);
    sentinel.sendCommand(Protocol.Command.SENTINEL, "failover", MASTER_NAME);
    try {
      Thread.sleep(20000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    HostAndPort hostPort2 = pool.getResource().connection.getHostAndPort();

    pool.destroy();
    assertNotEquals(hostPort1, hostPort2);
  }
}
