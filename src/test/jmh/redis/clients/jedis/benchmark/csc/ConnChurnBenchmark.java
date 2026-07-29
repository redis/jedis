package redis.clients.jedis.benchmark.csc;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import redis.clients.jedis.Jedis;

import java.util.concurrent.TimeUnit;

/**
 * 99% GET / 1% SET, uniform 10K-key working set + a side thread dialing/closing fresh connections
 * at {@value #CHURN_DIALS_PER_SEC} per second.
 * <p>
 * Mirrors the {@code ConnChurn} workload from {@code CSC go-redis.md}. The headline finding in that
 * exploration was the 2,255× gap between Phase 1 and the BCAST design under churn pressure.
 * Connection lifecycle churn is also where Jedis CSC's per-connection tracking model is most
 * exposed: an entry whose owner connection is replaced becomes effectively stale until the next
 * push drain.
 * <p>
 * <b>Limitations:</b> the churn thread dials fresh sockets against Redis but cannot directly
 * recycle conns inside the workload client's pool — commons-pool2 doesn't expose a per-borrower
 * eviction hook here. The dial/close pressure exercises server-side accept/teardown and the auth
 * path; it does not by itself force the pool to rotate its tracked conns. Run with a short idle
 * timeout if you want pool rotation.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
@Threads(16)
public class ConnChurnBenchmark extends Workload {

  private static final int CHURN_DIALS_PER_SEC = 50;
  private volatile Thread churnThread;
  private volatile boolean churnStop;

  @Override
  protected int readPct() {
    return 99;
  }

  @Override
  protected KeyDistribution.IndexSampler sampler() {
    return KeyDistribution.UNIFORM;
  }

  @Setup(Level.Trial)
  public void setupChurn() {
    churnStop = false;
    churnThread = new Thread(this::churnLoop, "csc-churn");
    churnThread.setDaemon(true);
    churnThread.start();
  }

  @TearDown(Level.Trial)
  public void teardownChurn() throws InterruptedException {
    churnStop = true;
    if (churnThread != null) {
      churnThread.join(2_000);
    }
  }

  private void churnLoop() {
    final long perDialNanos = TimeUnit.SECONDS.toNanos(1) / CHURN_DIALS_PER_SEC;
    long next = System.nanoTime();
    while (!churnStop) {
      long now = System.nanoTime();
      if (now < next) {
        try {
          TimeUnit.NANOSECONDS.sleep(next - now);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
      next += perDialNanos;
      try (Jedis j = new Jedis(endpoint.getHostAndPort())) {
        String pwd = endpoint.getPassword();
        if (pwd != null && !pwd.isEmpty()) j.auth(pwd);
        j.ping();
      } catch (Exception ignored) {
        // dial-side churn — swallow transient errors; benchmark records throughput regardless
      }
    }
  }

  @Benchmark
  public Object workload(Rng rng) {
    return doOnce(rng);
  }
}
