package redis.clients.jedis.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.ScanIteration;
import redis.clients.jedis.providers.ConnectionProvider;

public class JedisCommandIterationBaseTest {

  /**
   * A replica holds the same keys as its primary, so an iteration that visits it returns every key
   * once per copy. Regression test for jedis#4797.
   */
  @Test
  public void scanIterationSkipsReplicaNodes() {
    ConnectionPool primaryPool = mock(ConnectionPool.class);
    ConnectionPool replicaPool = mock(ConnectionPool.class);
    Connection primaryConnection = mock(Connection.class);
    Connection replicaConnection = mock(Connection.class);
    when(primaryPool.getResource()).thenReturn(primaryConnection);
    when(replicaPool.getResource()).thenReturn(replicaConnection);
    when(primaryConnection.executeCommand(any(CommandArguments.class)))
        .thenReturn(scanReply("primary-key"));
    when(replicaConnection.executeCommand(any(CommandArguments.class)))
        .thenReturn(scanReply("primary-key"));

    Map<String, ConnectionPool> allNodes = new LinkedHashMap<>();
    allNodes.put("127.0.0.1:6379", primaryPool);
    allNodes.put("127.0.0.1:6380", replicaPool);

    ConnectionProvider connectionProvider = mock(ConnectionProvider.class);
    doReturn(allNodes).when(connectionProvider).getConnectionMap();
    doReturn(Collections.singletonMap("127.0.0.1:6379", primaryPool)).when(connectionProvider)
        .getPrimaryNodesConnectionMap();

    List<String> keys = new ArrayList<>();
    new ScanIteration(connectionProvider, 10, "*").collect(keys);

    assertEquals(Collections.singletonList("primary-key"), keys);
    verify(replicaConnection, never()).executeCommand(any(CommandArguments.class));
  }

  @Test
  public void scanIterationVisitsEveryPrimaryNode() {
    ConnectionPool primaryPool = mock(ConnectionPool.class);
    ConnectionPool otherPrimaryPool = mock(ConnectionPool.class);
    Connection primaryConnection = mock(Connection.class);
    Connection otherPrimaryConnection = mock(Connection.class);
    when(primaryPool.getResource()).thenReturn(primaryConnection);
    when(otherPrimaryPool.getResource()).thenReturn(otherPrimaryConnection);
    when(primaryConnection.executeCommand(any(CommandArguments.class)))
        .thenReturn(scanReply("key-on-first"));
    when(otherPrimaryConnection.executeCommand(any(CommandArguments.class)))
        .thenReturn(scanReply("key-on-second"));

    Map<String, ConnectionPool> primaryNodes = new LinkedHashMap<>();
    primaryNodes.put("127.0.0.1:6379", primaryPool);
    primaryNodes.put("127.0.0.1:6381", otherPrimaryPool);

    ConnectionProvider connectionProvider = mock(ConnectionProvider.class);
    doReturn(primaryNodes).when(connectionProvider).getPrimaryNodesConnectionMap();

    List<String> keys = new ArrayList<>();
    new ScanIteration(connectionProvider, 10, "*").collect(keys);

    assertEquals(2, keys.size());
    assertEquals(2, new java.util.HashSet<>(keys).size());
    assertEquals(2, keys.stream().filter(k -> k.startsWith("key-on-")).count());
  }

  @Test
  public void collectStopsAfterLastPrimaryNode() {
    ConnectionPool primaryPool = mock(ConnectionPool.class);
    Connection primaryConnection = mock(Connection.class);
    when(primaryPool.getResource()).thenReturn(primaryConnection);
    when(primaryConnection.executeCommand(any(CommandArguments.class)))
        .thenReturn(scanReply("only-key"));

    ConnectionProvider connectionProvider = mock(ConnectionProvider.class);
    doReturn(Collections.singletonMap("127.0.0.1:6379", primaryPool)).when(connectionProvider)
        .getPrimaryNodesConnectionMap();

    ScanIteration iteration = new ScanIteration(connectionProvider, 10, "*");
    assertEquals(Collections.singletonList("only-key"), new ArrayList<>(iteration.nextBatchList()));
    assertThrows(NoSuchElementException.class, iteration::nextBatch);
  }

  /**
   * A single SCAN batch covering every key. The "0" cursor marks the node as done, so each
   * connection is visited exactly once per iteration.
   */
  private static List<Object> scanReply(String key) {
    return Arrays.asList("0".getBytes(), Collections.singletonList(key.getBytes()));
  }
}
