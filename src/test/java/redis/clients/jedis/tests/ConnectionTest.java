package redis.clients.jedis.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class ConnectionTest {
  private Connection client;

  @Before
  public void setUp() throws Exception {
    client = new Connection();
  }

  @After
  public void tearDown() throws Exception {
    client.disconnect();
  }

  @Test(expected = JedisConnectionException.class)
  public void checkUnkownHost() {
    client.setHost("someunknownhost");
    client.connect();
  }

  @Test(expected = JedisConnectionException.class)
  public void checkWrongPort() {
    client.setHost("localhost");
    client.setPort(55665);
    client.connect();
  }

  @Test
  public void connectIfNotConnectedWhenSettingTimeoutInfinite() {
    client.setHost("localhost");
    client.setPort(6379);
    client.setTimeoutInfinite();
  }

  // Need to set up a proxy server first.
  @Test
  public void connectViaSocksProxy() {
    Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("localhost", 7788));
    client.setProxy(proxy);
    client.setHost("localhost");
    client.setPort(6379);
    client.connect();
    client.close();
  }

  @Test
  public void checkCloseable() {
    client.setHost("localhost");
    client.setPort(6379);
    client.connect();
    client.close();
  }

  @Test
  public void getErrorAfterConnectionReset() throws Exception {
    class TestConnection extends Connection {
      public TestConnection() {
        super("localhost", 6379);
      }

      @Override
      public Connection sendCommand(ProtocolCommand cmd, byte[]... args) {
        return super.sendCommand(cmd, args);
      }
    }

    TestConnection conn = new TestConnection();

    try {
      conn.sendCommand(Command.HMSET, new byte[1024 * 1024 + 1][0]);
      fail("Should throw exception");
    } catch (JedisConnectionException jce) {
      assertEquals("ERR Protocol error: invalid multibulk length", jce.getMessage());
    }
  }
}
