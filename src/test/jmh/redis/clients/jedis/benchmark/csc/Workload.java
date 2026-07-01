package redis.clients.jedis.benchmark.csc;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.builders.StandaloneClientBuilder;
import redis.clients.jedis.csc.CacheConfig;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared scaffolding for the CSC workload benchmarks under this package.
 * <p>
 * Each concrete workload class extends {@link Workload} and supplies its own access mix (read/write
 * ratio), {@link KeyDistribution.IndexSampler}, and {@code @Threads} count via JMH annotations.
 * Pool size, key set, value payload, and CSC config are configured here so all workloads use the
 * same baseline.
 * <p>
 * The {@link #impl} parameter selects between:
 * <ul>
 * <li>{@code none} — {@link RedisClient} without {@link CacheConfig}; the no-CSC baseline.</li>
 * <li>{@code csc} — {@link RedisClient} with the default Jedis CSC stack.</li>
 * </ul>
 * The {@link #mode} parameter selects {@code cold} (cache starts empty) vs {@code warm} (cache is
 * pre-populated by reading every key once before measurement). Mode is ignored for the {@code none}
 * baseline — it makes no difference there.
 * <p>
 * Pool size defaults to {@link #POOL_SIZE_DEFAULT} (15) which is sized to a typical local Redis
 * test box, not the 140-conn pool the go-redis exploration used. The performance pathologies in the
 * canonical CSC topology surface at much lower concurrency; for a stricter apples-to-apples
 * comparison override with {@code -Dcsc.pool=140}. Cache size matches the go-redis run
 * ({@code 50_000}) so working sets up to {@code WORKING_SET_DEFAULT} keys fit entirely.
 * <p>
 * <b>WARNING:</b> {@code @Setup} calls {@code FLUSHDB}. Never run against a shared database.
 */
@State(Scope.Benchmark)
public abstract class Workload {

  static final int POOL_SIZE_DEFAULT = 15;
  static final int POOL_SIZE = Integer.getInteger("csc.pool", POOL_SIZE_DEFAULT);
  static final int CACHE_SIZE = Integer.getInteger("csc.cacheSize", 50_000);
  static final int WORKING_SET_DEFAULT = 10_000;
  static final int VALUE_BYTES_DEFAULT = 64;

  @Param({ "none", "csc" })
  public String impl;

  @Param({ "cold", "warm" })
  public String mode;

  protected final EndpointConfig endpoint = Endpoints.getRedisEndpoint("standalone0");
  protected RedisClient client;
  protected String[] keys;
  protected String value;

  /**
   * Per-thread RNG seeded uniquely so multi-threaded variants don't lockstep on the same key
   * sequence — independent samples are what we want when measuring contention.
   */
  @State(Scope.Thread)
  public static class Rng {
    private static final AtomicInteger TID = new AtomicInteger();
    public final Random r;

    public Rng() {
      this.r = new Random(0xCAFEBABEL ^ TID.getAndIncrement());
    }
  }

  protected int workingSet() {
    return WORKING_SET_DEFAULT;
  }

  protected int valueBytes() {
    return VALUE_BYTES_DEFAULT;
  }

  /** Read percentage in {@code [0, 100]}. */
  protected abstract int readPct();

  /** Per-workload sampler. */
  protected abstract KeyDistribution.IndexSampler sampler();

  @Setup(Level.Trial)
  public void setupTrial() {
    keys = new String[workingSet()];
    for (int i = 0; i < keys.length; i++) {
      keys[i] = "csc:k" + i;
    }
    char[] vchars = new char[valueBytes()];
    Arrays.fill(vchars, 'v');
    value = new String(vchars);

    seedDatabase();

    client = buildClient();

    if ("warm".equalsIgnoreCase(mode)) {
      // Pre-populate the working set and (for CSC) also fill the local cache to 100% hit rate.
      for (String k : keys) {
        client.get(k);
      }
    }
  }

  @TearDown(Level.Trial)
  public void teardownTrial() {
    if (client != null) {
      try {
        client.close();
      } catch (Exception ignored) {
        // shutdown best-effort; metrics already collected
      }
    }
  }

  private void seedDatabase() {
    try (Jedis seed = new Jedis(endpoint.getHostAndPort())) {
      String pwd = endpoint.getPassword();
      if (pwd != null && !pwd.isEmpty()) {
        seed.auth(pwd);
      }
      seed.flushDB();
      for (String k : keys) {
        seed.set(k, value);
      }
    }
  }

  private RedisClient buildClient() {
    GenericObjectPoolConfig<Connection> pool = new GenericObjectPoolConfig<>();
    pool.setMaxTotal(POOL_SIZE);
    pool.setMaxIdle(POOL_SIZE);
    pool.setMinIdle(POOL_SIZE);
    pool.setBlockWhenExhausted(true);
    pool.setMaxWait(java.time.Duration.ofSeconds(10));

    boolean withCsc = "csc".equalsIgnoreCase(impl);
    DefaultJedisClientConfig.Builder cfg = DefaultJedisClientConfig.builder()
        .password(endpoint.getPassword());
    if (withCsc) {
      // CSC requires RESP3 — invalidation messages are RESP3 push frames.
      cfg.protocol(RedisProtocol.RESP3);
    }

    StandaloneClientBuilder<RedisClient> b = RedisClient.builder()
        .hostAndPort(endpoint.getHost(), endpoint.getPort()).clientConfig(cfg.build())
        .poolConfig(pool);
    if (withCsc) {
      b.cacheConfig(CacheConfig.builder().maxSize(CACHE_SIZE).build());
    }
    return b.build();
  }

  /** Run a single op picked by {@link #readPct()} on a key sampled by {@link #sampler()}. */
  protected Object doOnce(Rng rng) {
    int idx = sampler().next(rng.r, keys.length);
    if (rng.r.nextInt(100) < readPct()) {
      return client.get(keys[idx]);
    }
    return client.set(keys[idx], value);
  }
}
