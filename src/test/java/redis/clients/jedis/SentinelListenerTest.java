package redis.clients.jedis;

import org.junit.Before;
import org.junit.Test;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

public class SentinelListenerTest {

  private static final String MASTER_NAME = "mymasterfailover";

  public static final HostAndPort sentinel1 = HostAndPorts.getSentinelServers().get(2);

  public final Set<String> sentinels = new HashSet<>();

  public final Set<HostAndPort> hostAndPortsSentinels = new HashSet<>();

  @Before
  public void setUp() throws Exception {
    sentinels.clear();
    hostAndPortsSentinels.clear();

    sentinels.add(sentinel1.toString());

    hostAndPortsSentinels.add(sentinel1);
  }

  @Test
  public void testSentinelListener() {
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

    HostAndPort hostPort2 = null;
    for (int i = 0; i < 24; i++) { // timeout time 24*5000
      hostPort2 = pool.getResource().connection.getHostAndPort();

      if (!hostPort2.equals(hostPort1)) {
        break;
      }
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    pool.destroy();
    assertNotNull(hostPort2);
    assertNotEquals(hostPort1, hostPort2);
  }
}
