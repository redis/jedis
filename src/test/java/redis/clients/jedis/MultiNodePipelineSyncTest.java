package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
 * fails, the failed node must be dropped from the pipeline's bookkeeping so that the next command
 * for it obtains a fresh connection, and the other nodes must be left untouched.
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

  @Test
  void connectionFailureDuringSyncDropsOnlyTheFailedNode() throws Exception {
    Connection connA = mock(Connection.class);
    Connection connB = mock(Connection.class);
    when(provider.getConnection(NODE_A)).thenReturn(connA);
    when(provider.getConnection(NODE_B)).thenReturn(connB);

    // Node A fails only once node B's read has started, i.e. after the calling thread has
    // iterated past node A's entry.
    CountDownLatch nodeBReading = new CountDownLatch(1);
    when(connA.getMany(1)).thenAnswer(invocation -> {
      nodeBReading.await(5, TimeUnit.SECONDS);
      throw new JedisConnectionException("node-a went away");
    });
    when(connB.getMany(1)).thenAnswer(invocation -> {
      nodeBReading.countDown();
      return Collections.singletonList(SafeEncoder.encode("OK"));
    });

    ClusterPipeline pipeline = new ClusterPipeline(provider,
        new ClusterCommandObjects(RedisProtocol.RESP2), StaticCommandFlagsRegistry.registry(),
        executor);
    pipeline.set(KEY_A, "1");
    Response<String> replyB = pipeline.set(KEY_B, "1");
    pipeline.sync();

    assertEquals("OK", replyB.get());
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
}
