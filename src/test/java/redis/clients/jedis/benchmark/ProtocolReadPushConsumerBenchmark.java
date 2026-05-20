package redis.clients.jedis.benchmark;

import java.io.ByteArrayInputStream;

import redis.clients.jedis.Protocol;
import redis.clients.jedis.PushConsumerChain;
import redis.clients.jedis.PushConsumerChainImpl;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheFactory;
import redis.clients.jedis.csc.PushInvalidateConsumer;
import redis.clients.jedis.util.RedisInputStream;

/**
 * Manual benchmark for {@link Protocol#read(RedisInputStream, PushConsumerChain)} measuring the
 * cost of processing a burst of RESP3 {@code invalidate} push messages preceding a regular command
 * response.
 * <p>
 * Each measured operation feeds a pre-built byte payload containing {@value #INVALIDATIONS_PER_OP}
 * invalidate push frames (one key each) followed by a single {@code +OK\r\n} reply through a
 * push-consumer chain. One {@code Protocol.read} call drains all push frames and returns the
 * trailing OK.
 * <p>
 * Two scenarios are measured, mirroring the default chains installed by the production code:
 * <ul>
 * <li>{@code Connection} defaults: {@link PushConsumerChainImpl#PUBSUB_CONSUMER} only. The
 * invalidate type is not in the pub/sub set, so it falls through to end-of-chain and is silently
 * dropped — this measures RESP parsing + minimal chain traversal.
 * <li>{@code CacheConnection} defaults: {@code PUBSUB_CONSUMER} + {@link PushInvalidateConsumer}.
 * The invalidate consumer matches, evicts the referenced keys from the cache and drops the message
 * — this measures the production CSC dispatch path.
 * </ul>
 * <p>
 * The cache is empty, so each invalidation resolves to a fast hash-map miss; the delta between the
 * two scenarios is the cost of one {@code PushInvalidateConsumer} invocation per push.
 */
public class ProtocolReadPushConsumerBenchmark {

  private static final int INVALIDATIONS_PER_OP = 100;
  private static final int TOTAL_OPERATIONS = 500_000;
  private static final int WARMUP_ITERATIONS = 1;
  private static final int MEASUREMENT_ITERATIONS = 1;

  public static void main(String[] args) throws Exception {
    byte[] payload = buildPayload(INVALIDATIONS_PER_OP);
    Cache cache = CacheFactory.getCache(CacheConfig.builder().build());

    // Mirrors Connection#initPushConsumers — only PUBSUB_CONSUMER is registered by default.
    PushConsumerChain connectionDefaults = PushConsumerChainImpl
        .of(PushConsumerChainImpl.PUBSUB_CONSUMER);

    // Mirrors CacheConnection: PUBSUB_CONSUMER (inherited) + PushInvalidateConsumer.
    PushConsumerChain cacheConnectionDefaults = PushConsumerChainImpl
        .of(PushConsumerChainImpl.PUBSUB_CONSUMER, new PushInvalidateConsumer(cache));

    runScenario("Connection defaults [PUBSUB_CONSUMER]", payload, connectionDefaults);
    runScenario("CacheConnection defaults [PUBSUB_CONSUMER, PushInvalidateConsumer]", payload,
      cacheConnectionDefaults);
  }

  private static void runScenario(String name, byte[] payload, PushConsumerChain chain) {
    System.out.println();
    System.out.println("--- " + name + " ---");
    for (int at = 0; at < WARMUP_ITERATIONS + MEASUREMENT_ITERATIONS; ++at) {
      long elapsedNanos = measure(payload, chain);
      double nsPerOp = (double) elapsedNanos / TOTAL_OPERATIONS;
      long opsPerSecond = (1_000_000_000L * TOTAL_OPERATIONS) / Math.max(1, elapsedNanos);
      String tag = at < WARMUP_ITERATIONS ? "warmup " : "measure";
      System.out.printf("  %s iter %d: %,d ops/s (%.2f ns/op)%n", tag, at, opsPerSecond, nsPerOp);
    }
  }

  private static long measure(byte[] payload, PushConsumerChain chain) {
    long duration = 0;
    for (int n = 0; n < TOTAL_OPERATIONS; n++) {
      RedisInputStream in = new RedisInputStream(new ByteArrayInputStream(payload));
      long start = System.nanoTime();
      Object reply = Protocol.read(in, chain);
      duration += System.nanoTime() - start;
    }
    return duration;
  }

  /**
   * Build a wire-format payload of {@code n} RESP3 {@code invalidate} push messages (each carrying
   * a single key) followed by a {@code +OK\r\n} simple-string reply.
   */
  private static byte[] buildPayload(int n) {
    StringBuilder sb = new StringBuilder(n * 48);
    for (int i = 0; i < n; i++) {
      String key = "key:" + i;
      sb.append(">2\r\n$10\r\ninvalidate\r\n*1\r\n$").append(key.length()).append("\r\n")
          .append(key).append("\r\n");
    }
    sb.append("+OK\r\n");
    return sb.toString().getBytes();
  }
}
