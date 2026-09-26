package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.util.JedisSentinelTestUtil;

@Tag("integration")
public class JedisSentinelPoolTest {

  private static final String MASTER_NAME = "mymaster";

  protected static HostAndPort sentinel1;
  protected static HostAndPort sentinel2;

  protected final Set<String> sentinels = new HashSet<>();

  @BeforeAll
  public static void prepare() {
    sentinel1 = Endpoints.getRedisEndpoint("sentinel-standalone2-1").getHostAndPort();
    sentinel2 = Endpoints.getRedisEndpoint("sentinel-standalone2-3").getHostAndPort();
  }

  @BeforeEach
  public void setUp() throws Exception {
    sentinels.clear();

    sentinels.add(sentinel1.toString());
    sentinels.add(sentinel2.toString());
  }

  @Test
  public void repeatedSentinelPoolInitialization() {

    for (int i = 0; i < 20; ++i) {
      GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();

      JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
          "foobared", 2);
      pool.getResource().close();
      pool.destroy();
    }
  }

  @Test
  public void initializeWithNotAvailableSentinelsShouldThrowException() {
    Set<String> wrongSentinels = new HashSet<String>();
    wrongSentinels.add(new HostAndPort("localhost", 65432).toString());
    wrongSentinels.add(new HostAndPort("localhost", 65431).toString());

    assertThrows(JedisConnectionException.class,
        () -> new JedisSentinelPool(MASTER_NAME, wrongSentinels).close());
  }

  @Test
  public void initializeWithNotMonitoredMasterNameShouldThrowException() {
    final String wrongMasterName = "wrongMasterName";
    assertThrows(JedisException.class, ()-> new JedisSentinelPool(wrongMasterName, sentinels).close());
  }

  @Test
  public void checkCloseableConnections() throws Exception {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "foobared", 2);
    Jedis jedis = pool.getResource();
    jedis.auth("foobared");
    jedis.set("foo", "bar");
    assertEquals("bar", jedis.get("foo"));
    jedis.close();
    pool.close();
    assertTrue(pool.isClosed());
  }

  @Test
  public void returnResourceShouldResetState() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    try (JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "foobared", 2)) {

      Jedis jedis = null;
      try (Jedis jedis1 = pool.getResource()) {
        jedis = jedis1;
        jedis1.set("hello", "jedis");
        Transaction t = jedis1.multi();
        t.set("hello", "world");
      }

      try (Jedis jedis2 = pool.getResource()) {
        assertSame(jedis, jedis2);
        assertEquals("jedis", jedis2.get("hello"));
      }
    }
  }

  @Test
  public void checkResourceIsCloseable() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "foobared", 2);

    Jedis jedis = pool.getResource();
    try {
      jedis.set("hello", "jedis");
    } finally {
      jedis.close();
    }

    Jedis jedis2 = pool.getResource();
    try {
      assertEquals(jedis, jedis2);
    } finally {
      jedis2.close();
    }
  }

  @Test
  public void customClientName() {
    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(false);
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000,
        "foobared", 0, "my_shiny_client_name");

    Jedis jedis = pool.getResource();

    try {
      assertEquals("my_shiny_client_name", jedis.clientGetname());
    } finally {
      jedis.close();
      pool.destroy();
    }

    assertTrue(pool.isClosed());
  }

  @Test
  public void connectionToOldMasterIsNotReusedAfterFailover() throws Exception {
    final String masterName = "mymasterfailover";
    HostAndPort sentinel = Endpoints.getRedisEndpoint("sentinel-failover").getHostAndPort();
    Set<String> failoverSentinels = Collections.singleton(sentinel.toString());

    GenericObjectPoolConfig<Jedis> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    try (JedisSentinelPool pool = new JedisSentinelPool(masterName, failoverSentinels, config, 1000,
        "foobared", 0)) {
      HostAndPort oldMaster = pool.getCurrentHostMaster();

      // Borrowed for the whole failover, so the clear() in initMaster cannot reach it
      Jedis borrowed = pool.getResource();
      assertEquals(oldMaster.getPort(), serverPort(borrowed));

      try (Jedis sentinelJedis = new Jedis(sentinel); Jedis commandJedis = new Jedis(sentinel)) {
        JedisSentinelTestUtil.waitForNewPromotedMaster(masterName, sentinelJedis, commandJedis);
      }
      HostAndPort newMaster = awaitMasterSwitch(pool, oldMaster);
      assertNotEquals(oldMaster, newMaster);

      // Returned after the switch while its socket still points at the demoted node
      borrowed.close();

      try (Jedis reborrowed = pool.getResource()) {
        assertEquals(newMaster, reborrowed.getClient().getHostAndPort());
        assertEquals(newMaster.getPort(), serverPort(reborrowed));
      }

      // Leave the demoted node as a healthy replica for the next failover test
      awaitReplicaReconfigured(sentinel, masterName);
    }
  }

  private static HostAndPort awaitMasterSwitch(JedisSentinelPool pool, HostAndPort oldMaster)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + 10_000;
    while (oldMaster.equals(pool.getCurrentHostMaster())) {
      assertTrue(System.currentTimeMillis() < deadline, "pool did not observe +switch-master");
      Thread.sleep(50);
    }
    return pool.getCurrentHostMaster();
  }

  private static int serverPort(Jedis jedis) {
    for (String line : jedis.info("server").split("\r\n")) {
      if (line.startsWith("tcp_port:")) {
        return Integer.parseInt(line.substring("tcp_port:".length()));
      }
    }
    throw new AssertionError("tcp_port not found in INFO server");
  }

  private static void awaitReplicaReconfigured(HostAndPort sentinel, String masterName)
      throws InterruptedException {
    long deadline = System.currentTimeMillis() + 60_000;
    try (Jedis jedis = new Jedis(sentinel)) {
      while (true) {
        List<Map<String, String>> replicas = jedis.sentinelReplicas(masterName);
        boolean healthy = replicas.stream().anyMatch(
          r -> "slave".equals(r.get("flags")) && "ok".equals(r.get("master-link-status")));
        if (healthy) {
          return;
        }
        assertTrue(System.currentTimeMillis() < deadline, "demoted node did not rejoin as replica");
        Thread.sleep(500);
      }
    }
  }
}
