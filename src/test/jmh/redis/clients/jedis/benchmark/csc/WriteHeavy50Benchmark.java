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
 * 50% GET / 50% SET, uniform distribution over a 10K-key working set.
 * <p>
 * Mirrors the {@code WriteHeavy50} workload from {@code CSC go-redis.md}. SETs trigger continuous
 * invalidations that hammer the cache lock — the test isolates write-side contention from read-side
 * hit-rate gains.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(50)
public class WriteHeavy50Benchmark extends Workload {

  @Override
  protected int readPct() {
    return 50;
  }

  @Override
  protected KeyDistribution.IndexSampler sampler() {
    return KeyDistribution.UNIFORM;
  }

  @Benchmark
  public Object workload(Rng rng) {
    return doOnce(rng);
  }
}
