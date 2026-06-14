package redis.clients.jedis;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import redis.clients.jedis.Protocol.Command;
import redis.clients.jedis.exceptions.JedisClusterOperationException;
import redis.clients.jedis.exceptions.JedisException;
import redis.clients.jedis.providers.ClusterConnectionProvider;

public class MultiNodePipelineBaseDeadlockTest {

  private static final HostAndPort NODE_A = new HostAndPort("127.0.0.1", 7000);
  private static final HostAndPort NODE_B = new HostAndPort("127.0.0.1", 7001);

  private final ExecutorService executor = Executors.newCachedThreadPool();

  @AfterEach
  public void tearDown() throws InterruptedException {
    executor.shutdownNow();
    executor.awaitTermination(1, SECONDS);
  }

  @Test
  public void appendingCommandBorrowsAndWritesBeforeSync() {
    ControlledNodeConnections connections = ControlledNodeConnections.unbounded(NODE_A);
    TestPipeline pipeline = new TestPipeline(connections);

    Response<String> response = pipeline.sendTo(NODE_A, "a-1");

    assertEquals(1, connections.pool(NODE_A).borrowAttempts());
    assertEquals(1, connections.pool(NODE_A).connections().size());
    assertEquals(1, connections.pool(NODE_A).connections().get(0).sentCommands());
    assertThrows(IllegalStateException.class, response::get);

    pipeline.sync();

    assertEquals("OK", response.get());
  }

  @Test
  public void commandsForSameNodeReuseBorrowedConnectionUntilClose() {
    ControlledNodeConnections connections = ControlledNodeConnections.unbounded(NODE_A);
    TestPipeline pipeline = new TestPipeline(connections);

    pipeline.sendTo(NODE_A, "a-1");
    pipeline.sendTo(NODE_A, "a-2");

    assertEquals(1, connections.pool(NODE_A).borrowAttempts());
    assertEquals(1, connections.pool(NODE_A).connections().size());
    assertEquals(2, connections.pool(NODE_A).connections().get(0).sentCommands());
    assertFalse(connections.pool(NODE_A).connections().get(0).isClosed());

    pipeline.close();

    assertTrue(connections.pool(NODE_A).connections().get(0).isClosed());
  }

  @Test
  public void syncReadsResponsesButDoesNotReleaseBorrowedConnections() {
    ControlledNodeConnections connections = ControlledNodeConnections.unbounded(NODE_A);
    TestPipeline pipeline = new TestPipeline(connections);

    Response<String> response = pipeline.sendTo(NODE_A, "a-1");

    pipeline.sync();

    RecordingConnection connection = connections.pool(NODE_A).connections().get(0);
    assertEquals("OK", response.get());
    assertEquals(1, connection.getManyCalls());
    assertFalse(connection.isClosed());

    pipeline.close();

    assertTrue(connection.isClosed());
  }

  @Test
  public void syncReadsEachNodeQueueWithItsOwnPendingResponseCount() {
    ControlledNodeConnections connections = ControlledNodeConnections.unbounded(NODE_A, NODE_B);
    TestPipeline pipeline = new TestPipeline(connections);

    Response<String> firstA = pipeline.sendTo(NODE_A, "a-1");
    Response<String> firstB = pipeline.sendTo(NODE_B, "b-1");
    Response<String> secondA = pipeline.sendTo(NODE_A, "a-2");
    Response<String> secondB = pipeline.sendTo(NODE_B, "b-2");

    pipeline.sync();

    RecordingConnection connectionA = connections.pool(NODE_A).connections().get(0);
    RecordingConnection connectionB = connections.pool(NODE_B).connections().get(0);

    assertEquals(1, connections.pool(NODE_A).borrowAttempts());
    assertEquals(1, connections.pool(NODE_B).borrowAttempts());
    assertEquals(2, connectionA.sentCommands());
    assertEquals(2, connectionB.sentCommands());
    assertEquals(1, connectionA.getManyCalls());
    assertEquals(1, connectionB.getManyCalls());
    assertEquals(2, connectionA.lastGetManyCount());
    assertEquals(2, connectionB.lastGetManyCount());
    assertEquals("OK", firstA.get());
    assertEquals("OK", firstB.get());
    assertEquals("OK", secondA.get());
    assertEquals("OK", secondB.get());

    pipeline.close();
  }

