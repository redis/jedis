package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import redis.clients.jedis.MaintenanceEventController.MaintenanceHandoff;

import org.apache.commons.pool2.PooledObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.ReflectionTestUtil;
import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Unit tests for the {@link MaintenanceEventController}: the lock-free, sequence-guarded,
 * time-bounded MOVING rebind overlay and the relax-on-borrow behaviour. The socket factory resolves
 * its target through {@code controller::targetOverride}; an injected monotonic clock makes expiry
 * deterministic. MOVING events are driven through {@link MaintenanceEventController#onMoving} with a
 * throwaway, socket-less receiver connection.
 */
@Tag("sch")
public class MaintenanceEventControllerTest {

  private static final HostAndPort ORIGINAL = new HostAndPort("original.example.com", 6379);
  private static final HostAndPort TARGET_B = new HostAndPort("node-b.example.com", 6380);
  private static final HostAndPort TARGET_C = new HostAndPort("node-c.example.com", 6381);

  private DefaultJedisSocketFactory socketFactory;
  private MaintenanceEventController controller;
  private Connection receiver;
  private AtomicLong now;

  @BeforeEach
  public void setUp() {
    now = new AtomicLong(0);
    controller = MaintenanceEventController.from(MaintenanceNotificationsConfig.builder().build());
    controller.setClockNanos(now::get);
    socketFactory = new DefaultJedisSocketFactory(ORIGINAL);
    socketFactory.setHostAndPortSupplier(controller::targetOverride);
    receiver = new Connection(); // socket-less; absorbs the per-connection relax/rebind calls
  }

  private void moving(long seq, HostAndPort target, long ttlSeconds) {
    controller.onMoving(new MovingEvent(seq, ttlSeconds, target), receiver);
  }

  private void advanceSeconds(long seconds) {
    now.addAndGet(TimeUnit.SECONDS.toNanos(seconds));
  }

  @Test
  public void noRebind_usesOriginal() {
    assertEquals(ORIGINAL, socketFactory.getHostAndPort());
  }

  @Test
  public void applyNewTarget_thenExpiresBackToOriginal() {
    moving(1L, TARGET_B, 10);
    assertEquals(TARGET_B, socketFactory.getHostAndPort(), "within grace window uses new target");

    advanceSeconds(9);
    assertEquals(TARGET_B, socketFactory.getHostAndPort(), "still within window");

    advanceSeconds(2); // now past the 10s deadline
    assertEquals(ORIGINAL, socketFactory.getHostAndPort(), "after expiry reverts to original");
  }

  @Test
  public void staleAndOutOfOrderEvents_areIgnored() {
    moving(5L, TARGET_B, 100);
    moving(5L, TARGET_C, 100); // same seq is a duplicate
    moving(3L, TARGET_C, 100); // lower seq is out-of-order
    assertEquals(TARGET_B, socketFactory.getHostAndPort(), "target unchanged by stale events");
  }

  @Test
  public void higherSeqSameTarget_isApplied() {
    moving(5L, TARGET_B, 10);
    moving(6L, TARGET_B, 10); // dedup is by seq, not by target
    assertEquals(TARGET_B, socketFactory.getHostAndPort());
  }

  @Test
  public void higherSeqNewTarget_supersedes() {
    moving(5L, TARGET_B, 100);
    moving(6L, TARGET_C, 100);
    assertEquals(TARGET_C, socketFactory.getHostAndPort());
  }

  @Test
  public void newEventAfterExpiry_appliesAgain() {
    moving(5L, TARGET_B, 10);
    advanceSeconds(11); // expire
    assertEquals(ORIGINAL, socketFactory.getHostAndPort());

    moving(6L, TARGET_C, 10);
    assertEquals(TARGET_C, socketFactory.getHostAndPort(), "a newer event after expiry applies again");
  }

  @Test
  public void handoffHook_firesOncePerAppliedHandoff() {
    AtomicInteger fires = new AtomicInteger();
    controller.addHandoffHook(handoff -> fires.incrementAndGet());

    moving(5L, TARGET_B, 100);
    moving(5L, TARGET_C, 100); // stale: no fire
    moving(6L, TARGET_C, 100); // newer: fire

    assertEquals(2, fires.get());
    receiverWasMarkedAndRelaxed();
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

  private void receiverWasMarkedAndRelaxed() {
    assertTrue(receiver.isRelaxedTimeoutActive(), "receiver relaxed on MOVING");
  }

  @Test
  public void poolWiresControllerIntoFactoryAndSocketFactory() {
    DefaultJedisSocketFactory sf = new DefaultJedisSocketFactory(ORIGINAL);
    JedisClientConfig config = DefaultJedisClientConfig.builder()
        .maintNotificationsConfig(MaintenanceNotificationsConfig.builder().build()).build();
    ConnectionFactory factory = new ConnectionFactory(sf, config);

    new ConnectionPool(factory); // constructing the pool wires the controller from the config
    MaintenanceEventController wired = factory.getMaintenanceController();
    assertNotNull(wired, "pool creates and attaches a controller when maintenance is enabled");
    assertEquals(ORIGINAL, sf.getHostAndPort(), "no rebind yet → configured host");

    wired.onMoving(new MovingEvent(1L, 100, TARGET_B), new Connection());
    assertEquals(TARGET_B, sf.getHostAndPort(), "socket factory resolves through the controller");
  }

  @Test
  public void disabledMaintenance_leavesFactoryUnwired() {
    JedisClientConfig config = DefaultJedisClientConfig.builder().maintNotificationsConfig(
        MaintenanceNotificationsConfig.builder().mode(MaintenanceNotificationsConfig.Mode.DISABLED)
            .build()).build();
    ConnectionFactory factory = new ConnectionFactory(new DefaultJedisSocketFactory(ORIGINAL), config);

    new ConnectionPool(factory);
    assertNull(factory.getMaintenanceController());
  }

  @Test
  public void borrowRelaxesConnectionDuringRebindWindow() throws Exception {
    int soTimeoutMs = 2000;
    int relaxedTimeoutMs = 10000;
    TcpMockServer mockServer = new TcpMockServer();
    mockServer.start();
    try {
      MaintenanceNotificationsConfig maintConfig = MaintenanceNotificationsConfig.builder()
          .timeoutOptions(TimeoutOptions.builder()
              .proactiveTimeoutsRelaxing(Duration.ofMillis(relaxedTimeoutMs)).build())
          .build();
      DefaultJedisClientConfig clientConfig = DefaultJedisClientConfig.builder()
          .socketTimeoutMillis(soTimeoutMs).maintNotificationsConfig(maintConfig)
          .protocol(RedisProtocol.RESP3).build();
      HostAndPort mock = new HostAndPort("localhost", mockServer.getPort());

      DefaultJedisSocketFactory mockSocketFactory = new DefaultJedisSocketFactory(mock, clientConfig);
      ConnectionFactory factory = new ConnectionFactory(mockSocketFactory, clientConfig);
      MaintenanceEventController controller = MaintenanceEventController.from(maintConfig);
      factory.attachMaintenanceController(controller);

      controller.onMoving(new MovingEvent(1L, 100, mock), new Connection()); // open the window
      PooledObject<Connection> pooled = factory.makeObject();
      try {
        factory.activateObject(pooled); // simulate a pool borrow
        Connection borrowed = pooled.getObject();
        assertTrue(borrowed.isRelaxedTimeoutActive(),
          "a connection borrowed during a rebind window is relaxed");
        Socket socket = ReflectionTestUtil.getField(borrowed, "socket");
        assertEquals(relaxedTimeoutMs, socket.getSoTimeout());
      } finally {
        pooled.getObject().close();
      }
    } finally {
      mockServer.stop();
    }
  }
}
