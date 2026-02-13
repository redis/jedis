package redis.clients.jedis.commands.unified;

import io.redis.test.annotations.ConditionalOnEnv;
import io.redis.test.annotations.EnabledOnCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.util.TestEnvUtil;

@Tag("integration")
@EnabledOnCommand("HOTKEYS")
@ConditionalOnEnv(value = TestEnvUtil.ENV_OSS_DOCKER, enabled = true)
public abstract class HotkeysCommandsTestBase extends UnifiedJedisCommandsTestBase {

  public HotkeysCommandsTestBase(RedisProtocol protocol) {
    super(protocol);
  }

  @BeforeEach
  void setUp() {
    clearState();
  }

  @AfterEach
  void tearDown() {
    clearState();
  }

  private void clearState() {
    if (jedis != null) {
      jedis.flushAll();
      jedis.hotkeysStop();
      jedis.hotkeysReset();
    }
  }
}
