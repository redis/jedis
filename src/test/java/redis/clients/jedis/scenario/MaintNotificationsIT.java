package redis.clients.jedis.scenario;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.awaitility.core.ConditionTimeoutException;

import com.redis.test.fi.Scenario;
import com.redis.test.fi.StandaloneEffect;
import com.redis.test.fi.Trigger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.CommandObject;
import redis.clients.jedis.Connection;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.MaintNotificationsTestSupport;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvent;
import redis.clients.jedis.MaintNotificationsTestSupport.ReceivedEvents;
import redis.clients.jedis.MaintenanceNotificationsConfig.EndpointType;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.PushMessageTypes;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 * Verifies timeouts are relaxed while a maintenance window (MIGRATING/FAILING_OVER ..
 * MIGRATED/FAILED_OVER) is open and restored after it closes — behaviorally, for non-blocking and
 * blocking commands, through the public client: the server-side BLPOP delay (3 s) only survives a 1
 * s base socket timeout when relaxation is in effect. MIGRATING/FAILING_OVER announce the operation
 * ~2 s ahead (ttl), so probes launched on the opening always complete inside the window. A
 * timed-out probe is protocol-desynced and its connection discarded by the pool, so window state
 * rides on a pinned observer connection the probes never touch.
 */
@Tag("scenario")
public class MaintNotificationsIT extends MaintNotificationsScenarioBase {

  private static final int PROBE_DELAY_SECONDS = 3;
  /** Relaxed probes succeed at the server delay, never at the base timeout. */
  private static final long RELAXED_SUCCESS_FLOOR_MS = 2_500;
  private static final Duration PUSH_WAIT_TIMEOUT = Duration.ofSeconds(60);

  private static final CommandObject<List<String>> REGULAR_PROBE = new CommandObject<>(
      new CommandArguments(Protocol.Command.BLPOP).key("missing{probe}").add(PROBE_DELAY_SECONDS),
      BuilderFactory.STRING_LIST); // -> socketTimeout / relaxedTimeout
  private static final CommandObject<List<String>> BLOCKING_PROBE = new CommandObject<>(
      new CommandArguments(Protocol.Command.BLPOP).key("missing{probe}").add(PROBE_DELAY_SECONDS)
          .blocking(),
      BuilderFactory.STRING_LIST); // -> blockingSocketTimeout / relaxedBlockingTimeout

  private ExecutorService probeExecutor;

  static Stream<Scenario> noConnDropScenarios() {
    return CATALOG.effect(StandaloneEffect.DATA_MOVEMENT_NO_CONN_DROP).triggers().stream()
        .map(Trigger::scenario);
  }

  static Stream<Scenario> movingScenarios() {
    // conn_drop/endpoint_rebind emits exactly [MOVING]
    return Stream
        .of(CATALOG.effect(StandaloneEffect.CONN_DROP).trigger("endpoint_rebind").scenario());
  }

  static Stream<Arguments> handoffScenarios() {
    return movingScenarios().flatMap(scenario -> Stream.of(EndpointType.EXTERNAL_IP,
      EndpointType.INTERNAL_IP, EndpointType.EXTERNAL_FQDN, EndpointType.INTERNAL_FQDN)
        .map(type -> Arguments.of(scenario, type)));
  }

