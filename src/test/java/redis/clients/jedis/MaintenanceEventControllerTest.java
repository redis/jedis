package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
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

import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Unit tests for {@link MaintenanceEventController}: the (seq, endpoint)-deduplicated, time-bounded
 * MOVING events (overlapping events coexist until each expires); the {@link SocketAddressMapper}
 * contract (affected-only redirect); the relax-on-borrow scope; and the handoff hook fan-out. The
 * receiver connections are given real (TcpMockServer-backed) sockets so
 * {@code getRemoteSocketAddress()} returns real, distinct peers.
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
  private TcpMockServer mockServer2;
  private MaintenanceEventController controller;
  private Connection receiver;
  private Connection receiver2;
  private SocketAddress receiverPeer;
  private SocketAddress receiver2Peer;
  private AtomicLong now;

  @BeforeEach
  public void setUp() throws Exception {
    now = new AtomicLong(0);
    controller = MaintenanceEventController.from(MaintenanceNotificationsConfig.builder().build());
    NanoClock.INSTANCE = now::get;

    mockServer = new TcpMockServer();
    mockServer.start();
    mockServer2 = new TcpMockServer();
    mockServer2.start();

    receiver = new Connection(new HostAndPort("127.0.0.1", mockServer.getPort()));
    receiver.connect();
    receiverPeer = receiver.getRemoteSocketAddress();
    assertNotNull(receiverPeer, "receiver must have a real peer for the affected-key tests");

    receiver2 = new Connection(new HostAndPort("127.0.0.1", mockServer2.getPort()));
    receiver2.connect();
    receiver2Peer = receiver2.getRemoteSocketAddress();
    assertNotNull(receiver2Peer, "second receiver must have a real, distinct peer");
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (receiver != null) receiver.close();
    if (receiver2 != null) receiver2.close();
    if (mockServer != null) mockServer.stop();
    if (mockServer2 != null) mockServer2.stop();
    NanoClock.INSTANCE = System::nanoTime;
  }

  private void moving(long seq, HostAndPort target, long ttlSeconds) {
    controller.onMoving(new MovingEvent(seq, ttlSeconds, target), receiver);
  }

  private void movingOn(Connection c, long seq, HostAndPort target, long ttlSeconds) {
    controller.onMoving(new MovingEvent(seq, ttlSeconds, target), c);
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
    assertFalse(controller.isAffected(receiver), "no rebind yet");
    moving(1L, TARGET_B, 10);
    assertTrue(controller.isAffected(receiver));
    advanceSeconds(11);
    assertFalse(controller.isAffected(receiver), "after deadline");
  }

  // --- Event identity: (seq, endpoint); overlapping events coexist ---

  @Test
  public void sameSeqDifferentTarget_isDistinctEvent() {
    // seq is per-source, so the same seq with a different endpoint from another set of
    // connections is a distinct concurrent event — each peer remaps to its own event's target.
    moving(5L, TARGET_B, 100);
    movingOn(receiver2, 5L, TARGET_C, 100);

    assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer));
    assertEquals(TARGET_C_ADDR, controller.getSocketAddress(receiver2Peer));
  }

  @Test
  public void lowerSeqDifferentTarget_coexists() {
    moving(5L, TARGET_B, 100);
    movingOn(receiver2, 3L, TARGET_C, 100); // lower seq from another source: not a stale replay

    assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer));
    assertEquals(TARGET_C_ADDR, controller.getSocketAddress(receiver2Peer));
  }

  @Test
  public void sameSeqSameTarget_mergesAffectedSources() throws Exception {
    AtomicInteger fires = new AtomicInteger();
    controller.addHandoffHook(fires::incrementAndGet);

    // First MOVING from `receiver` (connected to 127.0.0.1 in setUp()).
    moving(1L, TARGET_B, 100);
    assertEquals(1, fires.get(), "hook fires on new seq");
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
      assertEquals(2, fires.get(), "hook fires again on same-seq merge");

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
  public void newerEventDoesNotDisplaceEarlier() {
    moving(5L, TARGET_B, 100);
    movingOn(receiver2, 6L, TARGET_C, 10); // newer, shorter-lived event on another peer

    assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer));
    assertEquals(TARGET_C_ADDR, controller.getSocketAddress(receiver2Peer));

    advanceSeconds(11); // the newer event expires first
    assertNull(controller.getSocketAddress(receiver2Peer));
    assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer),
      "earlier event survives the newer event's expiry");
    assertTrue(controller.isRebindActive(), "rebind active until the last event expires");

    advanceSeconds(90); // 101s: past the earlier event's ttl too
    assertNull(controller.getSocketAddress(receiverPeer));
    assertFalse(controller.isRebindActive());
  }

  @Test
  public void relaxActiveUntilLastEventExpires() {
    moving(1L, TARGET_B, 10);
    movingOn(receiver2, 2L, TARGET_C, 30);

    advanceSeconds(11); // first event expired, second still active
    assertNotNull(controller.getTimeoutSupplier().get(),
      "pool stays relaxed while any MOVING event is active");
    assertTrue(controller.isRebindActive());

    advanceSeconds(20); // 31s: both expired
    assertNull(controller.getTimeoutSupplier().get());
    assertFalse(controller.isRebindActive());
  }

  @Test
  public void noneAndTargetedOverlap_coexist() {
    moving(5L, TARGET_B, 100);
    movingOn(receiver2, 6L, null, 100); // 'none' event on another peer

    assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer));
    assertNull(controller.getSocketAddress(receiver2Peer),
      "'none' never remaps: reconnect to the configured endpoint");
    assertTrue(controller.isAffected(receiver2), "'none' peer is still affected");
    assertTrue(controller.isRebindActive());
  }

  @Test
  public void redeliveryAfterExpiry_startsFreshOperation() {
    moving(1L, TARGET_B, 10);
    advanceSeconds(11);
    assertNull(controller.getSocketAddress(receiverPeer)); // expired (and removed on this read)

    // The server never delivers a MOVING past its ttl (connections are dropped at window end);
    // a delivery that does arrive is a new announcement and starts a fresh operation.
    moving(1L, TARGET_B, 10);
    assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer));
    assertTrue(controller.isRebindActive());
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
    controller.addHandoffHook(fires::incrementAndGet);

    moving(5L, TARGET_B, 100); // new event: fire
    moving(5L, TARGET_C, 100); // distinct key (same seq, other endpoint): fire
    moving(6L, TARGET_C, 100); // distinct key (other seq): fire
    moving(6L, TARGET_C, 100); // known key, known peer: no fire

    assertEquals(3, fires.get());
  }

  @Test
  public void handoffHook_multipleHooksAllFire() {
    AtomicInteger first = new AtomicInteger();
    AtomicInteger second = new AtomicInteger();
    controller.addHandoffHook(first::incrementAndGet);
    controller.addHandoffHook(second::incrementAndGet);

    moving(1L, TARGET_B, 100);

    assertEquals(1, first.get());
    assertEquals(1, second.get());
  }

  // --- Relax-on-borrow ---

  @Test
  public void borrowRelaxesConnection_duringRebindWindow() throws Exception {
    int soTimeoutMs = 2000;
    int relaxedTimeoutMs = 10000;
    MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig.builder()
        .relaxedTimeout(relaxedTimeoutMs).build();
    DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
        .socketTimeoutMillis(soTimeoutMs).protocol(RedisProtocol.RESP3).build();
    HostAndPort mock = new HostAndPort("127.0.0.1", mockServer.getPort());

    MaintenanceEventController poolCtl = MaintenanceEventController.from(maintConfig);
    ConnectionFactory factory = ConnectionFactory.builder().hostAndPort(mock)
        .clientConfig(clientConfig).maintenanceController(poolCtl).build();

    poolCtl.onMoving(new MovingEvent(1L, 100, mock), receiver);

    PooledObject<Connection> pooled = factory.makeObject();
    try {
      factory.activateObject(pooled); // borrow-time hook is now a no-op
      Connection borrowed = pooled.getObject();
      // Relaxation is pool-wide: even though this connection never received a MOVING itself, it
      // consults the controller's rebind window through its relaxed-timeout source, so while that
      // window is open its next command runs with the relaxed timeout.
      assertTrue(ConnectionTestHelper.isRelaxedTimeoutActive(borrowed),
        "a connection borrowed during an active pool-wide rebind window is relaxed via the controller");
      assertTrue(poolCtl.isRebindActive(),
        "an active pool-wide rebind window relaxes any borrowed connection's next command");
    } finally {
      pooled.getObject().close();
    }
  }

  @Test
  public void onMoving_relaxWindowUsesServerTtl() {
    // MOVING's time_s is the server's completion bound, so the window runs the full
    // server-supplied ttl; the relaxedWindowMaxDuration backstop applies only to
    // MIGRATING/FAILING_OVER.
    MaintenanceEventController backstopped = MaintenanceEventController
        .from(MaintenanceNotificationsConfig.builder()
            .relaxedWindowMaxDuration(Duration.ofSeconds(10)).build());
    NanoClock.INSTANCE = now::get;

    backstopped.onMoving(new MovingEvent(1L, 100, TARGET_B), receiver);
    assertNotNull(backstopped.getSocketAddress(receiverPeer), "active just after MOVING");

    advanceSeconds(11);
    assertNotNull(backstopped.getSocketAddress(receiverPeer),
      "window outlives the 10s backstop: MOVING uses the raw server ttl");

    advanceSeconds(90); // 101s total, past the server-supplied 100s
    assertNull(backstopped.getSocketAddress(receiverPeer), "window ends at the server ttl");
  }

}
