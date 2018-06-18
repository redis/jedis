package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertEquals;

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
  }
  
  @After
  public void tearDown() {
    jedis.disconnect();
  }
  
  @Test
  public void renameCommand() {
	  Command.SET.rename("NEWSET");
	  Command.GET.rename("NEWGET");
	  
	  jedis.set("mykey", "hello world");
	  String value = jedis.get("mykey");
	  assertEquals("hello world", value);
  }

  @Test(expected = JedisDataException.class)
  public void disableCommand() {
	  Command.SET.rename("NEWSET");
	  Command.GET.rename("NEWGET");

	  jedis.set("mykey", "hello world");
	  jedis.del("mykey");
  }
}