  /**
   * T.1.2 Timeout Handling During Notifications — covers timeoutRelaxedOnMigratingTest,
   * timeoutUnrelaxedOnMigratedTest, timeoutRelaxedOnFailoverTest, timeoutUnrelaxedOnFailedoverTest.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("noConnDropScenarios")
  @Timeout(300)
  void timeoutRelaxedWithinMaintenanceWindow(Scenario scenario) {
    setUpDatabaseAndClient(scenario);
    warmUpPool();
    pinned = pinConnection();
    ReceivedEvents received = MaintNotificationsTestSupport.record(pinned);

    // base timeouts enforced for non-blocking and blocking commands, in parallel
    assertProbesTimeOutAtBase("baseline");

    startEffect(scenario);

    // the opening announces the operation ~2s ahead: the window is open from here to the closing
    awaitPush(received::openingReceived, "opening (MIGRATING/FAILING_OVER)", received);
    assertEquals(RELAXED_TIMEOUT_MS, MaintNotificationsTestSupport.effectiveTimeoutMillis(pinned),
      "opening notification must relax the observer socket timeout");

    // relaxation enforced behaviorally for non-blocking and blocking commands, in parallel
    observeAndAwait(runProbes())
        .forEach(probe -> assertProbeSucceededAtServerDelay(probe, received));

    awaitPush(received::closingReceived, "closing (MIGRATED/FAILED_OVER)", received);
    assertEquals(CLIENT_SOCKET_TIMEOUT_MS,
      MaintNotificationsTestSupport.effectiveTimeoutMillis(pinned),
      "closing notification must restore the observer socket timeout");

    joinEffectThreads();
    assertEffectSucceeded();

    // base restored behaviorally for non-blocking and blocking commands, in parallel
    assertProbesTimeOutAtBase("after close");
    releasePinned();
  }

  /**
   * T.1.2 Timeout Handling During Notifications — covers:
   * <ul>
   * <li>timeoutRelaxedOnMovingTest — send MOVING, send commands/traffic: commands complete without
   * timeout during the grace period (no timeout exceptions during handoff).</li>
   * <li>timeoutUnrelaxedAfterMovingDelayTest — wait for handoff completion, send commands/traffic:
   * normal timeout behavior restored (timeout exceptions occur as expected).</li>
   * </ul>
   * MOVING has no closing notification: receiving it on any pool connection relaxes timeouts on all
   * connections toward the affected node, until the MOVING ttl window expires.
   */
  @ParameterizedTest(name = "{0}")
  @MethodSource("movingScenarios")
  @Timeout(300)
  void timeoutRelaxedWithinMovingWindow(Scenario scenario) {
    setUpDatabaseAndClient(scenario);
    warmUpPool();
    pinned = pinConnection();
    ReceivedEvents received = MaintNotificationsTestSupport.record(pinned);

    assertProbesTimeOutAtBase("baseline");

    startEffect(scenario);

    awaitPush(received::movingReceived, "MOVING", received);
    ReceivedEvent moving = received.all().stream()
        .filter(e -> PushMessageTypes.MOVING.equals(e.type)).findFirst().get();
    assertEquals(RELAXED_TIMEOUT_MS, MaintNotificationsTestSupport.effectiveTimeoutMillis(pinned),
      "MOVING must relax the observer socket timeout");

    // pool-wide relaxation: the probes run on other pool connections (the pool recreates the
    // retired ones toward the MOVING target) and must survive to the server delay
    runProbes().join().forEach(probe -> assertProbeSucceededAtServerDelay(probe, received));

    assertEquals(RELAXED_TIMEOUT_MS, MaintNotificationsTestSupport.effectiveTimeoutMillis(pinned),
      "no closing notification: relaxation holds until the window expires");

    // un-relaxation happens only by expiry of the MOVING ttl window
    await().atMost(Duration.ofSeconds(moving.timeSeconds + 10)).pollInterval(Duration.ofMillis(100))
        .until(() -> MaintNotificationsTestSupport
            .effectiveTimeoutMillis(pinned) == CLIENT_SOCKET_TIMEOUT_MS);
    long relaxedForMillis = (System.nanoTime() - moving.atNanos) / 1_000_000;
    assertTrue(relaxedForMillis >= TimeUnit.SECONDS.toMillis(moving.timeSeconds) - 100,
      "relaxation ended " + relaxedForMillis + " ms after MOVING, before its " + moving.timeSeconds
          + " s window expired");

    joinEffectThreads();
    assertEffectSucceeded();

    // the observer's socket toward the old endpoint may be dead after the rebind, so probe
    // without pumping it
    runProbes().join().forEach(probe -> assertProbeTimedOutAtBase(probe, "after expiry"));
    releasePinned();
  }

