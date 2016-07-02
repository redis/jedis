package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import redis.clients.jedis.Module;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.tests.commands.JedisCommandTestBase;
import redis.clients.util.SafeEncoder;

public class ModuleTest extends JedisCommandTestBase {

  static enum ModuleCommand implements ProtocolCommand {
    SIMPLE("testmodule.simple")  ;

    private final byte[] raw;

    ModuleCommand(String alt) {
      raw = SafeEncoder.encode(alt);
    }

    @Override
    public byte[] getRaw() {
      return SafeEncoder.encode("testmodule.simple");
    }
  }

  @Test
  public void testModules() {
    String res = jedis.moduleLoad("/tmp/testmodule.so");
    assertEquals(res, "OK");

    List<Module> modules = jedis.moduleList();

    assertEquals(modules.get(0).getName(), "testmodule");

    jedis.getClient().sendCommand(ModuleCommand.SIMPLE);
    Long out = jedis.getClient().getIntegerReply();
    assertTrue(out > 0);

    res = jedis.moduleUnload("testmodule");
    assertEquals(res, "OK");
  }

}