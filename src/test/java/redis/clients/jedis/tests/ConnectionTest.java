package redis.clients.jedis.tests;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class ConnectionTest extends Assert {
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

  @Test
  public void checkCloseable() {
    client.setHost("localhost");
    client.setPort(6379);
    client.connect();
    client.close();
  }

  @Test
  public void getErrorAfterConnectionReset() throws Exception {
    brokenReason(new byte[1024 * 1024 + 1][0], "ERR Protocol error: invalid multibulk length");
    brokenReason(new byte[1][512 * 1024 * 1024 + 1], "ERR Protocol error: invalid bulk length");
  }

  private void brokenReason(byte[][] params, String expectedMessage) throws Exception {
    class TestConnection extends Connection {
      public TestConnection() {
        super("localhost", 6379);
      }

      @Override
      protected Connection sendCommand(ProtocolCommand cmd, byte[]... args) {
        return super.sendCommand(cmd, args);
      }
    }

    TestConnection conn = new TestConnection();

    try {
      conn.sendCommand(Command.HMSET, params);
      fail("Should throw exception");
    } catch (JedisConnectionException jce) {
      assertEquals(expectedMessage, jce.getMessage());
    }
  }
}
