package redis.clients.jedis.benchmark.csc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheStats;

/**
 * Cost of a client-side cache hit through the public {@link RedisClient} API, by reply shape. Each
 * reply is cached during setup, and the trial fails if any measured call misses. Only public API
 * and {@code Serializable} results are used, so the benchmark runs unchanged whether the cache
 * stores serialized values or raw replies.
 * <p>
 * Requires Redis 7.4+ on the {@code standalone0} endpoint. Writes and deletes its own
 * {@code csc-hit-benchmark:} keys. Add {@code -prof gc} to compare allocation per call.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(2)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 2)
public class CacheHitBenchmark {

  public enum Reply {
    GET, LRANGE_100, HGETALL_100
  }

  private static final String STRING_KEY = "csc-hit-benchmark:string";
  private static final String LIST_KEY = "csc-hit-benchmark:list";
  private static final String HASH_KEY = "csc-hit-benchmark:hash";
  private static final int ELEMENTS = 100;

  @Param
  public Reply reply;

  private RedisClient client;
  private CacheStats stats;
  private long missesBeforeMeasurement;

  @Setup(Level.Trial)
  public void setUp() {
    EndpointConfig endpoint = Endpoints.getRedisEndpoint("standalone0");
    client = RedisClient.builder().hostAndPort(endpoint.getHostAndPort())
        .clientConfig(endpoint.getClientConfigBuilder().protocol(RedisProtocol.RESP3).build())
        .cacheConfig(CacheConfig.builder().build()).build();
    stats = client.getCache().getStats();

    client.del(STRING_KEY, LIST_KEY, HASH_KEY);
    client.set(STRING_KEY, value(64));
    String[] elements = new String[ELEMENTS];
    Map<String, String> fields = new HashMap<>();
    for (int i = 0; i < ELEMENTS; i++) {
      elements[i] = value(16);
      fields.put("field:" + i, value(16));
    }
    client.rpush(LIST_KEY, elements);
    client.hset(HASH_KEY, fields);

    hit(); // a miss: caches the reply
    long hits = stats.getHitCount();
    hit();
    if (stats.getHitCount() != hits + 1) {
      throw new IllegalStateException(reply + " reply is not served from the cache");
    }
    missesBeforeMeasurement = stats.getMissCount();
  }

  @TearDown(Level.Trial)
  public void tearDown() {
    long misses = stats.getMissCount() - missesBeforeMeasurement;
    try {
      client.del(STRING_KEY, LIST_KEY, HASH_KEY);
    } finally {
      client.close();
    }
    if (misses != 0) {
      throw new IllegalStateException(misses + " measured calls missed the cache");
    }
  }

  @Benchmark
  public Object hit() {
    switch (reply) {
      case GET:
        return client.get(STRING_KEY);
      case LRANGE_100:
        return client.lrange(LIST_KEY, 0, -1);
      default:
        return client.hgetAll(HASH_KEY);
    }
  }

  private static String value(int length) {
    char[] chars = new char[length];
    Arrays.fill(chars, 'v');
    return new String(chars);
  }
}
