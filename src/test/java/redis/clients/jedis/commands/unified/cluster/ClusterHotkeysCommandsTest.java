package redis.clients.jedis.commands.unified.cluster;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
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

  /**
   * Comprehensive test that verifies response fields. Uses SAMPLE > 1 to trigger sampling-related
   * fields.
   */
  @Test
  public void hotkeysWithAllFields() {
    // Use SAMPLE > 1 and both metrics - start on all masters
    jedis.hotkeysStart(
      HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU, HotkeysMetric.NET).sample(2));

    // Generate traffic on keys
    for (int i = 0; i < 50; i++) {
      jedis.set("foo", "value" + i);
      jedis.get("foo");
      jedis.set("bar", "value" + i);
      jedis.get("bar");
      jedis.set("baz", "value" + i);
      jedis.get("baz");
    }

    HotkeysInfo reply = jedis.hotkeysGet();
    assertNotNull(reply);

    // 1) tracking-active
    assertTrue(reply.isTrackingActive());

    // 3) sample-ratio
    assertThat(reply.getSampleRatio(), equalTo(2L));

    // 5) selected-slots - returns ranges; each node returns its own slot range
    assertThat(reply.getSelectedSlots(), is(not(empty())));
    int[] firstRange = reply.getSelectedSlots().get(0);
    // Verify the range structure is valid (start <= end, within valid slot range)
    assertThat(firstRange[0], greaterThanOrEqualTo(0));
    assertThat(firstRange[1], lessThanOrEqualTo(16383));
    assertThat(firstRange[0], lessThanOrEqualTo(firstRange[1]));

    // 11) all-commands-all-slots-us
    assertThat(reply.getAllCommandsAllSlotsUs(), greaterThanOrEqualTo(0L));

    // 17) net-bytes-all-commands-all-slots
    assertNotNull(reply.getNetBytesAllCommandsAllSlots());
    assertThat(reply.getNetBytesAllCommandsAllSlots(), greaterThanOrEqualTo(0L));

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

    // 29) by-cpu-time-us - should have entries for keys
    assertThat(reply.getByCpuTimeUs().keySet(), is(not(empty())));
    assertThat(reply.getByCpuTimeUs().keySet(),
      anyOf(hasItem("foo"), hasItem("bar"), hasItem("baz")));

    // 31) by-net-bytes - should have entries for keys
    assertThat(reply.getByNetBytes().keySet(), is(not(empty())));
    assertThat(reply.getByNetBytes().keySet(),
      anyOf(hasItem("foo"), hasItem("bar"), hasItem("baz")));

    jedis.hotkeysStop();
  }
}
