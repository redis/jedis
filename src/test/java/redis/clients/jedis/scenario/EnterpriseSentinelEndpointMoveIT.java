package redis.clients.jedis.scenario;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.RedisSentinelClient;
import redis.clients.jedis.providers.SentineledConnectionProvider;
import redis.clients.jedis.util.ClientTestUtil;
import redis.clients.jedis.util.Pool;

/**
 * Verifies that a Jedis {@link RedisSentinelClient} follows a Redis Enterprise database endpoint
 * when it moves to another node.
 * <p>
 * Redis Enterprise publishes {@code +switch-master} the moment the endpoint moves, and then keeps
 * the old proxy serving for {@code endpoint_rebind_propagation_grace_time} seconds so a client that
 * heard the announcement can move across without losing anything. This test widens that window and
 * asserts the handoff happens well inside it - which is only possible if the announcement was
 * consumed, since a client that had not heard it could not reach the new endpoint before the old
 * proxy goes away.
 * <p>
 * Two Jedis details make the assertions sharp. {@code SentinelClientBuilder} configures no retries,
 * so any command meeting a dropped connection surfaces a {@code JedisConnectionException} to the
 * caller - an empty exception list therefore proves the client was never disconnected. And
 * {@code SentineledConnectionProvider#initMaster} builds a new pool and closes the old one, so the
 * pool identity is independent evidence that the switch was processed rather than a field being
 * stale.
 * <p>
 * Known gaps, deliberately not covered here: node maintenance mode (the same announcement by a
 * different route, but it drains every database on the node and needs a two-step
 * {@code reset_cluster} undo); and the listener's payload guard, which checks {@code length > 3}
 * then reads index 4, so a four-token {@code +switch-master} would kill the listener thread with an
 * uncaught {@code ArrayIndexOutOfBoundsException} - not reachable from a live cluster, which always
 * sends five.
 * <p>
 * A hard-killed run may leave the endpoint excluded from its original node; the next run converges
 * by collapsing the endpoint back onto a single proxy, but a manual
 * {@code rladmin bind endpoint <uid> policy single} also fixes it - that form resets both
 * {@code include_proxies} and {@code exclude_proxies}, where {@code include <node>} clears only the
 * exclude and leaves the endpoint proxied by two nodes.
 */
@Tags({ @Tag("scenario") })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Jedis follows a Redis Enterprise endpoint move announced over Sentinel")
public class EnterpriseSentinelEndpointMoveIT {

  private static final Logger log = LoggerFactory.getLogger(EnterpriseSentinelEndpointMoveIT.class);

  /** Widened so "reacted to the announcement" is separable from "waited to be dropped". */
  private static final int GRACE_TIME_SECONDS = 60;

  private static final int DEFAULT_GRACE_TIME_SECONDS = 15;

  /** Comfortably inside the grace window: a failure here means "never reacted", not "was slow". */
  private static final Duration REACTION_TIMEOUT = Duration.ofSeconds(20);

  private static final Duration SWITCH_MASTER_TIMEOUT = Duration.ofMinutes(2);

  private static final Duration SILENCE_WINDOW = Duration.ofSeconds(45);

  private static final Duration RLADMIN_CHECK_INTERVAL = Duration.ofSeconds(3);

  private static final Duration RLADMIN_TIMEOUT = Duration.ofMinutes(2);

  private static final Duration FAILOVER_TIMEOUT = Duration.ofMinutes(5);

  /**
   * How long the discovery service may lag CCS before the endpoint it reports has to match the
   * endpoint's only proxy.
   */
  private static final Duration DISCOVERY_AGREEMENT_TIMEOUT = Duration.ofSeconds(30);

  private static EndpointConfig endpoint;

  private final FaultInjectionClient faultClient = new FaultInjectionClient();

  private List<HostAndPort> sentinels;

  private HostAndPort sentinel;

  private String masterName;

  private String bdbId;

  private RedisEnterpriseClusterStatus status;

