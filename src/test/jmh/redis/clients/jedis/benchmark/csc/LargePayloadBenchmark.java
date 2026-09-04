package redis.clients.jedis.benchmark.csc;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * 99% GET / 1% SET, 1K-key working set, configurable value size {@code 1KB / 10KB / 100KB}.
 * <p>
 * Mirrors the {@code LargePayload} workload from {@code CSC go-redis.md}. The interesting signal
 * here is memory pressure, not raw throughput — the larger the payload, the more allocations and GC
 * pressure the deserialization path on every cache hit (see {@code CacheEntry.toObject}) produces.
 * Capture RSS externally during the run; JMH won't report it.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Threads(16)
public class LargePayloadBenchmark extends Workload {

  @Param({ "1024", "10240", "102400" })
  public int payloadBytes;

  @Override
  protected int workingSet() {
    return 1_000;
  }

  @Override
  protected int valueBytes() {
    return payloadBytes;
  }

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
