package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
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

      assertEquals("testmodule", modules.get(0).getName());

      Object output = jedis.sendCommand(ModuleCommand.SIMPLE);
      assertTrue((Long) output > 0);

    } finally {

      assertEquals("OK", jedis.moduleUnload("testmodule"));
      assertEquals(Collections.emptyList(), jedis.moduleList());
    }
  }
}