  /**
   * Node the discovery service currently reports for the database. Draining this node is what makes
   * Redis Enterprise publish {@code +switch-master}; draining any other proxy is silent.
   */
  private String trackedNodeUid;

  private RedisSentinelClient client;

  private EnterpriseSentinelSupport.SwitchMasterCapture capture;

  private FakeApp workload;

  private Thread workloadThread;

  private final AtomicBoolean stopWorkload = new AtomicBoolean();

  private final AtomicLong commandsExecuted = new AtomicLong();

  private FaultInjectionClient.TriggerActionResponse pendingAction;

  private boolean endpointMoved;

  private boolean graceTimeChanged;

  @BeforeAll
  public static void setupEndpoint() {
    endpoint = Endpoints.getRedisEndpoint(EnterpriseSentinelSupport.DB_ENDPOINT_NAME);
  }

  @BeforeEach
  public void discoverCluster() throws IOException {
    // Every assumption is evaluated before anything mutates the cluster.
    sentinels = EnterpriseSentinelSupport.sentinelHostsAndPorts(endpoint);
    assumeTrue(!sentinels.isEmpty(), EnterpriseSentinelSupport.missingDiscoveryEndpoints());

    sentinel = EnterpriseSentinelSupport.firstReachableSentinel(sentinels);
    assumeTrue(sentinel != null, EnterpriseSentinelSupport.unreachableDiscoveryService(sentinels));
    assumeTrue(EnterpriseSentinelSupport.databaseReachable(endpoint),
      EnterpriseSentinelSupport.unreachableDatabase(endpoint));

    masterName = EnterpriseSentinelSupport.DB_ENDPOINT_NAME;
    bdbId = String.valueOf(endpoint.getBdbId());

    // Re-read the binding on every test rather than assuming the original one: that is what makes
    // the suite restartable after a run that died between the move and the restore.
    status = RedisEnterpriseClusterStatus.discover(faultClient, bdbId, RLADMIN_CHECK_INTERVAL,
      RLADMIN_TIMEOUT);
    assertEquals(masterName, status.getDbName(),
      "The Sentinel master name must be the Redis Enterprise database name");

    endpointMoved = false;
    graceTimeChanged = false;
    pendingAction = null;
    stopWorkload.set(false);
    commandsExecuted.set(0);

    // Collapse the endpoint onto a single proxy before measuring anything. A previous test may
    // have left it with two, and with two proxies the node rladmin lists and the node the
    // discovery service reports can differ.
    trackedNodeUid = normaliseToSingleProxy();
    log.info("Database {} (bdb {}): {}, discovery service tracks node:{}", masterName, bdbId,
      status, trackedNodeUid);

    graceTimeChanged = runRladminQuietly(
      "tune cluster endpoint_rebind_propagation_grace_time " + GRACE_TIME_SECONDS);
    assumeTrue(graceTimeChanged,
      "Skipping: could not widen endpoint_rebind_propagation_grace_time, so the test has no way to "
          + "distinguish reacting to the announcement from waiting to be disconnected");
  }

