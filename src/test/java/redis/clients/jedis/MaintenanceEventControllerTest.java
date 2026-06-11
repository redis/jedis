package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.apache.commons.pool2.PooledObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.MaintenanceEventController.MaintenanceHandoff;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Unit tests for {@link MaintenanceEventController}: the sequence-guarded, time-bounded MOVING
 * rebind overlay; the {@link SocketAddressMapper} contract (affected-only redirect); the
 * relax-on-borrow scope; and the handoff hook fan-out. The receiver connection is given a real
 * (TcpMockServer-backed) socket so {@code getRemoteSocketAddress()} returns a real peer.
 */
@Tag("sch")
public class MaintenanceEventControllerTest {

  private static final HostAndPort TARGET_B = new HostAndPort("node-b.example.com", 6380);
  private static final HostAndPort TARGET_C = new HostAndPort("node-c.example.com", 6381);
  private static final SocketAddress TARGET_B_ADDR = new InetSocketAddress("node-b.example.com",
      6380);
  private static final SocketAddress TARGET_C_ADDR = new InetSocketAddress("node-c.example.com",
      6381);

  private TcpMockServer mockServer;
  private MaintenanceEventController controller;
  private Connection receiver;
  private SocketAddress receiverPeer;
  private AtomicLong now;

  @BeforeEach
  public void setUp() throws Exception {
    now = new AtomicLong(0);
    controller = MaintenanceEventController.from(MaintenanceNotificationsConfig.builder().build());
    controller.setClockNanos(now::get);

    mockServer = new TcpMockServer();
    mockServer.start();

    HostAndPort mock = new HostAndPort("127.0.0.1", mockServer.getPort());
    receiver = new Connection(mock);
    receiver.connect();
    receiverPeer = receiver.getRemoteSocketAddress();
    assertNotNull(receiverPeer, "receiver must have a real peer for the affected-key tests");
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (receiver != null) receiver.close();
    if (mockServer != null) mockServer.stop();
  }

  private void moving(long seq, HostAndPort target, long ttlSeconds) {
    controller.onMoving(new MovingEvent(seq, ttlSeconds, target), receiver);
  }

  private void advanceSeconds(long seconds) {
    now.addAndGet(TimeUnit.SECONDS.toNanos(seconds));
  }

  // --- SocketAddressMapper / affected-only redirect ---

