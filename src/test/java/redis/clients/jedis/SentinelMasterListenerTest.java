package redis.clients.jedis;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import redis.clients.jedis.providers.SentineledConnectionProvider;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotEquals;

/**
 * In order to simulate the scenario of active/standby switching. this test case will effect all the
 * sentinel test case, you can run this test case separately
 */
@Ignore
public class SentinelMasterListenerTest {
  private static final String MASTER_NAME = "mymaster";

  public static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(0);
  public static final HostAndPort sentinel2 = HostAndPorts.getSentinelServers().get(1);

  public final Set<String> sentinels = new HashSet<>();

  public final Set<HostAndPort> hostAndPortsSentinels = new HashSet<>();

  private static final long WAIT_FAILOVER_TIMES_MILLS = 10000;

  @Before
  public void setUp() throws Exception {
    sentinels.clear();
    hostAndPortsSentinels.clear();

    sentinels.add(sentinel1.toString());
    sentinels.add(sentinel2.toString());

    hostAndPortsSentinels.add(sentinel1);
    hostAndPortsSentinels.add(sentinel2);
  }

  @Test
  public void testSentinelMasterSubscribeListener() {
    // case 1: default : subscribe on ,active off
    SentinelPoolConfig config = new SentinelPoolConfig();

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000, "foobared",
        2);
    HostAndPort hostPort1 = pool.getResource().connection.getHostAndPort();

    Jedis sentinel = new Jedis(sentinel1);
    sentinel.sendCommand(Protocol.Command.SENTINEL, "failover", MASTER_NAME);
    try {
      Thread.sleep(WAIT_FAILOVER_TIMES_MILLS); // sleep. let the failover finish
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    HostAndPort hostPort2 = pool.getResource().connection.getHostAndPort();

    pool.destroy();
    assertNotEquals(hostPort1, hostPort2);
  }

  @Test
  public void testSentinelMasterActiveDetectListener() {
    // case 2: subscribe off ,active on
    SentinelPoolConfig config = new SentinelPoolConfig();
    config.setEnableActiveDetectListener(true);
    config.setEnableDefaultSubscribeListener(false);

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000, "foobared",
        2);
    HostAndPort hostPort1 = pool.getResource().connection.getHostAndPort();

    Jedis sentinel = new Jedis(sentinel1);
    sentinel.sendCommand(Protocol.Command.SENTINEL, "failover", MASTER_NAME);
    try {
      Thread.sleep(WAIT_FAILOVER_TIMES_MILLS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    HostAndPort hostPort2 = pool.getResource().connection.getHostAndPort();

    pool.destroy();
    assertNotEquals(hostPort1, hostPort2);
  }

  @Test
  public void testALLSentinelMasterListener() {
    // case 2: subscribe on ,active on
    SentinelPoolConfig config = new SentinelPoolConfig();
    config.setEnableActiveDetectListener(true);
    config.setEnableDefaultSubscribeListener(true);
    config.setActiveDetectIntervalTimeMillis(5 * 1000);
    config.setSubscribeRetryWaitTimeMillis(5 * 1000);

    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000, "foobared",
        2);
    HostAndPort hostPort1 = pool.getResource().connection.getHostAndPort();

    Jedis sentinel = new Jedis(sentinel1);
    sentinel.sendCommand(Protocol.Command.SENTINEL, "failover", MASTER_NAME);
    try {
      Thread.sleep(WAIT_FAILOVER_TIMES_MILLS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    HostAndPort hostPort2 = pool.getResource().connection.getHostAndPort();

    pool.destroy();
    assertNotEquals(hostPort1, hostPort2);
  }

  @Test
  public void testSentinelMasterSubscribeListenerForSentineledConnectionProvider() {
    SentinelPoolConfig config = new SentinelPoolConfig();

    SentineledConnectionProvider sentineledConnectionProvider = new SentineledConnectionProvider(
        MASTER_NAME, DefaultJedisClientConfig.builder().timeoutMillis(1000).password("foobared")
            .database(2).build(),
        config, hostAndPortsSentinels, DefaultJedisClientConfig.builder().build());

    try (JedisSentineled jedis = new JedisSentineled(sentineledConnectionProvider)) {

      HostAndPort master1 = jedis.provider.getConnection().getHostAndPort();
      Jedis sentinel = new Jedis(sentinel1);
      sentinel.sendCommand(Protocol.Command.SENTINEL, "failover", MASTER_NAME);
      try {
        Thread.sleep(WAIT_FAILOVER_TIMES_MILLS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      HostAndPort master2 = jedis.provider.getConnection().getHostAndPort();

      assertNotEquals(master1, master2);
    }
  }

  @Test
  public void testSentinelMasterActiveDetectListenerForSentineledConnectionProvider() {
    SentinelPoolConfig config = new SentinelPoolConfig();
    config.setEnableActiveDetectListener(true);
    config.setEnableDefaultSubscribeListener(false);

    SentineledConnectionProvider sentineledConnectionProvider = new SentineledConnectionProvider(
        MASTER_NAME, DefaultJedisClientConfig.builder().timeoutMillis(1000).password("foobared")
            .database(2).build(),
        config, hostAndPortsSentinels, DefaultJedisClientConfig.builder().build());

    try (JedisSentineled jedis = new JedisSentineled(sentineledConnectionProvider)) {

      HostAndPort master1 = jedis.provider.getConnection().getHostAndPort();
      Jedis sentinel = new Jedis(sentinel1);
      sentinel.sendCommand(Protocol.Command.SENTINEL, "failover", MASTER_NAME);
      try {
        Thread.sleep(WAIT_FAILOVER_TIMES_MILLS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      HostAndPort master2 = jedis.provider.getConnection().getHostAndPort();

      assertNotEquals(master1, master2);
    }
  }

  @Test
  public void testALLSentinelMasterListenerForSentineledConnectionProvider() {
    SentinelPoolConfig config = new SentinelPoolConfig();
    config.setEnableActiveDetectListener(true);
    config.setEnableDefaultSubscribeListener(true);
    config.setActiveDetectIntervalTimeMillis(5 * 1000);
    config.setSubscribeRetryWaitTimeMillis(5 * 1000);

    SentineledConnectionProvider sentineledConnectionProvider = new SentineledConnectionProvider(
        MASTER_NAME, DefaultJedisClientConfig.builder().timeoutMillis(1000).password("foobared")
            .database(2).build(),
        config, hostAndPortsSentinels, DefaultJedisClientConfig.builder().build());

    try (JedisSentineled jedis = new JedisSentineled(sentineledConnectionProvider)) {

      HostAndPort master1 = jedis.provider.getConnection().getHostAndPort();
      Jedis sentinel = new Jedis(sentinel1);
      sentinel.sendCommand(Protocol.Command.SENTINEL, "failover", MASTER_NAME);
      try {
        Thread.sleep(WAIT_FAILOVER_TIMES_MILLS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      HostAndPort master2 = jedis.provider.getConnection().getHostAndPort();

      assertNotEquals(master1, master2);
    }
  }
}
