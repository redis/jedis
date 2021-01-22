package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.mockito.Mockito;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClusterCommand;
import redis.clients.jedis.JedisClusterConnectionHandler;
import redis.clients.jedis.JedisSlotBasedConnectionHandler;
import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterMaxAttemptsException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;

public class JedisClusterCommandTest {

  @Test(expected = JedisClusterMaxAttemptsException.class)
  public void runZeroAttempts() {
    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(null, 0) {
      @Override
      public String execute(Jedis connection) {
        return null;
      }
    };

    testMe.run("");
  }

  @Test
  public void runSuccessfulExecute() {
    JedisClusterConnectionHandler connectionHandler = Mockito
        .mock(JedisClusterConnectionHandler.class);
    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10) {
      @Override
      public String execute(Jedis connection) {
        return "foo";
      }
    };
    String actual = testMe.run("");
    assertEquals("foo", actual);
  }

  @Test
  public void runFailOnFirstExecSuccessOnSecondExec() {
    JedisClusterConnectionHandler connectionHandler = Mockito
        .mock(JedisClusterConnectionHandler.class);

    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10) {
      boolean isFirstCall = true;

      @Override
      public String execute(Jedis connection) {
        if (isFirstCall) {
          isFirstCall = false;
          throw new JedisConnectionException("Borkenz");
        }

        return "foo";
      }
    };

    String actual = testMe.run("");
    assertEquals("foo", actual);
  }

  @Test
  public void runReconnectWithRandomConnection() {
    JedisSlotBasedConnectionHandler connectionHandler = Mockito
        .mock(JedisSlotBasedConnectionHandler.class);
    // simulate failing connection
    Mockito.when(connectionHandler.getConnectionFromSlot(Mockito.anyInt())).thenReturn(null);
    // simulate good connection
    Mockito.when(connectionHandler.getConnection()).thenReturn(Mockito.mock(Jedis.class));

    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10) {
      @Override
      public String execute(Jedis connection) {
        if (connection == null) {
          throw new JedisConnectionException("");
        }
        return "foo";
      }
    };

    String actual = testMe.run("");
    assertEquals("foo", actual);
  }

  @Test
  public void runMovedSuccess() {
    JedisClusterConnectionHandler connectionHandler = Mockito
        .mock(JedisClusterConnectionHandler.class);

    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10) {
      boolean isFirstCall = true;

      @Override
      public String execute(Jedis connection) {
        if (isFirstCall) {
          isFirstCall = false;

          // Slot 0 moved
          throw new JedisMovedDataException("", null, 0);
        }

        return "foo";
      }
    };

    String actual = testMe.run("");
    assertEquals("foo", actual);

    Mockito.verify(connectionHandler).renewSlotCache(Mockito.<Jedis> any());
  }

  @Test
  public void runAskSuccess() {
    JedisSlotBasedConnectionHandler connectionHandler = Mockito
        .mock(JedisSlotBasedConnectionHandler.class);
    Jedis jedis = Mockito.mock(Jedis.class);
    Mockito.when(connectionHandler.getConnectionFromNode(Mockito.<HostAndPort> any())).thenReturn(
      jedis);

    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10) {
      boolean isFirstCall = true;

      @Override
      public String execute(Jedis connection) {
        if (isFirstCall) {
          isFirstCall = false;

          // Slot 0 moved
          throw new JedisAskDataException("", null, 0);
        }

        return "foo";
      }
    };

    String actual = testMe.run("");
    assertEquals("foo", actual);
    Mockito.verify(jedis).asking();
  }

  @Test
  public void runMovedFailSuccess() {
    // Test:
    // First attempt is a JedisMovedDataException() move, because we asked the wrong node
    // Second attempt is a JedisConnectionException, because this node is down
    // In response to that, runWithTimeout() requests a random node using
    // connectionHandler.getConnection()
    // Third attempt works
    JedisSlotBasedConnectionHandler connectionHandler = Mockito
        .mock(JedisSlotBasedConnectionHandler.class);

    Jedis fromGetConnectionFromSlot = Mockito.mock(Jedis.class);
    Mockito.when(fromGetConnectionFromSlot.toString()).thenReturn("getConnectionFromSlot");
    Mockito.when(connectionHandler.getConnectionFromSlot(Mockito.anyInt())).thenReturn(
      fromGetConnectionFromSlot);

    Jedis fromGetConnectionFromNode = Mockito.mock(Jedis.class);
    Mockito.when(fromGetConnectionFromNode.toString()).thenReturn("getConnectionFromNode");
    Mockito.when(connectionHandler.getConnectionFromNode(Mockito.<HostAndPort> any())).thenReturn(
      fromGetConnectionFromNode);

    Jedis fromGetConnection = Mockito.mock(Jedis.class);
    Mockito.when(fromGetConnection.toString()).thenReturn("getConnection");
    Mockito.when(connectionHandler.getConnection()).thenReturn(fromGetConnection);

    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10) {
      @Override
      public String execute(Jedis connection) {
        String source = connection.toString();
        if ("getConnectionFromSlot".equals(source)) {
          // First attempt, report moved
          throw new JedisMovedDataException("Moved", null, 0);
        }

        if ("getConnectionFromNode".equals(source)) {
          // Second attempt in response to the move, report failure
          throw new JedisConnectionException("Connection failed");
        }

        // This is the third and last case we handle
        assert "getConnection".equals(source);
        return "foo";
      }
    };

    String actual = testMe.run("");
    assertEquals("foo", actual);
  }

  @Test(expected = JedisNoReachableClusterNodeException.class)
  public void runRethrowsJedisNoReachableClusterNodeException() {
    JedisSlotBasedConnectionHandler connectionHandler = Mockito
        .mock(JedisSlotBasedConnectionHandler.class);
    Mockito.when(connectionHandler.getConnectionFromSlot(Mockito.anyInt())).thenThrow(
      JedisNoReachableClusterNodeException.class);

    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10) {
      @Override
      public String execute(Jedis connection) {
        return null;
      }
    };

    testMe.run("");
  }
}