  @Test
  @Timeout(5)
  public void oppositeSecondNodeAcquisitionOrderCanCircularlyBlockBeforeSync() throws Exception {
    ControlledNodeConnections connections = ControlledNodeConnections
        .limitedToOneImmediateBorrow(NODE_A, NODE_B);
    TestPipeline firstPipeline = new TestPipeline(connections);
    TestPipeline secondPipeline = new TestPipeline(connections);

    firstPipeline.sendTo(NODE_A, "first-a");
    secondPipeline.sendTo(NODE_B, "second-b");

    Future<Response<String>> firstWaitingForB = executor
        .submit(sendTo(firstPipeline, NODE_B, "first-b"));
    Future<Response<String>> secondWaitingForA = executor
        .submit(sendTo(secondPipeline, NODE_A, "second-a"));

    assertTrue(connections.pool(NODE_B).awaitBlockedBorrow());
    assertTrue(connections.pool(NODE_A).awaitBlockedBorrow());

    assertFutureStillWaiting(firstWaitingForB);
    assertFutureStillWaiting(secondWaitingForA);
    assertEquals(1, connections.pool(NODE_A).connections().size());
    assertEquals(1, connections.pool(NODE_B).connections().size());

    RecordingConnection firstA = connections.pool(NODE_A).connections().get(0);
    RecordingConnection secondB = connections.pool(NODE_B).connections().get(0);

    assertEquals(1, firstA.sentCommands());
    assertEquals(1, secondB.sentCommands());
    assertFalse(firstA.isClosed());
    assertFalse(secondB.isClosed());

    connections.releaseBlockedBorrows();

    Response<String> firstB = firstWaitingForB.get(1, SECONDS);
    Response<String> secondA = secondWaitingForA.get(1, SECONDS);
    assertEquals(2, connections.pool(NODE_A).connections().size());
    assertEquals(2, connections.pool(NODE_B).connections().size());

    firstPipeline.close();
    secondPipeline.close();

    assertEquals("OK", firstB.get());
    assertEquals("OK", secondA.get());
  }

  @Test
  @Timeout(5)
  public void blockedSecondNodeBorrowKeepsPreviouslyBorrowedConnectionHeld() throws Exception {
    ControlledNodeConnections connections = ControlledNodeConnections
        .withImmediateBorrows(Map.of(NODE_A, 1, NODE_B, 0));
    TestPipeline pipeline = new TestPipeline(connections);

    pipeline.sendTo(NODE_A, "a-1");

    Future<Response<String>> waitingForB = executor.submit(sendTo(pipeline, NODE_B, "b-1"));

    assertTrue(connections.pool(NODE_B).awaitBlockedBorrow());
    assertFutureStillWaiting(waitingForB);

    RecordingConnection firstA = connections.pool(NODE_A).connections().get(0);
    assertEquals(1, firstA.sentCommands());
    assertFalse(firstA.isClosed());

    connections.releaseBlockedBorrows();

    waitingForB.get(1, SECONDS);
    pipeline.close();
  }

  @Test
  public void firstNodeAcquisitionAllowsBlocking() {
    ControlledNodeConnections connections = ControlledNodeConnections.unbounded(NODE_A);
    PolicyAwareTestPipeline pipeline = new PolicyAwareTestPipeline(connections);

    pipeline.sendTo(NODE_A, "a-1");

    assertEquals(Collections.singletonList(true), pipeline.allowBlockingCalls());
    assertEquals(1, connections.pool(NODE_A).borrowAttempts());
  }