  /**
   * T.2.1 New Connection Establishment — covers newConnectionEstablishedTest,
   * connectionHandedOffToNewEndpointExternalIPTest, connectionHandedOffToNewEndpointInternalIPTest,
   * connectionHandoffWithStaticExternalNameTest, connectionHandoffWithStaticInternalNameTest. On an
   * endpoint_rebind MOVING:
   * <ul>
   * <li>a connection borrowed before MOVING completes its in-flight commands gracefully and is
   * destroyed when returned to the pool;</li>
   * <li>new connections are created toward the notification's target and get the relaxed timeout
   * like the rest of the pool; old- and new-endpoint connections work concurrently;</li>
   * <li>after the window expires, a connection that outlived it throws JedisConnectionException and
   * is destroyed on return, while the client keeps working through fresh connections to the
   * configured endpoint name.</li>
   * </ul>
   */
  @ParameterizedTest(name = "{0} [{1}]")
  @MethodSource("handoffScenarios")
  @Timeout(300)
  void connectionHandoffOnMoving(Scenario scenario, EndpointType endpointType) {
    setUpDatabaseAndClient(scenario, endpointType);
    warmUpPool();
    pinned = pinConnection();

    Connection inflight = client.getPool().getResource(); // borrowed before MOVING
    ReceivedEvents inflightEvents = MaintNotificationsTestSupport.record(inflight);

    startEffect(scenario);

    // continuous commands on the pre-MOVING connection: the read that consumes MOVING mid-command
    // must still complete gracefully — an exception here fails the test
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(120);
    while (!inflightEvents.movingReceived()) {
      assertTrue(System.nanoTime() < deadline, "no MOVING within 120 s; received: "
          + inflightEvents.all() + ", effect failures: " + effectFailures);
      inflight.executeCommand(Protocol.Command.PING);
    }
    ReceivedEvent moving = inflightEvents.all().stream()
        .filter(e -> PushMessageTypes.MOVING.equals(e.type)).findFirst().get();
    assertNotNull(moving.target, "endpoint_rebind MOVING must carry a target");
    // a long in-flight command on the old endpoint still completes inside the grace window
    inflight.executeCommand(REGULAR_PROBE);

    assumeTargetReachable(moving.target, endpointType);

    // new connections are created toward the notification's endpoint, relaxed like the rest
    Connection handedOff = client.getPool().getResource();
    assertEquals(moving.target, MaintNotificationsTestSupport.remoteAddress(handedOff),
      "new connection must target the MOVING endpoint");
    assertEquals(RELAXED_TIMEOUT_MS,
      MaintNotificationsTestSupport.effectiveTimeoutMillis(handedOff),
      "new connection must comply with the relaxed timeout");
    // two active connections during handoff: old and new endpoint usable concurrently
    handedOff.executeCommand(Protocol.Command.PING);
    inflight.executeCommand(Protocol.Command.PING);

    // the pre-MOVING connection is discarded on return, never handed out again
    long destroyedBefore = client.getPool().getDestroyedCount();
    inflight.close();
    assertTrue(client.getPool().getDestroyedCount() > destroyedBefore,
      "returning a connection borrowed before MOVING must destroy it");
    Connection next = client.getPool().getResource();
    assertEquals(moving.target, MaintNotificationsTestSupport.remoteAddress(next),
      "replacement for the discarded connection must target the MOVING endpoint");
    next.close();
    handedOff.close();

    joinEffectThreads();
    assertEffectSucceeded();

    // the client keeps working after the grace window expires: new connections can be created
    // toward the configured endpoint name (repointed by the rebind) and used
    long windowLeftMillis = TimeUnit.SECONDS.toMillis(moving.timeSeconds)
        - elapsedMillis(moving.atNanos);
    if (windowLeftMillis > 0) {
      sleepQuietly(windowLeftMillis + 1_000);
    }
    // a borrowed connection that outlived the rebind window has been disconnected by the server
    assertThrows(JedisConnectionException.class, () -> pinned.executeCommand(Protocol.Command.PING),
      "using a connection to the departed endpoint must throw after the rebind window");

    client.getPool().clear(); // drop window-era connections: the next borrow must connect fresh
    assertEquals("PONG", client.ping(), "client must keep working after the MOVING window expires");
    releasePinned();
  }

