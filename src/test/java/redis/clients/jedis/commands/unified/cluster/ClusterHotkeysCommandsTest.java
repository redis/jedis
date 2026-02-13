package redis.clients.jedis.commands.unified.cluster;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.args.HotkeysMetric;
import redis.clients.jedis.params.HotkeysParams;

/**
 * Tests that HOTKEYS commands are not supported in cluster mode.
 */
@Tag("integration")
@EnabledOnCommand("HOTKEYS")
public class ClusterHotkeysCommandsTest {

  protected UnifiedJedis jedis;
  protected RedisProtocol protocol;

  public ClusterHotkeysCommandsTest() {
    this.protocol = RedisProtocol.RESP3;
  }

  @BeforeEach
  public void setUp() {
    jedis = ClusterCommandsTestHelper.getCleanCluster(protocol);
  }

  @AfterEach
  public void tearDown() {
    if (jedis != null) {
      jedis.close();
    }
  }

  @Test
  public void hotkeysStartNotSupportedInCluster() {
    assertThrows(UnsupportedOperationException.class,
      () -> jedis.hotkeysStart(HotkeysParams.hotkeysParams().metrics(HotkeysMetric.CPU)));
  }

  @Test
  public void hotkeysStopNotSupportedInCluster() {
    assertThrows(UnsupportedOperationException.class, () -> jedis.hotkeysStop());
  }

  @Test
  public void hotkeysResetNotSupportedInCluster() {
    assertThrows(UnsupportedOperationException.class, () -> jedis.hotkeysReset());
  }

  @Test
  public void hotkeysGetNotSupportedInCluster() {
    assertThrows(UnsupportedOperationException.class, () -> jedis.hotkeysGet());
  }
}
