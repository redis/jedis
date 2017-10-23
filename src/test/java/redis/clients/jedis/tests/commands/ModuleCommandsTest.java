package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ModuleCommandsTest extends JedisCommandTestBase{
  @Test
  public void exec() {
    Object obj = jedis.moduleExecute("testmodule.simple", "testkey", "arg1","arg2");
    assertEquals(obj instanceof String, true);
    
    String str = (String)obj;
    assertEquals(str,"OK");
  }
}
