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
 * 99% GET / 1% SET, uniform distribution over a 10K-key working set.
 * <p>
 * Mirrors the {@code ReadHeavy99} workload from {@code CSC go-redis.md}. Measures the best-case
 * hit-rate ceiling and pure-read throughput of the CSC stack vs the no-CSC baseline.
 * <p>
 * Run: {@code mvn -Pjmh test -Djmh.includes="csc.ReadHeavy99Benchmark"}
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(50)
public class ReadHeavy99Benchmark extends Workload {

  @Override
  protected int readPct() {
    return 99;
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
