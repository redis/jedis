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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.MaintenanceEventController.MaintenanceHandoff;
import redis.clients.jedis.util.ReflectionTestUtil;
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
  public void staleAndOutOfOrderEvents_areIgnored() {
    moving(5L, TARGET_B, 100);
    moving(5L, TARGET_C, 100); // same seq is a duplicate
    moving(3L, TARGET_C, 100); // lower seq is out-of-order
    assertEquals(TARGET_B_ADDR, controller.getSocketAddress(receiverPeer),
      "target unchanged by stale events");
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
    DefaultJedisSocketFactory sf = new DefaultJedisSocketFactory(
        new HostAndPort("localhost", mockServer.getPort()));
    JedisClientConfig config = DefaultJedisClientConfig.builder()
        .maintNotificationsConfig(MaintenanceNotificationsConfig.builder().build()).build();
    ConnectionFactory factory = new ConnectionFactory(sf, config);

    new ConnectionPool(factory); // constructing the pool wires the controller from the config
    MaintenanceEventController wired = factory.getMaintenanceController();
    assertNotNull(wired, "pool creates and attaches a controller when maintenance is enabled");
    assertSame(wired, sf.getSocketAddressMapper(),
      "socket factory uses the controller as its post-DNS mapper");
  }

  @Test
  public void disabledMaintenance_leavesFactoryUnwired() {
    JedisClientConfig config = DefaultJedisClientConfig.builder()
        .maintNotificationsConfig(MaintenanceNotificationsConfig.builder()
            .mode(MaintenanceNotificationsConfig.Mode.DISABLED).build())
        .build();
    ConnectionFactory factory = new ConnectionFactory(
        new DefaultJedisSocketFactory(new HostAndPort("localhost", mockServer.getPort())), config);

    new ConnectionPool(factory);
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

    DefaultJedisSocketFactory mockSocketFactory = new DefaultJedisSocketFactory(mock, clientConfig);
    ConnectionFactory factory = new ConnectionFactory(mockSocketFactory, clientConfig);
    MaintenanceEventController poolCtl = MaintenanceEventController.from(maintConfig);
    factory.attachMaintenanceController(poolCtl);

    poolCtl.onMoving(new MovingEvent(1L, 100, mock), receiver);

    PooledObject<Connection> pooled = factory.makeObject();
    try {
      factory.activateObject(pooled);
      Connection borrowed = pooled.getObject();
      assertTrue(borrowed.isRelaxedTimeoutActive(),
        "a connection borrowed during an active rebind window is relaxed");
      Socket socket = ReflectionTestUtil.getField(borrowed, "socket");
      assertEquals(relaxedTimeoutMs, socket.getSoTimeout());
    } finally {
      pooled.getObject().close();
    }
  }

  @Test
  public void relaxIfRebinding_relaxesAnyPeerWhileWindowIsOpen() throws Exception {
    moving(1L, TARGET_B, 100);

    // Borrow a fresh connection bound to the same mock (its peer is the receiver's affected peer).
    Connection samePeer = new Connection(new HostAndPort("127.0.0.1", mockServer.getPort()));
    samePeer.connect();
    try {
      controller.relaxIfRebinding(samePeer);
      assertTrue(samePeer.isRelaxedTimeoutActive(), "affected peer relaxed");
    } finally {
      samePeer.close();
    }

    TcpMockServer other = new TcpMockServer();
    other.start();
    try {
      Connection differentPeer = new Connection(new HostAndPort("127.0.0.1", other.getPort()));
      differentPeer.connect();
      try {
        controller.relaxIfRebinding(differentPeer);
        assertTrue(differentPeer.isRelaxedTimeoutActive(),
          "unaffected peer also relaxed during an active rebind window");
      } finally {
        differentPeer.close();
      }
    } finally {
      other.stop();
    }
  }
}
