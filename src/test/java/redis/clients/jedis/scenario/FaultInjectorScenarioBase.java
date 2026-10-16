package redis.clients.jedis.scenario;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.redis.test.fi.Database;
import com.redis.test.fi.FaultInjectorClient;
import com.redis.test.fi.Scenario;

import org.junit.jupiter.api.AfterEach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.SslOptions;
import redis.clients.jedis.util.TlsUtil;

/**
 * Shared environment for scenario tests driven by the fault injector: each test provisions its
 * own database from a discovery-provided requirement and releases it on teardown, pins TLS trust
 * to that database's server chain when it is a TLS one, and runs the effect on a background thread
 * so the test body can observe while it is in flight. Holds no client — a subclass builds the
 * client it tests and closes it in its own {@code @AfterEach}, which JUnit runs before this class
 * joins the effect and releases the database.
 */
abstract class FaultInjectorScenarioBase {

  private static final Logger logger = LoggerFactory.getLogger(FaultInjectorScenarioBase.class);

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

  /**
   * One FI client for parameter collection and test execution. Each client-family base resolves
   * its own trigger catalog from it, so a class pays discovery only for the family it tests.
   */
  static final FaultInjectorClient faultInjector = new FaultInjectorClient();

  static final String TRUSTSTORE_PASSWORD = "changeit";
  /** Grace past the FI's own effect budget before an effect thread is interrupted. */
  private static final long EFFECT_JOIN_GRACE_MS = 20_000;

  Database database;
  private DnsDiagnostics dnsDiag;
  private Path serverTruststore;

  final List<Thread> effectThreads = new ArrayList<>();
  final List<Throwable> effectFailures = Collections.synchronizedList(new ArrayList<>());
  /** The FI action id of the most recently started effect; null before the first one. */
  volatile String lastEffectActionId;
  private long effectJoinTimeoutMillis = TimeUnit.SECONDS.toMillis(200);

  /**
   * Provisions the scenario's test database and waits for its endpoint. The dbconfig comes from a
   * fresh discovery call: the FI generates a unique name and port per call, so every database gets
   * its own endpoint DNS name. Teardown releases it.
   */
  Database deployDatabase(Scenario scenario) {
    Scenario fresh = faultInjector.discover(scenario.effect()).trigger(scenario.trigger().name())
        .scenario(scenario.requirement().config());
    database = faultInjector.provision(fresh);
    awaitEndpointConnectable(database.endpoint());
    dnsDiag = DnsDiagnostics.follow(database.host());
    return database;
  }

  /**
   * A fresh endpoint's DNS record may not have propagated yet (or a negative lookup may be cached)
   * — wait until the endpoint accepts connections before building the client.
   */
  static void awaitEndpointConnectable(URI endpoint) {
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

  // --- TLS trust ---

  /**
   * Client TLS options for a database whose CA is not available to the test, or null for a
   * plaintext database. Bootstraps trust by downloading the certificate chain the server presents
   * and saving a truststore containing only that chain, so the client trusts exactly this server
   * and nothing else; every other {@link SslOptions} default is kept, so the client performs full
   * chain and hostname verification. The store is a temp file deleted on teardown.
   */
  SslOptions pinnedSslOptions(Database database) {
    if (!database.tls()) {
      return null;
    }
    URI endpoint = database.endpoint();
    try {
      X509Certificate[] chain = TlsUtil.captureServerChain(endpoint.getHost(), endpoint.getPort());
      logger.info("Pinning server chain of {}: subject={}, SANs={}", endpoint,
        chain[0].getSubjectX500Principal(), chain[0].getSubjectAlternativeNames());
      serverTruststore = TlsUtil.createAndSaveTruststore(chain,
        TlsUtil.tempTruststorePath("sch-pinned"), TRUSTSTORE_PASSWORD);
    } catch (Exception e) {
      throw new IllegalStateException("TLS trust bootstrap failed for " + endpoint, e);
    }
    return SslOptions.builder()
        .truststore(serverTruststore.toFile(), TRUSTSTORE_PASSWORD.toCharArray()).build();
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

  // --- effects ---

  /** Runs the effect on a background thread so it is in flight while the test body observes. */
  void startEffect(Scenario scenario) {
    startEffect(scenario, Collections.emptyMap());
  }

  /**
   * As {@link #startEffect(Scenario)}, with extra FI query parameters pinning what it would
   * otherwise auto-select (slot-migrate: {@code source_node}, {@code target_node}, ...).
   */
  void startEffect(Scenario scenario, Map<String, Object> extra) {
    Database target = database;
    long budget = faultInjector.effectTimeout(scenario).toMillis() + EFFECT_JOIN_GRACE_MS;
    effectJoinTimeoutMillis = Math.max(effectJoinTimeoutMillis, budget);
    Thread thread = new Thread(() -> {
      try {
        String actionId = faultInjector.startEffect(target, scenario, extra);
        lastEffectActionId = actionId;
        faultInjector.awaitAction(actionId, faultInjector.effectTimeout(scenario));
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
        thread.join(effectJoinTimeoutMillis);
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

  // --- teardown ---

  /**
   * Runs after every subclass {@code @AfterEach} closed its clients: an in-flight effect is joined
   * so the database is never released under it, then the pinned truststore and the database go.
   */
  @AfterEach
  final void tearDownDatabase() {
    try {
      joinEffectThreads();
    } finally {
      try {
        if (dnsDiag != null) {
          dnsDiag.close();
          dnsDiag = null;
        }
      } finally {
        try {
          deleteServerTruststore();
        } finally {
          database = releaseIfPresent(database);
          effectThreads.clear();
          effectFailures.clear();
          lastEffectActionId = null;
        }
      }
    }
  }

  /** Releases the database if one was deployed; returns null so the field is cleared in one step. */
  static Database releaseIfPresent(Database database) {
    if (database != null) {
      faultInjector.release(database);
    }
    return null;
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
