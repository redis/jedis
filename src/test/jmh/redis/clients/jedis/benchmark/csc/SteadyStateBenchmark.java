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
 * 80% GET / 20% SET, uniform 10K-key working set, single 30-second measurement iteration.
 * <p>
 * Mirrors the {@code SteadyState} workload from {@code CSC go-redis.md} — long-run RSS stability
 * and sustained tail latency. The single long iteration surfaces any slow leaks (waitCh
 * accumulation, push-buffer growth, eviction-queue churn) that short JMH iterations would miss.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
@Threads(20)
public class SteadyStateBenchmark extends Workload {

  @Override
  protected int readPct() {
    return 80;
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
