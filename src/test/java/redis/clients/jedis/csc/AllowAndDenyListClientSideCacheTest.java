package redis.clients.jedis.csc;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import org.hamcrest.Matchers;
import org.junit.Test;

import redis.clients.jedis.CommandObject;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.csc.util.AllowAndDenyListWithStringKeys;

public class AllowAndDenyListClientSideCacheTest extends ClientSideCacheTestBase {

  @Test
  public void none() {
    HashMap<CommandObject, Object> map = new HashMap<>();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        new MapClientSideCache(map, new AllowAndDenyListWithStringKeys(null, null, null, null)),
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertThat(map, Matchers.aMapWithSize(0));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(1));
    }
  }

  @Test
  public void whiteListCommand() {
    HashMap<CommandObject, Object> map = new HashMap<>();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        new MapClientSideCache(map, new AllowAndDenyListWithStringKeys(singleton(Protocol.Command.GET), null, null, null)),
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertThat(map, Matchers.aMapWithSize(0));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(1));
    }
  }

  @Test
  public void blackListCommand() {
    HashMap<CommandObject, Object> map = new HashMap<>();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        new MapClientSideCache(map, new AllowAndDenyListWithStringKeys(null, singleton(Protocol.Command.GET), null, null)),
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertThat(map, Matchers.aMapWithSize(0));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
    }
  }

  @Test
  public void whiteListKey() {
    HashMap<CommandObject, Object> map = new HashMap<>();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        new MapClientSideCache(map, new AllowAndDenyListWithStringKeys(null, null, singleton("foo"), null)),
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertThat(map, Matchers.aMapWithSize(0));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(1));
    }
  }

  @Test
  public void blackListKey() {
    HashMap<CommandObject, Object> map = new HashMap<>();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(),
        new MapClientSideCache(map, new AllowAndDenyListWithStringKeys(null, null, null, singleton("foo"))),
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertThat(map, Matchers.aMapWithSize(0));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
    }
  }
}
