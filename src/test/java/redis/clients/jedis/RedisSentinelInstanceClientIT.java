package redis.clients.jedis;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.sentinel.api.SentinelInstanceClient;
import redis.clients.jedis.util.JedisSentinelTestUtil;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class RedisSentinelInstanceClientIT {

  private static final String MASTER_NAME = "mymaster";
  private static final String MONITOR_MASTER_NAME = "mymastermonitor";
  private static final String REMOVE_MASTER_NAME = "mymasterremove";
  private static final String FAILOVER_MASTER_NAME = "mymasterfailover";

  protected static EndpointConfig master;
  protected static HostAndPort sentinel;

  protected static HostAndPort sentinelForFailover;

  @BeforeAll
  public static void prepare() {
    master = Endpoints.getRedisEndpoint("standalone2-primary");
    sentinel = Endpoints.getRedisEndpoint("sentinel-standalone2-1").getHostAndPort();
    sentinelForFailover = Endpoints.getRedisEndpoint("sentinel-failover").getHostAndPort();
  }

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
  public void ping() throws Exception {
    try (SentinelInstanceClient client = RedisSentinelInstanceClient.builder()
        .hostAndPort(sentinel.getHost(), sentinel.getPort()).build()) {

      String response = client.ping();
      assertEquals("PONG", response);
    }
  }

  @Test
  public void sentinel() throws Exception {
    try (SentinelInstanceClient client = RedisSentinelInstanceClient.builder()
        .hostAndPort(sentinel.getHost(), sentinel.getPort()).build()) {

      List<Map<String, String>> masters = client.sentinelMasters();

      boolean inMasters = false;
      for (Map<String, String> master : masters)
        if (MASTER_NAME.equals(master.get("name"))) inMasters = true;

      assertTrue(inMasters);

      List<String> masterHostAndPort = client.sentinelGetMasterAddrByName(MASTER_NAME);
      HostAndPort masterFromSentinel = new HostAndPort(masterHostAndPort.get(0),
          Integer.parseInt(masterHostAndPort.get(1)));
      assertEquals(master.getPort(), masterFromSentinel.getPort());

      List<Map<String, String>> slaves = client.sentinelReplicas(MASTER_NAME);
      await().atMost(Duration.ofSeconds(30))
          .until(() -> !client.sentinelReplicas(MASTER_NAME).isEmpty());
      assertEquals(master.getPort(), Integer.parseInt(slaves.get(0).get("master-port")));

      // RESET TAKES SOME TIME TO... RESET
      assertEquals(Long.valueOf(1), client.sentinelReset(MASTER_NAME));
      assertEquals(Long.valueOf(0), client.sentinelReset("woof" + MASTER_NAME));

      Awaitility.await().atMost(Duration.ofSeconds(30))
          .until(() -> !client.sentinelReplicas(MASTER_NAME).isEmpty());
    }
  }

  @Test
  public void sentinelFailover() throws Exception {
    try (SentinelInstanceClient client = RedisSentinelInstanceClient.builder()
        .hostAndPort(sentinelForFailover.getHost(), sentinelForFailover.getPort()).build()) {

      Jedis j = new Jedis(sentinelForFailover);
      Jedis j2 = new Jedis(sentinelForFailover);

      try {
        List<String> masterHostAndPort = client.sentinelGetMasterAddrByName(FAILOVER_MASTER_NAME);
        HostAndPort currentMaster = new HostAndPort(masterHostAndPort.get(0),
            Integer.parseInt(masterHostAndPort.get(1)));

        JedisSentinelTestUtil.waitForNewPromotedMaster(FAILOVER_MASTER_NAME, j, j2);

        masterHostAndPort = client.sentinelGetMasterAddrByName(FAILOVER_MASTER_NAME);
        HostAndPort newMaster = new HostAndPort(masterHostAndPort.get(0),
            Integer.parseInt(masterHostAndPort.get(1)));

        assertNotEquals(newMaster, currentMaster);
      } finally {
        j.close();
        j2.close();
      }
    }
  }

  @Test
  public void sentinelMonitor() throws Exception {
    try (SentinelInstanceClient client = RedisSentinelInstanceClient.builder()
        .hostAndPort(sentinel.getHost(), sentinel.getPort()).build()) {

      // monitor new master
      String result = client.sentinelMonitor(MONITOR_MASTER_NAME, master.getHost(),
        master.getPort(), 1);
      assertEquals("OK", result);

      // already monitored
      try {
        client.sentinelMonitor(MONITOR_MASTER_NAME, master.getHost(), master.getPort(), 1);
        fail();
      } catch (JedisDataException e) {
        // pass
      }
    }
  }

  @Test
  public void sentinelRemove() throws Exception {
    try (SentinelInstanceClient client = RedisSentinelInstanceClient.builder()
        .hostAndPort(sentinel.getHost(), sentinel.getPort()).build()) {

      ensureMonitored(sentinel, REMOVE_MASTER_NAME, master.getHost(), master.getPort(), 1);

      String result = client.sentinelRemove(REMOVE_MASTER_NAME);
      assertEquals("OK", result);

      // not exist
      try {
        result = client.sentinelRemove(REMOVE_MASTER_NAME);
        assertNotEquals("OK", result);
        fail();
      } catch (JedisDataException e) {
        // pass
      }
    }
  }

  @Test
  public void sentinelSet() throws Exception {
    try (SentinelInstanceClient client = RedisSentinelInstanceClient.builder()
        .hostAndPort(sentinel.getHost(), sentinel.getPort()).build()) {

      Map<String, String> parameterMap = new HashMap<String, String>();
      parameterMap.put("down-after-milliseconds", String.valueOf(1234));
      parameterMap.put("parallel-syncs", String.valueOf(3));
      parameterMap.put("quorum", String.valueOf(2));
      client.sentinelSet(MASTER_NAME, parameterMap);

      List<Map<String, String>> masters = client.sentinelMasters();
      for (Map<String, String> master : masters) {
        if (master.get("name").equals(MASTER_NAME)) {
          assertEquals(1234, Integer.parseInt(master.get("down-after-milliseconds")));
          assertEquals(3, Integer.parseInt(master.get("parallel-syncs")));
          assertEquals(2, Integer.parseInt(master.get("quorum")));
        }
      }

      parameterMap.put("quorum", String.valueOf(1));
      client.sentinelSet(MASTER_NAME, parameterMap);
    }
  }

  @Test
  public void sentinelMyId() throws Exception {
    try (SentinelInstanceClient client = RedisSentinelInstanceClient.builder()
        .hostAndPort(sentinel.getHost(), sentinel.getPort()).build()) {

      String myId = client.sentinelMyId();
      assertNotNull(myId);
      assertFalse(myId.isEmpty());
      // Sentinel ID should be a 40-character hex string
      assertEquals(40, myId.length());
    }
  }

  @Test
  public void sentinelMaster() throws Exception {
    try (SentinelInstanceClient client = RedisSentinelInstanceClient.builder()
        .hostAndPort(sentinel.getHost(), sentinel.getPort()).build()) {

      Map<String, String> masterInfo = client.sentinelMaster(MASTER_NAME);
      assertNotNull(masterInfo);
      assertFalse(masterInfo.isEmpty());
      assertEquals(MASTER_NAME, masterInfo.get("name"));
      assertNotNull(masterInfo.get("ip"));
      assertNotNull(masterInfo.get("port"));
      assertEquals(String.valueOf(master.getPort()), masterInfo.get("port"));
    }
  }

  @Test
  public void sentinelSentinels() throws Exception {
    try (SentinelInstanceClient client = RedisSentinelInstanceClient.builder()
        .hostAndPort(sentinel.getHost(), sentinel.getPort()).build()) {

      List<Map<String, String>> sentinels = client.sentinelSentinels(MASTER_NAME);
      assertNotNull(sentinels);
      // May be empty if only one sentinel is configured, or non-empty if multiple sentinels
      // Just verify the call works and returns a list
      for (Map<String, String> sentinelInfo : sentinels) {
        assertNotNull(sentinelInfo.get("ip"));
        assertNotNull(sentinelInfo.get("port"));
      }
    }
  }

  private void ensureMonitored(HostAndPort sentinel, String masterName, String ip, int port,
      int quorum) {

    try (SentinelInstanceClient client = RedisSentinelInstanceClient.builder()
        .hostAndPort(sentinel.getHost(), sentinel.getPort()).build()) {
      client.sentinelMonitor(masterName, ip, port, quorum);
    } catch (JedisDataException e) {
      // ignore
    } catch (Exception e) {
      // ignore
    }
  }

  private void ensureRemoved(String masterName) {
    try (SentinelInstanceClient client = RedisSentinelInstanceClient.builder()
        .hostAndPort(sentinel.getHost(), sentinel.getPort()).build()) {
      client.sentinelRemove(masterName);
    } catch (JedisDataException e) {
      // ignore
    } catch (Exception e) {
      // ignore
    }
  }

}
