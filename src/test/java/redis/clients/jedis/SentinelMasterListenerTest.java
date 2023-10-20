package redis.clients.jedis;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.util.JedisSentinelTestUtil;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotEquals;

/**
 * In order to simulate the scenario of active/standby switching. this test case will effect all the
 * sentinel test case, you can run this test case separately ZSentinelMasterListenerTest start with
 * "Z" to ensure this test case should be run as last one
 */
public class SentinelMasterListenerTest {

  private static final String FAILOVER_MASTER_NAME = "mymasterfailover";

  public final Set<String> sentinels = new HashSet<>();
  protected static HostAndPort sentinelForFailover = HostAndPorts.getSentinelServers().get(2);

  @Before
  public void setUp() throws Exception {
    sentinels.clear();
    sentinels.add(sentinelForFailover.toString());
  }

  @Test
  public void testSentinelMasterListener() throws InterruptedException {

    Jedis j = new Jedis(sentinelForFailover);
    Jedis j2 = new Jedis(sentinelForFailover);

    SentinelPoolConfig config = new SentinelPoolConfig();
    config.setEnableActiveDetectListener(true);
    config.setEnableDefaultSubscribeListener(true);
    config.setActiveDetectIntervalTimeMillis(5 * 1000);
    config.setSubscribeRetryWaitTimeMillis(5 * 1000);

    JedisSentinelPool pool = new JedisSentinelPool(FAILOVER_MASTER_NAME, sentinels, config, 1000,
        "foobared", 2);

    try {
      HostAndPort masterGetFromPoolBefore = pool.getResource().connection.getHostAndPort();

      JedisSentinelTestUtil.waitForNewPromotedMaster(FAILOVER_MASTER_NAME, j, j2);

      HostAndPort masterGetFromPoolCurrent = pool.getResource().connection.getHostAndPort();

      assertNotEquals(masterGetFromPoolBefore, masterGetFromPoolCurrent);
    } finally {
      j.close();
      j2.close();
      pool.destroy();
    }
  }

}
