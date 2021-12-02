//package redis.clients.jedis;
//
//import static org.junit.Assert.assertEquals;
//import static org.junit.Assert.assertNotNull;
//import static org.junit.Assert.assertTrue;
//import static org.junit.Assert.fail;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyInt;
//import static org.mockito.ArgumentMatchers.anyLong;
//import static org.mockito.Mockito.doAnswer;
//import static org.mockito.Mockito.inOrder;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.when;
//
//import java.time.Duration;
//import java.util.concurrent.atomic.AtomicLong;
//import java.util.function.LongConsumer;
//import org.junit.Test;
//import org.mockito.InOrder;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.stubbing.Answer;
//import redis.clients.jedis.HostAndPort;
//import redis.clients.jedis.Jedis;
//import redis.clients.jedis.JedisClusterCommand;
//import redis.clients.jedis.JedisClusterConnectionHandler;
//import redis.clients.jedis.JedisSlotBasedConnectionHandler;
//import redis.clients.jedis.exceptions.JedisAskDataException;
//import redis.clients.jedis.exceptions.JedisClusterMaxAttemptsException;
//import redis.clients.jedis.exceptions.JedisClusterOperationException;
//import redis.clients.jedis.exceptions.JedisConnectionException;
//import redis.clients.jedis.exceptions.JedisMovedDataException;
//import redis.clients.jedis.exceptions.JedisNoReachableClusterNodeException;
//
//public class JedisClusterCommandTest {
//
//  private static final Duration ONE_SECOND = Duration.ofSeconds(1);
//
//  @Test
//  public void runSuccessfulExecute() {
//    JedisClusterConnectionHandler connectionHandler = mock(JedisClusterConnectionHandler.class);
//    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10,
//        Duration.ZERO) {
//      @Override
//      public String execute(Jedis connection) {
//        return "foo";
//      }
//
//      @Override
//      protected void sleep(long ignored) {
//        throw new RuntimeException("This test should never sleep");
//      }
//    };
//    String actual = testMe.run("");
//    assertEquals("foo", actual);
//  }
//
//  @Test
//  public void runFailOnFirstExecSuccessOnSecondExec() {
//    JedisClusterConnectionHandler connectionHandler = mock(JedisClusterConnectionHandler.class);
//
//    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10,
//        ONE_SECOND) {
//      boolean isFirstCall = true;
//
//      @Override
//      public String execute(Jedis connection) {
//        if (isFirstCall) {
//          isFirstCall = false;
//          throw new JedisConnectionException("Borkenz");
//        }
//
//        return "foo";
//      }
//
//      @Override
//      protected void sleep(long ignored) {
//        throw new RuntimeException("This test should never sleep");
//      }
//    };
//
//    String actual = testMe.run("");
//    assertEquals("foo", actual);
//  }
//
//  @Test
//  public void runAlwaysFailing() {
//    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);
//
//    final LongConsumer sleep = mock(LongConsumer.class);
//    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 3,
//        ONE_SECOND) {
//      @Override
//      public String execute(Jedis connection) {
//        throw new JedisConnectionException("Connection failed");
//      }
//
//      @Override
//      protected void sleep(long sleepMillis) {
//        sleep.accept(sleepMillis);
//      }
//    };
//
//    try {
//      testMe.run("");
//      fail("cluster command did not fail");
//    } catch (JedisClusterMaxAttemptsException e) {
//      // expected
//    }
//    InOrder inOrder = inOrder(connectionHandler, sleep);
//    inOrder.verify(connectionHandler, times(2)).getConnectionFromSlot(anyInt());
//    inOrder.verify(sleep).accept(anyLong());
//    inOrder.verify(connectionHandler).renewSlotCache();
//    inOrder.verify(connectionHandler).getConnectionFromSlot(anyInt());
//    inOrder.verifyNoMoreInteractions();
//  }
//
//  @Test
//  public void runMovedSuccess() {
//    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);
//
//    final HostAndPort movedTarget = new HostAndPort(null, 0);
//    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10,
//        ONE_SECOND) {
//      boolean isFirstCall = true;
//
//      @Override
//      public String execute(Jedis connection) {
//        if (isFirstCall) {
//          isFirstCall = false;
//
//          // Slot 0 moved
//          throw new JedisMovedDataException("", movedTarget, 0);
//        }
//
//        return "foo";
//      }
//
//      @Override
//      protected void sleep(long ignored) {
//        throw new RuntimeException("This test should never sleep");
//      }
//    };
//
//    String actual = testMe.run("");
//    assertEquals("foo", actual);
//
//    InOrder inOrder = inOrder(connectionHandler);
//    inOrder.verify(connectionHandler).getConnectionFromSlot(anyInt());
//    inOrder.verify(connectionHandler).renewSlotCache(any());
//    inOrder.verify(connectionHandler).getConnectionFromNode(movedTarget);
//    inOrder.verifyNoMoreInteractions();
//  }
//
//  @Test
//  public void runAskSuccess() {
//    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);
//    Jedis connection = mock(Jedis.class);
//    final HostAndPort askTarget = new HostAndPort(null, 0);
//    when(connectionHandler.getConnectionFromNode(askTarget)).thenReturn(connection);
//
//    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10,
//        ONE_SECOND) {
//      boolean isFirstCall = true;
//
//      @Override
//      public String execute(Jedis connection) {
//        if (isFirstCall) {
//          isFirstCall = false;
//
//          // Slot 0 moved
//          throw new JedisAskDataException("", askTarget, 0);
//        }
//
//        return "foo";
//      }
//
//      @Override
//      protected void sleep(long ignored) {
//        throw new RuntimeException("This test should never sleep");
//      }
//    };
//
//    String actual = testMe.run("");
//    assertEquals("foo", actual);
//
//    InOrder inOrder = inOrder(connectionHandler, connection);
//    inOrder.verify(connectionHandler).getConnectionFromSlot(anyInt());
//    inOrder.verify(connectionHandler).getConnectionFromNode(askTarget);
//    inOrder.verify(connection).asking();
//    inOrder.verify(connection).close(); // From the finally clause in runWithRetries()
//    inOrder.verifyNoMoreInteractions();
//  }
//
//  @Test
//  public void runMovedThenAllNodesFailing() {
//    // Test:
//    // First attempt is a JedisMovedDataException() move, because we asked the wrong node.
//    // All subsequent attempts are JedisConnectionExceptions, because all nodes are now down.
//    // In response to the JedisConnectionExceptions, run() retries random nodes until maxAttempts is
//    // reached.
//    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);
//
//    final Jedis redirecter = mock(Jedis.class);
//    when(connectionHandler.getConnectionFromSlot(anyInt())).thenReturn(redirecter);
//
//    final Jedis failer = mock(Jedis.class);
//    when(connectionHandler.getConnectionFromNode(any(HostAndPort.class))).thenReturn(failer);
//    doAnswer((Answer) (InvocationOnMock invocation) -> {
//      when(connectionHandler.getConnectionFromSlot(anyInt())).thenReturn(failer);
//      return null;
//    }).when(connectionHandler).renewSlotCache();
//
//    final LongConsumer sleep = mock(LongConsumer.class);
//    final HostAndPort movedTarget = new HostAndPort(null, 0);
//    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 5,
//        ONE_SECOND) {
//      @Override
//      public String execute(Jedis connection) {
//        if (redirecter == connection) {
//          // First attempt, report moved
//          throw new JedisMovedDataException("Moved", movedTarget, 0);
//        }
//
//        if (failer == connection) {
//          // Second attempt in response to the move, report failure
//          throw new JedisConnectionException("Connection failed");
//        }
//
//        throw new IllegalStateException("Should have thrown jedis exception");
//      }
//
//      @Override
//      protected void sleep(long sleepMillis) {
//        sleep.accept(sleepMillis);
//      }
//    };
//
//    try {
//      testMe.run("");
//      fail("cluster command did not fail");
//    } catch (JedisClusterMaxAttemptsException e) {
//      // expected
//    }
//    InOrder inOrder = inOrder(connectionHandler, sleep);
//    inOrder.verify(connectionHandler).getConnectionFromSlot(anyInt());
//    inOrder.verify(connectionHandler).renewSlotCache(redirecter);
//    inOrder.verify(connectionHandler, times(2)).getConnectionFromNode(movedTarget);
//    inOrder.verify(sleep).accept(anyLong());
//    inOrder.verify(connectionHandler).renewSlotCache();
//    inOrder.verify(connectionHandler, times(2)).getConnectionFromSlot(anyInt());
//    inOrder.verify(sleep).accept(anyLong());
//    inOrder.verify(connectionHandler).renewSlotCache();
//    inOrder.verifyNoMoreInteractions();
//  }
//
//  @Test
//  public void runMasterFailingReplicaRecovering() {
//    // We have two nodes, master and replica, and master has just gone down permanently.
//    //
//    // Test:
//    // 1. We try to contact master => JedisConnectionException
//    // 2. We try to contact master => JedisConnectionException
//    // 3. sleep and renew
//    // 4. We try to contact replica => Success, because it has now failed over
//
//    final Jedis master = mock(Jedis.class);
//    when(master.toString()).thenReturn("master");
//
//    final Jedis replica = mock(Jedis.class);
//    when(replica.toString()).thenReturn("replica");
//
//    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);
//
//    when(connectionHandler.getConnectionFromSlot(anyInt())).thenReturn(master);
//
//    doAnswer((Answer) (InvocationOnMock invocation) -> {
//      when(connectionHandler.getConnectionFromSlot(anyInt())).thenReturn(replica);
//      return null;
//    }).when(connectionHandler).renewSlotCache();
//
//    final AtomicLong totalSleepMs = new AtomicLong();
//    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10,
//        ONE_SECOND) {
//
//      @Override
//      public String execute(Jedis connection) {
//        assertNotNull(connection);
//
//        if (connection == master) {
//          throw new JedisConnectionException("Master is down");
//        }
//
//        assert connection == replica;
//
//        return "Success!";
//      }
//
//      @Override
//      protected void sleep(long sleepMillis) {
//        assert sleepMillis > 0;
//        totalSleepMs.addAndGet(sleepMillis);
//      }
//    };
//
//    assertEquals("Success!", testMe.run(""));
//    InOrder inOrder = inOrder(connectionHandler);
//    inOrder.verify(connectionHandler, times(2)).getConnectionFromSlot(anyInt());
//    inOrder.verify(connectionHandler).renewSlotCache();
//    inOrder.verify(connectionHandler).getConnectionFromSlot(anyInt());
//    inOrder.verifyNoMoreInteractions();
//    assertTrue(totalSleepMs.get() > 0);
//  }
//
//  @Test(expected = JedisNoReachableClusterNodeException.class)
//  public void runRethrowsJedisNoReachableClusterNodeException() {
//    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);
//    when(connectionHandler.getConnectionFromSlot(anyInt())).thenThrow(
//      JedisNoReachableClusterNodeException.class);
//
//    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 10,
//        Duration.ZERO) {
//      @Override
//      public String execute(Jedis connection) {
//        return null;
//      }
//
//      @Override
//      protected void sleep(long ignored) {
//        throw new RuntimeException("This test should never sleep");
//      }
//    };
//
//    testMe.run("");
//  }
//
//  @Test
//  public void runStopsRetryingAfterTimeout() {
//    JedisSlotBasedConnectionHandler connectionHandler = mock(JedisSlotBasedConnectionHandler.class);
//
//    final LongConsumer sleep = mock(LongConsumer.class);
//    JedisClusterCommand<String> testMe = new JedisClusterCommand<String>(connectionHandler, 3,
//        Duration.ZERO) {
//      @Override
//      public String execute(Jedis connection) {
//        try {
//          // exceed deadline
//          Thread.sleep(2L);
//        } catch (InterruptedException e) {
//          throw new RuntimeException(e);
//        }
//        throw new JedisConnectionException("Connection failed");
//      }
//
//      @Override
//      protected void sleep(long sleepMillis) {
//        sleep.accept(sleepMillis);
//      }
//    };
//
//    try {
//      testMe.run("");
//      fail("cluster command did not fail");
//    } catch (JedisClusterOperationException e) {
//      // expected
//    }
//    InOrder inOrder = inOrder(connectionHandler, sleep);
//    inOrder.verify(connectionHandler).getConnectionFromSlot(anyInt());
//    inOrder.verifyNoMoreInteractions();
//  }
//}
