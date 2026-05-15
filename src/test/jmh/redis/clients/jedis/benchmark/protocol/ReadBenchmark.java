package redis.clients.jedis.benchmark.protocol;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.benchmark.CyclingInputStream;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheFactory;
import redis.clients.jedis.csc.DefaultCacheable;
import redis.clients.jedis.util.RedisInputStream;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for Jedis protocol read operations. Covers:
 * <ol>
 * <li>Baseline reads (no cache argument)
 * <li>Cache-aware reads (with cache argument; no push frames present)
 * <li>Mixed scenarios: regular response preceded by N push invalidations
 * </ol>
 * <p>
 * All streams are backed by a {@link CyclingInputStream} that endlessly repeats a single record
 * payload, so each {@code @Benchmark} invocation loops {@code BATCH} reads against a long-lived
 * stream — no per-invocation stream allocation.
 * <p>
 * Run with: {@code mvn -Pjmh clean test}
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Threads(1)
public class ReadBenchmark {

  private static final int BATCH = 100;

  // ========== SINGLE-FRAME RECORDS ==========

  private static final byte[] SIMPLE_STRING = "+OK\r\n".getBytes();
  private static final byte[] BULK_STRING = "$5\r\nHello\r\n".getBytes();
  private static final byte[] ARRAY = "*3\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$3\r\nbaz\r\n".getBytes();
  private static final byte[] MULTI_BULK = "*4\r\n$3\r\nfoo\r\n$13\r\nbarbarbarfooz\r\n$5\r\nHello\r\n$5\r\nWorld\r\n"
      .getBytes();

  // Built once per trial — each record contains N push frames + a final +OK response.
  private byte[] mixed1PushRecord;
  private byte[] mixed100PushRecord;

  // ========== STREAMS (long-lived across an iteration, never EOF) ==========

  private RedisInputStream simpleStringStream;
  private RedisInputStream bulkStringStream;
  private RedisInputStream arrayStream;
  private RedisInputStream multiBulkStream;

  private RedisInputStream cacheSimpleStringStream;
  private RedisInputStream cacheBulkStringStream;
  private RedisInputStream cacheArrayStream;
  private RedisInputStream cacheMultiBulkStream;

  private RedisInputStream mixed1PushStream;
  private RedisInputStream mixed100PushStream;

  private Cache cache;

  @Setup(Level.Trial)
  public void setupTrial() {
    mixed1PushRecord = ">2\r\n$10\r\ninvalidate\r\n*1\r\n$3\r\nkey\r\n+OK\r\n".getBytes();

    StringBuilder sb100 = new StringBuilder();
    for (int i = 0; i < 100; i++) {
      sb100.append(">2\r\n$10\r\ninvalidate\r\n*1\r\n$5\r\nkey").append(String.format("%02d", i))
          .append("\r\n");
    }
    sb100.append("+OK\r\n");
    mixed100PushRecord = sb100.toString().getBytes();
  }

  @Setup(Level.Iteration)
  public void setupIteration() {
    simpleStringStream = wrap(SIMPLE_STRING);
    bulkStringStream = wrap(BULK_STRING);
    arrayStream = wrap(ARRAY);
    multiBulkStream = wrap(MULTI_BULK);

    cacheSimpleStringStream = wrap(SIMPLE_STRING);
    cacheBulkStringStream = wrap(BULK_STRING);
    cacheArrayStream = wrap(ARRAY);
    cacheMultiBulkStream = wrap(MULTI_BULK);

    mixed1PushStream = wrap(mixed1PushRecord);
    mixed100PushStream = wrap(mixed100PushRecord);

    cache = CacheFactory.getCache(
      CacheConfig.builder().maxSize(10_000).cacheable(DefaultCacheable.INSTANCE).build());
  }

  private static RedisInputStream wrap(byte[] record) {
    return new RedisInputStream(new CyclingInputStream(record));
  }

  // ========== BASELINE READS (no cache) ==========

  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void readSimpleString(Blackhole blackhole) {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(Protocol.read(simpleStringStream));
    }
  }

  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void readBulkString(Blackhole blackhole) {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(Protocol.read(bulkStringStream));
    }
  }

  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void readArray(Blackhole blackhole) {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(Protocol.read(arrayStream));
    }
  }

  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void readMultiBulkResponse(Blackhole blackhole) {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(Protocol.read(multiBulkStream));
    }
  }

  // ========== CACHE-AWARE READS (push-check overhead, no pushes present) ==========

  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void cacheAwareReadSimpleString(Blackhole blackhole) {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(Protocol.read(cacheSimpleStringStream, cache));
    }
  }

  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void cacheAwareReadBulkString(Blackhole blackhole) {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(Protocol.read(cacheBulkStringStream, cache));
    }
  }

  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void cacheAwareReadArray(Blackhole blackhole) {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(Protocol.read(cacheArrayStream, cache));
    }
  }

  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void cacheAwareReadMultiBulkResponse(Blackhole blackhole) {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(Protocol.read(cacheMultiBulkStream, cache));
    }
  }

  // ========== MIXED (N pushes followed by one regular response) ==========

  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void readWith1PushMessage(Blackhole blackhole) {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(Protocol.read(mixed1PushStream, cache));
    }
  }

  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void readWith100PushMessages(Blackhole blackhole) {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(Protocol.read(mixed100PushStream, cache));
    }
  }
}
