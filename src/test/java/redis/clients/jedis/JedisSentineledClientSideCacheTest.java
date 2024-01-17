package redis.clients.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.hamcrest.Matchers;
import org.junit.Test;

public class JedisSentineledClientSideCacheTest {

  private static final String MASTER_NAME = "mymaster";

  protected static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(1);
  protected static final HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(3);

  private static final Set<HostAndPort> sentinels = Arrays.asList(sentinel1, sentinel2).stream().collect(Collectors.toSet());

  private static final JedisClientConfig masterClientConfig = DefaultJedisClientConfig.builder().resp3().password("foobared").build();

  private static final JedisClientConfig sentinelClientConfig = DefaultJedisClientConfig.builder().resp3().build();

  private static final Supplier<GenericObjectPoolConfig<Connection>> singleConnectionPoolConfig
      = () -> {
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(1);
        return poolConfig;
      };

  @Test
  public void simple() {
    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME, masterClientConfig, new ClientSideCache(), sentinels, sentinelClientConfig)) {
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      jedis.del("foo");
      assertThat(jedis.get("foo"), Matchers.oneOf("bar", null)); // ?
    }
  }

  @Test
  public void simpleWithSimpleMap() {
    HashMap<ByteBuffer, Object> map = new HashMap<>();
    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME, masterClientConfig, new ClientSideCache(map), sentinels, sentinelClientConfig)) {
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
    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME, masterClientConfig, new ClientSideCache(), sentinels, sentinelClientConfig)) {
      jedis.set("foo", "bar");
      assertEquals("bar", jedis.get("foo"));
      jedis.flushAll();
      assertThat(jedis.get("foo"), Matchers.oneOf("bar", null)); // ?
    }
  }

  @Test
  public void flushAllWithSimpleMap() {
    HashMap<ByteBuffer, Object> map = new HashMap<>();
    try (JedisSentineled jedis = new JedisSentineled(MASTER_NAME, masterClientConfig, new ClientSideCache(map), sentinels, sentinelClientConfig)) {
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
