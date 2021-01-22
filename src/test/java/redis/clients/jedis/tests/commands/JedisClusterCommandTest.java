package redis.clients.jedis.tests.commands;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.function.LongConsumer;
import org.junit.Test;
import org.mockito.InOrder;
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
    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(null, 0, Duration.ZERO) {
      @Override
      public String execute(Jedis connection) {
        return null;
      }

      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    testMe.run("");
  }

  @Test
  public void runSuccessfulExecute() {
    JedisClusterConnectionHandler connectionHandler = mock(JedisClusterConnectionHandler.class);
    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10,
        Duration.ZERO) {
      @Override
      public String execute(Jedis connection) {
        return "foo";
      }

      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };
    String actual = testMe.run("");
    assertEquals("foo", actual);
  }

  @Test
  public void runFailOnFirstExecSuccessOnSecondExec() {
    JedisClusterConnectionHandler connectionHandler = mock(JedisClusterConnectionHandler.class);

    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10,
        Duration.ZERO) {
      boolean isFirstCall = true;

      @Override
      public String execute(Jedis connection) {
        if (isFirstCall) {
          isFirstCall = false;
          throw new JedisConnectionException("Borkenz");
        }

        return "foo";
      }

      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    String actual = testMe.run("");
    assertEquals("foo", actual);
  }

  @Test
  public void runAlwaysFailing() {
    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);

    final LongConsumer sleep = mock(LongConsumer.class);
    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 3,
        Duration.ofSeconds(1)) {
      @Override
      public String execute(Jedis connection) {
        throw new JedisConnectionException("Connection failed");
      }

      @Override
      protected void sleep(long sleepMillis) {
        sleep.accept(sleepMillis);
      }
    };

    try {
      testMe.run("");
      fail("cluster command did not fail");
    } catch (JedisClusterMaxAttemptsException e) {
      // expected
    }
    InOrder inOrder = inOrder(connectionHandler, sleep);
    inOrder.verify(connectionHandler).getConnectionFromSlot(anyInt());
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verify(sleep).accept(anyLong());
    inOrder.verify(connectionHandler).getConnection();
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verify(sleep).accept(anyLong());
    inOrder.verify(connectionHandler).getConnection();
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void runMovedSuccess() {
    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);

    final HostAndPort movedTarget = new HostAndPort(null, 0);
    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10,
        Duration.ZERO) {
      boolean isFirstCall = true;

      @Override
      public String execute(Jedis connection) {
        if (isFirstCall) {
          isFirstCall = false;

          // Slot 0 moved
          throw new JedisMovedDataException("", movedTarget, 0);
        }

        return "foo";
      }

      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    String actual = testMe.run("");
    assertEquals("foo", actual);

    InOrder inOrder = inOrder(connectionHandler);
    inOrder.verify(connectionHandler).getConnectionFromSlot(anyInt());
    inOrder.verify(connectionHandler).renewSlotCache(any());
    inOrder.verify(connectionHandler).getConnectionFromNode(movedTarget);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void runAskSuccess() {
    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);
    Jedis connection = mock(Jedis.class);
    final HostAndPort askTarget = new HostAndPort(null, 0);
    when(connectionHandler.getConnectionFromNode(askTarget)).thenReturn(connection);

    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10,
        Duration.ZERO) {
      boolean isFirstCall = true;

      @Override
      public String execute(Jedis connection) {
        if (isFirstCall) {
          isFirstCall = false;

          // Slot 0 moved
          throw new JedisAskDataException("", askTarget, 0);
        }

        return "foo";
      }

      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    String actual = testMe.run("");
    assertEquals("foo", actual);

    InOrder inOrder = inOrder(connectionHandler, connection);
    inOrder.verify(connectionHandler).getConnectionFromSlot(anyInt());
    inOrder.verify(connectionHandler).getConnectionFromNode(askTarget);
    inOrder.verify(connection).asking();
    inOrder.verify(connection).close(); // From the finally clause in runWithRetries()
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void runMovedThenAllNodesFailing() {
    // Test:
    // First attempt is a JedisMovedDataException() move, because we asked the wrong node.
    // All subsequent attempts are JedisConnectionExceptions, because all nodes are now down.
    // In response to the JedisConnectionExceptions, run() retries random nodes until maxAttempts is
    // reached.
    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);

    final Jedis redirecter = mock(Jedis.class);
    when(connectionHandler.getConnectionFromSlot(anyInt())).thenReturn(redirecter);

    final Jedis failer = mock(Jedis.class);
    when(connectionHandler.getConnectionFromNode(any(HostAndPort.class))).thenReturn(failer);
    when(connectionHandler.getConnection()).thenReturn(failer);

    final LongConsumer sleep = mock(LongConsumer.class);
    final HostAndPort movedTarget = new HostAndPort(null, 0);
    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 3,
        Duration.ofSeconds(1)) {
      @Override
      public String execute(Jedis connection) {
        if (redirecter == connection) {
          // First attempt, report moved
          throw new JedisMovedDataException("Moved", movedTarget, 0);
        }

        if (failer == connection) {
          // Second attempt in response to the move, report failure
          throw new JedisConnectionException("Connection failed");
        }

        throw new IllegalStateException("Should have thrown jedis exception");
      }

      @Override
      protected void sleep(long sleepMillis) {
        sleep.accept(sleepMillis);
      }
    };

    try {
      testMe.run("");
      fail("cluster command did not fail");
    } catch (JedisClusterMaxAttemptsException e) {
      // expected
    }
    InOrder inOrder = inOrder(connectionHandler, sleep);
    inOrder.verify(connectionHandler).getConnectionFromSlot(anyInt());
    inOrder.verify(connectionHandler).renewSlotCache(redirecter);
    inOrder.verify(connectionHandler).getConnectionFromNode(movedTarget);
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verify(sleep).accept(anyLong());
    inOrder.verify(connectionHandler).getConnection();
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verifyNoMoreInteractions();
  }

  @Test(expected = JedisNoReachableClusterNodeException.class)
  public void runRethrowsJedisNoReachableClusterNodeException() {
    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);
    when(connectionHandler.getConnectionFromSlot(anyInt())).thenThrow(
      JedisNoReachableClusterNodeException.class);

    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10,
        Duration.ZERO) {
      @Override
      public String execute(Jedis connection) {
        return null;
      }

      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    testMe.run("");
  }

  @Test
  public void runStopsRetryingAfterTimeout() {
    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);

    final LongConsumer sleep = mock(LongConsumer.class);
    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 3,
        Duration.ZERO) {
      @Override
      public String execute(Jedis connection) {
        try {
          // exceed deadline
          Thread.sleep(100L);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
        throw new JedisConnectionException("Connection failed");
      }

      @Override
      protected void sleep(long sleepMillis) {
        sleep.accept(sleepMillis);
      }
    };

    try {
      testMe.run("");
      fail("cluster command did not fail");
    } catch (JedisClusterMaxAttemptsException e) {
      // expected
    }
    InOrder inOrder = inOrder(connectionHandler, sleep);
    inOrder.verify(connectionHandler).getConnectionFromSlot(anyInt());
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void runClosesConnectionBeforeRenewingSlotsCache() {
    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);

    Jedis connection = mock(Jedis.class);
    when(connectionHandler.getConnectionFromSlot(anyInt())).thenReturn(connection);

    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 1, Duration.ZERO) {
      @Override
      public String execute(Jedis connection) {
        throw new JedisConnectionException("Connection failure");
      }
    };

    try {
      testMe.run("");
      fail("Didn't get the expected exception");
    } catch (JedisClusterMaxAttemptsException e) {
      // Expected case, do nothing
    }

    InOrder inOrder = inOrder(connectionHandler, connection);
    // Must close connection before renewing slot cache, otherwise
    // JedisClusterTest.testReturnConnectionOnJedisClusterConnection
    // will start failing intermittently.
    inOrder.verify(connection).close();
    inOrder.verify(connectionHandler).renewSlotCache();

    // This one is because of a finally block, and isn't needed but doesn't hurt.
    // If you rewrite the code so this close() call goes away, fell free to
    // update this test as well to match!
    inOrder.verify(connection).close();

    inOrder.verifyNoMoreInteractions();
  }
}
