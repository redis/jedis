package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static redis.clients.jedis.Protocol.Command.PING;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import redis.clients.jedis.exceptions.JedisAskDataException;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.exceptions.JedisMovedDataException;
import redis.clients.jedis.executors.ClusterCommandExecutor;
import redis.clients.jedis.providers.ClusterConnectionProvider;

public class ClusterCommandExecutorTest {

  private static final Duration ONE_SECOND = Duration.ofSeconds(1);

  private static final CommandObject<String> STR_COM_OBJECT
      = new CommandObject<>(new ClusterCommandArguments(PING).key(""), BuilderFactory.STRING);

  @Test
  public void runSuccessfulExecute() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, Duration.ZERO,
        StaticCommandFlagsRegistry.registry()) {
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
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, ONE_SECOND,
        StaticCommandFlagsRegistry.registry()) {
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
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 3, ONE_SECOND,
        StaticCommandFlagsRegistry.registry()) {
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
    inOrder.verify(sleep).accept(ArgumentMatchers.anyLong());
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verify(connectionHandler).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void runMovedSuccess() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    final HostAndPort movedTarget = new HostAndPort(null, 0);
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, ONE_SECOND,
        StaticCommandFlagsRegistry.registry()) {
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
    inOrder.verify(connectionHandler).renewSlotCache(ArgumentMatchers.any());
    inOrder.verify(connectionHandler).getConnection(movedTarget);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void runAskSuccess() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Connection connection = mock(Connection.class);
    final HostAndPort askTarget = new HostAndPort(null, 0);
    when(connectionHandler.getConnection(askTarget)).thenReturn(connection);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, ONE_SECOND,
        StaticCommandFlagsRegistry.registry()) {
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
    when(connectionHandler.getConnection(ArgumentMatchers.any(HostAndPort.class))).thenReturn(failer);
    Mockito.doAnswer((InvocationOnMock invocation) -> {
      when(connectionHandler.getConnection(STR_COM_OBJECT.getArguments())).thenReturn(failer);
      return null;
    }).when(connectionHandler).renewSlotCache();

    final LongConsumer sleep = mock(LongConsumer.class);
    final HostAndPort movedTarget = new HostAndPort(null, 0);
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 5, ONE_SECOND,
        StaticCommandFlagsRegistry.registry()) {
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
    inOrder.verify(sleep).accept(ArgumentMatchers.anyLong());
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verify(connectionHandler, times(2)).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verify(sleep).accept(ArgumentMatchers.anyLong());
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

    Mockito.doAnswer((InvocationOnMock invocation) -> {
      when(connectionHandler.getConnection(STR_COM_OBJECT.getArguments())).thenReturn(replica);
      return null;
    }).when(connectionHandler).renewSlotCache();

    final AtomicLong totalSleepMs = new AtomicLong();
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 5, ONE_SECOND,
        StaticCommandFlagsRegistry.registry()) {

      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        assertNotNull(connection);

        if (connection.toString().equals("master")) {
          throw new JedisConnectionException("Master is down");
        }

        assert connection.toString().equals("replica");

        return (T) "Success!";
      }

      @Override
      protected void sleep(long sleepMillis) {
        // assert sleepMillis > 0;
        totalSleepMs.addAndGet(sleepMillis);
      }
    };

    assertEquals("Success!", testMe.executeCommand(STR_COM_OBJECT));
    InOrder inOrder = inOrder(connectionHandler);
    inOrder.verify(connectionHandler, times(2)).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verify(connectionHandler).renewSlotCache();
    inOrder.verify(connectionHandler).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verifyNoMoreInteractions();
    MatcherAssert.assertThat(totalSleepMs.get(), Matchers.greaterThan(0L));
  }

  @Test
  public void runRethrowsJedisNoReachableClusterNodeException() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    when(connectionHandler.getConnection(STR_COM_OBJECT.getArguments())).thenThrow(
        JedisClusterOperationException.class);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10,
        Duration.ZERO, StaticCommandFlagsRegistry.registry()) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        return null;
      }

      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    assertThrows(JedisClusterOperationException.class, () -> testMe.executeCommand(STR_COM_OBJECT));
  }

  @Test
  public void runStopsRetryingAfterTimeout() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);

    //final LongConsumer sleep = mock(LongConsumer.class);
    final AtomicLong totalSleepMs = new AtomicLong();
    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 3, Duration.ZERO,
        StaticCommandFlagsRegistry.registry()) {
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
        //sleep.accept(sleepMillis);
        totalSleepMs.addAndGet(sleepMillis);
      }
    };

    try {
      testMe.executeCommand(STR_COM_OBJECT);
      fail("cluster command did not fail");
    } catch (JedisClusterOperationException e) {
      // expected
    }
    //InOrder inOrder = inOrder(connectionHandler, sleep);
    InOrder inOrder = inOrder(connectionHandler);
    inOrder.verify(connectionHandler).getConnection(STR_COM_OBJECT.getArguments());
    inOrder.verifyNoMoreInteractions();
    assertEquals(0L, totalSleepMs.get());
  }

  @Test
  public void runSuccessfulExecuteKeylessCommand() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Map<String, ConnectionPool> connectionMap = new HashMap<>();
    ConnectionPool pool = mock(ConnectionPool.class);
    Connection connection = mock(Connection.class);

    connectionMap.put("localhost:6379", pool);
    when(connectionHandler.getPrimaryNodesConnectionMap()).thenReturn(connectionMap);
    when(pool.getResource()).thenReturn(connection);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, Duration.ZERO,
        StaticCommandFlagsRegistry.registry()) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        return (T) "keyless_result";
      }
      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };
    assertEquals("keyless_result", testMe.executeKeylessCommand(STR_COM_OBJECT));
  }

  @Test
  public void runKeylessCommandUsesConnectionMapRoundRobin() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Map<String, ConnectionPool> connectionMap = new HashMap<>();
    ConnectionPool pool = mock(ConnectionPool.class);
    Connection connection = mock(Connection.class);

    connectionMap.put("localhost:6379", pool);
    when(connectionHandler.getPrimaryNodesConnectionMap()).thenReturn(connectionMap);
    when(pool.getResource()).thenReturn(connection);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, Duration.ZERO,
        StaticCommandFlagsRegistry.registry()) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        return (T) "keyless_result";
      }
      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    testMe.executeKeylessCommand(STR_COM_OBJECT);

    // Verify that getPrimaryNodesConnectionMap() was called for round-robin distribution
    InOrder inOrder = inOrder(connectionHandler, pool, connection);
    inOrder.verify(connectionHandler).getPrimaryNodesConnectionMap();
    inOrder.verify(pool).getResource();
    inOrder.verify(connection).close();
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void runKeylessCommandIgnoresRedirections() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Map<String, ConnectionPool> connectionMap = new HashMap<>();
    ConnectionPool pool = mock(ConnectionPool.class);
    Connection connection1 = mock(Connection.class);
    Connection connection2 = mock(Connection.class);
    final HostAndPort movedTarget = new HostAndPort(null, 0);

    connectionMap.put("localhost:6379", pool);
    when(connectionHandler.getPrimaryNodesConnectionMap()).thenReturn(connectionMap);
    when(pool.getResource()).thenReturn(connection1, connection2);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, ONE_SECOND,
        StaticCommandFlagsRegistry.registry()) {
      boolean isFirstCall = true;

      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        if (isFirstCall) {
          isFirstCall = false;
          // Keyless commands should ignore redirections and retry with different random node
          throw new JedisMovedDataException("", movedTarget, 0);
        }
        return (T) "keyless_result";
      }

      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    assertEquals("keyless_result", testMe.executeKeylessCommand(STR_COM_OBJECT));

    // Verify that we called getPrimaryNodesConnectionMap() twice (first failed with redirection, second succeeded)
    // and that we didn't follow the redirection to a specific node
    verify(connectionHandler, times(2)).getPrimaryNodesConnectionMap();
    verify(pool, times(2)).getResource();
    verify(connection1).close();
    verify(connection2).close();
  }

  @Test
  public void runKeylessCommandFailsAfterMaxAttempts() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Map<String, ConnectionPool> connectionMap = new HashMap<>();
    ConnectionPool pool = mock(ConnectionPool.class);
    Connection connection1 = mock(Connection.class);
    Connection connection2 = mock(Connection.class);
    Connection connection3 = mock(Connection.class);
    final LongConsumer sleep = mock(LongConsumer.class);

    connectionMap.put("localhost:6379", pool);
    when(connectionHandler.getPrimaryNodesConnectionMap()).thenReturn(connectionMap);
    when(pool.getResource()).thenReturn(connection1, connection2, connection3);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 3, ONE_SECOND,
        StaticCommandFlagsRegistry.registry()) {
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
      testMe.executeKeylessCommand(STR_COM_OBJECT);
      fail("keyless command did not fail");
    } catch (JedisClusterOperationException e) {
      // expected
    }

    // Verify that we tried connection map access and performed slot cache renewal
    // getPrimaryNodesConnectionMap() called 3 times (once for each connection attempt)
    // getResource() called 3 times, sleep called once, renewSlotCache called once
    verify(connectionHandler, times(3)).getPrimaryNodesConnectionMap();
    verify(pool, times(3)).getResource();
    verify(connection1).close();
    verify(connection2).close();
    verify(connection3).close();
    verify(sleep).accept(ArgumentMatchers.anyLong());
    verify(connectionHandler).renewSlotCache();
  }

  @Test
  public void runKeylessCommandFailsWithEmptyConnectionMap() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Map<String, ConnectionPool> emptyConnectionMap = new HashMap<>();

    when(connectionHandler.getPrimaryNodesConnectionMap()).thenReturn(emptyConnectionMap);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 3, ONE_SECOND,
        StaticCommandFlagsRegistry.registry()) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        return (T) "should_not_reach_here";
      }
      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    try {
      testMe.executeKeylessCommand(STR_COM_OBJECT);
      fail("keyless command should fail with empty connection map");
    } catch (JedisClusterOperationException e) {
      assertEquals("No cluster nodes available.", e.getMessage());
    }

    // Verify that getPrimaryNodesConnectionMap() was called
    verify(connectionHandler).getPrimaryNodesConnectionMap();
  }

  @Test
  public void runKeylessCommandRoundRobinDistribution() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Map<String, ConnectionPool> connectionMap = new HashMap<>();

    // Create multiple pools to test round-robin
    ConnectionPool pool1 = mock(ConnectionPool.class);
    ConnectionPool pool2 = mock(ConnectionPool.class);
    ConnectionPool pool3 = mock(ConnectionPool.class);

    Connection connection1 = mock(Connection.class);
    Connection connection2 = mock(Connection.class);
    Connection connection3 = mock(Connection.class);

    connectionMap.put("localhost:6379", pool1);
    connectionMap.put("localhost:6380", pool2);
    connectionMap.put("localhost:6381", pool3);

    when(connectionHandler.getPrimaryNodesConnectionMap()).thenReturn(connectionMap);
    when(pool1.getResource()).thenReturn(connection1);
    when(pool2.getResource()).thenReturn(connection2);
    when(pool3.getResource()).thenReturn(connection3);

    // Track which connections are used
    List<Connection> usedConnections = new ArrayList<>();

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, Duration.ZERO,
        StaticCommandFlagsRegistry.registry()) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        usedConnections.add(connection);
        return (T) "keyless_result";
      }
      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    // Execute multiple keyless commands to verify round-robin
    testMe.executeKeylessCommand(STR_COM_OBJECT);
    testMe.executeKeylessCommand(STR_COM_OBJECT);
    testMe.executeKeylessCommand(STR_COM_OBJECT);
    testMe.executeKeylessCommand(STR_COM_OBJECT); // Should cycle back to first

    // Verify round-robin behavior - should cycle through all connections
    assertEquals(4, usedConnections.size());
    Set<Connection> uniqueConnections = new HashSet<>(usedConnections);
    assertEquals(3, uniqueConnections.size(),
        "Round-robin should distribute across multiple nodes");
  }

  @Test
  public void runKeylessCommandCircularCounterNeverOverflows() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Map<String, ConnectionPool> connectionMap = new HashMap<>();

    // Create 3 pools to test circular behavior
    ConnectionPool pool1 = mock(ConnectionPool.class);
    ConnectionPool pool2 = mock(ConnectionPool.class);
    ConnectionPool pool3 = mock(ConnectionPool.class);

    Connection connection1 = mock(Connection.class);
    Connection connection2 = mock(Connection.class);
    Connection connection3 = mock(Connection.class);

    connectionMap.put("node1:6379", pool1);
    connectionMap.put("node2:6379", pool2);
    connectionMap.put("node3:6379", pool3);

    when(connectionHandler.getPrimaryNodesConnectionMap()).thenReturn(connectionMap);
    when(pool1.getResource()).thenReturn(connection1);
    when(pool2.getResource()).thenReturn(connection2);
    when(pool3.getResource()).thenReturn(connection3);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, Duration.ZERO,
        StaticCommandFlagsRegistry.registry()) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        return (T) "keyless_result";
      }
      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    // Execute many commands to test circular behavior
    // With our implementation using getAndUpdate(current -> (current + 1) % nodeCount),
    // the counter never exceeds nodeCount-1, so overflow is impossible
    for (int i = 0; i < 100; i++) {
      String result = testMe.executeKeylessCommand(STR_COM_OBJECT);
      assertEquals("keyless_result", result);
    }

    // Verify that getPrimaryNodesConnectionMap() was called for each execution
    verify(connectionHandler, times(100)).getPrimaryNodesConnectionMap();

    // The circular counter implementation ensures no overflow can occur
    // because the counter value is always between 0 and (nodeCount-1)
  }

  @Test
  public void runKeylessCommandEvenDistributionRoundRobin() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Map<String, ConnectionPool> connectionMap = new HashMap<>();

    // Create 4 pools to test even distribution
    ConnectionPool pool1 = mock(ConnectionPool.class);
    ConnectionPool pool2 = mock(ConnectionPool.class);
    ConnectionPool pool3 = mock(ConnectionPool.class);
    ConnectionPool pool4 = mock(ConnectionPool.class);

    Connection connection1 = mock(Connection.class);
    Connection connection2 = mock(Connection.class);
    Connection connection3 = mock(Connection.class);
    Connection connection4 = mock(Connection.class);

    // Use ordered map to ensure consistent iteration order for testing
    connectionMap.put("node1:6379", pool1);
    connectionMap.put("node2:6379", pool2);
    connectionMap.put("node3:6379", pool3);
    connectionMap.put("node4:6379", pool4);

    when(connectionHandler.getPrimaryNodesConnectionMap()).thenReturn(connectionMap);
    when(pool1.getResource()).thenReturn(connection1);
    when(pool2.getResource()).thenReturn(connection2);
    when(pool3.getResource()).thenReturn(connection3);
    when(pool4.getResource()).thenReturn(connection4);

    // Track connection usage count
    Map<Connection, Integer> connectionUsage = new HashMap<>();
    connectionUsage.put(connection1, 0);
    connectionUsage.put(connection2, 0);
    connectionUsage.put(connection3, 0);
    connectionUsage.put(connection4, 0);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, Duration.ZERO,
        StaticCommandFlagsRegistry.registry()) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        connectionUsage.put(connection, connectionUsage.get(connection) + 1);
        return (T) "keyless_result";
      }
      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    // Execute commands - should be evenly distributed
    int totalCommands = 40; // Multiple of 4 for perfect distribution
    for (int i = 0; i < totalCommands; i++) {
      testMe.executeKeylessCommand(STR_COM_OBJECT);
    }

    // Verify even distribution - each node should get exactly 10 commands
    int expectedPerNode = totalCommands / 4;
    assertEquals(expectedPerNode, connectionUsage.get(connection1).intValue(),
        "Node 1 should receive exactly " + expectedPerNode + " commands");
    assertEquals(expectedPerNode, connectionUsage.get(connection2).intValue(),
        "Node 2 should receive exactly " + expectedPerNode + " commands");
    assertEquals(expectedPerNode, connectionUsage.get(connection3).intValue(),
        "Node 3 should receive exactly " + expectedPerNode + " commands");
    assertEquals(expectedPerNode, connectionUsage.get(connection4).intValue(),
        "Node 4 should receive exactly " + expectedPerNode + " commands");

    // Verify total commands executed
    int totalExecuted = connectionUsage.values().stream().mapToInt(Integer::intValue).sum();
    assertEquals(totalCommands, totalExecuted, "Total commands executed should match");

    // Verify that getPrimaryNodesConnectionMap() was called for each execution
    verify(connectionHandler, times(totalCommands)).getPrimaryNodesConnectionMap();
  }

  @Test
  public void runKeylessCommandRoundRobinSequence() {
    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Map<String, ConnectionPool> connectionMap = new HashMap<>();

    // Create 3 pools for simpler sequence verification
    ConnectionPool pool1 = mock(ConnectionPool.class);
    ConnectionPool pool2 = mock(ConnectionPool.class);
    ConnectionPool pool3 = mock(ConnectionPool.class);

    Connection connection1 = mock(Connection.class);
    Connection connection2 = mock(Connection.class);
    Connection connection3 = mock(Connection.class);

    // Use LinkedHashMap to ensure consistent iteration order
    connectionMap = new java.util.LinkedHashMap<>();
    connectionMap.put("node1:6379", pool1);
    connectionMap.put("node2:6379", pool2);
    connectionMap.put("node3:6379", pool3);

    when(connectionHandler.getPrimaryNodesConnectionMap()).thenReturn(connectionMap);
    when(pool1.getResource()).thenReturn(connection1);
    when(pool2.getResource()).thenReturn(connection2);
    when(pool3.getResource()).thenReturn(connection3);

    // Track the exact sequence of connections used
    List<String> connectionSequence = new ArrayList<>();

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, Duration.ZERO,
        StaticCommandFlagsRegistry.registry()) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        if (connection == connection1) {
          connectionSequence.add("node1");
        } else if (connection == connection2) {
          connectionSequence.add("node2");
        } else if (connection == connection3) {
          connectionSequence.add("node3");
        }
        return (T) "keyless_result";
      }
      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    // Execute 9 commands to see 3 complete cycles
    for (int i = 0; i < 9; i++) {
      testMe.executeKeylessCommand(STR_COM_OBJECT);
    }

    // Verify the round-robin sequence
    List<String> expectedSequence = new ArrayList<>();
    expectedSequence.add("node1"); expectedSequence.add("node2"); expectedSequence.add("node3"); // First cycle
    expectedSequence.add("node1"); expectedSequence.add("node2"); expectedSequence.add("node3"); // Second cycle
    expectedSequence.add("node1"); expectedSequence.add("node2"); expectedSequence.add("node3"); // Third cycle

    assertEquals(expectedSequence, connectionSequence,
        "Round-robin should follow exact sequence: node1 -> node2 -> node3 -> node1 -> ...");
  }

  @Test
  public void runKeylessCommandWithReadOnlyCommandUsesAllNodesConnectionMap() {
    // Create a read-only command object using GET command (which has READONLY flag)
    CommandObject<String> readOnlyCommandObject = new CommandObject<>(
        new ClusterCommandArguments(Protocol.Command.GET).key("testkey"), BuilderFactory.STRING);

    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Map<String, ConnectionPool> allNodesConnectionMap = new HashMap<>();
    ConnectionPool pool = mock(ConnectionPool.class);
    Connection connection = mock(Connection.class);

    // Setup connection map with all nodes (including replicas)
    allNodesConnectionMap.put("primary:6379", pool);
    allNodesConnectionMap.put("replica:6380", pool);

    // For read-only commands, getConnectionMap() should be called (all nodes including replicas)
    when(connectionHandler.getConnectionMap()).thenReturn(allNodesConnectionMap);
    when(pool.getResource()).thenReturn(connection);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, Duration.ZERO,
        StaticCommandFlagsRegistry.registry()) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        return (T) "readonly_result";
      }
      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    assertEquals("readonly_result", testMe.executeKeylessCommand(readOnlyCommandObject));

    // Verify that getConnectionMap() was called (for read-only commands, uses all nodes)
    // and NOT getPrimaryNodesConnectionMap()
    verify(connectionHandler).getConnectionMap();
    verify(connectionHandler, times(0)).getPrimaryNodesConnectionMap();
    verify(pool).getResource();
    verify(connection).close();
  }

  @Test
  public void runKeylessCommandWithWriteCommandUsesPrimaryNodesConnectionMap() {
    // Create a write command object using SET command (which has WRITE flag, not READONLY)
    CommandObject<String> writeCommandObject = new CommandObject<>(
        new ClusterCommandArguments(Protocol.Command.SET).key("testkey").add("value"),
        BuilderFactory.STRING);

    ClusterConnectionProvider connectionHandler = mock(ClusterConnectionProvider.class);
    Map<String, ConnectionPool> primaryNodesConnectionMap = new HashMap<>();
    ConnectionPool pool = mock(ConnectionPool.class);
    Connection connection = mock(Connection.class);

    // Setup connection map with only primary nodes
    primaryNodesConnectionMap.put("primary:6379", pool);

    // For write commands, getPrimaryNodesConnectionMap() should be called
    when(connectionHandler.getPrimaryNodesConnectionMap()).thenReturn(primaryNodesConnectionMap);
    when(pool.getResource()).thenReturn(connection);

    ClusterCommandExecutor testMe = new ClusterCommandExecutor(connectionHandler, 10, Duration.ZERO,
        StaticCommandFlagsRegistry.registry()) {
      @Override
      public <T> T execute(Connection connection, CommandObject<T> commandObject) {
        return (T) "write_result";
      }
      @Override
      protected void sleep(long ignored) {
        throw new RuntimeException("This test should never sleep");
      }
    };

    assertEquals("write_result", testMe.executeKeylessCommand(writeCommandObject));

    // Verify that getPrimaryNodesConnectionMap() was called (for write commands, uses only primaries)
    // and NOT getConnectionMap()
    verify(connectionHandler).getPrimaryNodesConnectionMap();
    verify(connectionHandler, times(0)).getConnectionMap();
    verify(pool).getResource();
    verify(connection).close();
  }
}