  @Test
  public void getSocketAddress_remapsAffectedOnly() {
    moving(1L, TARGET_B, 10);

    assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer),
      "affected peer remapped to target");
    SocketAddress other = new InetSocketAddress("unaffected.example.com", 7000);
    assertNull(controller.getSocketAddress(other), "unaffected peer not remapped");
  }

  @Test
  public void getSocketAddress_returnsNullAfterDeadline() {
    moving(1L, TARGET_B, 10);
    advanceSeconds(11);
    assertNull(controller.getSocketAddress(receiverPeer));
  }

  @Test
  public void isAffected_matchesActiveAffectedWithinWindow() {
    assertFalse(controller.isAffected(receiverPeer), "no rebind yet");
    moving(1L, TARGET_B, 10);
    assertTrue(controller.isAffected(receiverPeer));
    advanceSeconds(11);
    assertFalse(controller.isAffected(receiverPeer), "after deadline");
  }

  // --- Seq guard ---

  @Test
  public void sameSeqDifferentTarget_isRejected() {
    moving(5L, TARGET_B, 100);
    moving(5L, TARGET_C, 100); // conflicting target for the same seq is treated as a server bug
    assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer),
      "target unchanged by conflicting-target same-seq event");
  }

  @Test
  public void lowerSeq_isIgnored() {
    moving(5L, TARGET_B, 100);
    moving(3L, TARGET_C, 100); // older event
    assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer));
  }

  @Test
  public void sameSeqSameTarget_mergesAffectedSources() throws Exception {
    AtomicInteger fires = new AtomicInteger();
    controller.addHandoffHook(handoff -> fires.incrementAndGet());

    // First MOVING from `receiver` (connected to 127.0.0.1 in setUp()).
    moving(1L, TARGET_B, 100);
    assertEquals(1, fires.get(), "handoff hook fires on new seq");
    assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer));

    // Dual-stack receiver: connect the SAME mock server via the IPv6 loopback. The mock binds the
    // wildcard address (new ServerSocket(0)) and accepts both families on the same port. Skip on
    // environments without IPv6 (some CI/containers).
    Connection ipv6Receiver = openIpv6Receiver(mockServer.getPort());
    Assumptions.assumeTrue(ipv6Receiver != null,
      "IPv6 loopback not available; skipping merge test");
    SocketAddress ipv6Peer = ipv6Receiver.getRemoteSocketAddress();
    try {
      // Same seq, same target, different IP family → must merge and re-fire the hook so
      // subscribers (e.g. pool.evict()) re-scan with the larger affected set.
      controller.onMoving(new MovingEvent(1L, 100, TARGET_B), ipv6Receiver);

      assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer),
        "original IPv4 peer still remaps");
      assertEquals(TARGET_B_ADDR, controller.getSocketAddress(ipv6Peer),
        "merged IPv6 peer also remaps to same target");
      assertEquals(2, fires.get(), "handoff hook fires again on same-seq merge");

      // Idempotent: re-delivering the same MOVING to the same connection is a no-op (source
      // already in the affected set; no state change → no hook fire).
      controller.onMoving(new MovingEvent(1L, 100, TARGET_B), ipv6Receiver);
      assertEquals(2, fires.get(), "no fire on duplicate merge of an already-present source");
    } finally {
      ipv6Receiver.close();
    }
  }

  /** Connect to localhost:{@code port} over IPv6; returns null if IPv6 is unavailable. */
  private static Connection openIpv6Receiver(int port) {
    try {
      Connection c = new Connection(new HostAndPort("::1", port));
      c.connect();
      return c;
    } catch (Exception e) {
      return null;
    }
  }

  @Test
  public void higherSeqNewTarget_supersedes() {
    moving(5L, TARGET_B, 100);
    moving(6L, TARGET_C, 100);
    assertEquals(TARGET_C_ADDR, controller.getSocketAddress(receiverPeer));
  }

  @Test
  public void newEventAfterExpiry_appliesAgain() {
    moving(5L, TARGET_B, 10);
    advanceSeconds(11); // expire
    assertNull(controller.getSocketAddress(receiverPeer));

    moving(6L, TARGET_C, 10);
    assertEquals(TARGET_C_ADDR, controller.getSocketAddress(receiverPeer),
      "newer event after expiry applies again");
  }

  // --- Handoff hooks ---

  @Test
  public void handoffHook_firesOncePerAppliedHandoff() {
    AtomicInteger fires = new AtomicInteger();
    controller.addHandoffHook(handoff -> fires.incrementAndGet());

    moving(5L, TARGET_B, 100);
    moving(5L, TARGET_C, 100); // stale: no fire
    moving(6L, TARGET_C, 100); // newer: fire

    assertEquals(2, fires.get());
    assertTrue(receiver.isRelaxedTimeoutActive(), "receiver relaxed on MOVING");
  }

  @Test
  public void handoffHook_payloadCarriesEventFields() {
    AtomicReference<MaintenanceHandoff> seen = new AtomicReference<>();
    controller.addHandoffHook(seen::set);

    moving(7L, TARGET_B, 30);

    MaintenanceHandoff h = seen.get();
    assertEquals(7L, h.getSeq());
    assertEquals(TARGET_B, h.getTarget());
    assertEquals(Duration.ofSeconds(30), h.getTtl());
  }

  @Test
  public void handoffHook_multipleHooksAllFire() {
    AtomicInteger first = new AtomicInteger();
    AtomicInteger second = new AtomicInteger();
    controller.addHandoffHook(handoff -> first.incrementAndGet());
    controller.addHandoffHook(handoff -> second.incrementAndGet());

    moving(1L, TARGET_B, 100);

    assertEquals(1, first.get());
    assertEquals(1, second.get());
  }

  @Test
  public void handoffHook_removedHookStopsFiring() {
    AtomicInteger fires = new AtomicInteger();
    Consumer<MaintenanceHandoff> hook = handoff -> fires.incrementAndGet();
    controller.addHandoffHook(hook);

    moving(1L, TARGET_B, 100);
    controller.removeHandoffHook(hook);
    moving(2L, TARGET_C, 100);

    assertEquals(1, fires.get(), "removed hook should not fire on subsequent handoffs");
  }

  // --- Pool wiring ---

  @Test
  public void poolWiresControllerAsSocketAddressMapper() {
    HostAndPort hp = new HostAndPort("localhost", mockServer.getPort());
    JedisClientConfig config = DefaultJedisClientConfig.builder()
        .maintNotificationsConfig(MaintenanceNotificationsConfig.builder().build()).build();

    // Convenience constructor: the pool builds the ConnectionFactory and injects a controller into
    // its default DefaultJedisSocketFactory at construction time.
    ConnectionPool pool = new ConnectionPool(hp, config);
    ConnectionFactory factory = (ConnectionFactory) pool.getFactory();
    MaintenanceEventController wired = factory.getMaintenanceController();
    assertNotNull(wired, "pool creates and attaches a controller when maintenance is enabled");
    DefaultJedisSocketFactory sf = (DefaultJedisSocketFactory) factory.getConnectionBuilder()
        .getSocketFactory();
    assertSame(wired, sf.getSocketAddressMapper(),
      "socket factory uses the controller as its post-DNS mapper");
  }

  @Test
  public void disabledMaintenance_leavesFactoryUnwired() {
    HostAndPort hp = new HostAndPort("localhost", mockServer.getPort());
    JedisClientConfig config = DefaultJedisClientConfig.builder()
        .maintNotificationsConfig(MaintenanceNotificationsConfig.builder()
            .mode(MaintenanceNotificationsConfig.Mode.DISABLED).build())
        .build();

    ConnectionPool pool = new ConnectionPool(hp, config);
    ConnectionFactory factory = (ConnectionFactory) pool.getFactory();
    assertNull(factory.getMaintenanceController());
  }

  // --- Relax-on-borrow ---

  @Test
  public void borrowRelaxesConnection_duringRebindWindow() throws Exception {
    int soTimeoutMs = 2000;
    int relaxedTimeoutMs = 10000;
    MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig.builder()
        .timeoutOptions(TimeoutOptions.builder()
            .proactiveTimeoutsRelaxing(Duration.ofMillis(relaxedTimeoutMs)).build())
        .build();
    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(soTimeoutMs).maintNotificationsConfig(maintConfig)
        .protocol(RedisProtocol.RESP3).build();
    HostAndPort mock = new HostAndPort("127.0.0.1", mockServer.getPort());

    MaintenanceEventController poolCtl = MaintenanceEventController.from(maintConfig);
    ConnectionFactory factory = ConnectionFactory.builder().hostAndPort(mock)
        .clientConfig(clientConfig).maintenanceController(poolCtl).build();

    poolCtl.onMoving(new MovingEvent(1L, 100, mock), receiver);

    PooledObject<Connection> pooled = factory.makeObject();
    try {
      factory.activateObject(pooled); // borrow-time hook is now a no-op
      Connection borrowed = pooled.getObject();
      // The lazy/sticky model applies the relaxed timeout on the next command, not at borrow time.
      // The controller's per-connection query is what executeCommand consults.
      assertTrue(poolCtl.isTimeoutRelaxed(borrowed),
        "controller reports relaxed for any connection during an active rebind window");
    } finally {
      pooled.getObject().close();
    }
  }

  @Test
  public void isTimeoutRelaxed_trueForAnyConnectionWhileRebindActive() throws Exception {
    moving(1L, TARGET_B, 100);

    // A peer that never received a MOVING is still reported relaxed while a rebind window is open.
    TcpMockServer other = new TcpMockServer();
    other.start();
    try {
      Connection peer = new Connection(new HostAndPort("127.0.0.1", other.getPort()));
      peer.connect();
      try {
        assertFalse(peer.isRelaxedTimeoutActive(),
          "this connection never got a MOVING, so it should have no relaxation window of its own");
        assertTrue(controller.isTimeoutRelaxed(peer),
          "controller should still flag the connection as relaxed while a pool-wide MOVING window is open");
      } finally {
        peer.close();
      }
    } finally {
      other.stop();
    }
  }

  @Test
  public void onMoving_capsRelaxDurationAtMax() {
    // Configure a 10s max-relax backstop.
    MaintenanceEventController capped = MaintenanceEventController
        .from(MaintenanceNotificationsConfig.builder()
            .timeoutOptions(
              TimeoutOptions.builder().relaxedTimeoutMaxDuration(Duration.ofSeconds(10)).build())
            .build());
    capped.setClockNanos(now::get);

    // Server says 100s; the cap shortens it to 10s.
    capped.onMoving(new MovingEvent(1L, 100, TARGET_B), receiver);
    assertNotNull(capped.getSocketAddress(receiverPeer), "active just after MOVING");

    advanceSeconds(11);
    assertNull(capped.getSocketAddress(receiverPeer),
      "ttl is capped at maxRelaxedDuration (10s), not the server-supplied 100s");
  }
}
