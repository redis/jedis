package redis.clients.jedis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.pool2.PooledObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import redis.clients.jedis.util.server.TcpMockServer;

/**
 * Maintenance marking passes: every state transition (new epoch or merged source) marks against the
 * snapshot it produced — inline for a real target, scheduled at the epoch's reconnect instant (half
 * the grace) for a {@code none} (null-target) MOVING — flagging registered connections whose peer
 * is an affected source and running the handoff hooks. Uses a deterministic scheduler stub: tests
 * assert marking behavior, never registry contents.
 */
@Tag("sch")
public class MaintenanceMarkingTest {

  private static final HostAndPort TARGET_B = new HostAndPort("node-b.example.com", 6380);

  private TcpMockServer mockServer;
  private TcpMockServer otherServer;
  private StubScheduler scheduler;
  private MaintenanceEventController controller;
  private Connection receiver;

  @BeforeEach
  public void setUp() throws Exception {
    mockServer = new TcpMockServer();
    mockServer.start();
    otherServer = new TcpMockServer();
    otherServer.start();

    scheduler = new StubScheduler();
    controller = MaintenanceEventController.from(MaintenanceNotificationsConfig.builder()
        .endpointType(MaintenanceNotificationsConfig.EndpointType.NONE).build(),
      scheduler);

    receiver = connect(mockServer);
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (receiver != null) receiver.close();
    controller.close();
    mockServer.stop();
    otherServer.stop();
  }

  private Connection connect(TcpMockServer server) throws Exception {
    Connection conn = new Connection(new HostAndPort("127.0.0.1", server.getPort()));
    conn.connect();
    controller.registry().register(conn);
    return conn;
  }

  private void movingNone(long seq, long ttlSeconds) {
    controller.onMoving(new MovingEvent(seq, ttlSeconds, null), receiver);
  }

  private void moving(long seq, HostAndPort target, long ttlSeconds) {
    controller.onMoving(new MovingEvent(seq, ttlSeconds, target), receiver);
  }

  // --- marking scheduling and coverage ---

  @Test
  public void noneSchedulesMarkingAtHalfGrace() {
    AtomicInteger notified = new AtomicInteger();
    controller.addHandoffHook(notified::incrementAndGet);

    movingNone(1L, 10);

    assertFalse(scheduler.pending.isEmpty(), "'none' schedules the marking pass");
    assertTrue(scheduler.lastDelayNanos > TimeUnit.SECONDS.toNanos(4)
        && scheduler.lastDelayNanos <= TimeUnit.SECONDS.toNanos(5),
      "at half the raw grace");
    assertFalse(receiver.isMarkedForReconnect(), "nothing is marked before the pass runs");
    assertEquals(0, notified.get());

    scheduler.runPending();

    assertTrue(receiver.isMarkedForReconnect(), "the pass marks the affected source's connection");
    assertEquals(1, notified.get(), "the pass runs the handoff hooks");
  }

  @Test
  public void targetMarksInline() {
    AtomicInteger notified = new AtomicInteger();
    controller.addHandoffHook(notified::incrementAndGet);

    moving(1L, TARGET_B, 30);

    assertEquals(0, scheduler.scheduleCount, "a real target never schedules");
    assertTrue(receiver.isMarkedForReconnect(), "marked synchronously on the notifying thread");
    assertEquals(1, notified.get());
  }

  @Test
  public void markingCoversOnlyAffectedPeers() throws Exception {
    Connection unrelated = connect(otherServer);
    try {
      movingNone(1L, 10);
      scheduler.runPending();

      assertTrue(receiver.isMarkedForReconnect());
      assertFalse(unrelated.isMarkedForReconnect(), "different peer is out of scope");
    } finally {
      unrelated.close();
    }
  }

  // --- churn immunity: post-marking connections stay unmarked ---

  @Test
  public void postMarkingRedeliveryDoesNotMark() throws Exception {
    movingNone(1L, 10);
    scheduler.runPending(); // marking done

    // The reconnect re-lands on the still-moving node (DNS not repointed) and is re-notified with
    // the same-seq MOVING. Known source, no state change, no pass: it stays unmarked until the
    // remote close (temporal churn immunity).
    Connection reconnect = connect(mockServer);
    try {
      controller.onMoving(new MovingEvent(1L, 10, null), reconnect);
      assertFalse(reconnect.isMarkedForReconnect(), "post-marking connection is immune");
      assertTrue(scheduler.pending.isEmpty(), "no second marking pass scheduled");
    } finally {
      reconnect.close();
    }
  }

  // --- factory registration order ---