  @Test
  public void secondNodeAcquisitionDoesNotAllowBlocking() {
    ControlledNodeConnections connections = ControlledNodeConnections.unbounded(NODE_A, NODE_B);
    PolicyAwareTestPipeline pipeline = new PolicyAwareTestPipeline(connections);

    pipeline.sendTo(NODE_A, "a-1");
    pipeline.sendTo(NODE_B, "b-1");

    assertEquals(Arrays.asList(true, false), pipeline.allowBlockingCalls());
    assertEquals(1, connections.pool(NODE_A).borrowAttempts());
    assertEquals(1, connections.pool(NODE_B).borrowAttempts());

    pipeline.close();
  }

  @Test
  public void failingSecondNodeAcquisitionMarksHeldConnectionsBrokenAndClearsPipeline() {
    ControlledNodeConnections connections = ControlledNodeConnections.unbounded(NODE_A, NODE_B);
    PolicyAwareTestPipeline pipeline = new PolicyAwareTestPipeline(connections);
    pipeline.failNonBlockingAcquisition(new JedisException("node B exhausted"));

    Response<String> first = pipeline.sendTo(NODE_A, "a-1");

    JedisException thrown = assertThrows(JedisException.class,
      () -> pipeline.sendTo(NODE_B, "b-1"));

    assertEquals("node B exhausted", thrown.getMessage());
    assertEquals(Arrays.asList(true, false), pipeline.allowBlockingCalls());
    RecordingConnection held = connections.pool(NODE_A).connections().get(0);
    assertTrue(held.isBroken());
    assertTrue(held.isClosed());
    assertEquals(1, held.closeCalls());
    assertEquals(0, held.getManyCalls());
    assertThrows(IllegalStateException.class, first::get);

    pipeline.close();

    assertEquals(0, held.getManyCalls());
    assertEquals(1, held.closeCalls());
    assertEquals(0, connections.pool(NODE_B).borrowAttempts());
  }

  @Test
  public void failingFirstNodeAcquisitionDoesNotRunHeldConnectionCleanup() {
    ControlledNodeConnections connections = ControlledNodeConnections.unbounded(NODE_A);
    PolicyAwareTestPipeline pipeline = new PolicyAwareTestPipeline(connections);
    pipeline.failAllAcquisitions(new JedisException("node A unavailable"));

    JedisException thrown = assertThrows(JedisException.class,
      () -> pipeline.sendTo(NODE_A, "a-1"));

    assertEquals("node A unavailable", thrown.getMessage());
    assertEquals(Collections.singletonList(true), pipeline.allowBlockingCalls());
    assertEquals(0, connections.pool(NODE_A).borrowAttempts());
    assertTrue(connections.pool(NODE_A).connections().isEmpty());
  }

  @Test
  public void clusterPipelineUsesBlockingProviderForFirstNode() {
    FakeClusterConnectionProvider provider = new FakeClusterConnectionProvider(NODE_A);
    RecordingConnection connection = new RecordingConnection(NODE_A);
    provider.useBlockingConnection(NODE_A, connection);
    ClusterPipeline pipeline = new ClusterPipeline(provider,
        new ClusterCommandObjects(RedisProtocol.RESP2));

    Response<String> response = pipeline.set("first", "value");

    assertEquals(1, connection.sentCommands());
    assertEquals(Collections.singletonList(NODE_A), provider.blockingNodes());
    assertTrue(provider.nonBlockingAcquisitions().isEmpty());

    pipeline.sync();

    assertEquals("OK", response.get());
  }

