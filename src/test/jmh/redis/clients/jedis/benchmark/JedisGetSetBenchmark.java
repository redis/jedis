package redis.clients.jedis.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import redis.clients.jedis.*;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for {@link Jedis} GET/SET operations on a live Redis server.
 * <p>
 * Benchmarks both regular and pipelined operations:
 * <ul>
 * <li>Regular GET/SET: single command per round-trip
 * <li>Pipelined GET/SET: batched commands for reduced network overhead
 * </ul>
 * <p>
 * Requirements:
 * <ul>
 * <li>Running Redis instance on {@code localhost:6379} (or configured endpoint)
 * <li>Redis 6.0+ recommended
 * </ul>
 * <p>
 * Run with: {@code mvn -Pjmh test -Djmh.includes="JedisGetSetBenchmark"}
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Threads(1)
public class JedisGetSetBenchmark {

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

  private Jedis jedis;

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
    // Connect to Redis
    jedis = new Jedis(endpoint.getHostAndPort());
    jedis.connect();

    // Authenticate if password is configured
    String password = endpoint.getPassword();
    if (password != null && !password.isEmpty()) {
      jedis.auth(password);
    }

    // Clean database and pre-populate KEY so GET benchmarks hit a real value
    jedis.flushDB();
    jedis.set(KEY, VALUE);
  }

  @TearDown(Level.Trial)
  public void teardownTrial() {
    if (jedis != null) {
      jedis.disconnect();
    }
  }

  /**
   * Benchmark SET operation.
   */
  @Benchmark
  public void set(Blackhole blackhole) {
    String result = jedis.set(KEY, VALUE);
    blackhole.consume(result);
  }

  /**
   * Benchmark GET operation.
   */
  @Benchmark
  public void get(Blackhole blackhole) {
    String result = jedis.get(KEY);
    blackhole.consume(result);
  }

  /**
   * Benchmark pipelined SET operation. Performs 1000 SET operations with intermediate sync every
   * 100 operations.
   */
  @Benchmark
  @OperationsPerInvocation(TOTAL_OPERATIONS)
  public void pipelinedSet(PipelineState ps, Blackhole blackhole) {
    try (Pipeline pipeline = jedis.pipelined()) {
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
   * Benchmark pipelined GET operation. Performs 1000 GET operations with intermediate sync every
   * 100 operations.
   */
  @Benchmark
  @OperationsPerInvocation(TOTAL_OPERATIONS)
  public void pipelinedGet(PipelineState ps, Blackhole blackhole) {
    try (Pipeline pipeline = jedis.pipelined()) {
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
}
