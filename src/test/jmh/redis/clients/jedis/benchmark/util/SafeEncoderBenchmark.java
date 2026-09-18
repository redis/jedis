package redis.clients.jedis.benchmark.util;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import redis.clients.jedis.util.SafeEncoder;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for {@link SafeEncoder} UTF-8 encoding/decoding of the fixed string "foo bar!".
 * <p>
 * Run with: {@code mvn -Pjmh clean test}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Threads(1)
public class SafeEncoderBenchmark {

  private static final int BATCH = 100;

  private static final String TEST_STRING = "foo bar!";
  private byte[] testBytes;

  @Setup
  public void setup() {
    testBytes = SafeEncoder.encode(TEST_STRING);
  }

  /** String → byte[] (UTF-8). Used when sending commands/data to Redis. */
  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void encodeStringToBytes(Blackhole blackhole) {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(SafeEncoder.encode(TEST_STRING));
    }
  }

  /** byte[] → String (UTF-8). Used when receiving responses from Redis. */
  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void decodeBytesToString(Blackhole blackhole) {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(SafeEncoder.encode(testBytes));
    }
  }
}
