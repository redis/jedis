package redis.clients.jedis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HostAndPortTest {

  @Test
  public void checkFrom() throws Exception {
    String host = "2a11:1b1:0:111:e111:1f11:1111:1f1e:1999";
    int port = 6379;
    HostAndPort hp = HostAndPort.from(host + ":" + Integer.toString(port));
    assertEquals(host, hp.getHost());
    assertEquals(port, hp.getPort());
  }

  @Test
  public void checkFromWithoutPort() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> HostAndPort.from("localhost:"));
  }
}