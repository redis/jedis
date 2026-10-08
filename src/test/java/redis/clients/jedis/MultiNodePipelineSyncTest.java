package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.exceptions.JedisConnectionException;
import redis.clients.jedis.providers.ClusterConnectionProvider;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.SafeEncoder;

/**
 * {@link MultiNodePipelineBase#sync()} reads each node's replies on a worker thread. When a read
 * fails, the other nodes must still be read to completion, the failed node's responses must carry
 * the connection error, the failed node must be dropped from the pipeline's bookkeeping so that the
 * next command for it obtains a fresh connection, and the error must reach the caller.
 */
class MultiNodePipelineSyncTest {

  private static final HostAndPort NODE_A = new HostAndPort("node-a", 7000);
  private static final HostAndPort NODE_B = new HostAndPort("node-b", 7001);

  // Distinct hash slots so the two keys route to two different nodes.
  private static final String KEY_A = "{a}";
  private static final String KEY_B = "{b}";

  private ClusterConnectionProvider provider;
  private ExecutorService executor;

  @BeforeEach
  void setUp() {
    provider = mock(ClusterConnectionProvider.class);
    when(provider.getNode(JedisClusterCRC16.getSlot(KEY_A))).thenReturn(NODE_A);
    when(provider.getNode(JedisClusterCRC16.getSlot(KEY_B))).thenReturn(NODE_B);
    executor = Executors.newFixedThreadPool(2);
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  private ClusterPipeline pipeline() {
    return new ClusterPipeline(provider, new ClusterCommandObjects(RedisProtocol.RESP2),
        StaticCommandFlagsRegistry.registry(), executor);
  }

  @Test
  void connectionFailureDuringSyncDropsOnlyTheFailedNode() {
    Connection connA = mock(Connection.class);
    Connection connB = mock(Connection.class);
    when(provider.getConnection(NODE_A)).thenReturn(connA);
    when(provider.getConnection(NODE_B)).thenReturn(connB);

    // Node A fails only once node B's read has started, i.e. after the calling thread has
    // iterated past node A's entry.
    CountDownLatch nodeBReading = new CountDownLatch(1);
    JedisConnectionException nodeAFailure = new JedisConnectionException("node-a went away");
    when(connA.getMany(1)).thenAnswer(invocation -> {
      nodeBReading.await(5, TimeUnit.SECONDS);
      throw nodeAFailure;
    });
    when(connB.getMany(1)).thenAnswer(invocation -> {
      nodeBReading.countDown();
      return Collections.singletonList(SafeEncoder.encode("OK"));
    });

    ClusterPipeline pipeline = pipeline();
    Response<String> replyA = pipeline.set(KEY_A, "1");
    Response<String> replyB = pipeline.set(KEY_B, "1");

    assertSame(nodeAFailure, assertThrows(JedisConnectionException.class, pipeline::sync));
    assertArrayEquals(new Throwable[0], nodeAFailure.getSuppressed());

    // The healthy node was read to completion; the failed node's command is reported as lost
    // rather than as a response that has not been synced yet.
    assertEquals("OK", replyB.get());
    assertSame(nodeAFailure, assertThrows(JedisConnectionException.class, replyA::get));
    verify(connA).close();

    // The failed node must be re-acquired from the provider on the next command.
    Connection connA2 = mock(Connection.class);
    when(provider.getConnection(NODE_A)).thenReturn(connA2);
    pipeline.set(KEY_A, "2");
    verify(connA2).sendCommand(any(CommandArguments.class));

    // The healthy node keeps its existing connection.
    pipeline.set(KEY_B, "2");
    verify(connB, times(2)).sendCommand(any(CommandArguments.class));
    verify(provider, times(1)).getConnection(NODE_B);
  }

  @Test
  void furtherFailuresAreReportedAsSuppressed() {
    Connection connA = mock(Connection.class);
    Connection connB = mock(Connection.class);
    when(provider.getConnection(NODE_A)).thenReturn(connA);
    when(provider.getConnection(NODE_B)).thenReturn(connB);

    JedisConnectionException nodeAFailure = new JedisConnectionException("node-a went away");
    JedisConnectionException nodeBFailure = new JedisConnectionException("node-b went away");
    when(connA.getMany(1)).thenThrow(nodeAFailure);
    when(connB.getMany(1)).thenThrow(nodeBFailure);

    ClusterPipeline pipeline = pipeline();
    pipeline.set(KEY_A, "1");
    pipeline.set(KEY_B, "1");

    // Reported in pipeline order, so the outcome does not depend on which worker finished first.
    JedisConnectionException thrown = assertThrows(JedisConnectionException.class, pipeline::sync);
    assertSame(nodeAFailure, thrown);
    assertArrayEquals(new Throwable[] { nodeBFailure }, thrown.getSuppressed());

    verify(connA).close();
    verify(connB).close();
  }

  /**
   * A pipeline that reached a single node reads it inline on the calling thread instead of
   * submitting to the executor; the failure has to surface the same way.
   */
  @Test
  void singleNodeFailureIsPropagatedAndCleanedUp() {
    Connection connA = mock(Connection.class);
    when(provider.getConnection(NODE_A)).thenReturn(connA);

    JedisConnectionException nodeAFailure = new JedisConnectionException("node-a went away");
    when(connA.getMany(anyInt())).thenThrow(nodeAFailure);

    ClusterPipeline pipeline = pipeline();
    Response<String> replyA = pipeline.set(KEY_A, "1");

    assertSame(nodeAFailure, assertThrows(JedisConnectionException.class, pipeline::sync));
    assertSame(nodeAFailure, assertThrows(JedisConnectionException.class, replyA::get));
    verify(connA).close();

    Connection connA2 = mock(Connection.class);
    when(provider.getConnection(NODE_A)).thenReturn(connA2);
    pipeline.set(KEY_A, "2");
    verify(connA2).sendCommand(any(CommandArguments.class));
  }
}
