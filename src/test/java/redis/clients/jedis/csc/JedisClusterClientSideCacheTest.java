package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;

public class JedisClusterClientSideCacheTest {

  private static final Set<HostAndPort> hnp = new HashSet<>(HostAndPorts.getStableClusterServers());

  private static final Supplier<JedisClientConfig> clientConfig
      = () -> DefaultJedisClientConfig.builder().resp3().password("cluster").build();

  private static final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig
      = () -> {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(1);
        return poolConfig;
      };

  protected JedisCluster control;

  @Before
  public void setUp() throws Exception {
    control = new JedisCluster(hnp, clientConfig.get());
    control.flushAll();
  }

  @After
  public void tearDown() throws Exception {
    control.close();
  }

  @Test
  public void simple() {
    try (JedisCluster jedis = new JedisCluster(hnp, clientConfig.get(), new MapClientSideCache())) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.del("foo");
      assertThat(jedis.get("foo"), Matchers.oneOf("bar", null)); // ?
    }
  }

  @Test
  public void simpleWithSimpleMap() {
    HashMap<Long, Object> map = new HashMap<>();
    try (JedisCluster jedis = new JedisCluster(hnp, clientConfig.get(), new MapClientSideCache(map),
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertThat(map, Matchers.aMapWithSize(0));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(1));
      control.del("foo");
      assertThat(map, Matchers.aMapWithSize(1));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(1));
      jedis.ping();
      assertThat(map, Matchers.aMapWithSize(0));
      assertNull(jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
    }
  }

  @Test
  public void flushAll() {
    try (JedisCluster jedis = new JedisCluster(hnp, clientConfig.get(), new MapClientSideCache())) {
      control.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      control.flushAll();
      assertThat(jedis.get("foo"), Matchers.oneOf("bar", null)); // ?
    }
  }

  @Test
  public void flushAllWithSimpleMap() {
    HashMap<Long, Object> map = new HashMap<>();
    try (JedisCluster jedis = new JedisCluster(hnp, clientConfig.get(), new MapClientSideCache(map),
        singleConnectionPoolConfig.get())) {
      control.set("foo", "bar");
      assertThat(map, Matchers.aMapWithSize(0));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(1));
      control.flushAll();
      assertThat(map, Matchers.aMapWithSize(1));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(1));
      jedis.ping();
      assertThat(map, Matchers.aMapWithSize(0));
      assertNull(jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(0));
    }
  }
}
