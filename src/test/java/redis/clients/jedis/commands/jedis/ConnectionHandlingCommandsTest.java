package redis.clients.jedis.commands.jedis;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.HostAndPorts;

public class ConnectionHandlingCommandsTest {

  private static HostAndPort hnp = HostAndPorts.getRedisServers().get(0);

  @Test
  public void quit() {
    Jedis jedis = new Jedis(hnp);
    assertEquals("OK", jedis.quit());
  }
}
