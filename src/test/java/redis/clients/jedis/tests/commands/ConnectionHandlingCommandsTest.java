package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.Jedis;

public class ConnectionHandlingCommandsTest {

  @Test
  public void quit() {
    Jedis jedis = new Jedis();
    assertEquals("OK", jedis.quit());
  }

  @Test
  public void binary_quit() {
    BinaryJedis bj = new BinaryJedis();
    assertEquals("OK", bj.quit());
  }
}