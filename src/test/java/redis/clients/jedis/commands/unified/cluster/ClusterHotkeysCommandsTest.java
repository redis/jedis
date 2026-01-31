package redis.clients.jedis.commands.unified.cluster;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.args.HotkeysMetric;
import redis.clients.jedis.commands.unified.HotkeysCommandsTestBase;
import redis.clients.jedis.params.HotkeysParams;
import redis.clients.jedis.resps.HotkeysInfo;
import redis.clients.jedis.util.JedisClusterCRC16;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@Tag("integration")
public class ClusterHotkeysCommandsTest extends HotkeysCommandsTestBase {

  public ClusterHotkeysCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  protected UnifiedJedis createTestClient() {
    return ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @Test
  public void hotkeysWithSlotsAndAllFields() {
    // Get actual slot numbers for our test keys
    int fooSlot = JedisClusterCRC16.getSlot("foo");
    int barSlot = JedisClusterCRC16.getSlot("bar");
    int bazSlot = JedisClusterCRC16.getSlot("baz");

    // Use SAMPLE > 1 and SLOTS to trigger all conditional fields
    jedis.hotkeysStart(HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU, HotkeysMetric.NET)
        .sample(2).slots(fooSlot, barSlot, bazSlot));

    // Generate traffic on keys that hash to the selected slots
    for (int i = 0; i < 50; i++) {
      jedis.set("foo", "value" + i);
      jedis.get("foo");
      jedis.set("bar", "value" + i);
      jedis.get("bar");
      jedis.set("baz", "value" + i);
      jedis.get("baz");
    }
    // Also set a key outside selected slots
    jedis.set("other", "value");

    HotkeysInfo reply = jedis.hotkeysGet();
    assertNotNull(reply);

    // 1) tracking-active
    assertTrue(reply.isTrackingActive());

    // 3) sample-ratio
    assertThat(reply.getSampleRatio(), equalTo(2L));

    // 5) selected-slots - verify SLOTS filtering works
    assertThat(reply.getSelectedSlots(), containsInAnyOrder(fooSlot, barSlot, bazSlot));

    // 7) sampled-command-selected-slots-ms (conditional)
    assertNotNull(reply.getSampledCommandSelectedSlotsMs());

    // 9) all-commands-selected-slots-ms (conditional)
    assertNotNull(reply.getAllCommandsSelectedSlotsMs());

    // 11) all-commands-all-slots-ms
    assertThat(reply.getAllCommandsAllSlotsMs(), greaterThanOrEqualTo(0L));

    // 13) net-bytes-sampled-commands-selected-slots (conditional)
    assertNotNull(reply.getNetBytesSampledCommandsSelectedSlots());

    // 15) net-bytes-all-commands-selected-slots (conditional)
    assertNotNull(reply.getNetBytesAllCommandsSelectedSlots());

    // 17) net-bytes-all-commands-all-slots
    assertNotNull(reply.getNetBytesAllCommandsAllSlots());
    assertThat(reply.getNetBytesAllCommandsAllSlots(), greaterThan(0L));

    // 19) collection-start-time-unix-ms
    assertThat(reply.getCollectionStartTimeUnixMs(), greaterThan(0L));

    // 21) collection-duration-ms
    assertThat(reply.getCollectionDurationMs(), greaterThanOrEqualTo(0L));

    // 23) total-cpu-time-user-ms
    assertThat(reply.getTotalCpuTimeUserMs(), greaterThanOrEqualTo(0L));

    // 25) total-cpu-time-sys-ms
    assertThat(reply.getTotalCpuTimeSysMs(), greaterThanOrEqualTo(0L));

    // 27) total-net-bytes
    assertThat(reply.getTotalNetBytes(), greaterThanOrEqualTo(0L));

    // 29) by-cpu-time - should have entries for keys in selected slots
    assertThat(reply.getByCpuTime().keySet(), is(not(empty())));
    assertThat(reply.getByCpuTime().keySet(),
      anyOf(hasItem("foo"), hasItem("bar"), hasItem("baz")));

    // 31) by-net-bytes - should have entries for keys in selected slots
    assertThat(reply.getByNetBytes().keySet(), is(not(empty())));
    assertThat(reply.getByNetBytes().keySet(),
      anyOf(hasItem("foo"), hasItem("bar"), hasItem("baz")));
  }
}
