package redis.clients.jedis.tests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import redis.clients.jedis.Connection;
import redis.clients.jedis.JedisCommandListener;
import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.commands.ProtocolCommand;
import redis.clients.jedis.exceptions.JedisConnectionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ConnectionTest {
  private Connection client;

  @Before
  public void setUp() throws Exception {
    client = new Connection();
  }

  @After
  public void tearDown() throws Exception {
    client.close();
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
  public void getErrorMultibulkLength() throws Exception {
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
  public void readWithBrokenConnection() {
    class BrokenConnection extends Connection {
      private BrokenConnection() {
        super("nonexistinghost", 0);
        try {
          connect();
          fail("Client should fail connecting to nonexistinghost");
        } catch (JedisConnectionException ignored) {
        }
      }

      private Object read() {
        return readProtocolWithCheckingBroken();
      }
    }

    BrokenConnection conn = new BrokenConnection();
    try {
      conn.read();
      fail("Read should fail as connection is broken");
    } catch (JedisConnectionException jce) {
      assertEquals("Attempting to read from a broken connection", jce.getMessage());
    }
  }

  @Test
  public void notifiesListenerOnSuccessfulCommand() {
    TrackingCommandListener listener = new TrackingCommandListener();
    this.client.addListener(listener);
    this.client.setHost("localhost");
    this.client.setPort(6379);

    this.client.sendCommand(Command.PING);

    assertTrue(listener.isCommandStarted());
    assertTrue(listener.isCommandConnected());
    assertTrue(listener.isCommandFinished());
    assertFalse(listener.isCommandFailed());
  }

  @Test
  public void notifiesListenerOnConnectionError() {
    TrackingCommandListener listener = new TrackingCommandListener();
    this.client.addListener(listener);
    this.client.setHost("someunknownhost");

    try {
      this.client.sendCommand(Command.PING);
      fail("Test did not throw exception as expected");
    } catch (JedisConnectionException jce) {
    }

    assertTrue(listener.isCommandStarted());
    assertTrue(listener.isCommandFailed());
    assertFalse(listener.isCommandConnected());
    assertFalse(listener.isCommandFinished());
  }

  @Test
  public void notifiesListenerOnUnexpectedError() {
    class UnexpectedBrokenConnection extends Connection {
      @Override
      public void connect() {
        throw new IllegalStateException("unexpected exception");
      }
    }

    TrackingCommandListener listener = new TrackingCommandListener();
    this.client = new UnexpectedBrokenConnection();
    this.client.addListener(listener);

    try {
      this.client.sendCommand(Command.PING);
      fail("Test did not throw exception as expected");
    } catch (IllegalStateException ise) {
    }

    assertTrue(listener.isCommandStarted());
    assertTrue(listener.isCommandFailed());
    assertFalse(listener.isCommandConnected());
    assertFalse(listener.isCommandFinished());
  }

  @Test
  public void supportsMultipleListeners() {
    TrackingCommandListener listener1 = new TrackingCommandListener();
    TrackingCommandListener listener2 = new TrackingCommandListener();
    List<JedisCommandListener> listeners = new LinkedList<>();
    listeners.add(listener1);
    listeners.add(listener2);

    this.client.addListeners(listeners);
    this.client.setHost("localhost");
    this.client.setPort(6379);

    this.client.sendCommand(Command.PING);

    assertTrue(listener1.isCommandStarted());
    assertTrue(listener1.isCommandConnected());
    assertTrue(listener1.isCommandFinished());
    assertFalse(listener1.isCommandFailed());
    assertTrue(listener2.isCommandStarted());
    assertTrue(listener2.isCommandConnected());
    assertTrue(listener2.isCommandFinished());
    assertFalse(listener2.isCommandFailed());
  }

  @Test
  public void interpretsNullListenerListAsEmptyList() {
    try {
      this.client.addListeners(null);
      this.client.setHost("localhost");
      this.client.setPort(6379);
      this.client.sendCommand(Command.PING);
    } catch (NullPointerException e) {
      fail("A null listener list wasn't properly coerced to empty list.");
    }
  }

  @Test
  public void addingListenersDoesntRemovePreexistingOnes() {
    TrackingCommandListener listener1 = new TrackingCommandListener();
    TrackingCommandListener listener2 = new TrackingCommandListener();

    this.client.addListener(listener1);
    this.client.addListener(listener2);
    this.client.setHost("localhost");
    this.client.setPort(6379);

    this.client.sendCommand(Command.PING);

    assertTrue(listener1.isCommandStarted());
    assertTrue(listener1.isCommandConnected());
    assertTrue(listener1.isCommandFinished());
    assertFalse(listener1.isCommandFailed());
    assertTrue(listener2.isCommandStarted());
    assertTrue(listener2.isCommandConnected());
    assertTrue(listener2.isCommandFinished());
    assertFalse(listener2.isCommandFailed());
  }

  @Test
  public void batchAddingListenersDoesntRemovePreexistingOnes() {
    TrackingCommandListener listener1 = new TrackingCommandListener();

    TrackingCommandListener listener2 = new TrackingCommandListener();
    List<JedisCommandListener> listeners = new LinkedList<>();
    listeners.add(listener2);

    this.client.addListener(listener1);
    this.client.addListeners(listeners);
    this.client.setHost("localhost");
    this.client.setPort(6379);

    this.client.sendCommand(Command.PING);

    assertTrue(listener1.isCommandStarted());
    assertTrue(listener1.isCommandConnected());
    assertTrue(listener1.isCommandFinished());
    assertFalse(listener1.isCommandFailed());
    assertTrue(listener2.isCommandStarted());
    assertTrue(listener2.isCommandConnected());
    assertTrue(listener2.isCommandFinished());
    assertFalse(listener2.isCommandFailed());
  }

  class TrackingCommandListener implements JedisCommandListener {

    private boolean commandStarted = false;
    private boolean commandConnected = false;
    private boolean commandFinished = false;
    private boolean commandFailed = false;

    @Override public void commandStarted(Connection connection, ProtocolCommand event,
        byte[]... args) {
      this.commandStarted = true;
    }

    @Override public void commandConnected(Connection connection, ProtocolCommand event) {
      this.commandConnected = true;
    }

    @Override public void commandFinished(Connection connection, ProtocolCommand event) {
      this.commandFinished = true;
    }

    @Override public void commandFailed(Connection connection, ProtocolCommand event, Throwable t) {
      this.commandFailed = true;
    }

    public boolean isCommandStarted() {
      return commandStarted;
    }

    public boolean isCommandConnected() {
      return commandConnected;
    }

    public boolean isCommandFinished() {
      return commandFinished;
    }

    public boolean isCommandFailed() {
      return commandFailed;
    }
  }
}
