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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redis.test.fi.FaultInjectorClient;
import com.redis.test.fi.Scenario;
import com.redis.test.fi.StandaloneTriggerCatalog;

import org.junit.jupiter.api.AfterEach;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig.EndpointType;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.SslOptions;
import redis.clients.jedis.util.TlsUtil;

/**
 * Shared environment for maintenance-notification scenario tests. The FI is the single source of
 * truth: each test creates its own database from a discovery-provided dbconfig, builds the client
 * from the create output, and deletes the database on teardown.
 */
abstract class MaintNotificationsScenarioBase {

  private static final Logger logger = LoggerFactory
      .getLogger(MaintNotificationsScenarioBase.class);

  // Short JVM DNS cache: these tests reconnect across endpoint rebinds, where the default 30s
  // positive cache can outlive the DNS repoint, and await fresh database names, where the
  // default 10s negative cache delays convergence. The JVM reads these once, at its first name
  // lookup — this block must run before that (it does under the surefire fork; an IDE may
  // resolve earlier, in which case pass -Dsun.net.inetaddr.ttl=2 and
  // -Dsun.net.inetaddr.negative.ttl=2 in the run configuration).
  static {
    java.security.Security.setProperty("networkaddress.cache.ttl", "2");
    java.security.Security.setProperty("networkaddress.cache.negative.ttl", "2");
  }

  /** Base socket timeout kept low so relaxed/restored timeouts are directly observable. */
  static final int CLIENT_SOCKET_TIMEOUT_MS = 1_000;
  /** Deliberately different from the non-blocking base, so probes prove which timeout applied. */
  static final int CLIENT_BLOCKING_SOCKET_TIMEOUT_MS = 2_000;
  static final int RELAXED_TIMEOUT_MS = 30_000;
  static final long EFFECT_JOIN_TIMEOUT_MS = 200_000;
  static final String TRUSTSTORE_PASSWORD = "changeit";

  /** One FI client for parameter collection and test execution. */
  static final FaultInjectorClient faultInjector = new FaultInjectorClient();

  RedisClient client;
  Connection pinned;
  Path serverTruststore;
  URI endpoint;
  private DnsDiagnostics dnsDiag;
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
    setUpDatabaseAndClient(scenario, null);
  }

  /** As {@link #setUpDatabaseAndClient(Scenario)}, requesting a fixed MOVING endpoint type. */
  void setUpDatabaseAndClient(Scenario scenario, EndpointType endpointType) {
    Map<String, Object> dbConfig = faultInjector.getStandaloneTriggers(scenario.effect())
        .trigger(scenario.trigger().name()).requirement(scenario.requirement().config()).dbConfig();
    Map<String, Object> output = faultInjector.createDatabase(dbConfig);
    bdbId = ((Number) output.get("bdb_id")).longValue();
    endpoint = URI.create((String) ((List<?>) output.get("endpoints")).get(0));
    awaitEndpointConnectable(endpoint);
    dnsDiag = DnsDiagnostics.follow(endpoint.getHost());
    SslOptions sslOptions = null;
    if (Boolean.TRUE.equals(output.get("tls"))) {
      serverTruststore = createServerTruststore(endpoint);
      sslOptions = sslOptionsFor(serverTruststore);
    }
    client = buildClient(output, endpointType, sslOptions);
  }

  /**
   * Bootstraps client trust for an endpoint whose CA is not available to the test: downloads the
   * certificate chain the server presents and saves a truststore containing only that chain, so the
   * client trusts exactly this server and nothing else. The store is a temp file under the test
   * work folder, deleted when the test completes.
   */
  private static Path createServerTruststore(URI endpoint) {
    try {
      X509Certificate[] chain = TlsUtil.captureServerChain(endpoint.getHost(), endpoint.getPort());
      logger.info("Pinning server chain of {}: subject={}, SANs={}", endpoint,
        chain[0].getSubjectX500Principal(), chain[0].getSubjectAlternativeNames());
      return TlsUtil.createAndSaveTruststore(chain, TlsUtil.tempTruststorePath("sch-pinned"),
        TRUSTSTORE_PASSWORD);
    } catch (Exception e) {
      throw new IllegalStateException("TLS trust bootstrap failed for " + endpoint, e);
    }
  }

  /**
   * All {@link SslOptions} defaults except the truststore are kept, so the client under test
   * performs full certificate-chain and hostname verification.
   */
  private static SslOptions sslOptionsFor(Path truststore) {
    return SslOptions.builder().truststore(truststore.toFile(), TRUSTSTORE_PASSWORD.toCharArray())
        .build();
  }

  private void deleteServerTruststore() {
    if (serverTruststore == null) {
      return;
    }
    try {
      Files.deleteIfExists(serverTruststore);
    } catch (IOException e) {
      logger.warn("Failed to delete truststore {}", serverTruststore, e);
    }
    serverTruststore = null;
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

  private static RedisClient buildClient(Map<String, Object> output, EndpointType endpointType,
      SslOptions sslOptions) {
    URI endpoint = URI.create((String) ((List<?>) output.get("endpoints")).get(0));

    DefaultJedisClientConfig.Builder config = DefaultJedisClientConfig.builder()
        .protocol(RedisProtocol.RESP3) // push notifications require RESP3
        .socketTimeoutMillis(CLIENT_SOCKET_TIMEOUT_MS)
        // finite (default 0 = infinite would hang blocking probes) and distinct from the
        // non-blocking base, so a probe's failure time identifies the timeout that fired
        .blockingSocketTimeoutMillis(CLIENT_BLOCKING_SOCKET_TIMEOUT_MS)
        .ssl(Boolean.TRUE.equals(output.get("tls"))).password((String) output.get("password"));
    if (sslOptions != null) {
      config.sslOptions(sslOptions);
    }
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
    if (endpointType != null) {
      maintenance.endpointType(endpointType); // null: auto-resolve from connection characteristics
    }

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
      logger.trace("releasing pinned: {}", pinned.toIdentityString());
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

  /**
   * Creates every pool connection up front — holding maxTotal borrows simultaneously forces
   * distinct creations, so tests measure command time, not handshakes.
   */
  void warmUpPool() {
    int size = client.getPool().getMaxTotal();
    List<Connection> borrowed = new ArrayList<>(size);
    for (int i = 0; i < size; i++) {
      borrowed.add(client.getPool().getResource());
    }
    borrowed.forEach(Connection::close);
  }

  /**
   * Observes the pinned connection until {@code future} completes, then returns its value — the
   * test thread keeps reading pushes (events, timeout changes stay current) while it waits.
   */
  <T> T observeAndAwait(CompletableFuture<T> future) {
    while (!future.isDone()) {
      observe();
      sleepQuietly(100);
    }
    return future.join();
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
      if (dnsDiag != null) {
        dnsDiag.close();
        dnsDiag = null;
      }
      deleteServerTruststore();
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