  @Test
  public void clusterPipelineUsesZeroWaitProviderForSecondNode() {
    FakeClusterConnectionProvider provider = new FakeClusterConnectionProvider(NODE_A, NODE_B);
    RecordingConnection firstConnection = new RecordingConnection(NODE_A);
    RecordingConnection secondConnection = new RecordingConnection(NODE_B);
    provider.useBlockingConnection(NODE_A, firstConnection);
    provider.useNonBlockingConnection(NODE_B, secondConnection);
    ClusterPipeline pipeline = new ClusterPipeline(provider,
        new ClusterCommandObjects(RedisProtocol.RESP2));

    Response<String> first = pipeline.set("first", "value");
    Response<String> second = pipeline.set("second", "value");

    assertEquals(1, firstConnection.sentCommands());
    assertEquals(1, secondConnection.sentCommands());
    assertEquals(Collections.singletonList(NODE_A), provider.blockingNodes());
    assertEquals(Collections.singletonList(new Acquisition(NODE_B, Duration.ZERO)),
      provider.nonBlockingAcquisitions());

    pipeline.sync();

    assertEquals("OK", first.get());
    assertEquals("OK", second.get());
  }

  @Test
  public void clusterPipelineReusesSameNodeWithoutZeroWaitAcquisition() {
    FakeClusterConnectionProvider provider = new FakeClusterConnectionProvider(NODE_A);
    RecordingConnection connection = new RecordingConnection(NODE_A);
    provider.useBlockingConnection(NODE_A, connection);
    ClusterPipeline pipeline = new ClusterPipeline(provider,
        new ClusterCommandObjects(RedisProtocol.RESP2));

    pipeline.set("first", "value");
    pipeline.set("second", "value");

    assertEquals(2, connection.sentCommands());
    assertEquals(Collections.singletonList(NODE_A), provider.blockingNodes());
    assertTrue(provider.nonBlockingAcquisitions().isEmpty());

    pipeline.close();
  }

  @Test
  public void clusterPipelineFailFastMessageExplainsDeadlockAndPoolGuidance() {
    FakeClusterConnectionProvider provider = new FakeClusterConnectionProvider(NODE_A, NODE_B);
    RecordingConnection firstConnection = new RecordingConnection(NODE_A);
    JedisException poolExhausted = new JedisException("pool exhausted");
    provider.useBlockingConnection(NODE_A, firstConnection);
    provider.failNonBlockingAcquisition(poolExhausted);
    ClusterPipeline pipeline = new ClusterPipeline(provider,
        new ClusterCommandObjects(RedisProtocol.RESP2));

    Response<String> first = pipeline.set("first", "value");

    JedisClusterOperationException thrown = assertThrows(JedisClusterOperationException.class,
      () -> pipeline.set("second", "value"));

    assertSame(poolExhausted, thrown.getCause());
    assertEquals(Collections.singletonList(NODE_A), provider.blockingNodes());
    assertEquals(Collections.singletonList(new Acquisition(NODE_B, Duration.ZERO)),
      provider.nonBlockingAcquisitions());
    assertMessageContains(thrown, "without waiting");
    assertMessageContains(thrown, "deadlock");
    assertMessageContains(thrown, "maxTotal");
    assertMessageContains(thrown, "maxWait");
    assertMessageContains(thrown, "single shard");
    assertTrue(firstConnection.isBroken());
    assertTrue(firstConnection.isClosed());
    assertEquals(0, firstConnection.getManyCalls());
    assertThrows(IllegalStateException.class, first::get);

    pipeline.close();

    assertEquals(0, firstConnection.getManyCalls());
    assertEquals(1, firstConnection.closeCalls());
  }

  @Test
  @Timeout(2)
  public void connectionPoolDurationBorrowFailsFastWhenPoolIsExhausted() {
    GenericObjectPoolConfig<Connection> config = new GenericObjectPoolConfig<>();
    config.setMaxTotal(1);
    config.setBlockWhenExhausted(true);
    config.setMaxWait(Duration.ofSeconds(30));
    ConnectionPool pool = new ConnectionPool(new PlainConnectionFactory(), config);
    Connection first = pool.getResource();

    try {
      JedisException thrown = assertThrows(JedisException.class,
        () -> pool.getResource(Duration.ZERO));

      assertMessageContains(thrown, "Could not get a resource from the pool");
    } finally {
      first.close();
      pool.close();
    }
  }

