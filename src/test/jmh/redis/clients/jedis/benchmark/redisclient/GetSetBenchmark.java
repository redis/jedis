package redis.clients.jedis.benchmark.redisclient;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import redis.clients.jedis.*;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for {@link RedisClient} (pooled connection) GET/SET operations on a live Redis
 * server. This base class is extended by nested classes with specific thread counts:
 * <ul>
 * <li>{@link Threads1}: single-threaded baseline
 * <li>{@link Threads8}: moderate concurrency
 * <li>{@link Threads64}: high concurrency
 * </ul>
 * <p>
 * Requirements:
 * <ul>
 * <li>Running Redis instance on {@code localhost:6379} (or configured endpoint)
 * <li>Redis 6.0+ recommended
 * </ul>
 * <p>
 * Run with: {@code mvn -Pjmh test -Djmh.includes="redisclient.GetSetBenchmark"}
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
public abstract class GetSetBenchmark {

  private static final EndpointConfig endpoint = Endpoints.getRedisEndpoint("standalone0");

  /**
   * Total operations per benchmark invocation.
   */
  private static final int TOTAL_OPERATIONS = 1000;

  /**
   * Batch size for pipelined operations. Number of commands to batch before calling sync().
   */
  private static final int BATCH_SIZE = 100;

  private static final String KEY = "foo";
  private static final String VALUE = "bar";

  private RedisClient redisClient;

  /**
   * Per-thread reusable buffer for pipeline responses, avoids per-invocation array allocation in
   * hot pipelined benchmarks.
   */
  @State(Scope.Thread)
  public static class PipelineState {
    @SuppressWarnings("unchecked")
    final Response<String>[] responses = (Response<String>[]) new Response[BATCH_SIZE];
  }

  @Setup(Level.Trial)
  public void setupTrial() {
    // Configure connection pool sized for max concurrency (64 threads)
    // Pool size must exceed thread count to avoid measuring pool contention
    GenericObjectPoolConfig<Connection> poolConfig = new GenericObjectPoolConfig<>();
    poolConfig.setMaxTotal(64); // >= max thread count across all subclasses
    poolConfig.setMaxIdle(64); // Keep connections alive for reuse
    poolConfig.setMinIdle(8); // Warm pool for baseline scenarios
    poolConfig.setBlockWhenExhausted(true); // Block if pool exhausted (shouldn't happen)
    poolConfig.setMaxWait(java.time.Duration.ofSeconds(5)); // Max wait time

    // Create RedisClient with properly-sized connection pool
    redisClient = RedisClient.builder().hostAndPort(endpoint.getHost(), endpoint.getPort())
        .clientConfig(DefaultJedisClientConfig.builder().password(endpoint.getPassword()).build())
        .poolConfig(poolConfig) // Critical: avoid pool contention
        .build();

    // Clean database and pre-populate KEY so GET benchmarks hit a real value
    try (Jedis jedis = new Jedis(endpoint.getHostAndPort())) {
      if (endpoint.getPassword() != null && !endpoint.getPassword().isEmpty()) {
        jedis.auth(endpoint.getPassword());
      }
      jedis.flushDB();
      jedis.set(KEY, VALUE);
    }
  }

  @TearDown(Level.Trial)
  public void teardownTrial() {
    if (redisClient != null) {
      redisClient.close();
    }
  }

  /**
   * Benchmark SET operation using RedisClient.
   */
  @Benchmark
  public void set(Blackhole blackhole) {
    String result = redisClient.set(KEY, VALUE);
    blackhole.consume(result);
  }

  /**
   * Benchmark GET operation using RedisClient.
   */
  @Benchmark
  public void get(Blackhole blackhole) {
    String result = redisClient.get(KEY);
    blackhole.consume(result);
  }

  /**
   * Benchmark pipelined SET operation using RedisClient. Performs 1000 SET operations with
   * intermediate sync every 100 operations.
   */
  @Benchmark
  @OperationsPerInvocation(TOTAL_OPERATIONS)
  public void pipelinedSet(PipelineState ps, Blackhole blackhole) {
    try (Pipeline pipeline = redisClient.pipelined()) {
      Response<String>[] responses = ps.responses;
      int batchIndex = 0;

      for (int n = 0; n < TOTAL_OPERATIONS; n++) {
        responses[batchIndex++] = pipeline.set(KEY, VALUE);

        // Sync every BATCH_SIZE operations
        if ((n + 1) % BATCH_SIZE == 0) {
          pipeline.sync();
          // Consume all responses from this batch
          for (int i = 0; i < BATCH_SIZE; i++) {
            blackhole.consume(responses[i].get());
          }
          batchIndex = 0;
        }
      }
    }
  }

  /**
   * Benchmark pipelined GET operation using RedisClient. Performs 1000 GET operations with
   * intermediate sync every 100 operations.
   */
  @Benchmark
  @OperationsPerInvocation(TOTAL_OPERATIONS)
  public void pipelinedGet(PipelineState ps, Blackhole blackhole) {
    try (Pipeline pipeline = redisClient.pipelined()) {
      Response<String>[] responses = ps.responses;
      int batchIndex = 0;

      for (int n = 0; n < TOTAL_OPERATIONS; n++) {
        responses[batchIndex++] = pipeline.get(KEY);

        // Sync every BATCH_SIZE operations
        if ((n + 1) % BATCH_SIZE == 0) {
          pipeline.sync();
          // Consume all responses from this batch
          for (int i = 0; i < BATCH_SIZE; i++) {
            blackhole.consume(responses[i].get());
          }
          batchIndex = 0;
        }
      }
    }
  }

  // ========== Concrete implementations with different thread counts ==========

  /** Single-threaded benchmark (baseline). */
  @Threads(1)
  public static class Threads1 extends GetSetBenchmark {
  }

  /** 8-thread benchmark (moderate concurrency). */
  @Threads(8)
  public static class Threads8 extends GetSetBenchmark {
  }

  /** 64-thread benchmark (high concurrency). */
  @Threads(64)
  public static class Threads64 extends GetSetBenchmark {
  }
}
