package redis.clients.jedis.sch;

import static org.awaitility.Awaitility.await;
import static redis.clients.jedis.JedisClusterInfoCache.getNodeKey;
import static redis.clients.jedis.util.ClusterKeyRegistry.keyInRange;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig.Mode;
import redis.clients.jedis.RedisClusterClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.builders.ClusterClientBuilder;
import redis.clients.jedis.util.server.ClusterSlotsResponder;
import redis.clients.jedis.util.server.MaintenanceEventMessages;
import redis.clients.jedis.util.server.MockClusterTopology;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Shared fixture for the cluster maintenance (SMIGRATING / SMIGRATED) mock tests: a three-node mock
 * cluster — A owns slots 0–8191, B owns 8192–16383, C owns nothing until a test moves slots to it —
 * each node a {@link TcpMockServer} driven by a {@link ClusterSlotsResponder} over one shared
 * {@link MockClusterTopology}, plus a {@link RedisClusterClient} bootstrapped from A.
 * <p>
 * Conventions used by every test: a push is written by the server immediately, but the client only
 * reads it on its next read of that connection; {@link #deliver} borrows the connection for a slot,
 * pushes, and pings so the push is consumed on a known connection. Slot moves are first applied to
 * the shared topology (the server side completed the migration) and then announced with the push,
 * so a client that ignored the push would be redirected with {@code MOVED} and show up in the
 * {@code CLUSTER SLOTS} counters.
 */
@Tag("sch")
public abstract class AbstractClusterMaintenanceMockTest {

  protected static final int SO_TIMEOUT_MS = 2000;
  protected static final int BLOCKING_SO_TIMEOUT_MS = 1000;
  protected static final int RELAXED_TIMEOUT_MS = 10_000;
  protected static final int RELAXED_BLOCKING_TIMEOUT_MS = 4_000;

  protected static final int A_FROM = 0;
  protected static final int A_TO = 8191;
  protected static final int B_FROM = 8192;
  protected static final int B_TO = 16383;

  protected TcpMockServer serverA;
  protected TcpMockServer serverB;
  protected TcpMockServer serverC;
  protected HostAndPort nodeA;
  protected HostAndPort nodeB;
  protected HostAndPort nodeC;
  protected ClusterSlotsResponder handlerA;
  protected ClusterSlotsResponder handlerB;
  protected ClusterSlotsResponder handlerC;
  protected MockClusterTopology topology;
  protected RedisClusterClient client;

  @BeforeEach
  public void clusterSetUp() throws IOException {
    serverA = startServer();
    serverB = startServer();
    serverC = startServer();
    // 127.0.0.1 so the configured address and the peer the server announces are identical
    nodeA = new HostAndPort("127.0.0.1", serverA.getPort());
    nodeB = new HostAndPort("127.0.0.1", serverB.getPort());
    nodeC = new HostAndPort("127.0.0.1", serverC.getPort());
    topology = new MockClusterTopology().assign(A_FROM, A_TO, nodeA).assign(B_FROM, B_TO, nodeB);
    handlerA = new ClusterSlotsResponder(topology, nodeA);
    handlerB = new ClusterSlotsResponder(topology, nodeB);
    handlerC = new ClusterSlotsResponder(topology, nodeC);
    serverA.setCommandHandler(handlerA);
    serverB.setCommandHandler(handlerB);
    serverC.setCommandHandler(handlerC);
  }

  @AfterEach
  public void clusterTearDown() throws IOException {
    if (client != null) {
      client.close();
      client = null;
    }
    for (TcpMockServer server : new TcpMockServer[] { serverA, serverB, serverC }) {
      if (server != null) {
        server.stop();
      }
    }
    ConnectionTestHelper.resetClockNanos();
  }

  private static TcpMockServer startServer() throws IOException {
    TcpMockServer server = new TcpMockServer();
    server.start();
    return server;
  }

  // ---- configuration ----------------------------------------------------------------------

  protected static JedisClientConfig clientConfig(RedisProtocol protocol, int soTimeoutMs,
      int blockingSoTimeoutMs) {
    return DefaultJedisClientConfig.builder().socketTimeoutMillis(soTimeoutMs)
        .blockingSocketTimeoutMillis(blockingSoTimeoutMs).protocol(protocol).build();
  }

  protected static JedisClientConfig clientConfig(RedisProtocol protocol, int soTimeoutMs) {
    return clientConfig(protocol, soTimeoutMs, BLOCKING_SO_TIMEOUT_MS);
  }

  /** RESP3, finite baseline timeouts smaller than their relaxed counterparts. */
  protected static JedisClientConfig defaultClientConfig() {
    return clientConfig(RedisProtocol.RESP3, SO_TIMEOUT_MS);
  }

  protected static MaintenanceNotificationsConfig.Builder maintConfigBuilder(Mode mode) {
    return MaintenanceNotificationsConfig.builder().mode(mode).relaxedTimeout(RELAXED_TIMEOUT_MS)
        .relaxedBlockingTimeout(RELAXED_BLOCKING_TIMEOUT_MS);
  }

  protected static MaintenanceNotificationsConfig maintConfig(Mode mode) {
    return maintConfigBuilder(mode).build();
  }

  /** Builder bootstrapped from node A with the given client and maintenance config. */
  protected ClusterClientBuilder<RedisClusterClient> clientBuilder(JedisClientConfig clientConfig,
      MaintenanceNotificationsConfig maintConfig) {
    return RedisClusterClient.builder().nodes(Collections.singleton(nodeA))
        .clientConfig(clientConfig).maintenanceNotifications(maintConfig);
  }

  protected RedisClusterClient openClient(JedisClientConfig clientConfig,
      MaintenanceNotificationsConfig maintConfig) {
    client = clientBuilder(clientConfig, maintConfig).build();
    return client;
  }

  /** RESP3, AUTO maintenance notifications, relaxed timeouts configured. */
  protected RedisClusterClient openDefaultClient() {
    return openClient(defaultClientConfig(), maintConfig(Mode.AUTO));
  }

  // ---- keys and slots ---------------------------------------------------------------------

  protected static String keyOnA() {
    return keyInRange(A_FROM, A_TO, "onA");
  }

  protected static String keyOnB() {
    return keyInRange(B_FROM, B_TO, "onB");
  }

  protected ConnectionPool poolOf(HostAndPort node) {
    return client.getClusterNodes().get(getNodeKey(node));
  }

  protected boolean hasNode(HostAndPort node) {
    return client.getClusterNodes().containsKey(getNodeKey(node));
  }

  protected TcpMockServer serverOf(HostAndPort node) {
    return node.equals(nodeA) ? serverA : node.equals(nodeB) ? serverB : serverC;
  }

  // ---- pushes -----------------------------------------------------------------------------

  protected static Object[] migrating(long seq, String slots) {
    return MaintenanceEventMessages.sMigrating(seq, slots);
  }

  protected static Object[] migrated(long seq, HostAndPort src, HostAndPort dest, String slots) {
    return MaintenanceEventMessages.sMigrated(seq,
      MaintenanceEventMessages.entry(src.toString(), dest.toString(), slots));
  }

  /** An SMIGRATED without entries: a closer that only ends the relax window. */
  protected static Object[] neutralCloser(long seq) {
    return MaintenanceEventMessages.sMigrated(seq);
  }

  /**
   * Pushes the messages from {@code server} and reads them on the connection currently serving
   * {@code readSlot}: the push is consumed there, then the connection goes back to its pool.
   */
  protected void deliver(TcpMockServer server, int readSlot, Object[]... messages) {
    try (Connection connection = client.getConnectionFromSlot(readSlot)) {
      for (Object[] message : messages) {
        server.sendPushMessageToAll(message);
      }
      connection.ping();
    }
  }

  // ---- observation ------------------------------------------------------------------------

  /** Server-side connection counts move asynchronously to the client's close(). */
  protected static void awaitConnections(TcpMockServer server, int expected) {
    await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(20))
        .until(() -> server.getConnectedClientCount() == expected);
  }

  protected static int totalClusterSlotsCalls(ClusterSlotsResponder... handlers) {
    int total = 0;
    for (ClusterSlotsResponder handler : handlers) {
      total += handler.clusterSlotsCalls();
    }
    return total;
  }
}
