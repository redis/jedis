package redis.clients.jedis;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static redis.clients.jedis.JedisClusterInfoCache.getNodeKey;
import static redis.clients.jedis.Protocol.Command.CLUSTER;
import static redis.clients.jedis.util.CommandArgumentMatchers.commandWithArgs;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
public class JedisClusterInfoCacheTest {

  private static final HostAndPort MASTER_HOST = new HostAndPort("127.0.0.1", 7000);
  private static final HostAndPort REPLICA_1_HOST = new HostAndPort("127.0.0.1", 7001);
  private static final HostAndPort REPLICA_2_HOST = new HostAndPort("127.0.0.1", 7002);
  private static final int TEST_SLOT = 0;

  @Mock
  private Connection mockConnection;

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
