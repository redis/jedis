package redis.clients.jedis.scenario;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.jupiter.api.AfterEach;

import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.MaintenanceNotificationsConfig;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.RedisProtocol;

/**
 * Toolkit for tests that observe maintenance notifications on a standalone client: a client tuned
 * so relaxed and restored timeouts are observable, a pinned connection that reads the pushes, and
 * pool warm-up. The effect runs on a background thread (see {@link FaultInjectorScenarioBase})
 * while the test body observes.
 */
abstract class MaintNotificationsObserverBase extends MaintNotificationsScenarioBase {

  private static final Logger logger = LoggerFactory
      .getLogger(MaintNotificationsObserverBase.class);

  /** Base socket timeout kept low so relaxed/restored timeouts are directly observable. */
  static final int CLIENT_SOCKET_TIMEOUT_MS = 1_000;
  /** Deliberately different from the non-blocking base, so probes prove which timeout applied. */
  static final int CLIENT_BLOCKING_SOCKET_TIMEOUT_MS = 2_000;
  static final int RELAXED_TIMEOUT_MS = 30_000;

  Connection pinned;

  /**
   * Builder with the observer tests' common settings: notifications on, both relaxed timeouts far
   * above the base ones. The caller chooses the endpoint-type resolution and builds.
   */
  static MaintenanceNotificationsConfig.Builder relaxedMaintenance() {
    return MaintenanceNotificationsConfig.builder()
        .mode(MaintenanceNotificationsConfig.Mode.ENABLED).relaxedTimeout(RELAXED_TIMEOUT_MS)
        .relaxedBlockingTimeout(RELAXED_TIMEOUT_MS);
  }

  /**
   * Push notifications require RESP3. Base socket timeouts stay low so relaxed/restored timeouts
   * remain observable; the blocking one is finite (default 0 = infinite would hang blocking probes)
   * and distinct from the non-blocking base, so a probe's failure time identifies the timeout that
   * fired.
   */
  static void observerClientConfig(DefaultJedisClientConfig.Builder config) {
    config.protocol(RedisProtocol.RESP3).socketTimeoutMillis(CLIENT_SOCKET_TIMEOUT_MS)
        .blockingSocketTimeoutMillis(CLIENT_BLOCKING_SOCKET_TIMEOUT_MS);
  }

  @Override
  void customiseClientConfig(DefaultJedisClientConfig.Builder config) {
    observerClientConfig(config);
  }

  /** One pinned observer + two parallel probe slots. */
  @Override
  void customiseClientPoolConfig(GenericObjectPoolConfig<Connection> poolConfig) {
    poolConfig.setMaxTotal(3);
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

  /**
   * Runs before the client is closed by the layer below. A still-pinned connection is released only
   * after the pool is closed there: a closed pool destroys the returned connection without creating
   * a replacement, which on aborted runs could target a rebind endpoint that is unroutable from
   * this host — so the pool is closed here first, and the superclass close is then a no-op.
   */
  @AfterEach
  final void tearDownObserver() {
    client = closeIfOpen(client);
    releasePinned();
  }
}
