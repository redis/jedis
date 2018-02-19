package redis.clients.jedis.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisConnectionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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
      public void sendCommand(ProtocolCommand cmd, byte[]... args) {
        super.sendCommand(cmd, args);
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

  @Test
  public void testReadProtocolWithBrokenState() {
    client.connect();
    try {
      client.getOne();
      fail("Should have thrown an exception as there is nothing to read");
    } catch (JedisConnectionException ignored) {
        assertEquals("Unexpected end of stream.", ignored.getMessage());
    }
    try {
      client.getOne();
      fail("Should have failed due to reading from a broken connection");
    } catch (JedisConnectionException exc) {
      assertEquals("Trying to read from a broken connection", exc.getMessage());
    }
  }
}
