package redis.clients.jedis.tests;

import org.junit.Test;
import redis.clients.jedis.ClientOptions;
import redis.clients.jedis.Connection;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisConnectionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ConnectionTest {

  public static final String LOCAL_HOST = "localhost";
  public static final String UNKNOWN_HOST = "someunknownhost";
  public static final int PORT = 6379;
  public static final int WRONG_PORT = 55665;

  @Test(expected = JedisConnectionException.class)
  public void checkUnkownHost() {
    try(Connection connection = new Connection(ClientOptions.builder().withHost(UNKNOWN_HOST).build())){
      connection.connect();
    }
  }

  @Test(expected = JedisConnectionException.class)
  public void checkWrongPort() {
    try(Connection connection = new Connection(ClientOptions.builder().withHost(LOCAL_HOST).withPort(WRONG_PORT).build())){
      connection.connect();
    }
  }

  @Test
  public void connectIfNotConnectedWhenSettingTimeoutInfinite() {
    try(Connection connection = new Connection(ClientOptions.builder().withHost(LOCAL_HOST).withPort(PORT).build())){
      connection.setTimeoutInfinite();
    }
  }

  @Test
  public void checkCloseable() {
    try(Connection connection = new Connection(ClientOptions.builder().withHost(LOCAL_HOST).withPort(PORT).build())){
      connection.connect();
    }
  }

  @Test
  public void getErrorAfterConnectionReset() throws Exception {
    class TestConnection extends Connection {
      public TestConnection() {
        super(ClientOptions.builder().withHost(LOCAL_HOST).withPort(PORT).build());
      }

      @Override
      public Connection sendCommand(ProtocolCommand cmd, byte[]... args) {
        return super.sendCommand(cmd, args);
      }
    }

    try (TestConnection conn = new TestConnection();){
      conn.sendCommand(Command.HMSET, new byte[1024 * 1024 + 1][0]);
      fail("Should throw exception");
    } catch (JedisConnectionException jce) {
      assertEquals("ERR Protocol error: invalid multibulk length", jce.getMessage());
    }
  }
}
