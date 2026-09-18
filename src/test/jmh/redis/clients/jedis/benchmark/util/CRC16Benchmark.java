package redis.clients.jedis.benchmark.util;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import redis.clients.jedis.util.JedisClusterCRC16;
import redis.clients.jedis.util.SafeEncoder;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for CRC16 hash slot calculation.
 * <p>
 * Tests the performance of Redis Cluster hash slot computation using a realistic mix of key
 * patterns. Each benchmark invocation rotates through different key types (short, medium, long,
 * hashtag, etc.) to simulate real-world workload distribution.
 * <p>
 * Compares {@code String}-based vs {@code byte[]}-based slot calculation.
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
public class CRC16Benchmark {

  /**
   * Mixed test set covering various key patterns encountered in production:
   * <ul>
   * <li>Empty string (edge case)
   * <li>Short numeric (common)
   * <li>Medium alphanumeric (typical)
   * <li>Long alphanumeric (worst case for CRC16)
   * <li>Repeated characters (pathological case)
   * <li>Typical string value
   * <li>Hash tag keys (cluster routing)
   * </ul>
   */
  private static final String[] STRING_TEST_SET = { "", // Empty (edge case)
      "123456789", // Short numeric (9 chars)
      "sfger132515", // Medium alphanum (11 chars)
      "hae9Napahngaikeethievubaibogiech", // Long alphanum (32 chars)
      "AAAAAAAAAAAAAAAAAAAAAA", // Repeated chars (22 chars)
      "Hello, World!", // Typical string (13 chars)
      "{user}:1000:followers", // Hash tag (21 chars, hashes "{user}")
      "{customer:12345}:profile:metadata" // Hash tag (34 chars, hashes "{customer:12345}")
  };

  /**
   * Pre-encoded byte arrays for testing byte[]-based slot calculation. Avoids UTF-8 encoding
   * overhead during benchmark execution.
   */
  private byte[][] byteTestSet;

  /**
   * Counter for rotating through test set.
   */
  private long counter = 0;

  // Multiple of KEYS.length so each invocation hits every pattern equally.
  private static final int BATCH_SIZE = 128; // = 8 patterns × 16 // 128

  @Setup
  public void setup() {
    // Pre-encode all test strings to bytes
    byteTestSet = new byte[STRING_TEST_SET.length][];
    for (int i = 0; i < STRING_TEST_SET.length; i++) {
      byteTestSet[i] = SafeEncoder.encode(STRING_TEST_SET[i]);
    }
  }

  // ========== STRING-BASED SLOT CALCULATION ==========

  /**
   * Benchmark String-based slot calculation with mixed key patterns. Performs BATCH_SIZE operations
   * per invocation to reduce measurement overhead. Rotates through test set to simulate realistic
   * workload.
   */
  @Benchmark
  @OperationsPerInvocation(BATCH_SIZE)
  public void getSlotString(Blackhole blackhole) {
    for (int i = 0; i < BATCH_SIZE; i++) {
      int slot = JedisClusterCRC16
          .getSlot(STRING_TEST_SET[(int) (counter++ % STRING_TEST_SET.length)]);
      blackhole.consume(slot);
    }
  }

  // ========== BYTE[]-BASED SLOT CALCULATION ==========

  /**
   * Benchmark byte[]-based slot calculation with mixed key patterns. Performs BATCH_SIZE operations
   * per invocation to reduce measurement overhead. Tests performance when key is already available
   * as byte array. This is more efficient as it avoids UTF-8 encoding overhead.
   */
  @Benchmark
  @OperationsPerInvocation(BATCH_SIZE)
  public void getSlotBytes(Blackhole blackhole) {
    for (int i = 0; i < BATCH_SIZE; i++) {
      int slot = JedisClusterCRC16.getSlot(byteTestSet[(int) (counter++ % byteTestSet.length)]);
      blackhole.consume(slot);
    }
  }
}