  @Test
  public void factoryRegistersBeforeSocketInit() throws Exception {
    // Registration precedes socket init, so a connect racing a MOVING commit is either visible to
    // the marking pass or sees the committed rebind via the address mapper — no unmarked
    // connection can land on the old node.
    AtomicBoolean registeredAtInit = new AtomicBoolean();
    ConnectionFactory factory = new ConnectionFactory.Builder() {
      @Override
      protected ConnectionFactory create() {
        return new ConnectionFactory(this) {
          @Override
          protected void initialize(Connection conn) {
            registeredAtInit.set(isRegistered(conn));
            super.initialize(conn);
          }
        };
      }
    }.hostAndPort(new HostAndPort("127.0.0.1", mockServer.getPort()))
        .clientConfig(DefaultJedisClientConfig.builder().protocol(RedisProtocol.RESP3).build())
        .maintenanceController(controller).build();

    PooledObject<Connection> pooled = factory.makeObject();
    try {
      assertTrue(registeredAtInit.get(), "registered before socket init");
    } finally {
      pooled.getObject().close();
    }
  }

  private boolean isRegistered(Connection conn) {
    AtomicBoolean found = new AtomicBoolean();
    controller.registry().forEachLive(c -> {
      if (c == conn) {
        found.set(true);
      }
    });
    return found.get();
  }

  // --- same-seq merges ---

  @Test
  public void pendingMarkingCoversSourcesMergedBeforeDue() throws Exception {
    movingNone(1L, 10); // marking pending at +5s, sources = {receiver's peer}

    // A second source joins the same epoch BEFORE the reconnect instant: nothing may be marked
    // early — its own scheduled pass fires at that instant and covers it.
    Connection otherSource = connect(otherServer);
    try {
      controller.onMoving(new MovingEvent(1L, 10, null), otherSource);
      assertFalse(receiver.isMarkedForReconnect(), "no early marking on merge");
      assertFalse(otherSource.isMarkedForReconnect(), "no early marking on merge");

      scheduler.runPending();

      assertTrue(receiver.isMarkedForReconnect());
      assertTrue(otherSource.isMarkedForReconnect(), "merged source covered by its scheduled pass");
    } finally {
      otherSource.close();
    }
  }

  @Test
  public void mergeAfterMarkingMarksTheNewSource() throws Exception {
    moving(1L, TARGET_B, 30); // this epoch's inline marking already ran

    // A genuinely new source (same endpoint reachable over another address) joins the same epoch
    // after its marking ran: the merge's own pass runs immediately and covers the new source.
    Connection otherSource = connect(otherServer);
    try {
      controller.onMoving(new MovingEvent(1L, 30, TARGET_B), otherSource);
      assertTrue(otherSource.isMarkedForReconnect(), "late-joining source marked immediately");
    } finally {
      otherSource.close();
    }
  }

  // --- overlapping events and stale fires ---

  @Test
  public void pendingNonePassSurvivesNewerEvent() throws Exception {
    AtomicInteger notified = new AtomicInteger();
    controller.addHandoffHook(notified::incrementAndGet);

    movingNone(1L, 10); // pass pending at +5s
    Runnable pendingPass = scheduler.pending.poll();

    // An overlapping newer event on another peer marks inline; the earlier unexpired event is NOT
    // orphaned — its pending pass still covers its own affected set.
    Connection otherSource = connect(otherServer);
    try {
      controller.onMoving(new MovingEvent(2L, 30, TARGET_B), otherSource);
      assertTrue(otherSource.isMarkedForReconnect(), "newer event marked inline");
      assertEquals(1, notified.get());
      assertFalse(receiver.isMarkedForReconnect(), "earlier 'none' event never marks early");

      pendingPass.run();
      assertTrue(receiver.isMarkedForReconnect(),
        "the pending pass of a still-active event runs despite the newer event");
      assertEquals(2, notified.get(), "the pending pass fires the hooks");
    } finally {
      otherSource.close();
    }
  }

  @Test
  public void expiredOperationPassIsNoop() throws Exception {
    AtomicLong now = new AtomicLong(0);
    NanoClock.INSTANCE = now::get;
    try {
      AtomicInteger notified = new AtomicInteger();
      controller.addHandoffHook(notified::incrementAndGet);

      movingNone(1L, 10); // pass pending at +5s
      Runnable stalePass = scheduler.pending.poll();

      now.addAndGet(TimeUnit.SECONDS.toNanos(11)); // past the ttl: operation expired

      // The server has dropped the affected connections by the window end; a pass firing late
      // must not mark connections that landed on the same peer afterwards.
      Connection fresh = connect(mockServer);
      try {
        stalePass.run();
        assertFalse(fresh.isMarkedForReconnect(), "expired operation's pass marks nothing");
        assertEquals(0, notified.get(), "expired operation's pass notifies nothing");
      } finally {
        fresh.close();
      }
    } finally {
      NanoClock.INSTANCE = System::nanoTime;
    }
  }

  // --- lifecycle ---

