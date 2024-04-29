package redis.clients.jedis.csc;

import java.util.ArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
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

public class ClientSideCacheFunctionalityTest {

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
  public void flushEntireCache() {
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    HashMap<Long, Object> map = new HashMap<>();
    ClientSideCache clientSideCache = new MapClientSideCache(map);
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), clientSideCache)) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
      }
    }

    assertEquals(count, map.size());
    clientSideCache.clear();
    assertEquals(0, map.size());
  }

  @Test
  public void removeSpecificKey() {
    int count = 1000;
    for (int i = 0; i < count; i++) {
      control.set("k" + i, "v" + i);
    }

    // By using LinkedHashMap, we can get the hashes (map keys) at the same order of the actual keys.
    LinkedHashMap<Long, Object> map = new LinkedHashMap<>();
    ClientSideCache clientSideCache = new MapClientSideCache(map);
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), clientSideCache)) {
      for (int i = 0; i < count; i++) {
        jedis.get("k" + i);
      }
    }

    ArrayList<Long> commandHashes = new ArrayList<>(map.keySet());
    assertEquals(count, map.size());
    for (int i = 0; i < count; i++) {
      String key = "k" + i;
      Long hash = commandHashes.get(i);
      assertTrue(map.containsKey(hash));
      clientSideCache.removeKey(key);
      assertFalse(map.containsKey(hash));
    }
  }

  @Test
  public void multiKeyOperation() {
    control.set("k1", "v1");
    control.set("k2", "v2");

    HashMap<Long, Object> map = new HashMap<>();
    try (JedisPooled jedis = new JedisPooled(hnp, clientConfig.get(), new MapClientSideCache(map))) {
      jedis.mget("k1", "k2");
      assertEquals(1, map.size());
    }
  }

}
