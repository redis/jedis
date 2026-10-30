package redis.clients.jedis.benchmark.csc;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * 99% GET / 1% SET, Zipfian {@code s=1.1} distribution — the classic "hot-key" workload.
 * <p>
 * Mirrors the {@code HotKeyZipfian} workload from {@code CSC go-redis.md}, which is where the
 * canonical Phase 1 CSC implementation collapsed (25 ops/s in the go-redis run). The same hot-key
 * single-flight pile-up is the primary risk for Jedis CSC — every thread races for the same handful
 * of keys, which serializes on the cache lock and floods the owner connection with invalidation
 * traffic.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(100)
public class HotKeyZipfianBenchmark extends Workload {

  private final KeyDistribution.IndexSampler sampler = KeyDistribution.zipf(1.1);

  @Override
  protected int readPct() {
    return 99;
  }

  @Override
  protected KeyDistribution.IndexSampler sampler() {
    return sampler;
  }

  @Benchmark
  public Object workload(Rng rng) {
    return doOnce(rng);
  }
}