  @Test
  public void closeIsIdempotentAndLaterSchedulingIsRejected() {
    movingNone(1L, 10);
    assertFalse(scheduler.pending.isEmpty());

    controller.close();
    assertTrue(scheduler.shutdownNowCalled, "scheduler released on close");

    controller.close(); // idempotent

    // A MOVING racing the close: the executor rejects the pass and the controller swallows it —
    // stragglers are covered by the remote close at time_s.
    movingNone(2L, 10);
    assertEquals(1, scheduler.scheduleCount, "no marking pass scheduled after close");
  }

  @Test
  public void poolCloseReleasesMaintenanceScheduler() throws Exception {
    ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
    poolConfig.setMaxTotal(1);
    JedisClientConfig clientConfig = DefaultJedisClientConfig.builder().socketTimeoutMillis(5000)
        .protocol(RedisProtocol.RESP3).build();
    MaintenanceNotificationsConfig maint = MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.ENABLED)
        .endpointType(MaintenanceNotificationsConfig.EndpointType.NONE).build();

    java.util.Set<String> preexisting = liveMaintenanceThreads();
    ConnectionPool pool = new ConnectionPool(ConnectionFactory.builder()
        .hostAndPort(new HostAndPort("127.0.0.1", mockServer.getPort())).clientConfig(clientConfig),
        poolConfig, maint);
    try {
      Connection conn = pool.getResource();
      assertEquals(preexisting, liveMaintenanceThreads(),
        "no maintenance thread before the first null-target rebind");
      mockServer.sendPushMessageToAll("MOVING", 7, 60, null);
      assertTrue(conn.ping()); // reads the push; schedules the pass (spawns the worker thread)
      conn.close();
      assertFalse(liveMaintenanceThreads().equals(preexisting),
        "this pool's maintenance thread is live");
    } finally {
      pool.close(); // destroy() funnel -> controller.close() -> shutdownNow
    }
    org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(2))
        .until(() -> liveMaintenanceThreads().equals(preexisting));
  }

  private static java.util.Set<String> liveMaintenanceThreads() {
    java.util.Set<String> names = new java.util.HashSet<>();
    for (Thread t : Thread.getAllStackTraces().keySet()) {
      if (t.isAlive() && t.getName().startsWith("jedis-maintenance-")) {
        names.add(t.getName());
      }
    }
    return names;
  }

  /** Captures at most one scheduled task; runs it on demand on the caller's thread. */
  private static final class StubScheduler implements ScheduledExecutorService {

    final java.util.ArrayDeque<Runnable> pending = new java.util.ArrayDeque<>();
    long lastDelayNanos = -1;
    int scheduleCount;
    boolean shutdownNowCalled;

    /** Runs every queued marking pass, in scheduling order. */
    void runPending() {
      Runnable task;
      while ((task = pending.poll()) != null) {
        task.run();
      }
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
      if (shutdownNowCalled) {
        throw new java.util.concurrent.RejectedExecutionException("scheduler is shut down");
      }
      pending.add(command);
      lastDelayNanos = unit.toNanos(delay);
      scheduleCount++;
      return new StubFuture();
    }

    @Override
    public List<Runnable> shutdownNow() {
      shutdownNowCalled = true;
      List<Runnable> dropped = new java.util.ArrayList<>(pending);
      pending.clear();
      return dropped;
    }

    @Override
    public void shutdown() {
      shutdownNowCalled = true;
    }

    @Override
    public boolean isShutdown() {
      return shutdownNowCalled;
    }

    @Override
    public boolean isTerminated() {
      return shutdownNowCalled;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
      return true;
    }

    @Override
    public void execute(Runnable command) {
      command.run();
    }

    // Unused surface — the controller only ever calls schedule(Runnable) and shutdownNow().
    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period,
        TimeUnit unit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay,
        long delay, TimeUnit unit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> java.util.concurrent.Future<T> submit(Callable<T> task) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> java.util.concurrent.Future<T> submit(Runnable task, T result) {
      throw new UnsupportedOperationException();
    }

    @Override
    public java.util.concurrent.Future<?> submit(Runnable task) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(
        java.util.Collection<? extends Callable<T>> tasks) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> List<java.util.concurrent.Future<T>> invokeAll(
        java.util.Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(java.util.Collection<? extends Callable<T>> tasks) {
      throw new UnsupportedOperationException();
    }

    @Override
    public <T> T invokeAny(java.util.Collection<? extends Callable<T>> tasks, long timeout,
        TimeUnit unit) {
      throw new UnsupportedOperationException();
    }

    private final class StubFuture implements ScheduledFuture<Object> {

      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
        return false; // production code never cancels marking passes
      }

      @Override
      public boolean isCancelled() {
        return false;
      }

      @Override
      public boolean isDone() {
        return pending.isEmpty();
      }

      @Override
      public Object get() {
        throw new UnsupportedOperationException();
      }

      @Override
      public Object get(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
      }

      @Override
      public long getDelay(TimeUnit unit) {
        return unit.convert(lastDelayNanos, TimeUnit.NANOSECONDS);
      }

      @Override
      public int compareTo(Delayed o) {
        return Long.compare(getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
      }
    }
  }

}