  private static Callable<Response<String>> sendTo(TestPipeline pipeline, HostAndPort node,
      String key) {
    return () -> pipeline.sendTo(node, key);
  }

  private static void assertMessageContains(Throwable thrown, String expected) {
    assertTrue(thrown.getMessage().contains(expected),
      "Expected message to contain '" + expected + "' but was: " + thrown.getMessage());
  }

  private static void assertFutureStillWaiting(Future<?> future) throws Exception {
    try {
      future.get(100, MILLISECONDS);
      throw new AssertionError("Expected future to remain blocked");
    } catch (TimeoutException expected) {
      assertFalse(future.isDone());
    }
  }

  private static final class TestPipeline extends MultiNodePipelineBase {

    private final ControlledNodeConnections connections;
    private final Map<CommandArguments, HostAndPort> routes = new IdentityHashMap<>();

    TestPipeline(ControlledNodeConnections connections) {
      super(new CommandObjects(RedisProtocol.RESP2));
      this.connections = connections;
    }

    Response<String> sendTo(HostAndPort node, String key) {
      CommandArguments args = new CommandArguments(Command.SET).key(key).add("value");
      routes.put(args, node);
      return appendCommand(new CommandObject<>(args, BuilderFactory.STRING));
    }

    @Override
    protected HostAndPort getNodeKey(CommandArguments args) {
      return routes.get(args);
    }

    @Override
    protected Connection getConnection(HostAndPort nodeKey) {
      return connections.pool(nodeKey).borrow();
    }
  }

  private static final class PolicyAwareTestPipeline extends MultiNodePipelineBase {

    private final ControlledNodeConnections connections;
    private final Map<CommandArguments, HostAndPort> routes = new IdentityHashMap<>();
    private final List<Boolean> allowBlockingCalls = new ArrayList<>();
    private RuntimeException failAllAcquisitions;
    private RuntimeException failNonBlockingAcquisition;

    PolicyAwareTestPipeline(ControlledNodeConnections connections) {
      super(new CommandObjects(RedisProtocol.RESP2));
      this.connections = connections;
    }

    Response<String> sendTo(HostAndPort node, String key) {
      CommandArguments args = new CommandArguments(Command.SET).key(key).add("value");
      routes.put(args, node);
      return appendCommand(new CommandObject<>(args, BuilderFactory.STRING));
    }

    void failAllAcquisitions(RuntimeException failure) {
      this.failAllAcquisitions = failure;
    }

    void failNonBlockingAcquisition(RuntimeException failure) {
      this.failNonBlockingAcquisition = failure;
    }

    List<Boolean> allowBlockingCalls() {
      return allowBlockingCalls;
    }

    @Override
    protected HostAndPort getNodeKey(CommandArguments args) {
      return routes.get(args);
    }

    @Override
    protected Connection getConnection(HostAndPort nodeKey) {
      return connections.pool(nodeKey).borrow();
    }

    @Override
    protected Connection getConnection(HostAndPort nodeKey, boolean allowBlocking) {
      allowBlockingCalls.add(allowBlocking);
      if (failAllAcquisitions != null) {
        throw failAllAcquisitions;
      }
      if (!allowBlocking && failNonBlockingAcquisition != null) {
        throw failNonBlockingAcquisition;
      }
      return getConnection(nodeKey);
    }
  }

  private static final class FakeClusterConnectionProvider extends ClusterConnectionProvider {

    private static String previousInitNoErrorProperty;

    private final List<HostAndPort> nodeSequence;
    private final AtomicInteger nodeLookups = new AtomicInteger();
    private final Map<HostAndPort, RecordingConnection> blockingConnections = new HashMap<>();
    private final Map<HostAndPort, RecordingConnection> nonBlockingConnections = new HashMap<>();
    private final List<HostAndPort> blockingNodes = new ArrayList<>();
    private final List<Acquisition> nonBlockingAcquisitions = new ArrayList<>();
    private RuntimeException nonBlockingFailure;

