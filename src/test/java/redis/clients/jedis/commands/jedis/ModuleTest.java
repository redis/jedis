package redis.clients.jedis.commands.jedis;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Assume;
import org.junit.BeforeClass;
import redis.clients.jedis.util.TestEnvUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import redis.clients.jedis.Module;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

@RunWith(Parameterized.class)
public class ModuleTest extends JedisCommandsTestBase {

  @BeforeClass
  public static void checkDockerEnvironment() {
    Assume.assumeFalse("Module tests not supported against dockerised test env yet!", TestEnvUtil.isContainerEnv());
  }

  static enum ModuleCommand implements ProtocolCommand {

    SIMPLE("testmodule.simple");

    private final byte[] raw;

    private ModuleCommand(String alt) {
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
