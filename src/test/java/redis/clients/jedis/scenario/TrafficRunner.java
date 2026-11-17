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
import redis.clients.jedis.util.JedisClusterCRC16;

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
  // every failed command with its key, for per-slot classification by the test
  private final List<Failure> failures = Collections.synchronizedList(new ArrayList<>());
  private volatile boolean running = true;

  /** One failed command: the key it addressed, that key's slot and the exception as thrown. */
  static final class Failure {
    final String key;
    final int slot;
    final Throwable error;

    Failure(String key, Throwable error) {
      this.key = key;
      this.slot = JedisClusterCRC16.getSlot(key);
      this.error = error;
    }

    /** The innermost cause: the reply or socket failure behind the client's wrapper. */
    Throwable rootCause() {
      Throwable cause = error;
      while (cause.getCause() != null) {
        cause = cause.getCause();
      }
      return cause;
    }

    @Override
    public String toString() {
      return key + " (slot " + slot + "): " + errorSignature(error) + " <- "
          + errorSignature(rootCause());
    }
  }

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
            final String value = Long.toString(n++);
            // the GET is checked against the SET only when the SET was accepted: after a failed
            // SET the old value is the correct answer, not a stale read
            if (runCommand(key, () -> client.set(key, value))) {
              runCommand(key, () -> {
                String read = client.get(key);
                if (!value.equals(read)) {
                  throw new StaleReadException(key, value, read);
                }
              });
            } else {
              runCommand(key, () -> client.get(key));
            }
          }
        }
      });
    }
  }

  /**
   * Runs one command on {@code key}, counting it as submitted then processed; records any error.
   * Returns whether the command completed without one.
   */
  private boolean runCommand(String key, Runnable command) {
    submitted.incrementAndGet();
    try {
      command.run();
      return true;
    } catch (RuntimeException e) {
      errors.incrementAndGet();
      firstError.compareAndSet(null, e);
      failures.add(new Failure(key, e));
      uniqueErrors.computeIfAbsent(errorSignature(e), k -> new AtomicLong()).incrementAndGet();
      return false;
    } finally {
      processed.incrementAndGet();
    }
  }

  /**
   * A GET that did not return what the preceding SET on the same key wrote: a routing or data
   * transfer defect that surfaced as a wrong answer instead of an exception. Each key has one
   * writer, so there is no other explanation.
   */
  static final class StaleReadException extends RuntimeException {
    StaleReadException(String key, String expected, String actual) {
      super("stale read on " + key + ": expected " + expected + ", got " + actual);
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

  /** Every failed command, in the order the failures were recorded. */
  List<Failure> failures() {
    return new ArrayList<>(failures);
  }

  String summary() {
    return errors.get() + " errors over " + processed.get() + " processed / " + submitted.get()
        + " submitted commands" + (uniqueErrors.isEmpty() ? "" : "; unique types: " + uniqueErrors);
  }

  private static String errorSignature(Throwable e) {
    return e.getClass().getSimpleName() + ": " + e.getMessage();
  }
}