  @AfterEach
  public void restoreCluster() {
    if (capture != null) {
      // The restore moves the endpoint back; keep that out of the next test's assertions.
      capture.stopRecording();
    }

    stopWorkload.set(true);
    if (workloadThread != null) {
      try {
        workloadThread.join(Duration.ofSeconds(5).toMillis());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      workloadThread = null;
    }

    // Restoring while the endpoint-rebind state machine is still running races it, and the
    // single-worker fault injector would serialise the restore behind it anyway.
    if (pendingAction != null) {
      try {
        faultClient.awaitAction(pendingAction.getActionId(), RLADMIN_CHECK_INTERVAL,
          FAILOVER_TIMEOUT);
      } catch (RuntimeException e) {
        log.warn("Pending action {} did not finish cleanly: {}", pendingAction.getActionId(),
          e.toString());
      }
      pendingAction = null;
    }

    if (endpointMoved) {
      // 'policy single' resets both overriding constraints - rladmin sends empty include_proxies
      // and exclude_proxies alongside the policy - and collapses the endpoint back onto one proxy.
      // 'include <node>' would clear the exclude but leave a two-proxy endpoint behind, which then
      // makes the next test's 'exclude' a coin flip: draining a proxy the discovery service is not
      // tracking changes nothing and publishes nothing.
      runRladminQuietly("bind endpoint " + status.getEndpointUid() + " policy single");
    }

    if (graceTimeChanged) {
      runRladminQuietly(
        "tune cluster endpoint_rebind_propagation_grace_time " + DEFAULT_GRACE_TIME_SECONDS);
    }

    if (client != null) {
      client.close();
      client = null;
    }
    if (capture != null) {
      capture.close();
      capture = null;
    }
  }

  @Test
  @Order(1)
  @DisplayName("moves to the announced endpoint without waiting to be disconnected")
  public void followsSwitchMasterAfterEndpointRebind() throws IOException {
    HostAndPort before = connectThroughSentinel();
    Pool<Connection> poolBefore = currentPool();

    String key = "jedis-re-endpoint-move-" + System.currentTimeMillis();
    client.set(key, "before-move");
    startWorkload();

    // Drain the node the discovery service tracks, not whichever proxy rladmin printed first: only
    // removing the tracked node from proxy_uids forces the reported address to change.
    String command = "bind endpoint " + status.getEndpointUid() + " exclude " + trackedNodeUid;
    endpointMoved = true;
    // Started, not awaited: the grace-window wait happens inside the action, so blocking here would
    // only start measuring once the window had already closed.
    pendingAction = faultClient.triggerRladminCommand(bdbId, command);

    HostAndPort after = assertHandoffWithinGraceWindow(before, poolBefore);
    assertEquals("before-move", client.get(key), "the new endpoint serves the pre-move data");
    log.info("Endpoint moved {} -> {}", before, after);
    client.del(key);
  }

  @Test
  @Order(2)
  @DisplayName("negative control: a shard failover moves no endpoint and publishes nothing")
  public void shardFailoverMovesNoEndpointAndPublishesNothing() throws IOException {
    HostAndPort before = connectThroughSentinel();
    capture.clear();

    // Redis Enterprise only rebinds an endpoint when the recomputed proxy set is a strict superset
    // of the current one, which a failover swap never is. So the endpoint - and the address clients
    // connect to - does not move, and nothing is published. Worth pinning because the fault
    // injector has a first-class 'failover' action that a reader expects to produce an event.
    HashMap<String, Object> parameters = new HashMap<>();
    parameters.put("bdb_id", bdbId);
    FaultInjectionClient.TriggerActionResponse response = faultClient.triggerAction("failover",
      parameters);
    assertTrue(
      faultClient.awaitAction(response.getActionId(), RLADMIN_CHECK_INTERVAL, FAILOVER_TIMEOUT),
      "the failover action should complete successfully");

    sleep(SILENCE_WINDOW);

    assertTrue(capture.switchMasterPayloads(masterName).isEmpty(),
      "no +switch-master for the plain master name");
    assertTrue(capture.switchMasterPayloads(masterName + EnterpriseSentinelSupport.INTERNAL_SUFFIX)
        .isEmpty(),
      "no +switch-master for the @internal master name");
    assertEquals(before, EnterpriseSentinelSupport.reportedMaster(sentinel, masterName),
      "the discovery service still reports the same endpoint");
    assertEquals(before, client.getCurrentMaster(), "the client still targets the same endpoint");
    // Otherwise "no event" would be indistinguishable from "the subscriber was dropped".
    assertTrue(capture.isAlive(), "the discovery-service subscriber stayed connected");
    // Deliberately no assertion on workload exceptions: a shard failover legitimately drops data
    // connections, which is not what this test is about.
  }

  /**
   * Connect the way an application would and return the endpoint the client landed on.
   */
  private HostAndPort connectThroughSentinel() {
    capture = new EnterpriseSentinelSupport.SwitchMasterCapture(sentinel);

    client = RedisSentinelClient.builder().masterName(masterName)
        .sentinels(new HashSet<>(sentinels))
        .sentinelClientConfig(EnterpriseSentinelSupport.sentinelClientConfig())
        .clientConfig(endpoint.getClientConfigBuilder().build())
        .poolConfig(RecommendedSettings.poolConfig).build();

    assertEquals("PONG", client.ping());

    HostAndPort reported = EnterpriseSentinelSupport.reportedMaster(sentinel, masterName);
    assertEquals(reported, client.getCurrentMaster(),
      "Sentinel discovery put the client on the endpoint the service reports");
    log.info("Connected to {} (endpoint reported by the discovery service)", reported);
    return reported;
  }

  private Pool<Connection> currentPool() {
    return client.getPrimaryNodesConnectionMap().values().iterator().next();
  }

  /**
   * A light command loop on its own thread. Its purpose is the exception list: with no retries
   * configured, a dropped connection would surface here.
   */
  private void startWorkload() {
    // Never given an action, so FakeApp's loop condition stays true and it runs until the lambda
    // reports failure - that is the shutdown signal.
    workload = new FakeApp(client, (FakeApp.ExecutedAction) c -> {
      c.ping();
      commandsExecuted.incrementAndGet();
      try {
        Thread.sleep(50);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
      return !stopWorkload.get();
    });
    workloadThread = new Thread(workload, "re-sentinel-workload");
    workloadThread.setDaemon(true);
    workloadThread.start();
  }

  /**
   * Await the announcement, then assert the client acted on it inside the grace window.
   */
  private HostAndPort assertHandoffWithinGraceWindow(HostAndPort before,
      Pool<Connection> poolBefore) {
    await().atMost(SWITCH_MASTER_TIMEOUT).pollInterval(Duration.ofMillis(500))
        .until(() -> !capture.switchMasterPayloads(masterName).isEmpty());

    String payload = capture.switchMasterPayloads(masterName).get(0);
    log.info("+switch-master payload: {}", payload);

    // One event per master name, and only the plain name is addressed to a client asking for it.
    assertEquals(1, capture.switchMasterPayloads(masterName).size(),
      "+switch-master for the plain master name");
    assertEquals(1,
      capture.switchMasterPayloads(masterName + EnterpriseSentinelSupport.INTERNAL_SUFFIX).size(),
      "+switch-master for the @internal master name");

    String[] parts = payload.split(" ");
    assertEquals(5, parts.length, "payload is '<name> <old-ip> <old-port> <new-ip> <new-port>'");
    assertEquals(masterName, parts[0]);
    assertEquals(before, new HostAndPort(parts[1], Integer.parseInt(parts[2])), "old address");
    assertEquals(parts[2], parts[4], "the port never changes on an endpoint move");

    HostAndPort after = new HostAndPort(parts[3], Integer.parseInt(parts[4]));
    assertNotEquals(before, after, "the new address differs from the old one");

    Instant announcedAt = capture.firstSwitchMasterAt();
    assertNotNull(announcedAt);
    log.info("Announced move: {} -> {}. Client has {}s of grace window to act.", before, after,
      GRACE_TIME_SECONDS);

    // The old proxy is still serving at this point - that is the whole purpose of the grace window
    // -
    // so the client cannot have been forced off it.
    try (Jedis old = new Jedis(before,
        DefaultJedisClientConfig.builder().user(endpoint.getUsername())
            .password(endpoint.getPassword()).socketTimeoutMillis(3000)
            .connectionTimeoutMillis(3000).build())) {
      assertEquals("PONG", old.ping(), "the old endpoint is still serving during the grace window");
    }

    await().atMost(REACTION_TIMEOUT).pollInterval(Duration.ofMillis(250))
        .until(() -> after.equals(client.getCurrentMaster()));

    Duration reaction = Duration.between(announcedAt, Instant.now());
    log.info("Client moved to {} in {}ms", after, reaction.toMillis());
    assertTrue(reaction.getSeconds() < GRACE_TIME_SECONDS,
      "reacted inside the " + GRACE_TIME_SECONDS + "s grace window rather than waiting to be "
          + "disconnected, took " + reaction.toMillis() + "ms");

    // With no retry executor configured, a dropped connection would have surfaced here.
    assertTrue(commandsExecuted.get() > 0, "the workload actually ran");
    assertTrue(workload.capturedExceptions().isEmpty(),
      "the handoff was driven by the announcement, not by a disconnect: "
          + workload.capturedExceptions());

    // The provider built a new pool and closed the old one, so this is the switch being processed
    // rather than a stale field.
    Pool<Connection> poolAfter = currentPool();
    assertNotSame(poolBefore, poolAfter, "a new connection pool was created for the new endpoint");
    assertTrue(poolBefore.isClosed(), "the pool for the old endpoint was closed");

    SentineledConnectionProvider provider = ClientTestUtil.getConnectionProvider(client);
    try (Connection connection = provider.getConnection()) {
      assertEquals(after, connection.getHostAndPort(), "new connections target the new endpoint");
    }

    assertTrue(capture.isAlive(), "the discovery-service subscriber stayed connected");
    return after;
  }

  /**
   * Collapse the endpoint onto exactly one proxy and return the node uid of that proxy.
   * <p>
   * Redis Enterprise publishes {@code +switch-master} only when the address it reports for the
   * database changes, and it keeps reporting the same node for as long as that node stays in the
   * endpoint's {@code proxy_uids}. With more than one proxy, draining the node rladmin happens to
   * print first is therefore not enough: if it is not the node being tracked, the reported address
   * never changes and nothing is published. Starting from a single proxy removes the ambiguity,
   * because then rladmin and the discovery service cannot disagree.
   * @return the bare node uid of the single proxy, which is also the node the discovery service
   *         reports.
   */
  private String normaliseToSingleProxy() throws IOException {
    if (status.getProxyNodeUids().size() > 1) {
      log.info("Endpoint {} is proxied by node:{}; collapsing onto one", status.getEndpointUid(),
        status.getProxyNodeUids());
      // rladmin exits non-zero when there is nothing to do, so the outcome is checked by re-reading
      // the endpoint rather than trusted from the command.
      runRladminQuietly("bind endpoint " + status.getEndpointUid() + " policy single");
      status = RedisEnterpriseClusterStatus.discover(faultClient, bdbId, RLADMIN_CHECK_INTERVAL,
        RLADMIN_TIMEOUT);
    }

    assertEquals(1, status.getProxyNodeUids().size(),
      "The endpoint must have a single proxy before the move, otherwise which node to drain is "
          + "ambiguous. Proxies: " + status.getProxyNodeUids());
    String proxyNodeUid = status.getProxyNodeUids().get(0);

    // A single-proxy endpoint leaves the discovery service no choice but to report that node - but
    // it watches CCS and can still be a moment behind it. Waiting for the two to agree is what
    // makes the drain meaningful; without it the test could drain a node that is not the one being
    // reported, and nothing would be published.
    await().atMost(DISCOVERY_AGREEMENT_TIMEOUT).pollInterval(Duration.ofSeconds(2))
        .untilAsserted(() -> assertEquals(proxyNodeUid,
          EnterpriseSentinelSupport.trackedNodeUid(sentinel, masterName, status),
          "the discovery service must report the endpoint's only proxy"));

    return proxyNodeUid;
  }

  private boolean runRladminQuietly(String command) {
    try {
      return faultClient.executeRladminCommand(bdbId, command, RLADMIN_CHECK_INTERVAL,
        RLADMIN_TIMEOUT);
    } catch (RuntimeException | IOException e) {
      // rladmin exits non-zero on a no-op, which the fault injector reports as a failed action.
      log.warn("rladmin {} did not complete cleanly: {}", command, e.toString());
      return false;
    }
  }

  private static void sleep(Duration duration) {
    try {
      Thread.sleep(duration.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted while waiting", e);
    }
  }
}
