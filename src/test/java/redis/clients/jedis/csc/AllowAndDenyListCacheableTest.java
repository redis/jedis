package redis.clients.jedis.csc;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.csc.util.AllowAndDenyListWithStringKeys;

public class AllowAndDenyListCacheableTest extends ClientSideCacheTestBase {

  private static Cache createTestCache(Cacheable cacheable) {
    return new TestCache(cacheable);
  }

  @Test
  public void none() {
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        createTestCache(new AllowAndDenyListWithStringKeys(null, null, null, null)))) {
      control.set("foo", "bar");
      assertEquals(0, jedis.getCache().getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, jedis.getCache().getSize());
    }
  }

  @Test
  public void whiteListCommand() {
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        createTestCache(new AllowAndDenyListWithStringKeys(singleton(Protocol.Command.GET), null, null, null)))) {
      control.set("foo", "bar");
      assertEquals(0, jedis.getCache().getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, jedis.getCache().getSize());
    }
  }

  @Test
  public void blackListCommand() {
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        createTestCache(new AllowAndDenyListWithStringKeys(null, singleton(Protocol.Command.GET), null, null)))) {
      control.set("foo", "bar");
      assertEquals(0, jedis.getCache().getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(0, jedis.getCache().getSize());
    }
  }

  @Test
  public void whiteListKey() {
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        createTestCache(new AllowAndDenyListWithStringKeys(null, null, singleton("foo"), null)))) {
      control.set("foo", "bar");
      assertEquals(0, jedis.getCache().getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(1, jedis.getCache().getSize());
    }
  }

  @Test
  public void blackListKey() {
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        createTestCache(new AllowAndDenyListWithStringKeys(null, null, null, singleton("foo"))))) {
      control.set("foo", "bar");
      assertEquals(0, jedis.getCache().getSize());
      assertEquals("bar", jedis.get("foo"));
      assertEquals(0, jedis.getCache().getSize());
    }
  }
}
