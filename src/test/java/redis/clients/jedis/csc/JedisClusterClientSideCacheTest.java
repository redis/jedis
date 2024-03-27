package redis.clients.jedis.csc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.hamcrest.Matchers;
import org.junit.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisClusterTestBase;

public class JedisClusterClientSideCacheTest extends JedisClusterTestBase {

  private static final Set<HostAndPort> hnp = Arrays.asList(nodeInfo1, nodeInfo2, nodeInfo3).stream().collect(Collectors.toSet());

  private static final Supplier<JedisClientConfig> clientConfig
      = () -> DefaultJedisClientConfig.builder().resp3().password("cluster").build();

  private static final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig
      = () -> {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(1);
        return poolConfig;
      };

  @Test
  public void simple() {
    try (JedisCluster jedis = new JedisCluster(hnp, clientConfig.get(), new MapClientSideCache())) {
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      jedis.del("foo");
      assertThat(jedis.get("foo"), Matchers.oneOf("bar", null)); // ?
    }
  }

  @Test
  public void simpleWithSimpleMap() {
    HashMap<Long, Object> map = new HashMap<>();
    try (JedisCluster jedis = new JedisCluster(hnp, clientConfig.get(), new MapClientSideCache(map),
        singleConnectionPoolConfig.get())) {
      jedis.set("foo", "bar");
      assertThat(map, Matchers.aMapWithSize(0));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(1));
      jedis.del("foo");
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
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      jedis.flushAll();
      assertThat(jedis.get("foo"), Matchers.oneOf("bar", null)); // ?
    }
  }

  @Test
  public void flushAllWithSimpleMap() {
    HashMap<Long, Object> map = new HashMap<>();
    try (JedisCluster jedis = new JedisCluster(hnp, clientConfig.get(), new MapClientSideCache(map),
        singleConnectionPoolConfig.get())) {
      jedis.set("foo", "bar");
      assertThat(map, Matchers.aMapWithSize(0));
      assertEquals("bar", jedis.get("foo"));
      assertThat(map, Matchers.aMapWithSize(1));
      jedis.flushAll();
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
