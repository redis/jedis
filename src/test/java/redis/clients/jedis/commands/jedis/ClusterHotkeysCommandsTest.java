package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import io.redis.test.annotations.ConditionalOnEnv;
import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.args.HotkeysMetric;
import redis.clients.jedis.params.HotkeysParams;
import redis.clients.jedis.resps.HotkeysInfo;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.TestEnvUtil;

/**
 * Tests that HOTKEYS commands are not supported in cluster mode.
 * <p>
 * The HOTKEYS command is a node-local operation that tracks hot keys on a single Redis instance. In
 * a Redis Cluster, keys are distributed across multiple nodes, and there is no built-in mechanism
 * to aggregate hotkeys data across all nodes. Therefore, HOTKEYS commands are intentionally
 * disabled in cluster mode to avoid confusion and incorrect results.
 * <p>
 * Users who need hotkeys functionality in a cluster environment should connect directly to
 * individual nodes and run HOTKEYS commands on each node separately.
 */
@Tag("integration")
@EnabledOnCommand("HOTKEYS")
@ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_DOCKER, enabled = true)
public class ClusterHotkeysCommandsTest extends ClusterJedisCommandsTestBase {

  @Test
  public void hotkeysStartNotSupportedInCluster() {
    assertThrows(UnsupportedOperationException.class,
      () -> cluster.hotkeysStart(HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU)));
  }

  @Test
  public void hotkeysStopNotSupportedInCluster() {
    assertThrows(UnsupportedOperationException.class, () -> cluster.hotkeysStop());
  }

  @Test
  public void hotkeysResetNotSupportedInCluster() {
    assertThrows(UnsupportedOperationException.class, () -> cluster.hotkeysReset());
  }

  @Test
  public void hotkeysGetNotSupportedInCluster() {
    assertThrows(UnsupportedOperationException.class, () -> cluster.hotkeysGet());
  }

  // Test slots - consecutive slots (server groups them as a range) and a non-consecutive slot
  private static final int SLOT_0 = 0;
  private static final int SLOT_1 = 1;
  private static final int SLOT_2 = 2;
  private static final int SLOT_100 = 100; // Non-consecutive slot to test multiple ranges

  // Keys with hash tags that hash to slots 0, 1, 2
  // The hash tag content was found by iterating JedisClusterCRC16.getSlot()
  private static final String KEY_SLOT_0 = "key{3560}"; // {3560} hashes to slot 0
  private static final String KEY_SLOT_1 = "key{22179}"; // {22179} hashes to slot 1
  private static final String KEY_SLOT_2 = "key{48756}"; // {48756} hashes to slot 2

  /**
   * Tests HOTKEYS with SLOTS parameter by connecting directly to a single cluster node using a
   * standalone RedisClient. Verifies all response fields are correctly parsed.
   */
  @Test
  public void hotkeysWithSlotsOnSingleClusterNode() {
    // Verify our pre-computed keys hash to the expected slots
    assertEquals(SLOT_0, JedisClusterCRC16.getSlot(KEY_SLOT_0));
    assertEquals(SLOT_1, JedisClusterCRC16.getSlot(KEY_SLOT_1));
    assertEquals(SLOT_2, JedisClusterCRC16.getSlot(KEY_SLOT_2));

    HostAndPort nodeHostAndPort = endpoint.getHostsAndPorts().get(0);

    try (RedisClient client = RedisClient.builder().hostAndPort(nodeHostAndPort)
        .clientConfig(endpoint.getClientConfigBuilder().build()).build()) {

      // Clean up any previous state
      client.hotkeysStop();
      client.hotkeysReset();

      // Start hotkeys tracking with consecutive slots (0, 1, 2) and a non-consecutive slot (100)
      // Server should group consecutive slots into a range and keep non-consecutive as single slot
      String result = client
          .hotkeysStart(HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU, HotkeysMetric.NET)
              .sample(2).slots(SLOT_0, SLOT_1, SLOT_2, SLOT_100));
      assertEquals("OK", result);

      // Generate traffic on keys that hash to slots 0, 1, 2
      String[] keys = { KEY_SLOT_0, KEY_SLOT_1, KEY_SLOT_2 };
      for (int i = 0; i < 50; i++) {
        for (String key : keys) {
          client.set(key, "value" + i);
          client.get(key);
        }
      }

      HotkeysInfo info = client.hotkeysGet();
      assertNotNull(info);

      // Verify tracking state
      assertTrue(info.isTrackingActive());
      assertEquals(2, info.getSampleRatio());

      // Verify selected slots - should have 2 entries:
      // 1. Range [0, 2] for consecutive slots 0, 1, 2
      // 2. Single slot [100] for non-consecutive slot
      List<int[]> selectedSlots = info.getSelectedSlots();
      assertNotNull(selectedSlots);
      assertEquals(2, selectedSlots.size());
      // First entry: range [0, 2]
      assertEquals(2, selectedSlots.get(0).length);
      assertEquals(SLOT_0, selectedSlots.get(0)[0]);
      assertEquals(SLOT_2, selectedSlots.get(0)[1]);
      // Second entry: single slot [100]
      assertEquals(1, selectedSlots.get(1).length);
      assertEquals(SLOT_100, selectedSlots.get(1)[0]);

      // Verify slot-specific CPU metrics (only present when SLOTS is used)
      assertNotNull(info.getSampledCommandSelectedSlotsUs());
      assertThat(info.getSampledCommandSelectedSlotsUs(), greaterThan(0L));
      assertNotNull(info.getAllCommandsSelectedSlotsUs());
      assertThat(info.getAllCommandsSelectedSlotsUs(), greaterThan(0L));
      assertThat(info.getAllCommandsAllSlotsUs(), greaterThan(0L));

      // Verify slot-specific network bytes metrics
      assertNotNull(info.getNetBytesSampledCommandsSelectedSlots());
      assertThat(info.getNetBytesSampledCommandsSelectedSlots(), greaterThan(0L));
      assertNotNull(info.getNetBytesAllCommandsSelectedSlots());
      assertThat(info.getNetBytesAllCommandsSelectedSlots(), greaterThan(0L));
      assertThat(info.getNetBytesAllCommandsAllSlots(), greaterThan(0L));

      // Verify timing fields
      assertThat(info.getCollectionStartTimeUnixMs(), greaterThan(0L));
      assertThat(info.getCollectionDurationMs(), greaterThanOrEqualTo(0L));
      assertThat(info.getTotalCpuTimeUserMs(), greaterThanOrEqualTo(0L));
      assertThat(info.getTotalCpuTimeSysMs(), greaterThanOrEqualTo(0L));
      assertThat(info.getTotalNetBytes(), greaterThan(0L));

      // Verify key metrics maps contain our 3 keys with values > 0
      Map<String, Long> byCpuTimeUs = info.getByCpuTimeUs();
      assertNotNull(byCpuTimeUs);
      assertEquals(3, byCpuTimeUs.size());
      assertTrue(byCpuTimeUs.containsKey(KEY_SLOT_0));
      assertTrue(byCpuTimeUs.containsKey(KEY_SLOT_1));
      assertTrue(byCpuTimeUs.containsKey(KEY_SLOT_2));
      assertThat(byCpuTimeUs.get(KEY_SLOT_0), greaterThan(0L));
      assertThat(byCpuTimeUs.get(KEY_SLOT_1), greaterThan(0L));
      assertThat(byCpuTimeUs.get(KEY_SLOT_2), greaterThan(0L));

      Map<String, Long> byNetBytes = info.getByNetBytes();
      assertNotNull(byNetBytes);
      assertEquals(3, byNetBytes.size());
      assertTrue(byNetBytes.containsKey(KEY_SLOT_0));
      assertTrue(byNetBytes.containsKey(KEY_SLOT_1));
      assertTrue(byNetBytes.containsKey(KEY_SLOT_2));
      assertThat(byNetBytes.get(KEY_SLOT_0), greaterThan(0L));
      assertThat(byNetBytes.get(KEY_SLOT_1), greaterThan(0L));
      assertThat(byNetBytes.get(KEY_SLOT_2), greaterThan(0L));

      client.hotkeysStop();
      client.hotkeysReset();
    }
  }
}
