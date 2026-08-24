package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.redis.test.fi.FaultInjectorClient;
import com.redis.test.fi.Scenario;
import com.redis.test.fi.StandaloneTriggerCatalog;

import org.junit.jupiter.api.AfterEach;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;

/**
 * Shared environment for maintenance-notification scenario tests. The FI is the single source of
 * truth: each test creates its own database from a discovery-provided dbconfig, builds the client
 * from the create output, and deletes the database on teardown.
 */
abstract class MaintNotificationsScenarioBase {

  /** Base socket timeout kept low so relaxed/restored timeouts are directly observable. */
  static final int CLIENT_SOCKET_TIMEOUT_MS = 1_000;
  /** Deliberately different from the non-blocking base, so probes prove which timeout applied. */
  static final int CLIENT_BLOCKING_SOCKET_TIMEOUT_MS = 2_000;
  static final int RELAXED_TIMEOUT_MS = 30_000;
  static final long EFFECT_JOIN_TIMEOUT_MS = 200_000;

  /** One FI client for parameter collection and test execution. */
  static final FaultInjectorClient faultInjector = new FaultInjectorClient();

  RedisClient client;
  Connection pinned;
  long bdbId = -1;
  final List<Thread> effectThreads = new ArrayList<>();
  final List<Throwable> effectFailures = Collections.synchronizedList(new ArrayList<>());

  /**
   * The full topology-change-standalone catalog, resolved once: effect -> triggers -> requirements.
   */
  static final StandaloneTriggerCatalog CATALOG = StandaloneTriggerCatalog.resolve(faultInjector);

  /**
   * Creates the scenario's test database and builds the client from the create output. The dbconfig
   * comes from a fresh discovery call: the FI generates a unique name and port per call, so every
   * database gets its own endpoint DNS name.
   */
  void setUpDatabaseAndClient(Scenario scenario) {
    Map<String, Object> dbConfig = faultInjector.getStandaloneTriggers(scenario.effect())
        .trigger(scenario.trigger().name()).requirement(scenario.requirement().config()).dbConfig();
    Map<String, Object> output = faultInjector.createDatabase(dbConfig);
    bdbId = ((Number) output.get("bdb_id")).longValue();
    awaitEndpointConnectable(URI.create((String) ((List<?>) output.get("endpoints")).get(0)));
    client = buildClient(output);
  }

  /**
   * A fresh endpoint's DNS record may not have propagated yet (or a negative lookup may be cached)
   * — wait until the endpoint accepts connections before building the client.
   */
  private static void awaitEndpointConnectable(URI endpoint) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
    IOException last = null;
    while (System.nanoTime() < deadline) {
      try (Socket socket = new Socket()) {
        socket.connect(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()), 2_000);
        return;
      } catch (IOException e) {
        last = e;
        sleepQuietly(1_000);
      }
    }
    throw new IllegalStateException("Endpoint " + endpoint + " not connectable within 60 s", last);
  }

  private static RedisClient buildClient(Map<String, Object> output) {
    URI endpoint = URI.create((String) ((List<?>) output.get("endpoints")).get(0));

    DefaultJedisClientConfig.Builder config = DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3) // push notifications require RESP3
        .socketTimeoutMillis(CLIENT_SOCKET_TIMEOUT_MS)
        // finite (default 0 = infinite would hang blocking probes) and distinct from the
        // non-blocking base, so a probe's failure time identifies the timeout that fired
        .blockingSocketTimeoutMillis(CLIENT_BLOCKING_SOCKET_TIMEOUT_MS)
        .ssl("rediss".equals(endpoint.getScheme())).password((String) output.get("password"));
    String username = (String) output.get("username");
    if (username != null && !"default".equals(username)) {
      config.user(username);
    }

    // one pinned observer + two parallel probe slots
    GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(3);

    MaintenanceNotificationsConfig.Builder maintenance = MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.ENABLED).relaxedTimeout(RELAXED_TIMEOUT_MS)
        .relaxedBlockingTimeout(RELAXED_TIMEOUT_MS);

    // Standalone RedisClient runs commands without retries, so timeouts stay observable.
    return RedisClient.builder().hostAndPort(endpoint.getHost(), endpoint.getPort())
        .clientConfig(config.build()).poolConfig(poolConfig)
        .maintenanceNotifications(maintenance.build()).build();
  }

  /**
   * Borrows (and keeps) a pool connection of the tested client, so maintenance pushes arrive on a
   * connection the test controls — the handshake enables CLIENT MAINT_NOTIFICATIONS before the
   * effect fires.
   */
  Connection pinConnection() {
    return client.getPool().getResource();
  }

  /**
   * Returns the pinned connection to the pool. Releasing is never an error, also mid- or
   * post-rebind: a broken return destroys the connection and the pool creates the replacement
   * toward the MOVING target (window open) or the configured endpoint (window expired).
   */
  void releasePinned() {
    if (pinned != null) {
      pinned.close();
      pinned = null;
    }
  }

  /**
   * Observes the pinned connection via a PING round-trip: buffered pushes are consumed inline
   * during the reply read, dispatching maintenance events on this thread. A command round-trip is
   * the only reliable way to read pushes — the commandless push read is a no-op on plain
   * connections and cannot see TLS-buffered pushes (SSLSocket available() stays 0).
   */
  void observe() {
    pinned.executeCommand(Protocol.Command.PING);
  }

  /** Runs the effect on a background thread so it is in flight while the test body observes. */
  void startEffect(Scenario scenario) {
    long id = bdbId;
    Thread thread = new Thread(() -> {
      try {
        faultInjector.triggerEffect(id, scenario.effect(), scenario.trigger().name());
      } catch (Throwable e) {
        effectFailures.add(e);
      }
    }, "fi-effect-" + scenario.trigger().name());
    thread.start();
    effectThreads.add(thread);
  }

  boolean effectDone() {
    for (Thread thread : effectThreads) {
      if (thread.isAlive()) {
        return false;
      }
    }
    return true;
  }

  void joinEffectThreads() {
    for (Thread thread : effectThreads) {
      try {
        thread.join(EFFECT_JOIN_TIMEOUT_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
      if (thread.isAlive()) {
        thread.interrupt();
      }
    }
  }

  void assertEffectSucceeded() {
    assertTrue(effectFailures.isEmpty(), "fault-injector effect failed: " + effectFailures);
  }

  @AfterEach
  void tearDownScenario() {
    try {
      // close the pool before releasing a still-pinned connection: a closed pool destroys the
      // returned connection without creating a replacement, which on aborted runs could target
      // a rebind endpoint that is unroutable from this host
      if (client != null) {
        client.close();
        client = null;
      }
      releasePinned();
      joinEffectThreads();
    } finally {
      effectThreads.clear();
      effectFailures.clear();
      if (bdbId >= 0) {
        faultInjector.deleteDatabase(bdbId);
        bdbId = -1;
      }
    }
  }

  static long elapsedMillis(long startNanos) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
  }

  static void sleepQuietly(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
