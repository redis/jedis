package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import redis.clients.jedis.Module;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.util.SafeEncoder;

public class ModuleTest extends JedisCommandsTestBase {

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

  @Test
  public void testModules() {
    try {
      assertEquals("OK", jedis.moduleLoad("/tmp/testmodule.so"));

      List<Module> modules = jedis.moduleList();
      Set<String> moduleNames = modules.stream().map(Module::getName).collect(Collectors.toSet());

      assertTrue(moduleNames.contains("testmodule"));

      Object output = jedis.sendCommand(ModuleCommand.SIMPLE);
      assertTrue((Long) output > 0);

    } finally {

      assertEquals("OK", jedis.moduleUnload("testmodule"));

      List<Module> modules = jedis.moduleList();
      Set<String> moduleNames = modules.stream().map(Module::getName).collect(Collectors.toSet());

      assertFalse(moduleNames.contains("testmodule"));
    }
  }
}
