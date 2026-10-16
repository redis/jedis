package redis.clients.jedis.sch;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static redis.clients.jedis.ConnectionTestHelper.getSocketSoTimeout;
import static redis.clients.jedis.ConnectionTestHelper.hasClusterMaintenanceController;
import static redis.clients.jedis.ConnectionTestHelper.hasPoolRelaxOverlay;
import static redis.clients.jedis.ConnectionTestHelper.hasPoolControllerListener;
import static redis.clients.jedis.ConnectionTestHelper.isRelaxedTimeoutActive;

import java.net.SocketException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import redis.clients.jedis.Connection;
import redis.clients.jedis.ConnectionPool;
import redis.clients.jedis.ConnectionTestHelper;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintenanceNotificationsConfig.Mode;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.util.server.ClusterSlotsResponder;
import redis.clients.jedis.util.server.MaintenanceEventMessages;

/**
 * Sections C (slot-delta application end to end) and F (lifecycle) of the cluster maintenance test
 * plan, against a three-node {@code TcpMockServer} cluster. Every mock node redirects a keyed
 * command for a slot it does not own with {@code MOVED}, and counts {@code CLUSTER SLOTS}; a client
 * that applied a delta in place therefore shows unchanged counters, one that fell back to a refresh
 * does not. The cases are defined in ClusterMaintenanceIT.md, in this package directory.
 */
public class ClusterSlotMigrationMockTest extends AbstractClusterMaintenanceMockTest {

  /** Cases C1, C2 (section C) and F2 (section F) in ClusterMaintenanceIT.md. */
  @Test
  public void sMigratedReroutesInPlaceAndDropsTheDepartedSource() {
    openDefaultClient();
    String key = keyOnA();
    client.set(key, "v");
    assertEquals(1, handlerA.commands("SET").size());
    int refreshesBefore = totalClusterSlotsCalls(handlerA, handlerB);
    ConnectionPool poolA = poolOf(nodeA);

    topology.assign(A_FROM, A_TO, nodeB); // the server side completed the migration
    Connection inFlight = client.getConnectionFromSlot(A_FROM); // F2: borrowed while A goes away
    serverA.sendPushMessageToAll(migrated(2L, nodeA, nodeB, "0-8191"));
    assertTrue(inFlight.ping(), "the command carrying the push completes");
    assertTrue(poolA.isClosed(), "A owns nothing anymore");
    inFlight.close(); // returned to a closed pool: destroyed, no exception

    client.set(key, "v2");

    assertEquals(1, handlerA.commands("SET").size(), "nothing else went to A (no MOVED)");
    assertEquals(1, handlerB.commands("SET").size());
    assertEquals(refreshesBefore, totalClusterSlotsCalls(handlerA, handlerB), "no refresh");
    assertFalse(hasNode(nodeA));
    assertTrue(hasNode(nodeB));
    awaitConnections(serverA, 0);
  }

  /** Case C3, section C, in ClusterMaintenanceIT.md. */
  @Test
  public void sourceStillOwningSlotsIsKept() {
    openDefaultClient();
    String low = keyInSlots(0, 100);
    String high = keyInSlots(101, A_TO);

    topology.assign(0, 100, nodeB);
    deliver(serverA, 0, migrated(2L, nodeA, nodeB, "0-100"));
    client.set(low, "v");
    client.set(high, "v");

    assertTrue(handlerB.sawKey("SET", low));
    assertFalse(handlerA.sawKey("SET", low));
    assertTrue(handlerA.sawKey("SET", high));
    assertTrue(hasNode(nodeA));
    assertFalse(poolOf(nodeA).isClosed());
    assertEquals(1, handlerA.clusterSlotsCalls(), "only the bootstrap discovery");
    assertEquals(0, handlerB.clusterSlotsCalls());
  }

