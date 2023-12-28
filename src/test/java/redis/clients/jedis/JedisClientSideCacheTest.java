package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class JedisClientSideCacheTest {

  protected static final HostAndPort hnp = HostAndPorts.getRedisServers().get(1);

  protected Jedis jedis;

  @Before
  public void setUp() throws Exception {
    jedis = new Jedis(hnp, DefaultJedisClientConfig.builder().timeoutMillis(500).password("foobared").build());
    jedis.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    jedis.close();
  }

  private static final JedisClientConfig configForCache = DefaultJedisClientConfig.builder()
      .resp3().socketTimeoutMillis(20).password("foobared").build();

  @Test
  public void simple() {
    try (JedisClientSideCache jCache = new JedisClientSideCache(hnp, configForCache)) {
      jedis.set("foo", "bar");
      assertEquals("bar", jCache.get("foo"));
      jedis.del("foo");
      assertNull(jCache.get("foo"));
    }
  }

  @Test
  public void simpleMock() {
    ClientSideCache cache = Mockito.mock(ClientSideCache.class);
    try (JedisClientSideCache jCache = new JedisClientSideCache(hnp, configForCache, cache)) {
      jedis.set("foo", "bar");
      assertEquals("bar", jCache.get("foo"));
      jedis.del("foo");
      assertNull(jCache.get("foo"));
    }

    InOrder inOrder = Mockito.inOrder(cache);
    inOrder.verify(cache).invalidateKeys(Mockito.notNull());
    inOrder.verify(cache).getValue("foo");
    inOrder.verify(cache).setKey("foo", "bar");
    inOrder.verify(cache).invalidateKeys(Mockito.notNull());
    inOrder.verify(cache).getValue("foo");
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void flushall() {
    try (JedisClientSideCache jCache = new JedisClientSideCache(hnp, configForCache)) {
      jedis.set("foo", "bar");
      assertEquals("bar", jCache.get("foo"));
      jedis.flushAll();
      assertNull(jCache.get("foo"));
    }
  }
}
