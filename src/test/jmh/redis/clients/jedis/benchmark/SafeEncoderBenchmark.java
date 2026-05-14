package redis.clients.jedis.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import redis.clients.jedis.util.SafeEncoder;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for {@link SafeEncoder} UTF-8 encoding/decoding.
 * <p>
 * Tests the performance of converting between {@code String} and {@code byte[]} using UTF-8
 * encoding. This is a critical operation in Redis clients as all Redis protocol data is transmitted
 * as bytes.
 * <p>
 * Benchmarks:
 * <ul>
 * <li>{@code String} &rarr; {@code byte[]} encoding
 * <li>{@code byte[]} &rarr; {@code String} decoding
 * </ul>
 * <p>
 * Uses batching (100 ops per invocation) to reduce measurement overhead for these fast operations.
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

  /**
   * Test strings covering various real-world Redis use cases.
   */
  private static final String[] TEST_STRINGS = { "OK", // Simple response (2 chars)
      "foo bar!", // Original benchmark string (8 chars)
      "user:12345:session:token", // Typical Redis key (25 chars)
      "GET", // Command (3 chars)
      "{user}:1000:profile:metadata", // Hash tag key (29 chars)
      "The quick brown fox jumps over the lazy dog" // Longer text (44 chars)
  };

  /**
   * Pre-encoded byte arrays for testing byte[] → String decoding.
   */
  private byte[][] testBytes;

  /**
   * Counter for rotating through test set.
   */
  private long counter = 0;

  /**
   * Batch size for each benchmark invocation. Multiple of TEST_STRINGS.length (6) to ensure each
   * batch tests all patterns equally.
   */
  private static final int BATCH_SIZE = 120; // 6 patterns × 20 = 120

  @Setup
  public void setup() {
    // Pre-encode all test strings to bytes
    testBytes = new byte[TEST_STRINGS.length][];
    for (int i = 0; i < TEST_STRINGS.length; i++) {
      testBytes[i] = SafeEncoder.encode(TEST_STRINGS[i]);
    }
  }

  // ========== STRING → BYTE[] ENCODING ==========

  /**
   * Benchmark String → byte[] encoding (UTF-8). This operation is used when sending commands and
   * data to Redis.
   */
  @Benchmark
  @OperationsPerInvocation(BATCH_SIZE)
  public void encodeStringToBytes(Blackhole blackhole) {
    for (int i = 0; i < BATCH_SIZE; i++) {
      byte[] encoded = SafeEncoder.encode(TEST_STRINGS[counter++ % TEST_STRINGS.length]);
      blackhole.consume(encoded);
    }
  }

  // ========== BYTE[] → STRING DECODING ==========

  /**
   * Benchmark byte[] → String decoding (UTF-8). This operation is used when receiving responses
   * from Redis.
   */
  @Benchmark
  @OperationsPerInvocation(BATCH_SIZE)
  public void decodeBytesToString(Blackhole blackhole) {
    for (int i = 0; i < BATCH_SIZE; i++) {
      String decoded = SafeEncoder.encode(testBytes[counter++ % testBytes.length]);
      blackhole.consume(decoded);
    }
  }

  // ========== COMPARATIVE: ENCODE MANY ==========

  /**
   * Benchmark encodeMany() - converting multiple strings at once. This is used in some bulk
   * operations.
   */
  @Benchmark
  public void encodeMany(Blackhole blackhole) {
    byte[][] encoded = SafeEncoder.encodeMany(TEST_STRINGS);
    blackhole.consume(encoded);
  }
}