  /** Case C4, section C, in ClusterMaintenanceIT.md. */
  @Test
  public void neverSeenDestinationGetsAPoolAndTheHandshake() {
    openDefaultClient();
    String key = keyOnA();

    topology.assign(A_FROM, A_TO, nodeC);
    deliver(serverA, A_FROM, migrated(2L, nodeA, nodeC, "0-8191"));
    client.set(key, "v");

    assertEquals(1, handlerC.commands("SET").size());
    assertEquals(1, handlerC.maintNotificationsHandshakes().size());
    assertEquals(0, handlerC.clusterSlotsCalls(), "C was never asked for the topology");
    ConnectionPool poolC = poolOf(nodeC);
    assertTrue(hasClusterMaintenanceController(poolC));
    try (Connection c = client.getConnectionFromSlot(A_FROM)) {
      assertTrue(hasPoolControllerListener(poolC, c));
      assertTrue(hasPoolRelaxOverlay(poolC, c));
    }
    assertFalse(hasNode(nodeA));
  }

  /** Case C6, section C, in ClusterMaintenanceIT.md. */
  @Test
  public void duplicateClosersOnSeveralConnectionsApplyOnce() {
    openDefaultClient();
    Connection a1 = client.getConnectionFromSlot(A_FROM);
    Connection a2 = client.getConnectionFromSlot(A_FROM);
    Connection a3 = client.getConnectionFromSlot(A_FROM);
    ConnectionPool poolA = poolOf(nodeA);

    topology.assign(A_FROM, A_TO, nodeB);
    serverA.sendPushMessageToAll(migrated(2L, nodeA, nodeB, "0-8191")); // one copy each
    assertTrue(a1.ping());
    assertTrue(poolA.isClosed());
    assertTrue(a2.ping(), "a duplicate copy is harmless on a connection of a closed pool");
    assertTrue(a3.ping());
    a1.close();
    a2.close();
    a3.close();

    client.set(keyOnA(), "v");
    assertEquals(1, handlerB.commands("SET").size());
    assertFalse(hasNode(nodeA));
    assertEquals(1, client.getClusterNodes().size());
    awaitConnections(serverA, 0);
  }

  /** Case C5, section C, in ClusterMaintenanceIT.md. */
  @Test
  public void emptySMigratedOnlyEndsTheRelaxWindow() throws SocketException {
    openDefaultClient();
    ConnectionPool poolA = poolOf(nodeA);
    try (Connection a = client.getConnectionFromSlot(A_FROM)) {
      serverA.sendPushMessageToAll(migrating(1L, "0-8191"));
      assertTrue(a.ping());
      assertTrue(isRelaxedTimeoutActive(a));

      serverA.sendPushMessageToAll(neutralCloser(2L)); // no entries
      assertTrue(a.ping());

      assertFalse(isRelaxedTimeoutActive(a), "the window is closed");
      assertEquals(SO_TIMEOUT_MS, getSocketSoTimeout(a));
    }
    client.set(keyOnA(), "v");
    assertEquals(1, handlerA.commands("SET").size(), "routing unchanged");
    assertFalse(poolA.isClosed(), "no pool dropped");
    assertEquals(2, client.getClusterNodes().size());
    assertEquals(1, handlerA.clusterSlotsCalls(), "no refresh");
  }

  /** Case C7, section C, in ClusterMaintenanceIT.md. */
  @Test
  public void malformedClusterFramesAreDroppedAndTheConnectionStaysUsable() throws SocketException {
    openDefaultClient();
    try (Connection a = client.getConnectionFromSlot(A_FROM)) {
      serverA.sendPushMessageToAll(MaintenanceEventMessages.sMigrating(1L, "abc"));
      serverA.sendPushMessageToAll(MaintenanceEventMessages.sMigrated(2L,
        MaintenanceEventMessages.entry(nodeA.toString(), "127.0.0.1:0", "0")));
      serverA.sendPushMessageToAll(MaintenanceEventMessages.sMigrated(3L,
        MaintenanceEventMessages.entry(nodeA.toString(), nodeB.toString(), "100-0")));

      assertTrue(a.ping(), "all three frames were consumed, the reply is aligned");
      assertFalse(isRelaxedTimeoutActive(a), "a malformed opener opens no window");
      assertEquals(SO_TIMEOUT_MS, getSocketSoTimeout(a));
    }
    client.set(keyOnA(), "v");
    assertEquals(1, handlerA.commands("SET").size(), "routing unchanged");
    assertEquals(2, client.getClusterNodes().size());
    assertEquals(1, handlerA.clusterSlotsCalls());
  }

