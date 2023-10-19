package redis.clients.jedis;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import redis.clients.jedis.providers.SentineledConnectionProvider;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * In order to simulate the scenario of active/standby switching. this test case will effect all the
 * sentinel test case, you can run this test case separately ZSentinelMasterListenerTest start with
 * "Z" to ensure this test case should be run as last one
 */
public class ZSentinelMasterListenerTest {
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
  public void testSentinelMasterListener() {
    // case : subscribe on ,active on
    SentinelPoolConfig config = new SentinelPoolConfig();
    config.setEnableActiveDetectListener(true);
    config.setEnableDefaultSubscribeListener(true);
    config.setActiveDetectIntervalTimeMillis(5 * 1000);
    config.setSubscribeRetryWaitTimeMillis(5 * 1000);

    // get resource by pool
    JedisSentinelPool pool = new JedisSentinelPool(MASTER_NAME, sentinels, config, 1000, "foobared",
        2);
    HostAndPort masterGetFromPool = pool.getResource().connection.getHostAndPort();

    // get resource by connection provider
    SentineledConnectionProvider sentineledConnectionProvider = new SentineledConnectionProvider(
        MASTER_NAME, DefaultJedisClientConfig.builder().timeoutMillis(1000).password("foobared")
            .database(2).build(),
        config, hostAndPortsSentinels, DefaultJedisClientConfig.builder().build());
    JedisSentineled jedisSentineled = new JedisSentineled(sentineledConnectionProvider);
    HostAndPort masterGetFromProvider = jedisSentineled.provider.getConnection().getHostAndPort();

    assertEquals(masterGetFromPool, masterGetFromProvider);

    // failover
    Jedis sentinel = new Jedis(sentinel1);
    sentinel.sendCommand(Protocol.Command.SENTINEL, "failover", MASTER_NAME);

    try {
      Thread.sleep(WAIT_FAILOVER_TIMES_MILLS);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

    HostAndPort masterGetFromPool2 = pool.getResource().connection.getHostAndPort();
    HostAndPort masterGetFromProvider2 = jedisSentineled.provider.getConnection().getHostAndPort();

    pool.destroy();
    assertEquals(masterGetFromPool2, masterGetFromPool2);
    assertNotEquals(masterGetFromPool, masterGetFromPool2);
    assertNotEquals(masterGetFromProvider, masterGetFromProvider2);
  }
}
