package redis.clients.jedis;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.util.JedisSentinelTestUtil;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotEquals;

public class SentinelMasterListenerTest {

  private static HostAndPort sentinelForFailover = HostAndPorts.getSentinelServers().get(5);
  private static final String FAILOVER_MASTER_NAME = "mymasterfailover2";
  private final Set<String> sentinels = new HashSet<>();

  @Before
  public void setUp() throws Exception {
    sentinels.clear();
    sentinels.add(sentinelForFailover.toString());
  }

  @Test
  public void failover() throws InterruptedException {
    Jedis sentinel = new Jedis(sentinelForFailover);

    SentinelPoolConfig config = new SentinelPoolConfig();
    config.setEnableActiveDetectListener(true);
    config.setEnableDefaultSubscribeListener(true);
    config.setActiveDetectIntervalTimeMillis(5 * 1000);
    config.setSubscribeRetryWaitTimeMillis(5 * 1000);

    JedisSentinelPool pool = new JedisSentinelPool(FAILOVER_MASTER_NAME, sentinels, config, 1000,
        "foobared", 2);

    try {
      HostAndPort masterGetFromPoolBefore = pool.getResource().connection.getHostAndPort();

      sentinel.sendCommand(Protocol.Command.SENTINEL, "failover", FAILOVER_MASTER_NAME);

      // ensure fail over
      Thread.sleep(10000);

      HostAndPort masterGetFromPoolCurrent = pool.getResource().connection.getHostAndPort();

      assertNotEquals(masterGetFromPoolBefore, masterGetFromPoolCurrent);
    } finally {
      sentinel.close();
      pool.destroy();
    }
  }

}