  /** Cases C8 (section C) and B8 (section B) in ClusterMaintenanceIT.md. */
  @Test
  public void deltaArrivingDuringARefreshIsAppliedAfterItAndRelaxesMeanwhile() throws Exception {
    openDefaultClient();
    CountDownLatch entered = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    handlerA.gateClusterSlots(entered, release);
    Thread refresh = new Thread(client::refreshClusterTopology, "refresh");
    refresh.start();
    assertTrue(entered.await(5, TimeUnit.SECONDS), "the refresh is parked mid-query on A");

    try {
      topology.assign(A_FROM, A_TO, nodeB);
      try (Connection b = client.getConnectionFromSlot(B_FROM)) {
        serverB.sendPushMessageToAll(migrated(2L, nodeA, nodeB, "0-8191"));
        assertTrue(b.ping(), "the delta is queued behind the refresh, the read does not block");
        assertTrue(isRelaxedTimeoutActive(b), "B8: relaxed while a delta is pending");
        assertEquals(RELAXED_TIMEOUT_MS, getSocketSoTimeout(b));
      }
      assertTrue(hasNode(nodeA), "not applied yet: A's pool is still there");
    } finally {
      release.countDown();
    }
    refresh.join(5000);
    assertFalse(refresh.isAlive());

    // the refresh answered with the pre-move table; the delta landed on top of it
    client.set(keyOnA(), "v");
    assertEquals(1, handlerB.commands("SET").size());
    assertTrue(handlerA.commands("SET").isEmpty(), "no MOVED detour");
    assertFalse(hasNode(nodeA));
    try (Connection b = client.getConnectionFromSlot(B_FROM)) {
      assertTrue(b.ping());
      assertFalse(isRelaxedTimeoutActive(b), "nothing pending, no window: back to the baseline");
      assertEquals(SO_TIMEOUT_MS, getSocketSoTimeout(b));
    }
  }

  /** Case C9, section C, in ClusterMaintenanceIT.md. */
  @Test
  public void clientSideCacheIsFlushedWhenADeltaIsApplied() {
    client = clientBuilder(defaultClientConfig(), maintConfig(Mode.AUTO))
        .cacheConfig(CacheConfig.builder().maxSize(100).build()).build();
    String key = keyInSlots(0, 3999);
    String other = keyInSlots(4000, 7999);
    client.set(key, "v");
    assertEquals("v", client.get(key));
    assertEquals("v", client.get(key));
    assertEquals(1, handlerA.commands("GET").size(), "the second read came from the cache");

    // a delta that leaves the key's slot where it is
    topology.assign(8000, A_TO, nodeB);
    deliver(serverA, 0, migrated(2L, nodeA, nodeB, "8000-8191"));
    client.set(other, "w");

    assertEquals("v", client.get(key));
    assertEquals(2, handlerA.commands("GET").size(), "the delta flushed the client-side cache");
  }

  /** Case C10, section C, in ClusterMaintenanceIT.md. */
  @Test
  public void lostCloserSelfHealsThroughMovedAndTheTtlBackstop() throws SocketException {
    AtomicLong now = new AtomicLong(System.nanoTime());
    ConnectionTestHelper.setClockNanos(now::get);
    client = clientBuilder(defaultClientConfig(),
      maintConfigBuilder(Mode.AUTO).relaxedWindowMaxDuration(Duration.ofSeconds(2)).build())
          .build();
    String key = keyOnA();
    deliver(serverA, A_FROM, migrating(1L, "0-8191")); // the opener; its closer never arrives

    topology.assign(A_FROM, A_TO, nodeB); // the server side completed the move anyway
    client.set(key, "v"); // A answers MOVED, the client refreshes and retries on B

    assertTrue(handlerB.sawKey("SET", key));
    assertEquals(2, handlerA.clusterSlotsCalls(), "bootstrap + the MOVED-triggered refresh");
    try (Connection b = client.getConnectionFromSlot(A_FROM)) {
      assertTrue(b.ping());
      assertTrue(isRelaxedTimeoutActive(b), "the window is still open");
    }

    now.addAndGet(TimeUnit.SECONDS.toNanos(3));
    try (Connection b = client.getConnectionFromSlot(A_FROM)) {
      assertTrue(b.ping());
      assertFalse(isRelaxedTimeoutActive(b), "the TTL backstop closed it");
      assertEquals(SO_TIMEOUT_MS, getSocketSoTimeout(b));
    }
  }

