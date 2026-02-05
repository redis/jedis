package redis.clients.jedis.commands.jedis;

import static org.junit.jupiter.api.Assertions.assertThrows;

import io.redis.test.annotations.ConditionalOnEnv;
import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.args.HotkeysMetric;
import redis.clients.jedis.params.HotkeysParams;
import redis.clients.jedis.util.TestEnvUtil;

/**
 * Tests that HOTKEYS commands are not supported in cluster mode.
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
}