  /** Internal targets may not be routable/resolvable from the test host; skip, do not fail. */
  private static void assumeTargetReachable(String target, EndpointType endpointType) {
    if (endpointType != EndpointType.INTERNAL_IP && endpointType != EndpointType.INTERNAL_FQDN) {
      return;
    }
    HostAndPort hostAndPort = HostAndPort.from(target);
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort()), 2_000);
    } catch (IOException e) {
      assumeTrue(false, "MOVING target " + target + " is not routable from the test host: " + e);
    }
  }

  // --- probes ---

  /** Starts the non-blocking and blocking command probes in parallel. */
  private CompletableFuture<List<ProbeContext>> runProbes() {
    CompletableFuture<ProbeContext> regular = CompletableFuture
        .supplyAsync(() -> runProbe(REGULAR_PROBE), probeExecutor());
    CompletableFuture<ProbeContext> blocking = CompletableFuture
        .supplyAsync(() -> runProbe(BLOCKING_PROBE), probeExecutor());
    return regular.thenCombine(blocking, Arrays::asList);
  }

  private static void assertProbeSucceededAtServerDelay(ProbeContext probe,
      ReceivedEvents received) {
    if (probe.error != null || probe.durationMillis < RELAXED_SUCCESS_FLOOR_MS) {
      fail(probe + " must survive to the server delay inside the relaxation window; events: "
          + received.all());
    }
  }

  /**
   * Both probes must fail at their own base timeout: the non-blocking probe at socketTimeout (1s),
   * the blocking probe at blockingSocketTimeout (2s) — the distinct values prove the blocking flag
   * routed to the right timeout.
   */
  private void assertProbesTimeOutAtBase(String phase) {
    observeAndAwait(runProbes()).forEach(probe -> assertProbeTimedOutAtBase(probe, phase));
  }

  private static void assertProbeTimedOutAtBase(ProbeContext probe, String phase) {
    long atLeast = probe.blocking() ? CLIENT_BLOCKING_SOCKET_TIMEOUT_MS : CLIENT_SOCKET_TIMEOUT_MS;
    assertTrue(probe.error instanceof JedisConnectionException,
      phase + ": expected a base-timeout failure, got " + probe);
    // a timeout never fires earlier than configured; it can fire later when a push arrives
    // before the reply — that read returns the push, and the reply wait restarts its own window
    assertTrue(probe.durationMillis >= atLeast,
      phase + ": timed out earlier than the configured " + atLeast + " ms: " + probe);
    // staying under the blocking base proves the non-blocking timeout was in force
    if (!probe.blocking()) {
      assertTrue(probe.durationMillis < CLIENT_BLOCKING_SOCKET_TIMEOUT_MS,
        phase + ": outlived the non-blocking timeout — wrong timeout in force: " + probe);
    }
  }

  private ProbeContext runProbe(CommandObject<List<String>> probe) {
    long start = System.nanoTime();
    try {
      client.executeCommand(probe);
      return new ProbeContext(probe, start, null, elapsedMillis(start));
    } catch (RuntimeException e) {
      return new ProbeContext(probe, start, e, elapsedMillis(start));
    }
  }

  /** One executed probe: the command it ran, when it started, and its outcome. */
  private static final class ProbeContext {
    final CommandObject<List<String>> probe;
    final long startedAtNanos;
    final Throwable error;
    final long durationMillis;

    ProbeContext(CommandObject<List<String>> probe, long startedAtNanos, Throwable error,
        long durationMillis) {
      this.probe = probe;
      this.startedAtNanos = startedAtNanos;
      this.error = error;
      this.durationMillis = durationMillis;
    }

    boolean blocking() {
      return probe.getArguments().isBlocking();
    }

    @Override
    public String toString() {
      return (blocking() ? "blocking" : "non-blocking") + " probe "
          + (error == null ? "success" : "failure(" + error + ")") + " after " + durationMillis
          + " ms";
    }
  }

  private ExecutorService probeExecutor() {
    if (probeExecutor == null) {
      probeExecutor = Executors.newFixedThreadPool(2);
    }
    return probeExecutor;
  }

  /**
   * Observes the pinned connection until {@code received} turns true; see
   * {@link MaintNotificationsScenarioBase#observe()} for why a command round-trip is used.
   */
  private void awaitPush(Callable<Boolean> received, String description, ReceivedEvents events) {
    try {
      await().atMost(PUSH_WAIT_TIMEOUT).pollInterval(Duration.ofMillis(100)).until(() -> {
        observe();
        return received.call();
      });
    } catch (ConditionTimeoutException e) {
      fail("Timed out waiting for " + description + " notification; received so far: "
          + events.all() + ", effect failures: " + effectFailures);
    }
  }

  @AfterEach
  void shutdownProbeExecutor() {
    if (probeExecutor != null) {
      probeExecutor.shutdownNow();
      probeExecutor = null;
    }
  }
}
