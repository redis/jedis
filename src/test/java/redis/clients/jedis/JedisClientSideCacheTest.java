package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.hamcrest.Matchers;
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
    jedis = new Jedis(hnp, DefaultJedisClientConfig.builder().password("foobared").build());
    jedis.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    jedis.close();
  }

  private static final JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().resp3().password("foobared").build();

  @Test
  public void simple() {
    try (JedisClientSideCache jCache = new JedisClientSideCache(hnp, clientConfig)) {
      jedis.set("foo", "bar");
      assertEquals("bar", jCache.get("foo"));
      jedis.del("foo");
      assertThat(jCache.get("foo"), Matchers.oneOf("bar", null)); // ?
    }
  }

  @Test
  public void simpleMoreAndMock() {
    ClientSideCache cache = Mockito.mock(ClientSideCache.class);
    Mockito.when(cache.getValue("foo")).thenReturn(null, "bar", null);

    try (JedisClientSideCache jCache = new JedisClientSideCache(hnp, clientConfig, cache)) {
      jedis.set("foo", "bar");

      assertEquals("bar", jCache.get("foo"));

      jedis.del("foo");

      assertEquals("bar", jCache.get("foo"));

      // there should be an invalid pending; any connection command will make it read
      jCache.ping();

      assertNull(jCache.get("foo"));
    }

    InOrder inOrder = Mockito.inOrder(cache);
    inOrder.verify(cache).getValue("foo");
    inOrder.verify(cache).setKey("foo", "bar");
    inOrder.verify(cache).getValue("foo");
    inOrder.verify(cache).invalidateKeys(Mockito.notNull());
    inOrder.verify(cache).getValue("foo");
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void flushAll() {
    try (JedisClientSideCache jCache = new JedisClientSideCache(hnp, clientConfig)) {
      jedis.set("foo", "bar");
      assertEquals("bar", jCache.get("foo"));
      jedis.flushAll();
      assertThat(jCache.get("foo"), Matchers.oneOf("bar", null)); // ?
    }
  }

  @Test
  public void flushAllMoreAndMock() {
    ClientSideCache cache = Mockito.mock(ClientSideCache.class);
    Mockito.when(cache.getValue("foo")).thenReturn(null, "bar", null);

    try (JedisClientSideCache jCache = new JedisClientSideCache(hnp, clientConfig, cache)) {
      jedis.set("foo", "bar");

      assertEquals("bar", jCache.get("foo"));

      jedis.flushAll();

      assertEquals("bar", jCache.get("foo"));

      // there should be an invalid pending; any connection command will make it read
      jCache.ping();

      assertNull(jCache.get("foo"));
    }

    InOrder inOrder = Mockito.inOrder(cache);
    inOrder.verify(cache).getValue("foo");
    inOrder.verify(cache).setKey("foo", "bar");
    inOrder.verify(cache).getValue("foo");
    inOrder.verify(cache).invalidateKeys(Mockito.isNull());
    inOrder.verify(cache).getValue("foo");
    inOrder.verifyNoMoreInteractions();
  }
}
