package redis.clients.jedis.scenario;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.redis.test.fi.Database;
import com.redis.test.fi.FaultInjectorClient;
import com.redis.test.fi.Scenario;
import com.redis.test.fi.StandaloneTriggerCatalog;

import org.junit.jupiter.api.AfterEach;

/**
 * Shared environment for scenario tests driven by the fault injector: each test creates its own
 * database from a discovery-provided dbconfig and deletes it on teardown. Holds no client — a
 * subclass builds the client it tests and closes it in its own {@code @AfterEach}, which JUnit runs
 * before this class deletes the database.
 */
abstract class FaultInjectorScenarioBase {

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

  /** One FI client for parameter collection and test execution. */
  static final FaultInjectorClient faultInjector = new FaultInjectorClient();

  /**
   * The full topology-change-standalone catalog, resolved once: effect -> triggers -> requirements.
   */
  static final StandaloneTriggerCatalog CATALOG = StandaloneTriggerCatalog.resolve(faultInjector);

  Database database;
  private DnsDiagnostics dnsDiag;

  /**
   * Creates the scenario's test database and waits for its endpoint. The dbconfig comes from a
   * fresh discovery call: the FI generates a unique name and port per call, so every database gets
   * its own endpoint DNS name. Teardown deletes it.
   */
  Database deployDatabase(Scenario scenario) {
    Map<String, Object> dbConfig = faultInjector.getStandaloneTriggers(scenario.effect())
        .trigger(scenario.trigger().name()).requirement(scenario.requirement().config()).dbConfig();
    database = faultInjector.createDatabase(dbConfig);
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

  @AfterEach
  final void tearDownDatabase() {
    try {
      if (dnsDiag != null) {
        dnsDiag.close();
        dnsDiag = null;
      }
    } finally {
      database = deleteIfPresent(database);
    }
  }

  /** Deletes the database if one was deployed; returns null so the field is cleared in one step. */
  static Database deleteIfPresent(Database database) {
    if (database != null) {
      faultInjector.deleteDatabase(database.id());
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