  /** Case C11, section C, in ClusterMaintenanceIT.md. */
  @Test
  public void deltasAndPeriodicRefreshesConverge() {
    client = clientBuilder(defaultClientConfig(), maintConfig(Mode.AUTO))
        .topologyRefreshPeriod(Duration.ofMillis(100)).build();
    String key = keyOnA();
    HostAndPort owner = nodeA;
    int rounds = 6;
    for (int i = 0; i < rounds; i++) {
      HostAndPort next = owner.equals(nodeA) ? nodeB : nodeA;
      // the push is read on a connection to the departing owner, borrowed before the move so a
      // refresh racing the move cannot take that pool away first
      try (Connection departing = client.getConnectionFromSlot(A_FROM)) {
        topology.assign(A_FROM, A_TO, next);
        serverOf(owner).sendPushMessageToAll(migrated(2L + i, owner, next, "0-8191"));
        assertTrue(departing.ping());
      }
      // whichever landed first — the delta, a refresh, or a MOVED detour — the command succeeds
      client.set(key, "v" + i);
      owner = next;
    }

    // converged: a command lands on the final owner in a single attempt, nothing goes astray
    ClusterSlotsResponder ownerHandler = owner.equals(nodeA) ? handlerA : handlerB;
    ClusterSlotsResponder otherHandler = owner.equals(nodeA) ? handlerB : handlerA;
    await().atMost(Duration.ofSeconds(5)).pollInterval(Duration.ofMillis(50)).until(() -> {
      int ownerBefore = ownerHandler.commands("SET").size();
      int otherBefore = otherHandler.commands("SET").size();
      client.set(key, "final");
      return ownerHandler.commands("SET").size() == ownerBefore + 1
          && otherHandler.commands("SET").size() == otherBefore;
    });
    assertTrue(hasNode(owner));
    assertTrue(hasNode(nodeB));
  }

  /** Case F1, section F, in ClusterMaintenanceIT.md. */
  @Test
  public void closingTheClientClosesEveryPoolAndConnection() {
    openDefaultClient();
    client.set(keyOnA(), "v");
    client.set(keyOnB(), "v");
    Map<String, ConnectionPool> pools = client.getClusterNodes();
    assertEquals(2, pools.size());

    client.close();
    client = null;

    for (ConnectionPool pool : pools.values()) {
      assertTrue(pool.isClosed());
    }
    awaitConnections(serverA, 0);
    awaitConnections(serverB, 0);
  }

  /** Case F3, section F, in ClusterMaintenanceIT.md. */
  @Test
  public void connectionsReopenedDuringAnOpenWindowAreRelaxedFromTheHandshake()
      throws SocketException {
    openDefaultClient();
    deliver(serverA, A_FROM, migrating(1L, "0-100"));
    assertEquals(1, handlerA.maintNotificationsHandshakes().size());

    serverA.disconnectAllClients();
    awaitConnections(serverA, 0);
    client.set(keyOnA(), "v"); // the stale pooled connection fails, the retry opens a fresh one

    assertEquals(2, handlerA.maintNotificationsHandshakes().size(), "re-handshake");
    assertEquals(1, handlerA.commands("SET").size());
    try (Connection a = client.getConnectionFromSlot(A_FROM)) {
      assertTrue(a.ping());
      assertTrue(isRelaxedTimeoutActive(a),
        "the window is client-wide: the new connection is relaxed at once");
      assertEquals(RELAXED_TIMEOUT_MS, getSocketSoTimeout(a));
    }
  }
}
