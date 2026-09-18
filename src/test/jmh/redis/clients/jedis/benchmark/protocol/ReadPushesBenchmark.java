package redis.clients.jedis.benchmark.protocol;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.PushConsumerChain;
import redis.clients.jedis.PushConsumerChainImpl;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheFactory;
import redis.clients.jedis.csc.DefaultCacheable;
import redis.clients.jedis.csc.PushInvalidateConsumer;
import redis.clients.jedis.util.RedisInputStream;

import java.io.ByteArrayInputStream;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for {@link Protocol#readPushes(RedisInputStream, PushConsumerChain)} — Jedis
 * client-side cache invalidation processing.
 * <p>
 * One {@code readPushes} call is one operation, regardless of how many pending push frames it
 * drains. Each {@code @Benchmark} method loops {@code BATCH} {@code readPushes} calls against a
 * pre-built buffer, resetting the underlying stream between calls so every invocation drains the
 * same buffer freshly. {@code @OperationsPerInvocation(BATCH)} normalizes the score to per-call
 * time.
 * <p>
 * The two methods differ in the burst size each call drains (1 vs 1000 pending frames), exposing
 * how per-call cost scales with payload size.
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
public class ReadPushesBenchmark {

  private static final int BATCH = 100;
  private static final int PENDING_1 = 1;
  private static final int PENDING_1000 = 1000;

  private byte[] pushBatch1;
  private byte[] pushBatch1000;

  private RedisInputStream stream1;
  private RedisInputStream stream1000;

  private Cache cache;
  private PushConsumerChain pushChain;

  @Setup(Level.Trial)
  public void setupTrial() {
    byte[] singlePush = ">2\r\n$10\r\ninvalidate\r\n*1\r\n$3\r\nkey\r\n".getBytes();
    pushBatch1 = repeat(singlePush, PENDING_1);
    pushBatch1000 = repeat(singlePush, PENDING_1000);
  }

  @Setup(Level.Iteration)
  public void setupIteration() {
    stream1 = new RedisInputStream(new ByteArrayInputStream(pushBatch1));
    stream1000 = new RedisInputStream(new ByteArrayInputStream(pushBatch1000));
    cache = CacheFactory.getCache(
      CacheConfig.builder().maxSize(10_000).cacheable(DefaultCacheable.INSTANCE).build());
    pushChain = PushConsumerChainImpl.of(PushConsumerChainImpl.PUBSUB_CONSUMER,
      new PushInvalidateConsumer(cache));
  }

  private static byte[] repeat(byte[] src, int times) {
    byte[] out = new byte[src.length * times];
    for (int i = 0; i < times; i++) {
      System.arraycopy(src, 0, out, i * src.length, src.length);
    }
    return out;
  }

  /**
   * One readPushes call drains 1 pending push frame. Per-call cost is dominated by fixed overhead.
   */
  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void drain1Pending(Blackhole blackhole) throws Exception {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(Protocol.readPushes(stream1, pushChain));
      stream1.reset();
    }
  }

  /** One readPushes call drains 1000 pending push frames. Per-call cost reflects bulk drainage. */
  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void drain1000Pending(Blackhole blackhole) throws Exception {
    for (int i = 0; i < BATCH; i++) {
      blackhole.consume(Protocol.readPushes(stream1000, pushChain));
      stream1000.reset();
    }
  }
}
