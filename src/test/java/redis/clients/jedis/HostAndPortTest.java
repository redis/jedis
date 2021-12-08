package redis.clients.jedis;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HostAndPortTest {

  @Test
  public void checkFrom() throws Exception {
    String host = "2a11:1b1:0:111:e111:1f11:1111:1f1e:1999";
    int port = 6379;
    HostAndPort hp = HostAndPort.from(host + ":" + Integer.toString(port));
    assertEquals(host, hp.getHost());
    assertEquals(port, hp.getPort());
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkFromWithoutPort() throws Exception {
    String host = "localhost";
    HostAndPort.from(host + ":");
  }
}