    FakeClusterConnectionProvider(HostAndPort... nodeSequence) {
      super(prepareInitNoErrorNodes(), DefaultJedisClientConfig.builder().connectionTimeoutMillis(1)
          .socketTimeoutMillis(1).build());
      restoreInitNoErrorProperty();
      this.nodeSequence = Arrays.asList(nodeSequence);
    }

    private static Set<HostAndPort> prepareInitNoErrorNodes() {
      previousInitNoErrorProperty = System.getProperty(RedisClusterClient.INIT_NO_ERROR_PROPERTY);
      System.setProperty(RedisClusterClient.INIT_NO_ERROR_PROPERTY, "true");
      return Collections.singleton(new HostAndPort("127.0.0.1", 1));
    }

    private static void restoreInitNoErrorProperty() {
      if (previousInitNoErrorProperty == null) {
        System.clearProperty(RedisClusterClient.INIT_NO_ERROR_PROPERTY);
      } else {
        System.setProperty(RedisClusterClient.INIT_NO_ERROR_PROPERTY, previousInitNoErrorProperty);
      }
    }

    void useBlockingConnection(HostAndPort node, RecordingConnection connection) {
      blockingConnections.put(node, connection);
    }

    void useNonBlockingConnection(HostAndPort node, RecordingConnection connection) {
      nonBlockingConnections.put(node, connection);
    }

    void failNonBlockingAcquisition(RuntimeException failure) {
      this.nonBlockingFailure = failure;
    }

    List<HostAndPort> blockingNodes() {
      return blockingNodes;
    }

    List<Acquisition> nonBlockingAcquisitions() {
      return nonBlockingAcquisitions;
    }

    @Override
    public HostAndPort getNode(int slot) {
      int index = Math.min(nodeLookups.getAndIncrement(), nodeSequence.size() - 1);
      return nodeSequence.get(index);
    }

    @Override
    public Connection getConnection(HostAndPort node) {
      blockingNodes.add(node);
      RecordingConnection connection = blockingConnections.get(node);
      if (connection == null) {
        throw new JedisException("No blocking connection configured for " + node);
      }
      return connection;
    }

    @Override
    public Connection getConnection(HostAndPort node, Duration maxWait) {
      nonBlockingAcquisitions.add(new Acquisition(node, maxWait));
      if (nonBlockingFailure != null) {
        throw nonBlockingFailure;
      }
      RecordingConnection connection = nonBlockingConnections.get(node);
      if (connection == null) {
        throw new JedisException("No non-blocking connection configured for " + node);
      }
      return connection;
    }

    @Override
    public void close() {
      // No-op. This fake provider does not own real pools.
    }
  }

  private static final class Acquisition {

    private final HostAndPort node;
    private final Duration maxWait;

