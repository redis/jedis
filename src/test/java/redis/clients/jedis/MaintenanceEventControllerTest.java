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
    NanoClock.INSTANCE = now::get;

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
    NanoClock.INSTANCE = System::nanoTime;
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
    assertFalse(controller.isAffected(receiver), "no rebind yet");
    moving(1L, TARGET_B, 10);
    assertTrue(controller.isAffected(receiver));
    advanceSeconds(11);
    assertFalse(controller.isAffected(receiver), "after deadline");
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

  // --- 'none' endpoint type (null target): lazy reconnect via per-connection deadline ---

  private void movingNone(long seq, long ttlSeconds) {
    controller.onMoving(new MovingEvent(seq, ttlSeconds, null), receiver);
  }

  @Test
  public void target_stampsReceiverExpiredImmediately() {
    moving(1L, TARGET_B, 100);
    assertTrue(receiver.isExpired(),
      "a real-target MOVING marks the receiver for immediate discard");
  }

  @Test
  public void noneTarget_doesNotRemap() {
    movingNone(1L, 10);
    assertNull(controller.getSocketAddress(receiverPeer), "'none' MOVING never remaps");
    assertTrue(controller.isAffected(receiver),
      "receiver is still affected (relaxed) during the window");
  }

  @Test
  public void noneTarget_reconnectsAtHalfGrace() {
    movingNone(1L, 10); // reconnect deadline = now + ttl/2 = 5s
    advanceSeconds(4);
    assertFalse(receiver.isExpired(), "before half-grace: reconnect not yet due");
    advanceSeconds(2); // total 6s > 5s
    assertTrue(receiver.isExpired(), "past half-grace: reconnect due");
  }

  @Test
  public void noneTarget_firesHandoffHookWithNullTarget() {
    AtomicReference<MaintenanceHandoff> seen = new AtomicReference<>();
    controller.addHandoffHook(seen::set);
    movingNone(1L, 10);
    // The hook drives the pool's eviction pass, which stamps idle affected connections with the
    // reconnect deadline (evicting only those already past it).
    assertNotNull(seen.get(), "'none' fires the handoff hook too");
    assertNull(seen.get().getTarget(), "'none' handoff carries a null target");
  }

  @Test
  public void olderSeqNone_stillStampsReceiver() {
    moving(5L, TARGET_B, 100); // current state at seq 5
    // A buffered older 'none' MOVING (seq 3) read late by this receiver: superseded (no state
    // change) but the receiver must still get a reconnect deadline.
    controller.onMoving(new MovingEvent(3L, 10, null), receiver);
    assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer),
      "state unchanged by older event");
    advanceSeconds(6); // > ttl/2 (5s) of the older event
    assertTrue(receiver.isExpired(), "older-seq receiver still marked for reconnect");
  }

  @Test
  public void sameSeqNone_sharesFirstNotificationDeadline() {
    movingNone(1L, 10); // deadline = 5s (now=0)
    advanceSeconds(3); // now = 3s
    controller.onMoving(new MovingEvent(1L, 10, null), receiver); // same seq, later now
    advanceSeconds(2); // now = 5s == first deadline
    assertTrue(receiver.isExpired(),
      "same-seq reporter inherits the FIRST notification's deadline (5s), not 3+5=8s");
  }

  @Test
  public void newerTarget_supersedesNoneDeadline() {
    movingNone(1L, 100); // reconnect deadline = now + 50s
    assertFalse(receiver.isExpired(), "'none' deadline is in the future");
    moving(2L, TARGET_B, 100); // newer target seq: discard now
    assertTrue(receiver.isExpired(), "newer target overwrites the future 'none' deadline");
  }

  @Test
  public void stampExpiryIfAffected_overwritesStaleNoneDeadline() throws Exception {
    movingNone(1L, 100); // receiver deadline = now + 50s
    // Newer target MOVING delivered on a sibling connection to the same endpoint (same peer).
    Connection sibling = new Connection(new HostAndPort("127.0.0.1", mockServer.getPort()));
    sibling.connect();
    try {
      controller.onMoving(new MovingEvent(2L, 100, TARGET_B), sibling);
      assertFalse(receiver.isExpired(), "receiver still holds the stale future 'none' deadline");
      assertTrue(controller.stampExpiryIfAffected(receiver),
        "receiver's peer is covered by the newer state");
      assertTrue(receiver.isExpired(),
        "stale 'none' deadline overwritten to immediate discard (last-writer-wins)");
    } finally {
      sibling.close();
    }
  }

  @Test
  public void stampExpiryIfAffected_skipsConnectionsCreatedAfterDeadline() throws Exception {
    movingNone(1L, 10); // committed at now=0, reconnect deadline 5s
    advanceSeconds(6); // past the deadline; relax window (10s) still open
    // The reconnect re-lands on the same peer; stamping it with the past shared deadline would
    // churn it on every return for the rest of the relax window.
    Connection reconnect = new Connection(new HostAndPort("127.0.0.1", mockServer.getPort()));
    reconnect.connect();
    try {
      assertFalse(controller.stampExpiryIfAffected(reconnect),
        "a connection created after the deadline is this rebind's own reconnect - never stamped");
      assertFalse(reconnect.isExpired());
      assertTrue(controller.stampExpiryIfAffected(receiver),
        "pre-existing connection is still stamped");
    } finally {
      reconnect.close();
    }
  }

  @Test
  public void sameSeqRenotificationAfterDeadline_doesNotStamp() throws Exception {
    movingNone(1L, 10); // reconnect deadline 5s
    advanceSeconds(6);
    // The moving node re-notifies every new connection (push follows the subscription +OK); a
    // reconnect that re-landed on it receives the same-seq MOVING and must not inherit the past
    // shared deadline.
    Connection reconnect = new Connection(new HostAndPort("127.0.0.1", mockServer.getPort()));
    reconnect.connect();
    try {
      controller.onMoving(new MovingEvent(1L, 10, null), reconnect);
      assertFalse(reconnect.isExpired(), "re-notified reconnect is outside the rebind's scope");
    } finally {
      reconnect.close();
    }
  }

  @Test
  public void sameSeqNotificationBeforeDeadline_alignsToSharedDeadline() throws Exception {
    movingNone(1L, 10); // reconnect deadline 5s
    advanceSeconds(2); // first half: newcomers sit on the dying node and must participate
    Connection newcomer = new Connection(new HostAndPort("127.0.0.1", mockServer.getPort()));
    newcomer.connect();
    try {
      controller.onMoving(new MovingEvent(1L, 10, null), newcomer);
      assertFalse(newcomer.isExpired(), "aligned to the shared deadline, which is still ahead");
      advanceSeconds(4); // now=6s, past the shared 5s deadline (not the newcomer's own 2+5)
      assertTrue(newcomer.isExpired(), "stamped with the FIRST notification's deadline");
    } finally {
      newcomer.close();
    }
  }

  @Test
  public void stampExpiryIfAffected_onlyWithinWindow() {
    assertFalse(controller.stampExpiryIfAffected(receiver), "no rebind yet");
    movingNone(1L, 10);
    assertTrue(controller.stampExpiryIfAffected(receiver));
    advanceSeconds(11); // relax window (min(10,60)=10s) closed
    assertFalse(controller.stampExpiryIfAffected(receiver), "not stamped after the window closes");
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
  public void onMoving_capsRelaxDurationAtMax() {
    // Configure a 10s max-relax backstop.
    MaintenanceEventController capped = MaintenanceEventController
        .from(MaintenanceNotificationsConfig.builder()
            .relaxedWindowMaxDuration(Duration.ofSeconds(10)).build());
    NanoClock.INSTANCE = now::get;

    // Server says 100s; the cap shortens it to 10s.
    capped.onMoving(new MovingEvent(1L, 100, TARGET_B), receiver);
    assertNotNull(capped.getSocketAddress(receiverPeer), "active just after MOVING");

    advanceSeconds(11);
    assertNull(capped.getSocketAddress(receiverPeer),
      "ttl is capped at maxRelaxedDuration (10s), not the server-supplied 100s");
  }
}
