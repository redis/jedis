package redis.clients.jedis.tests;

import org.junit.Test;

import java.util.Arrays;

import redis.clients.jedis.HostAndPort;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class HostAndPortTest {
  @Test
  public void checkExtractParts() throws Exception {
    String host = "2a11:1b1:0:111:e111:1f11:1111:1f1e:1999";
    String port = "6379";

    assertArrayEquals(new String[]{host, port}, HostAndPort.extractParts(host + ":" + port));

    host = "";
    port = "";
    assertArrayEquals(new String[]{host, port}, HostAndPort.extractParts(host + ":" + port));

    host = "localhost";
    port = "";
    assertArrayEquals(new String[]{host, port}, HostAndPort.extractParts(host + ":" + port));

    host = "";
    port = "6379";
    assertArrayEquals(new String[]{host, port}, HostAndPort.extractParts(host + ":" + port));

    host = "11:22:33:44:55";
    port = "";
    assertArrayEquals(new String[]{host, port}, HostAndPort.extractParts(host + ":" + port));
  }

  @Test
  public void checkParseString() throws Exception {
    String host = "2a11:1b1:0:111:e111:1f11:1111:1f1e:1999";
    int port = 6379;
    HostAndPort hp = HostAndPort.parseString(host + ":" + Integer.toString(port));
    assertEquals(host, hp.getHost());
    assertEquals(port, hp.getPort());
  }

  @Test(expected = IllegalArgumentException.class)
  public void checkParseStringWithoutPort() throws Exception {
    String host = "localhost";
    HostAndPort.parseString(host + ":");
  }

  @Test
  public void checkConvertHost() {
    String host = "2a11:1b1:0:111:e111:1f11:1111:1f1e";
    assertEquals(host, HostAndPort.convertHost(host));
  }
}