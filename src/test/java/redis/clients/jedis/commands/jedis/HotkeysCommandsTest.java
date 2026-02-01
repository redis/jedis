package redis.clients.jedis.commands.jedis;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.args.HotkeysMetric;
import redis.clients.jedis.params.HotkeysParams;
import redis.clients.jedis.resps.HotkeysInfo;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@Tag("integration")
@EnabledOnCommand("HOTEKYS")
public class HotkeysCommandsTest extends JedisCommandsTestBase {

  public HotkeysCommandsTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    clearState();
  }

  @AfterEach
  public void cleanUp() {
    clearState();
  }

  private void clearState() {
    jedis.flushAll();
    jedis.hotkeysStop();
    jedis.hotkeysReset();
  }

  @Test
  public void hotkeysGetBeforeStart() {
    HotkeysInfo reply = jedis.hotkeysGet();
    assertNull(reply);
  }

  @Test
  public void hotkeysLifecycle() {
    String startResult = jedis
        .hotkeysStart(HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU));
    assertEquals("OK", startResult);

    jedis.set("key1", "value1");
    jedis.get("key1");

    HotkeysInfo reply = jedis.hotkeysGet();
    assertNotNull(reply);
    assertTrue(reply.isTrackingActive());

    String stopResult = jedis.hotkeysStop();
    assertEquals("OK", stopResult);

    reply = jedis.hotkeysGet();
    assertNotNull(reply);
    assertFalse(reply.isTrackingActive());
    assertTrue(reply.getByCpuTime().containsKey("key1"));

    jedis.hotkeysStart(HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU));
    jedis.set("key2", "val2");
    reply = jedis.hotkeysGet();
    assertTrue(reply.isTrackingActive());
    assertTrue(reply.getByCpuTime().containsKey("key2"));
    assertFalse(reply.getByCpuTime().containsKey("key1"));

    jedis.hotkeysStop();
    String resetResult = jedis.hotkeysReset();
    assertEquals("OK", resetResult);

    reply = jedis.hotkeysGet();
    assertNull(reply);
  }

  @Test
  public void hotkeysBothMetrics() {
    jedis.hotkeysStart(
      HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU, HotkeysMetric.NET).sample(1));

    String cpuHot = "stats:counter";
    for (int i = 0; i < 20; i++) {
      jedis.incr(cpuHot);
    }

    String netHot = "blob:data";
    String largeValue = new String(new char[6000]).replace('\0', 'x');
    jedis.set(netHot, largeValue);
    jedis.get(netHot);

    HotkeysInfo reply = jedis.hotkeysGet();
    assertNotNull(reply);

    assertTrue(reply.getByCpuTime().containsKey(cpuHot));
    assertThat(reply.getByCpuTime().get(cpuHot), greaterThan(0L));

    assertTrue(reply.getByNetBytes().containsKey(netHot));
    assertThat(reply.getByNetBytes().get(netHot), greaterThan(6000L));
  }

  @Test
  public void hotkeysStartOptions() {
    jedis.hotkeysStart(HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU).sample(5));

    for (int i = 0; i < 20; i++) {
      jedis.set("samplekey" + i, "value" + i);
    }

    HotkeysInfo reply = jedis.hotkeysGet();
    assertNotNull(reply);
    assertEquals(5, reply.getSampleRatio());
    assertThat(reply.getByCpuTime().size(), lessThan(20));

    jedis.hotkeysStop();
    jedis.hotkeysReset();

    jedis.hotkeysStart(HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU).count(10));

    for (int i = 1; i <= 25; i++) {
      jedis.set("countkey" + i, "value" + i);
    }

    reply = jedis.hotkeysGet();
    assertNotNull(reply);
    assertThat(reply.getByCpuTime().size(), lessThanOrEqualTo(10));
  }

  @Test
  public void hotkeysResponseFields() {
    jedis.hotkeysStart(HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU, HotkeysMetric.NET));

    jedis.set("testkey", "testvalue");
    jedis.get("testkey");

    HotkeysInfo reply = jedis.hotkeysGet();
    assertNotNull(reply);

    assertTrue(reply.isTrackingActive());
    assertEquals(1, reply.getSampleRatio());
    assertNotNull(reply.getSelectedSlots());
    assertTrue(reply.getSelectedSlots().isEmpty());
    assertThat(reply.getCollectionStartTimeUnixMs(), greaterThan(0L));
    assertThat(reply.getCollectionDurationMs(), greaterThanOrEqualTo(0L));
    assertNotNull(reply.getByCpuTime());
    assertNotNull(reply.getByNetBytes());
  }

  @Test
  public void hotkeysDurationOption() {
    jedis.hotkeysStart(HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU).duration(1));

    jedis.set("durationkey", "testvalue");

    await().atMost(Duration.ofSeconds(2)).until(() -> {
      HotkeysInfo info = jedis.hotkeysGet();
      return info != null && !info.isTrackingActive();
    });

    HotkeysInfo reply = jedis.hotkeysGet();
    assertNotNull(reply);
    assertFalse(reply.isTrackingActive());
    assertThat(reply.getCollectionDurationMs(), greaterThanOrEqualTo(1000L));
    assertTrue(reply.getByCpuTime().containsKey("durationkey"));
  }

  @Test
  public void infoHotkeysSection() {
    String info = jedis.info();
    assertFalse(info.contains("# Hotkeys"));

    jedis.hotkeysStart(HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU));
    info = jedis.info();
    assertTrue(info.contains("# Hotkeys"));
    assertTrue(info.contains("hotkeys-tracking-active:1"));

    jedis.hotkeysStop();
    info = jedis.info();
    assertTrue(info.contains("# Hotkeys"));
    assertTrue(info.contains("hotkeys-tracking-active:0"));

    jedis.hotkeysReset();
    info = jedis.info();
    assertFalse(info.contains("# Hotkeys"));
  }
}
