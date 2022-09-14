package redis.clients.jedis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.executors.ClusterCommandExecutor;
import redis.clients.jedis.providers.ClusterConnectionProvider;

public class ClusterCommandExecutorTest {

  private static final Duration ONE_SECOND = Duration.ofSeconds(1);

  private static final CommandObject<String> STR_COM_OBJECT
      = new CommandObject<>(new ClusterCommandArguments(null).key(""), null);

  @Test
  public void runSuccessfulExecute() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, Duration.ZERO) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        return (T) "foo";
      }
      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };
    assertEquals("foo", testMe.executeCommand(STR_COM_OBJECT));
  }

  @Test
  public void runFailOnFirstExecSuccessOnSecondExec() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, ONE_SECOND) {
      boolean isFirstCall = true;

      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        if (isFirstCall) {
          isFirstCall = false;
          throw new JedisConnectionException("Borkenz");
        }

        return (T) "foo";
      }

      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    assertEquals("foo", testMe.executeCommand(STR_COM_OBJECT));
  }

  @Test
  public void runAlwaysFailing() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    final LongConsumer sleep = mock(LongConsumer.class);
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 3, ONE_SECOND) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        throw new JedisConnectionException("Connection failed");
      }

      @Override
      protected void sleep(long sleepMillis) {
        sleep.accept(sleepMillis);
      }
    };

    try {
      testMe.executeCommand(STR_COM_OBJECT);
      fail("cluster command did not fail");
    } catch (JedisClusterOperationException e) {
      // expected
    }
    InOrder inOrder = inOrder(connectionHandler, sleep);
    inOrder.verify(connectionHandler, times(2)).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verify(sleep).accept(anyLong());
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verify(connectionHandler).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void runMovedSuccess() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    final HostAndPort movedTarget = new HostAndPort(null, 0);
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, ONE_SECOND) {
      boolean isFirstCall = true;

      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        if (isFirstCall) {
          isFirstCall = false;

          // Slot 0 moved
          throw new JedisMovedDataException("", movedTarget, 0);
        }

        return (T) "foo";
      }

      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    assertEquals("foo", testMe.executeCommand(STR_COM_OBJECT));

    InOrder inOrder = inOrder(connectionHandler);
    inOrder.verify(connectionHandler).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verify(connectionHandler).renewSlotCache(any());
    inOrder.verify(connectionHandler).getConnection(movedTarget);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void runAskSuccess() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Connection connection = mock(Connection.class);
    final HostAndPort askTarget = new HostAndPort(null, 0);
    when(connectionHandler.getConnection(askTarget)).thenReturn(connection);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, ONE_SECOND) {
      boolean isFirstCall = true;

      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        if (isFirstCall) {
          isFirstCall = false;

          // Slot 0 moved
          throw new JedisAskDataException("", askTarget, 0);
        }

        return (T) "foo";
      }

      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    assertEquals("foo", testMe.executeCommand(STR_COM_OBJECT));

    InOrder inOrder = inOrder(connectionHandler, connection);
    inOrder.verify(connectionHandler).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verify(connectionHandler).getConnection(askTarget);
    // inOrder.verify(connection).asking();
    inOrder.verify(connection).close(); // From the finally clause in runWithRetries()
    inOrder.verifyNoMoreInteractions();
  }

  // requires 'execute(Connection connection, CommandObject<T> commandObject)' separately
  @Test
  public void runMovedThenAllNodesFailing() {
    // Test:
    // First attempt is a JedisMovedDataException() move, because we asked the wrong node.
    // All subsequent attempts are JedisConnectionExceptions, because all nodes are now down.
    // In response to the JedisConnectionExceptions, run() retries random nodes until maxAttempts is
    // reached.
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);

    final Connection redirecter = mock(Connection.class);
    when(connectionHandler.getConnection(STR_COM_OBJECT.getArguments())).thenReturn(redirecter);

    final Connection failer = mock(Connection.class);
    when(connectionHandler.getConnection(any(HostAndPort.class))).thenReturn(failer);
    doAnswer((Answer) (InvocationOnMock invocation) -> {
      when(connectionHandler.getConnection(STR_COM_OBJECT.getArguments())).thenReturn(failer);
      return null;
    }).when(connectionHandler).renewSlotCache();

    final LongConsumer sleep = mock(LongConsumer.class);
    final HostAndPort movedTarget = new HostAndPort(null, 0);
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 5, ONE_SECOND) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
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
      testMe.executeCommand(STR_COM_OBJECT);
      fail("cluster command did not fail");
    } catch (JedisClusterOperationException e) {
      // expected
    }
    InOrder inOrder = inOrder(connectionHandler, sleep);
    inOrder.verify(connectionHandler).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verify(connectionHandler).renewSlotCache(redirecter);
    inOrder.verify(connectionHandler, times(2)).getConnection(movedTarget);
    inOrder.verify(sleep).accept(anyLong());
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verify(connectionHandler, times(2)).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verify(sleep).accept(anyLong());
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verifyNoMoreInteractions();
  }

  // requires 'execute(Connection connection, CommandObject<T> commandObject)' separately
  @Test
  public void runMasterFailingReplicaRecovering() {
    // We have two nodes, master and replica, and master has just gone down permanently.
    //
    // Test:
    // 1. We try to contact master => JedisConnectionException
    // 2. We try to contact master => JedisConnectionException
    // 3. sleep and renew
    // 4. We try to contact replica => Success, because it has now failed over

    final Connection master = mock(Connection.class);
    when(master.toString()).thenReturn("master");

    final Connection replica = mock(Connection.class);
    when(replica.toString()).thenReturn("replica");

    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);

    when(connectionHandler.getConnection(STR_COM_OBJECT.getArguments())).thenReturn(master);

    doAnswer((Answer) (InvocationOnMock invocation) -> {
      when(connectionHandler.getConnection(STR_COM_OBJECT.getArguments())).thenReturn(replica);
      return null;
    }).when(connectionHandler).renewSlotCache();

    final AtomicLong totalSleepMs = new AtomicLong();
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, ONE_SECOND) {

      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        assertNotNull(connection);

        if (connection == master) {
          throw new JedisConnectionException("Master is down");
        }

        assert connection == replica;

        return (T) "Success!";
      }

      @Override
      protected void sleep(long sleepMillis) {
        assert sleepMillis > 0;
        totalSleepMs.addAndGet(sleepMillis);
      }
    };

    assertEquals("Success!", testMe.executeCommand(STR_COM_OBJECT));
    InOrder inOrder = inOrder(connectionHandler);
    inOrder.verify(connectionHandler, times(2)).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verify(connectionHandler).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verifyNoMoreInteractions();
    assertTrue(totalSleepMs.get() > 0);
  }

  @Test(expected = JedisClusterOperationException.class)
  public void runRethrowsJedisNoReachableClusterNodeException() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    when(connectionHandler.getConnection(STR_COM_OBJECT.getArguments())).thenThrow(
      JedisClusterOperationException.class);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, Duration.ZERO) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        return null;
      }

      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    testMe.executeCommand(STR_COM_OBJECT);
  }

  @Test
  public void runStopsRetryingAfterTimeout() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);

    final LongConsumer sleep = mock(LongConsumer.class);
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 3, Duration.ZERO) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        try {
          // exceed deadline
          Thread.sleep(2L);
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
      testMe.executeCommand(STR_COM_OBJECT);
      fail("cluster command did not fail");
    } catch (JedisClusterOperationException e) {
      // expected
    }
    InOrder inOrder = inOrder(connectionHandler, sleep);
    inOrder.verify(connectionHandler).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verifyNoMoreInteractions();
  }
}
