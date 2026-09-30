package redis.clients.jedis;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import redis.clients.jedis.csc.Cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static redis.clients.jedis.JedisClusterInfoCache.getNodeKey;
import static redis.clients.jedis.Protocol.Command.CLUSTER;
import static redis.clients.jedis.util.CommandArgumentsMatchers.commandWithArgs;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class JedisClusterInfoCacheTest {

  private static final HostAndPort MASTER_HOST = new HostAndPort("127.0.0.1", 7000);
  private static final HostAndPort REPLICA_1_HOST = new HostAndPort("127.0.0.1", 7001);
  private static final HostAndPort REPLICA_2_HOST = new HostAndPort("127.0.0.1", 7002);
  private static final int TEST_SLOT = 0;

  @Mock
  private Connection mockConnection;
  @Mock
  private Cache mockClientSideCache;
  @Mock
  private Connection mockEventConnection;

  @Test
  public void testReplicaNodeRemovalAndRediscovery() {
    // Create client config with read-only replicas enabled
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .readOnlyForRedisClusterReplicas().build();

    Set<HostAndPort> startNodes = new HashSet<>();
    startNodes.add(MASTER_HOST);

    JedisClusterInfoCache cache = new JedisClusterInfoCache(clientConfig, startNodes);

    // Mock the cluster slots responses
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS")))).thenReturn(
            masterReplicaSlotsResponse(MASTER_HOST, REPLICA_1_HOST)).thenReturn(masterOnlySlotsResponse())
        .thenReturn(masterReplica2SlotsResponse());

    // Initial discovery with one master and one replica (replica-1)
    cache.discoverClusterNodesAndSlots(mockConnection);
    assertMasterNodeAvailable(cache);
    assertReplicasAvailable(cache, REPLICA_1_HOST);

    // Simulate rediscovery - master only
    cache.discoverClusterNodesAndSlots(mockConnection);
    // Master should still be available
    // Replica should be cleared
    assertMasterNodeAvailable(cache);
    assertNoReplicasAvailable(cache);

    // Simulate rediscovery - another replica (replica-2) coming back
    cache.reset();
    cache.discoverClusterNodesAndSlots(mockConnection);
    assertReplicasAvailable(cache, REPLICA_2_HOST);
  }

  @Test
  public void testResetWithReplicaSlots() {
    // This test verifies that reset() properly clears replica slots

    JedisClusterInfoCache cache = createCacheWithReplicasEnabled();

    // Mock the cluster slots responses
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS")))).thenReturn(
        masterReplicaSlotsResponse(MASTER_HOST, REPLICA_1_HOST));

    // Initial discovery
    cache.discoverClusterNodesAndSlots(mockConnection);
    assertReplicasAvailable(cache, REPLICA_1_HOST);

    // Call reset() - this should clear and nullify replica slots
    cache.reset();

    assertNoReplicasAvailable(cache);

    // Rediscovery should work correctly
    cache.discoverClusterNodesAndSlots(mockConnection);
    assertReplicasAvailable(cache, REPLICA_1_HOST);
  }

  @Test
  public void getPrimaryNodesAfterReplicaNodeRemovalAndRediscovery() {
    // Create client config with read-only replicas enabled
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
            .readOnlyForRedisClusterReplicas().build();

    Set<HostAndPort> startNodes = new HashSet<>();
    startNodes.add(MASTER_HOST);

    JedisClusterInfoCache cache = new JedisClusterInfoCache(clientConfig, startNodes);

    // Mock the cluster slots responses
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS")))).thenReturn(
                    masterReplicaSlotsResponse(MASTER_HOST, REPLICA_1_HOST)).thenReturn(masterOnlySlotsResponse())
            .thenReturn(masterReplica2SlotsResponse());

    // Initial discovery with one master and one replica (replica-1)
    cache.discoverClusterNodesAndSlots(mockConnection);
    assertThat(cache.getPrimaryNodes(),aMapWithSize(1));
    assertThat(cache.getPrimaryNodes(),
                    hasEntry(equalTo(getNodeKey(MASTER_HOST)), equalTo(cache.getNode(MASTER_HOST))));

    // Simulate rediscovery - master only
    cache.discoverClusterNodesAndSlots(mockConnection);
    assertThat(  cache.getPrimaryNodes(),aMapWithSize(1));
    assertThat(cache.getPrimaryNodes(),
            hasEntry(equalTo(getNodeKey(MASTER_HOST)), equalTo(cache.getNode(MASTER_HOST))));
  }

  @Test
  public void getPrimaryNodesAfterMasterReplicaFailover() {
    // Create client config with read-only replicas enabled
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
            .readOnlyForRedisClusterReplicas().build();

    Set<HostAndPort> startNodes = new HashSet<>();
    startNodes.add(MASTER_HOST);

    JedisClusterInfoCache cache = new JedisClusterInfoCache(clientConfig, startNodes);

    // Mock the cluster slots responses
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS"))))
            .thenReturn(masterReplicaSlotsResponse(MASTER_HOST, REPLICA_1_HOST))
            .thenReturn(masterReplicaSlotsResponse(REPLICA_1_HOST, MASTER_HOST));

    // Initial discovery with one master and one replica (replica-1)
    cache.discoverClusterNodesAndSlots(mockConnection);
    assertThat(cache.getPrimaryNodes(),aMapWithSize(1));
    assertThat(cache.getPrimaryNodes(),
            hasEntry(equalTo(getNodeKey(MASTER_HOST)), equalTo(cache.getNode(MASTER_HOST))));

    // Simulate rediscovery - master only
    cache.discoverClusterNodesAndSlots(mockConnection);
    assertThat(  cache.getPrimaryNodes(),aMapWithSize(1));
    assertThat(cache.getPrimaryNodes(),
            hasEntry(equalTo(getNodeKey(REPLICA_1_HOST)), equalTo(cache.getNode(REPLICA_1_HOST))));
  }

  @Test
  public void getPrimaryNodesAfterMasterReplicaFailoverOnRenew() {
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
            .readOnlyForRedisClusterReplicas().build();

    Set<HostAndPort> startNodes = new HashSet<>();
    startNodes.add(MASTER_HOST);

    JedisClusterInfoCache cache = new JedisClusterInfoCache(clientConfig, startNodes);

    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS"))))
            .thenReturn(masterReplicaSlotsResponse(MASTER_HOST, REPLICA_1_HOST))
            .thenReturn(masterReplicaSlotsResponse(REPLICA_1_HOST, MASTER_HOST));

    cache.discoverClusterNodesAndSlots(mockConnection);
    assertThat(cache.getPrimaryNodes(),
            hasEntry(equalTo(getNodeKey(MASTER_HOST)), equalTo(cache.getNode(MASTER_HOST))));

    // Failover picked up through the slot renewal path (MOVED / topology refresh)
    cache.renewClusterSlots(mockConnection);
    assertThat(cache.getPrimaryNodes(), aMapWithSize(1));
    assertThat(cache.getPrimaryNodes(),
            hasEntry(equalTo(getNodeKey(REPLICA_1_HOST)), equalTo(cache.getNode(REPLICA_1_HOST))));
    assertThat(cache.getShuffledPrimaryNodesPool(), equalTo(
            Collections.singletonList(cache.getNode(REPLICA_1_HOST))));
  }

  @Test
  public void applySlotMigrationDestroysOnlyTheDeltaSourcesLeftWithoutSlots() {
    HostAndPort masterA = MASTER_HOST;
    HostAndPort masterB = REPLICA_1_HOST;
    HostAndPort masterC = REPLICA_2_HOST;
    JedisClusterInfoCache cache = new JedisClusterInfoCache(DefaultJedisClientConfig.builder().build(),
        new HashSet<>(Collections.singletonList(masterA)));
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS"))))
        .thenReturn(createClusterSlotsResponse(
          new SlotRange.Builder(0, 8191).master(masterA, "a").build(),
          new SlotRange.Builder(8192, 16383).master(masterB, "b").build()));
    cache.discoverClusterNodesAndSlots(mockConnection);
    ConnectionPool poolA = cache.getNode(masterA);
    assertThat(cache.getPrimaryNodes(), aMapWithSize(2));

    // the delta names C as the source, yet A is the one emptied: A leaves the primary set with the
    // slot table, but only the delta's own sources are candidates for pool removal
    cache.applySlotMigration(Collections.singletonList(
      new SlotMigration(masterC, masterB, HashSlotRanges.parse("0-8191"))));

    assertEquals(masterB, cache.getSlotNode(0));
    assertEquals(cache.getNode(masterB), cache.getSlotPool(0));
    assertThat(cache.getPrimaryNodes(), aMapWithSize(1));
    assertThat(cache.getPrimaryNodes(), hasKey(getNodeKey(masterB)));
    assertFalse(poolA.isClosed(), "A is not a source of this delta: the refresh sweeps it");
    assertEquals(poolA, cache.getNode(masterA));
    assertFalse(cache.hasPendingSlotDeltas());
  }

  @Test
  public void applySlotMigrationFlushesClientSideCache() {
    JedisClusterInfoCache cache = new JedisClusterInfoCache(DefaultJedisClientConfig.builder().build(),
        mockClientSideCache, new HashSet<>(Collections.singletonList(MASTER_HOST)));
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS"))))
        .thenReturn(masterOnlySlotsResponse());
    cache.renewClusterSlots(mockConnection);
    verify(mockClientSideCache, times(1)).flush(); // the full refresh flushes

    cache.applySlotMigration(Collections.singletonList(
      new SlotMigration(MASTER_HOST, REPLICA_1_HOST, HashSlotRanges.parse("0-100"))));

    assertEquals(REPLICA_1_HOST, cache.getSlotNode(0));
    verify(mockClientSideCache, times(2)).flush(); // and so does the delta
  }

  @Test
  public void slotDeltaQueuedDuringRefreshAppliesAfterIt() throws Exception {
    JedisClusterInfoCache cache = new JedisClusterInfoCache(DefaultJedisClientConfig.builder().build(),
        new HashSet<>(Collections.singletonList(MASTER_HOST)));
    CountDownLatch querying = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS")))).thenAnswer(inv -> {
      querying.countDown();
      release.await(5, TimeUnit.SECONDS); // refresh blocked mid-query, holding both locks
      return masterOnlySlotsResponse();
    });
    Thread refresh = new Thread(() -> cache.renewClusterSlots(mockConnection));
    refresh.start();
    assertTrue(querying.await(5, TimeUnit.SECONDS));

    // the read thread must not block behind the refresh: the delta is queued and left behind
    cache.applySlotMigration(Collections.singletonList(
      new SlotMigration(MASTER_HOST, REPLICA_1_HOST, HashSlotRanges.parse("0-100"))));
    assertTrue(cache.hasPendingSlotDeltas());
    assertNull(cache.getSlotNode(0));

    release.countDown();
    refresh.join(5000);
    assertFalse(refresh.isAlive());
    // the refreshing thread drained the queue after applying the topology: delta ordered last
    assertEquals(REPLICA_1_HOST, cache.getSlotNode(0));
    assertEquals(MASTER_HOST, cache.getSlotNode(101));
    assertFalse(cache.hasPendingSlotDeltas());
  }

  @Test
  public void applySlotMigrationDestroysTheDepartedSourcePool() {
    HostAndPort masterA = MASTER_HOST;
    HostAndPort masterB = REPLICA_1_HOST;
    JedisClusterInfoCache cache = new JedisClusterInfoCache(DefaultJedisClientConfig.builder().build(),
        new HashSet<>(Collections.singletonList(masterA)));
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS"))))
        .thenReturn(createClusterSlotsResponse(
          new SlotRange.Builder(0, 8191).master(masterA, "a").build(),
          new SlotRange.Builder(8192, 16383).master(masterB, "b").build()));
    cache.discoverClusterNodesAndSlots(mockConnection);
    ConnectionPool poolA = cache.getNode(masterA);
    ConnectionPool poolB = cache.getNode(masterB);

    List<SlotMigration> delta = Collections.singletonList(
      new SlotMigration(masterA, masterB, HashSlotRanges.parse("0-8191")));
    cache.applySlotMigration(delta);

    assertTrue(poolA.isClosed());
    assertNull(cache.getNode(masterA));
    assertThat(cache.getNodes(), aMapWithSize(1));
    assertThat(cache.getNodes(), hasKey(getNodeKey(masterB)));
    assertThat(cache.getPrimaryNodes(), aMapWithSize(1));
    assertFalse(poolB.isClosed());
    assertEquals(poolB, cache.getSlotPool(0));
    // a re-delivered delta finds nothing left to remove
    cache.applySlotMigration(delta);
    assertThat(cache.getNodes(), aMapWithSize(1));
    assertFalse(poolB.isClosed());
  }

  @Test
  public void applySlotMigrationKeepsASourceStillOwningSlots() {
    HostAndPort masterA = MASTER_HOST;
    HostAndPort masterB = REPLICA_1_HOST;
    JedisClusterInfoCache cache = new JedisClusterInfoCache(DefaultJedisClientConfig.builder().build(),
        new HashSet<>(Collections.singletonList(masterA)));
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS"))))
        .thenReturn(createClusterSlotsResponse(
          new SlotRange.Builder(0, 8191).master(masterA, "a").build(),
          new SlotRange.Builder(8192, 16383).master(masterB, "b").build()));
    cache.discoverClusterNodesAndSlots(mockConnection);
    ConnectionPool poolA = cache.getNode(masterA);

    cache.applySlotMigration(Collections.singletonList(
      new SlotMigration(masterA, masterB, HashSlotRanges.parse("0-100"))));

    assertEquals(masterB, cache.getSlotNode(0));
    assertEquals(masterA, cache.getSlotNode(101));
    assertFalse(poolA.isClosed());
    assertEquals(poolA, cache.getNode(masterA));
    assertThat(cache.getPrimaryNodes(), aMapWithSize(2));
  }

  @Test
  public void applySlotMigrationKeepsAReplicaServingSource() {
    JedisClusterInfoCache cache = createCacheWithReplicasEnabled();
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS"))))
        .thenReturn(masterReplicaSlotsResponse(MASTER_HOST, REPLICA_1_HOST));
    cache.discoverClusterNodesAndSlots(mockConnection);
    ConnectionPool replicaPool = cache.getNode(REPLICA_1_HOST);

    // a source that owns no primary slot but serves reads is not departed
    cache.applySlotMigration(Collections.singletonList(
      new SlotMigration(REPLICA_1_HOST, MASTER_HOST, HashSlotRanges.parse("0-100"))));
    assertFalse(replicaPool.isClosed());
    assertEquals(replicaPool, cache.getNode(REPLICA_1_HOST));
    assertReplicasAvailable(cache, REPLICA_1_HOST);
  }

  @Test
  public void chainedMigrationInOneDeltaKeepsTheIntermediateNode() {
    HostAndPort masterA = MASTER_HOST;
    HostAndPort masterB = REPLICA_1_HOST;
    HostAndPort masterC = REPLICA_2_HOST;
    JedisClusterInfoCache cache = new JedisClusterInfoCache(DefaultJedisClientConfig.builder().build(),
        new HashSet<>(Collections.singletonList(masterA)));
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS"))))
        .thenReturn(createClusterSlotsResponse(
          new SlotRange.Builder(0, 8191).master(masterA, "a").build(),
          new SlotRange.Builder(8192, 16383).master(masterB, "b").build()));
    cache.discoverClusterNodesAndSlots(mockConnection);
    ConnectionPool poolA = cache.getNode(masterA);
    ConnectionPool poolB = cache.getNode(masterB);

    // B is both a source and a destination: it still owns slots after the delta and must stay
    cache.applySlotMigration(Arrays.asList(
      new SlotMigration(masterA, masterC, HashSlotRanges.parse("0-8191")),
      new SlotMigration(masterB, masterC, HashSlotRanges.parse("8192-9000"))));

    assertEquals(masterC, cache.getSlotNode(0));
    assertEquals(masterC, cache.getSlotNode(9000));
    assertEquals(masterB, cache.getSlotNode(9001));
    assertTrue(poolA.isClosed());
    assertNull(cache.getNode(masterA));
    assertFalse(poolB.isClosed());
    assertEquals(poolB, cache.getNode(masterB));
    assertThat(cache.getNodes(), aMapWithSize(2));
  }

  @Test
  public void refreshCleansUpNodesEmptiedByDeltasDeferredBehindIt() throws Exception {
    HostAndPort masterA = MASTER_HOST;
    HostAndPort masterB = REPLICA_1_HOST;
    JedisClusterInfoCache cache = new JedisClusterInfoCache(DefaultJedisClientConfig.builder().build(),
        new HashSet<>(Collections.singletonList(masterA)));
    ClusterMaintenanceCoordinator coordinator = new ClusterMaintenanceCoordinator(cache,
        MaintenanceNotificationsConfig.builder().build());
    CountDownLatch querying = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    // the snapshot predates the migration: A is still listed, so the refresh's own dead-node
    // sweep keeps it
    List<Object> twoMasters = createClusterSlotsResponse(
      new SlotRange.Builder(0, 8191).master(masterA, "a").build(),
      new SlotRange.Builder(8192, 16383).master(masterB, "b").build());
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS"))))
        .thenReturn(twoMasters).thenAnswer(inv -> {
          querying.countDown();
          release.await(5, TimeUnit.SECONDS);
          return twoMasters;
        });
    cache.discoverClusterNodesAndSlots(mockConnection);
    ConnectionPool poolA = cache.getNode(masterA);

    Thread refresh = new Thread(() -> cache.renewClusterSlots(mockConnection));
    refresh.start();
    assertTrue(querying.await(5, TimeUnit.SECONDS));
    // SMIGRATED lands mid-refresh: its delta, and with it the source's pool removal, is deferred
    coordinator.onSMigrated(new SMigratedEvent(2L, Collections.singletonList(
      new SlotMigration(masterA, masterB, HashSlotRanges.parse("0-8191")))), mockEventConnection);
    assertTrue(cache.hasPendingSlotDeltas());
    assertFalse(poolA.isClosed());
    assertEquals(poolA, cache.getNode(masterA));

    release.countDown();
    refresh.join(5000);
    assertFalse(refresh.isAlive());
    // snapshot, then the deferred delta, then the refresh-end cleanup
    assertEquals(masterB, cache.getSlotNode(0));
    assertFalse(cache.hasPendingSlotDeltas());
    assertTrue(poolA.isClosed());
    assertNull(cache.getNode(masterA));
    assertThat(cache.getNodes(), aMapWithSize(1));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void failedRefreshKeepsUnrelatedPoolsWhenDeltasDrainBehindIt() throws Exception {
    HostAndPort masterA = MASTER_HOST;
    HostAndPort masterB = REPLICA_1_HOST;
    HostAndPort masterC = REPLICA_2_HOST;
    JedisClusterInfoCache cache = new JedisClusterInfoCache(DefaultJedisClientConfig.builder().build(),
        new HashSet<>(Collections.singletonList(masterA)));
    ClusterMaintenanceCoordinator coordinator = new ClusterMaintenanceCoordinator(cache,
        MaintenanceNotificationsConfig.builder().build());
    CountDownLatch querying = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    List<Object> threeMasters = createClusterSlotsResponse(
      new SlotRange.Builder(0, 5000).master(masterA, "a").build(),
      new SlotRange.Builder(5001, 10000).master(masterB, "b").build(),
      new SlotRange.Builder(10001, 16383).master(masterC, "c").build());
    // passes the slot-sequence validation but fails on host parsing, i.e. after the tables reset
    List<Object> malformed = createClusterSlotsResponse(
      new SlotRange.Builder(0, 5000).master(masterA, "a").build(),
      new SlotRange.Builder(5001, 10000).master(masterB, "b").build(),
      new SlotRange.Builder(10001, 16383).master(masterC, "c").build());
    ((List<Object>) ((List<Object>) malformed.get(0)).get(2)).set(0, "not-a-host");
    when(mockConnection.executeCommand(argThat(commandWithArgs(CLUSTER, "SLOTS"))))
        .thenReturn(threeMasters).thenAnswer(inv -> {
          querying.countDown();
          release.await(5, TimeUnit.SECONDS);
          return malformed;
        });
    cache.discoverClusterNodesAndSlots(mockConnection);
    ConnectionPool poolA = cache.getNode(masterA);
    ConnectionPool poolC = cache.getNode(masterC);

    AtomicReference<Throwable> refreshFailure = new AtomicReference<>();
    Thread refresh = new Thread(() -> {
      try {
        cache.renewClusterSlots(mockConnection);
      } catch (RuntimeException e) {
        refreshFailure.set(e);
      }
    });
    refresh.start();
    assertTrue(querying.await(5, TimeUnit.SECONDS));
    coordinator.onSMigrated(new SMigratedEvent(2L, Collections.singletonList(
      new SlotMigration(masterA, masterB, HashSlotRanges.parse("0-5000")))), mockEventConnection);
    assertTrue(cache.hasPendingSlotDeltas());

    release.countDown();
    refresh.join(5000);
    assertFalse(refresh.isAlive());
    assertThat(refreshFailure.get(), instanceOf(ClassCastException.class));
    // the deferred delta still lands on the reset table...
    assertFalse(cache.hasPendingSlotDeltas());
    assertEquals(masterB, cache.getSlotNode(0));
    assertNull(cache.getSlotNode(16383));
    // ...and only its own emptied source goes: a node the table merely lost track of stays
    assertTrue(poolA.isClosed());
    assertFalse(poolC.isClosed());
    assertThat(cache.getNodes(), aMapWithSize(2));
  }

  private List<Object> masterReplicaSlotsResponse(HostAndPort masterHost, HostAndPort replicaHost) {
    return createClusterSlotsResponse(
            new SlotRange.Builder(0, 16383).master(masterHost, masterHost.toString() + "-id")
                    .replica(replicaHost, replicaHost.toString() + "-id").build());
  }

  private List<Object> masterOnlySlotsResponse() {
    return createClusterSlotsResponse(
        new SlotRange.Builder(0, 16383).master(MASTER_HOST, "master-id-1").build());
  }

  private List<Object> masterReplica2SlotsResponse() {
    return createClusterSlotsResponse(
        new SlotRange.Builder(0, 16383).master(MASTER_HOST, "master-id-1")
            .replica(REPLICA_2_HOST, "replica-id-2").build());
  }

  private JedisClusterInfoCache createCacheWithReplicasEnabled() {

    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .readOnlyForRedisClusterReplicas().build();

    return new JedisClusterInfoCache(clientConfig,
        new HashSet<>(Collections.singletonList(MASTER_HOST)));
  }

  private void assertNoReplicasAvailable(JedisClusterInfoCache cache) {
    List<ConnectionPool> caheReplicaNodePools = cache.getSlotReplicaPools(TEST_SLOT);
    assertNull(caheReplicaNodePools);
  }

  private void assertReplicasAvailable(JedisClusterInfoCache cache, HostAndPort... replicaNodes) {
    List<ConnectionPool> caheReplicaNodePools = cache.getSlotReplicaPools(TEST_SLOT);
    assertEquals(replicaNodes.length, caheReplicaNodePools.size());
    for (HostAndPort expectedReplica : replicaNodes) {
      ConnectionPool expectedNodePool = cache.getNode(expectedReplica);
      assertThat(caheReplicaNodePools, hasItem(expectedNodePool));
    }
  }

  private void assertMasterNodeAvailable(JedisClusterInfoCache cache) {
    HostAndPort masterNode = cache.getSlotNode(TEST_SLOT);
    assertNotNull(masterNode);
    assertEquals(MASTER_HOST, masterNode);
  }

  /**
   * Helper method to create a cluster slots response with master and replica nodes
   */
  private List<Object> createClusterSlotsResponse(SlotRange... slotRanges) {
    return Arrays.stream(slotRanges).map(this::clusterSlotRange).collect(Collectors.toList());
  }

  private List<Object> clusterSlotRange(SlotRange slotRange) {
    List<Object> slotInfo = new ArrayList<>();
    slotInfo.add((long) slotRange.start);
    slotInfo.add((long) slotRange.end);
    Node master = slotRange.master();
    slotInfo.add(
        Arrays.asList(master.getHost().getBytes(), (long) master.getPort(), master.id.getBytes()));
    // Add replicas
    slotRange.replicas().forEach(r -> slotInfo.add(
        Arrays.asList(r.getHost().getBytes(), (long) r.getPort(), r.id.getBytes())));
    return slotInfo;
  }

  static class SlotRange {
    private final int start;
    private final int end;
    private final List<Node> nodes;

    private SlotRange(int start, int end, List<Node> nodes) {
      this.start = start;
      this.end = end;
      this.nodes = nodes;
    }

    public SlotRange.Builder builder(int start, int end) {
      return new SlotRange.Builder(start, end);
    }

    public Node master() {
      return nodes.get(0);
    }

    public List<Node> replicas() {
      return nodes.subList(1, nodes.size());
    }

    static class Builder {
      private final int start;
      private final int end;
      private final List<Node> nodes = new ArrayList<>();

      public Builder(int start, int end) {
        this.start = start;
        this.end = end;
      }

      public Builder master(Node node) {
        if (!nodes.isEmpty()) {
          nodes.set(0, node);
        } else {
          nodes.add(node);
        }
        return this;
      }

      public Builder master(HostAndPort hostPort, String id) {
        return master(new Node(hostPort, id));
      }

      public Builder replica(HostAndPort hostPort, String id) {
        return replica(new Node(hostPort, id));
      }

      public Builder replica(Node node) {
        if (nodes.isEmpty()) {
          throw new IllegalStateException("Master node must be added before adding replicas");
        }
        nodes.add(node);
        return this;
      }

      public SlotRange build() {
        return new SlotRange(start, end, nodes);
      }

    }

  }

  static class Node {
    private final HostAndPort hostPort;
    private final String id;

    public Node(HostAndPort hostPort, String id) {
      this.hostPort = hostPort;
      this.id = id;
    }

    public HostAndPort getHostPort() {
      return hostPort;
    }

    public String getHost() {
      return hostPort.getHost();
    }

    public int getPort() {
      return hostPort.getPort();
    }

    public String getId() {
      return id;
    }

  }

}
