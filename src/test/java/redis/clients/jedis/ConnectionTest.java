package redis.clients.jedis;

import org.junit.After;
import org.junit.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ConnectionTest {

  private Connection client;

  @After
  public void tearDown() throws Exception {
    if (client != null) {
      client.close();
    }
  }

  @Test(expected = JedisConnectionException.class)
  public void checkUnknownHost() {
    client = new Connection("someunknownhost", Protocol.DEFAULT_PORT);
    client.connect();
  }

  @Test(expected = JedisConnectionException.class)
  public void checkWrongPort() {
    client = new Connection(Protocol.DEFAULT_HOST, 55665);
    client.connect();
  }

  @Test
  public void connectIfNotConnectedWhenSettingTimeoutInfinite() {
    client = new Connection("localhost", 6379);
    client.setTimeoutInfinite();
  }

  @Test
  public void checkCloseable() {
    client = new Connection("localhost", 6379);
    client.connect();
    client.close();
  }

  @Test
  public void testConnectionPeerAddrInfo() {
    client = new Connection("127.0.0.1", 6379);
    HostAndPort remoteAddr = client.getRemoteHostAndPort();
    HostAndPort localAddr = client.getLocalHostAndPort();
    assertNull(remoteAddr);
    assertNull(localAddr);

    client.connect();
    remoteAddr = client.getRemoteHostAndPort();
    localAddr = client.getLocalHostAndPort();
    assertEquals(remoteAddr, HostAndPort.from("127.0.0.1:6379"));
    assertEquals(localAddr.getHost(), "127.0.0.1");
    assertTrue(localAddr.getPort() >= 0 && localAddr.getPort() < 65536);
    client.close();
  }

}
