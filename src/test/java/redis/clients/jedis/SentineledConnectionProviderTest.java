package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.SentineledConnectionProvider;

/**
 * @see JedisSentinelPoolTest
 */
public class SentineledConnectionProviderTest {

  private static final String MASTER_NAME = "mymaster";

  //protected static HostAndPort master = HostAndPorts.getRedisServers().get(2);
  //protected static HostAndPort slave1 = HostAndPorts.getRedisServers().get(3);

  protected static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(1);
  protected static final HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(3);

  protected Set<HostAndPort> sentinels = new HashSet<>();

  @Before
  public void setUp() throws Exception {
    sentinels.clear();

    sentinels.add(sentinel1);
    sentinels.add(sentinel2);
  }

  @Test
  public void repeatedSentinelPoolInitialization() {
    for (int i = 0; i < 20; ++i) {

      try (SentineledConnectionProvider provider = new SentineledConnectionProvider(MASTER_NAME,
          DefaultJedisClientConfig.builder().timeoutMillis(1000).password("foobared").database(2).build(),
          sentinels, DefaultJedisClientConfig.builder().build())) {

        provider.getConnection().close();
      }
    }
  }

  @Test(expected = JedisConnectionException.class)
  public void initializeWithNotAvailableSentinelsShouldThrowException() {
    Set<HostAndPort> wrongSentinels = new HashSet<>();
    wrongSentinels.add(new HostAndPort("localhost", 65432));
    wrongSentinels.add(new HostAndPort("localhost", 65431));

    try (SentineledConnectionProvider provider = new SentineledConnectionProvider(MASTER_NAME,
        DefaultJedisClientConfig.builder().build(), wrongSentinels, DefaultJedisClientConfig.builder().build())) {
    }
  }

  @Test(expected = JedisException.class)
  public void initializeWithNotMonitoredMasterNameShouldThrowException() {
    final String wrongMasterName = "wrongMasterName";
    try (SentineledConnectionProvider provider = new SentineledConnectionProvider(wrongMasterName,
        DefaultJedisClientConfig.builder().build(), sentinels, DefaultJedisClientConfig.builder().build())) {
    }
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();

    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME,
        DefaultJedisClientConfig.builder().timeoutMillis(1000).password("foobared").database(2).build(),
        config, sentinels, DefaultJedisClientConfig.builder().build())) {
      assertSame(SentineledConnectionProvider.class, jedis.provider.getClass());
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
    }
  }

  @Test
  public void checkResourceIsCloseable() {
    GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisSentineled jedis = new JedisSentineled(MASTER_NAME,
        DefaultJedisClientConfig.builder().timeoutMillis(1000).password("foobared").database(2).build(),
        config, sentinels, DefaultJedisClientConfig.builder().build());

    Connection conn = jedis.provider.getConnection();
    try {
      conn.ping();
    } finally {
      conn.close();
    }

    Connection conn2 = jedis.provider.getConnection();
    try {
      assertEquals(conn, conn2);
    } finally {
      conn2.close();
    }
  }
//
//  @Test
//  public void testResetInvalidPassword() {
//    JedisFactory factory = new JedisFactory(null, 0, 2000, 2000, "foobared", 0, "my_shiny_client_name") { };
//
//    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, new JedisPoolConfig(), factory)) {
//      Jedis obj1_ref;
//      try (Jedis obj1_1 = pool.getResource()) {
//        obj1_ref = obj1_1;
//        obj1_1.set("foo", "bar");
//        assertEquals("bar", obj1_1.get("foo"));
//      }
//      try (Jedis obj1_2 = pool.getResource()) {
//        assertSame(obj1_ref, obj1_2);
//        factory.setPassword("wrong password");
//        try (Jedis obj2 = pool.getResource()) {
//          fail("Should not get resource from pool");
//        } catch (JedisException e) { }
//      }
//    }
//  }
//
//  @Test
//  public void testResetValidPassword() {
//    JedisFactory factory = new JedisFactory(null, 0, 2000, 2000, "wrong password", 0, "my_shiny_client_name") { };
//
//    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, new JedisPoolConfig(), factory)) {
//      try (Jedis obj1 = pool.getResource()) {
//        fail("Should not get resource from pool");
//      } catch (JedisException e) { }
//
//      factory.setPassword("foobared");
//      try (Jedis obj2 = pool.getResource()) {
//        obj2.set("foo", "bar");
//        assertEquals("bar", obj2.get("foo"));
//      }
//    }
//  }
}
