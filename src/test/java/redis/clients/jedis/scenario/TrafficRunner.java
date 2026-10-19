package redis.clients.jedis.scenario;

import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntFunction;

import redis.clients.jedis.UnifiedJedis;

/**
 * Fixed pool of workers issuing individual SET/GET commands on per-worker keys through any
 * {@link UnifiedJedis} (standalone or cluster). Tracks submitted vs processed commands (so a test
 * can confirm none were left in-flight) and every unique error signature (type: message) seen, for
 * post-run troubleshooting.
 */
final class TrafficRunner {

  private static final long BASELINE_TIMEOUT_SECONDS = 30;
  private static final long POLL_INTERVAL_MS = 50;
  private static final long GRACEFUL_STOP_TIMEOUT_SECONDS = 30;
  private static final long AWAIT_PROCESSED_TIMEOUT_SECONDS = 10;

  private final UnifiedJedis client;
  private final int workers;
  private final IntFunction<List<String>> keysForWorker;
  private final ExecutorService pool;
  private final AtomicLong submitted = new AtomicLong();
  private final AtomicLong processed = new AtomicLong();
  private final AtomicLong errors = new AtomicLong();
  private final AtomicReference<Throwable> firstError = new AtomicReference<>();
  // unique error signature (type: message) -> occurrence count, for troubleshooting
  private final Map<String, AtomicLong> uniqueErrors = new ConcurrentHashMap<>();
  private volatile boolean running = true;

  /** One key per worker, {@code traffic:<worker>}. */
  TrafficRunner(UnifiedJedis client, int workers) {
    this(client, workers, worker -> Collections.singletonList("traffic:" + worker));
  }

  /**
   * Each worker cycles through the keys {@code keysForWorker} gives it; on a cluster, one key per
   * slot range makes every shard see traffic across the effect.
   */
  TrafficRunner(UnifiedJedis client, int workers, IntFunction<List<String>> keysForWorker) {
    this.client = client;
    this.workers = workers;
    this.keysForWorker = keysForWorker;
    this.pool = Executors.newFixedThreadPool(workers);
  }

  void start() {
    for (int i = 0; i < workers; i++) {
      final List<String> keys = keysForWorker.apply(i);
      pool.submit(() -> {
        long n = 0;
        while (running) {
          for (String key : keys) {
            if (!running) {
              break;
            }
            final long value = n++;
            runCommand(() -> client.set(key, Long.toString(value)));
            runCommand(() -> client.get(key));
          }
        }
      });
    }
  }

  /** Runs one command, counting it as submitted then processed; records any error. */
  private void runCommand(Runnable command) {
    submitted.incrementAndGet();
    try {
      command.run();
    } catch (RuntimeException e) {
      errors.incrementAndGet();
      firstError.compareAndSet(null, e);
      uniqueErrors.computeIfAbsent(errorSignature(e), k -> new AtomicLong()).incrementAndGet();
    } finally {
      processed.incrementAndGet();
    }
  }

  /** Waits until at least {@code target} commands have been submitted (baseline warm-up). */
  void awaitSubmitted(long target) {
    await().atMost(BASELINE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
        .until(() -> submitted.get() >= target || errors.get() > 0);
  }

  /** Stops submitting new commands and waits up to the grace timeout for workers to finish. */
  void stop() {
    running = false;
    pool.shutdown();
    try {
      if (!pool.awaitTermination(GRACEFUL_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        pool.shutdownNow();
      }
    } catch (InterruptedException e) {
      pool.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  /** Confirms every submitted command reached a terminal state (submitted == processed). */
  void awaitAllProcessed() {
    await().atMost(AWAIT_PROCESSED_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
        .until(() -> submitted.get() == processed.get());
  }

  /** Every key the workers write. */
  List<String> keys() {
    List<String> all = new ArrayList<>();
    for (int i = 0; i < workers; i++) {
      all.addAll(keysForWorker.apply(i));
    }
    return all;
  }

  long submittedCount() {
    return submitted.get();
  }

  long errorCount() {
    return errors.get();
  }

  Throwable firstError() {
    return firstError.get();
  }

  String summary() {
    return errors.get() + " errors over " + processed.get() + " processed / " + submitted.get()
        + " submitted commands" + (uniqueErrors.isEmpty() ? "" : "; unique types: " + uniqueErrors);
  }

  private static String errorSignature(Throwable e) {
    return e.getClass().getSimpleName() + ": " + e.getMessage();
  }
}
