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
 * 99% GET / 1% SET, Normal {@code N(n/2, n/6)} clamped — contiguous middle-band hot zone, the sort
 * of access pattern bucketed time-series or hashed shards exhibit.
 * <p>
 * Mirrors the {@code NormalDistribution} workload from {@code CSC go-redis.md}.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(50)
public class NormalDistributionBenchmark extends Workload {

  private final KeyDistribution.IndexSampler sampler = KeyDistribution.normal();

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
