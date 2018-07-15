package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.tests.HostAndPortUtil;

public class RenameProtocolCommandsTest {

  private Jedis jedis;

  @Before
  public void setUp() throws Exception {
    HostAndPort hnp = HostAndPortUtil.getRedisServers().get(7);
    jedis = new Jedis(hnp.getHost(), hnp.getPort(), 500);
    jedis.connect();
    jedis.auth("foobared");
    jedis.flushAll();

    Command.SET.rename("NEWSET");
    Command.GET.rename("NEWGET");
  }

  @After
  public void tearDown() {
    Command.SET.rename("SET");
    Command.GET.rename("GET");

    jedis.disconnect();
  }

  @Test
  public void renameCommand() {
    jedis.set("mykey", "hello world");
    String value = jedis.get("mykey");
    assertEquals("hello world", value);
  }

  @Test
  public void disabledCommand() {
    jedis.set("mykey", "hello world2");
    try {
      jedis.del("mykey");
      fail();
    } catch (JedisDataException expected) {
      assertEquals("ERR unknown command 'DEL'", expected.getMessage());
    }
    String value = jedis.get("mykey");
    assertEquals("hello world2", value);
  }
}