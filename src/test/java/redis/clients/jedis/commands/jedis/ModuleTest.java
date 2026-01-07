package redis.clients.jedis.commands.jedis;

import java.util.List;

import io.redis.test.annotations.EnabledOnEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import redis.clients.jedis.util.TestEnvUtil;

import redis.clients.jedis.Module;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ParameterizedClass
@MethodSource("redis.clients.jedis.commands.CommandsTestsParameters#respVersions")
@EnabledOnEnv(TestEnvUtil.ENV_OSS_SOURCE)
public class ModuleTest extends JedisCommandsTestBase {

  enum ModuleCommand implements ProtocolCommand {

    SIMPLE("testmodule.simple");

    private final byte[] raw;

    ModuleCommand(String alt) {
      raw = SafeEncoder.encode(alt);
    }

    @Override
    public byte[] getRaw() {
      return raw;
    }
  }

  public ModuleTest(RedisProtocol protocol) {
    super(protocol);
  }

  @Test
  public void testModules() {
    try {
      assertEquals("OK", jedis.moduleLoad(TestEnvUtil.testModuleSoPath()));

      List<Module> modules = jedis.moduleList();

      assertThat(modules, hasItem(hasProperty("name", equalTo("testmodule"))));

      Object output = jedis.sendCommand(ModuleCommand.SIMPLE);
      assertTrue((Long) output > 0);

    } finally {

      assertEquals("OK", jedis.moduleUnload("testmodule"));
      List<Module> modules = jedis.moduleList();
      assertThat(modules, not(hasItem(hasProperty("name", equalTo("testmodule")))));
    }
  }
}
