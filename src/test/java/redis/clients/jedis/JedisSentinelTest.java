package redis.clients.jedis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.util.JedisSentinelTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@Tag("integration")
public class JedisSentinelTest {

  private static final String MASTER_NAME = "mymaster";
  private static final String MONITOR_MASTER_NAME = "mymastermonitor";
  private static final String REMOVE_MASTER_NAME = "mymasterremove";
  private static final String FAILOVER_MASTER_NAME = "mymasterfailover";

  protected static EndpointConfig master = Endpoints.getRedisEndpoint("standalone2-primary");
  protected static HostAndPort sentinel = Endpoints.getRedisEndpoint("sentinel-standalone2-1").getHostAndPort();

  protected static HostAndPort sentinelForFailover = Endpoints.getRedisEndpoint("sentinel-failover").getHostAndPort();

  @BeforeEach
  public void setup() throws InterruptedException {
  }

  @AfterEach
  public void clear() throws InterruptedException {
    // New Sentinel (after 2.8.1)
    // when slave promoted to master (slave of no one), New Sentinel force
    // to restore it (demote)
    // so, promote(slaveof) slave to master has no effect, not same to old
    // Sentinel's behavior
    ensureRemoved(MONITOR_MASTER_NAME);
    ensureRemoved(REMOVE_MASTER_NAME);
  }

  @Test
  public void sentinel() {
    Jedis j = new Jedis(sentinel);

    try {
      List<Map<String, String>> masters = j.sentinelMasters();

      boolean inMasters = false;
      for (Map<String, String> master : masters)
        if (MASTER_NAME.equals(master.get("name"))) inMasters = true;

      assertTrue(inMasters);

      List<String> masterHostAndPort = j.sentinelGetMasterAddrByName(MASTER_NAME);
      HostAndPort masterFromSentinel = new HostAndPort(masterHostAndPort.get(0),
          Integer.parseInt(masterHostAndPort.get(1)));
      assertEquals(master.getPort(), masterFromSentinel.getPort());

      List<Map<String, String>> slaves = j.sentinelReplicas(MASTER_NAME);
      assertFalse(slaves.isEmpty());
      assertEquals(master.getPort(), Integer.parseInt(slaves.get(0).get("master-port")));

      // DO NOT RE-RUN TEST TOO FAST, RESET TAKES SOME TIME TO... RESET
      assertEquals(Long.valueOf(1), j.sentinelReset(MASTER_NAME));
      assertEquals(Long.valueOf(0), j.sentinelReset("woof" + MASTER_NAME));
    } finally {
      j.close();
    }
  }

  @Test
  public void sentinelFailover() throws InterruptedException {
    Jedis j = new Jedis(sentinelForFailover);
    Jedis j2 = new Jedis(sentinelForFailover);

    try {
      List<String> masterHostAndPort = j.sentinelGetMasterAddrByName(FAILOVER_MASTER_NAME);
      HostAndPort currentMaster = new HostAndPort(masterHostAndPort.get(0),
          Integer.parseInt(masterHostAndPort.get(1)));

      JedisSentinelTestUtil.waitForNewPromotedMaster(FAILOVER_MASTER_NAME, j, j2);

      masterHostAndPort = j.sentinelGetMasterAddrByName(FAILOVER_MASTER_NAME);
      HostAndPort newMaster = new HostAndPort(masterHostAndPort.get(0),
          Integer.parseInt(masterHostAndPort.get(1)));

      assertNotEquals(newMaster, currentMaster);
    } finally {
      j.close();
      j2.close();
    }
  }

  @Test
  public void sentinelMonitor() {
    Jedis j = new Jedis(sentinel);

    try {
      // monitor new master
      String result = j.sentinelMonitor(MONITOR_MASTER_NAME, master.getHost(), master.getPort(), 1);
      assertEquals("OK", result);

      // already monitored
      try {
        j.sentinelMonitor(MONITOR_MASTER_NAME, master.getHost(), master.getPort(), 1);
        fail();
      } catch (JedisDataException e) {
        // pass
      }
    } finally {
      j.close();
    }
  }

  @Test
  public void sentinelRemove() {
    Jedis j = new Jedis(sentinel);

    try {
      ensureMonitored(sentinel, REMOVE_MASTER_NAME, master.getHost(), master.getPort(), 1);

      String result = j.sentinelRemove(REMOVE_MASTER_NAME);
      assertEquals("OK", result);

      // not exist
      try {
        result = j.sentinelRemove(REMOVE_MASTER_NAME);
        assertNotEquals("OK", result);
        fail();
      } catch (JedisDataException e) {
        // pass
      }
    } finally {
      j.close();
    }
  }

  @Test
  public void sentinelSet() {
    Jedis j = new Jedis(sentinel);

    try {
      Map<String, String> parameterMap = new HashMap<String, String>();
      parameterMap.put("down-after-milliseconds", String.valueOf(1234));
      parameterMap.put("parallel-syncs", String.valueOf(3));
      parameterMap.put("quorum", String.valueOf(2));
      j.sentinelSet(MASTER_NAME, parameterMap);

      List<Map<String, String>> masters = j.sentinelMasters();
      for (Map<String, String> master : masters) {
        if (master.get("name").equals(MASTER_NAME)) {
          assertEquals(1234, Integer.parseInt(master.get("down-after-milliseconds")));
          assertEquals(3, Integer.parseInt(master.get("parallel-syncs")));
          assertEquals(2, Integer.parseInt(master.get("quorum")));
        }
      }

      parameterMap.put("quorum", String.valueOf(1));
      j.sentinelSet(MASTER_NAME, parameterMap);
    } finally {
      j.close();
    }
  }

  private void ensureMonitored(HostAndPort sentinel, String masterName, String ip, int port,
      int quorum) {

    try (Jedis j = new Jedis(sentinel)) {
      j.sentinelMonitor(masterName, ip, port, quorum);
    } catch (JedisDataException e) {
      // ignore
    }
  }

  private void ensureRemoved(String masterName) {
    Jedis j = new Jedis(sentinel);
    try {
      j.sentinelRemove(masterName);
    } catch (JedisDataException e) {
    } finally {
      j.close();
    }
  }

}