    Acquisition(HostAndPort node, Duration maxWait) {
      this.node = node;
      this.maxWait = maxWait;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Acquisition)) {
        return false;
      }
      Acquisition that = (Acquisition) o;
      return Objects.equals(node, that.node) && Objects.equals(maxWait, that.maxWait);
    }

    @Override
    public int hashCode() {
      return Objects.hash(node, maxWait);
    }

    @Override
    public String toString() {
      return "Acquisition{" + node + ", maxWait=" + maxWait + '}';
    }
  }

  private static final class ControlledNodeConnections {

    private final Map<HostAndPort, BlockingNodePool> pools;

    private ControlledNodeConnections(Map<HostAndPort, BlockingNodePool> pools) {
      this.pools = pools;
    }

    static ControlledNodeConnections unbounded(HostAndPort... nodes) {
      Map<HostAndPort, Integer> immediateBorrows = new java.util.HashMap<>();
      for (HostAndPort node : nodes) {
        immediateBorrows.put(node, Integer.MAX_VALUE);
      }
      return withImmediateBorrows(immediateBorrows);
    }

    static ControlledNodeConnections limitedToOneImmediateBorrow(HostAndPort... nodes) {
      Map<HostAndPort, Integer> immediateBorrows = new java.util.HashMap<>();
      for (HostAndPort node : nodes) {
        immediateBorrows.put(node, 1);
      }
      return withImmediateBorrows(immediateBorrows);
    }

    static ControlledNodeConnections withImmediateBorrows(
        Map<HostAndPort, Integer> immediateBorrows) {
      Map<HostAndPort, BlockingNodePool> pools = new java.util.HashMap<>();
      for (Map.Entry<HostAndPort, Integer> entry : immediateBorrows.entrySet()) {
        pools.put(entry.getKey(), new BlockingNodePool(entry.getKey(), entry.getValue()));
      }
      return new ControlledNodeConnections(pools);
    }

    BlockingNodePool pool(HostAndPort node) {
      BlockingNodePool pool = pools.get(node);
      if (pool == null) {
        throw new IllegalArgumentException("No pool configured for " + node);
      }
      return pool;
    }

    void releaseBlockedBorrows() {
      for (BlockingNodePool pool : pools.values()) {
        pool.releaseBlockedBorrows();
      }
    }
  }

  private static final class BlockingNodePool {

    private final HostAndPort node;
    private final int immediateBorrows;
    private final AtomicInteger borrowAttempts = new AtomicInteger();
    private final CountDownLatch blockedBorrowStarted = new CountDownLatch(1);
    private final CountDownLatch releaseBlockedBorrow = new CountDownLatch(1);
    private final List<RecordingConnection> connections = new CopyOnWriteArrayList<>();

    BlockingNodePool(HostAndPort node, int immediateBorrows) {
      this.node = node;
      this.immediateBorrows = immediateBorrows;
    }

    RecordingConnection borrow() {
      int attempt = borrowAttempts.incrementAndGet();
      if (attempt > immediateBorrows) {
        blockedBorrowStarted.countDown();
        try {
          releaseBlockedBorrow.await();
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          throw new RuntimeException(ex);
        }
      }

      RecordingConnection connection = new RecordingConnection(node);
      connections.add(connection);
      return connection;
    }

    int borrowAttempts() {
      return borrowAttempts.get();
    }

    List<RecordingConnection> connections() {
      return connections;
    }

    boolean awaitBlockedBorrow() throws InterruptedException {
      return blockedBorrowStarted.await(1, SECONDS);
    }

    void releaseBlockedBorrows() {
      releaseBlockedBorrow.countDown();
    }
  }

  private static final class RecordingConnection extends Connection {

    private final HostAndPort node;
    private final AtomicInteger sentCommands = new AtomicInteger();
    private final AtomicInteger getManyCalls = new AtomicInteger();
    private final AtomicInteger lastGetManyCount = new AtomicInteger();
    private final AtomicInteger closeCalls = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();

    RecordingConnection(HostAndPort node) {
      this.node = node;
    }

    @Override
    public void sendCommand(CommandArguments args) {
      sentCommands.incrementAndGet();
    }

    @Override
    public List<Object> getMany(int count) {
      getManyCalls.incrementAndGet();
      lastGetManyCount.set(count);
      List<Object> replies = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
        replies.add("OK".getBytes(UTF_8));
      }
      return replies;
    }

    @Override
    public void close() {
      closeCalls.incrementAndGet();
      closed.set(true);
    }

    int sentCommands() {
      return sentCommands.get();
    }

    int getManyCalls() {
      return getManyCalls.get();
    }

    int lastGetManyCount() {
      return lastGetManyCount.get();
    }

    boolean isClosed() {
      return closed.get();
    }

    int closeCalls() {
      return closeCalls.get();
    }

    @Override
    public String toString() {
      return "RecordingConnection{" + node + '}';
    }
  }

  private static final class PlainConnectionFactory extends BasePooledObjectFactory<Connection> {

    @Override
    public Connection create() {
      return new Connection();
    }

    @Override
    public PooledObject<Connection> wrap(Connection connection) {
      return new DefaultPooledObject<>(connection);
    }
  }
}
