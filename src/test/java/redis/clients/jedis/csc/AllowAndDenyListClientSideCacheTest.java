package redis.clients.jedis.csc;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.function.Supplier;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.HostAndPorts;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.csc.util.AllowAndDenyListWithStringKeys;

public class AllowAndDenyListClientSideCacheTest {

  protected static final HostAndPort hnp = HostAndPorts.getRedisServers().get(1);

  protected Jedis control;

  @Before
  public void setUp() throws Exception {
    control = new Jedis(hnp, DefaultJedisClientConfig.builder().password("foobared").build());
    control.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    control.close();
  }

  private static final Supplier<JedisClientConfig> clientConfig
      = () -> DefaultJedisClientConfig.builder().resp3().password("foobared").build();

  private static final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig
      = () -> {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(1);
        return poolConfig;
      };

  @Test
  public void none() {
    HashMap<Long, Object> map = new HashMap<>();
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
    HashMap<Long, Object> map = new HashMap<>();
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
    HashMap<Long, Object> map = new HashMap<>();
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
    HashMap<Long, Object> map = new HashMap<>();
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
    HashMap<Long, Object> map = new HashMap<>();
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
