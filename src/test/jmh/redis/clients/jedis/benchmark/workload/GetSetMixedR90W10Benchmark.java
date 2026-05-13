package redis.clients.jedis.benchmark.workload;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.openjdk.jmh.annotations.*;
import redis.clients.jedis.Connection;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.EndpointConfig;
import redis.clients.jedis.Endpoints;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.RedisProtocol;
import redis.clients.jedis.builders.StandaloneClientBuilder;
import redis.clients.jedis.csc.CacheConfig;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Workload benchmark: mixed GET/SET at 90% read / 10% write.
 *
 * Provides a single comparable throughput number per client configuration:
 * <ul>
 *   <li>{@link JedisT1} — standalone Jedis, single-threaded baseline.</li>
 *   <li>{@link JedisPoolT1}/{@link JedisPoolT8} — legacy JedisPool (deprecated), for comparison.</li>
 *   <li>{@link RedisClientT1}/{@link RedisClientT8} — pooled RedisClient, no CSC.</li>
 *   <li>{@link RedisClientCSCT1}/{@link RedisClientCSCT8} — pooled RedisClient with client-side cache.</li>
 * </ul>
 *
 * This is a user-facing comparison benchmark.
 * Workload parameters are fixed by design..
 *
 * <p>Workload:
 * <ul>
 *   <li>Working set: 1000 keys, pre-populated.</li>
 *   <li>CSC cache size (where applicable): 500 entries (50% of working set).</li>
 *   <li>Access pattern: uniform random over the working set.</li>
 *   <li>Operation mix: 90% GET, 10% SET (overwrite existing key).</li>
 * </ul>
 *
 * <p>Run with: {@code mvn -Pjmh-headline test -Djmh.includes="GetSetMixedR90W10Benchmark"}.
 *
 * <p>WARNING: {@code @Setup} calls {@code FLUSHDB} on the target Redis instance.
 * Do not run against a shared or production database.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(2)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
public abstract class GetSetMixedR90W10Benchmark {

    protected static final int WORKING_SET = 1_000;
    protected static final int CACHE_SIZE  = 500;
    protected static final int READ_PCT    = 90;
    protected static final int POOL_SIZE   = 8;

    private static final EndpointConfig endpoint = Endpoints.getRedisEndpoint("standalone0");

    protected String[] keys;
    protected String[] values;

    /** Per-thread seeded RNG for reproducible workload sequencing across runs. */
    @State(Scope.Thread)
    public static class Rng {
        private static final long SEED = 12_648_430L;
        final Random r = new Random(SEED);
    }

    @Setup(Level.Trial)
    public final void setupCommon() {
        keys = new String[WORKING_SET];
        values = new String[WORKING_SET];
        for (int i = 0; i < WORKING_SET; i++) {
            keys[i] = "mix9010:k" + i;
            values[i] = "v" + i;
        }

        try (Jedis seed = new Jedis(endpoint.getHostAndPort())) {
            String pwd = endpoint.getPassword();
            if (pwd != null && !pwd.isEmpty()) {
                seed.auth(pwd);
            }
            seed.flushDB();
            for (int i = 0; i < WORKING_SET; i++) {
                seed.set(keys[i], values[i]);
            }
        }

        setupClient();

        // Prime cache: read every key once so CSC variants enter measurement
        // with a populated cache. No-op for non-CSC clients.
        for (String k : keys) {
            doGet(k);
        }
    }

    @TearDown(Level.Trial)
    public final void teardownCommon() {
        teardownClient();
    }

    protected abstract void setupClient();
    protected abstract void teardownClient();
    protected abstract String doGet(String key);
    protected abstract String doSet(String key, String value);

    @Benchmark
    public Object workload(Rng rng) {
        int idx = rng.r.nextInt(WORKING_SET);
        if (rng.r.nextInt(100) < READ_PCT) {
            return doGet(keys[idx]);
        } else {
            return doSet(keys[idx], values[idx]);
        }
    }

    // ====================================================================
    // Jedis (single-threaded baseline)
    // ====================================================================

    /**
     * Standalone Jedis baseline. T1 only — {@link Jedis} is not thread-safe,
     * so this configuration cannot be safely run with @Threads > 1.
     * Pool/concurrency benefits are demonstrated by the RedisClient variants.
     */
    @Threads(1)
    public static class JedisT1 extends GetSetMixedR90W10Benchmark {

        private Jedis jedis;

        @Override
        protected void setupClient() {
            jedis = new Jedis(endpoint.getHostAndPort());
            String pwd = endpoint.getPassword();
            if (pwd != null && !pwd.isEmpty()) {
                jedis.auth(pwd);
            }
        }

        @Override
        protected void teardownClient() {
            if (jedis != null) jedis.close();
        }

        @Override
        protected String doGet(String key) {
            return jedis.get(key);
        }

        @Override
        protected String doSet(String key, String value) {
            return jedis.set(key, value);
        }
    }

    // ====================================================================
    // JedisPool (legacy pooled client, deprecated)
    // ====================================================================

    /**
     * Shared setup for {@link JedisPool} variants. Each benchmark invocation borrows a
     * {@link Jedis} from the pool, runs the operation, and returns it via try-with-resources.
     * Abstract — JMH skips it.
     */
    public static abstract class JedisPoolBase extends GetSetMixedR90W10Benchmark {

        protected JedisPool pool;

        @Override
        protected void setupClient() {
            GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
            poolConfig.setMaxTotal(POOL_SIZE);
            poolConfig.setMaxIdle(POOL_SIZE);
            pool = new JedisPool(poolConfig, new HostAndPort(endpoint.getHost(), endpoint.getPort()),
                    DefaultJedisClientConfig.builder().password(endpoint.getPassword()).build());
        }

        @Override
        protected void teardownClient() {
            if (pool != null) pool.close();
        }

        @Override
        protected String doGet(String key) {
            try (Jedis j = pool.getResource()) {
                return j.get(key);
            }
        }

        @Override
        protected String doSet(String key, String value) {
            try (Jedis j = pool.getResource()) {
                return j.set(key, value);
            }
        }
    }

    @Threads(1)
    public static class JedisPoolT1 extends JedisPoolBase {}

    @Threads(8)
    public static class JedisPoolT8 extends JedisPoolBase {}


    // ====================================================================
    // RedisClient (no client-side cache)
    // ====================================================================

    /** Shared setup for non-CSC RedisClient variants. Abstract — JMH skips it. */
    public static abstract class RedisClientBase extends GetSetMixedR90W10Benchmark {

        protected RedisClient client;

        @Override
        protected void setupClient() {
            client = buildRedisClient(false);
        }

        @Override
        protected void teardownClient() {
            if (client != null) client.close();
        }

        @Override
        protected String doGet(String key) {
            return client.get(key);
        }

        @Override
        protected String doSet(String key, String value) {
            return client.set(key, value);
        }
    }

    @Threads(1)
    public static class RedisClientT1 extends RedisClientBase {}

    @Threads(8)
    public static class RedisClientT8 extends RedisClientBase {}

    // ====================================================================
    // RedisClient + Client-Side Cache
    // ====================================================================

    /** Shared setup for CSC variants. Abstract — JMH skips it. */
    public static abstract class RedisClientCSCBase extends GetSetMixedR90W10Benchmark {

        protected RedisClient client;

        @Override
        protected void setupClient() {
            client = buildRedisClient(true);
        }

        @Override
        protected void teardownClient() {
            if (client != null) client.close();
        }

        @Override
        protected String doGet(String key) {
            return client.get(key);
        }

        @Override
        protected String doSet(String key, String value) {
            return client.set(key, value);
        }
    }

    @Threads(1)
    public static class RedisClientCSCT1 extends RedisClientCSCBase {}

    @Threads(8)
    public static class RedisClientCSCT8 extends RedisClientCSCBase {}

    // ====================================================================
    // Shared RedisClient construction
    // ====================================================================

    private static RedisClient buildRedisClient(boolean withCSC) {
        GenericObjectPoolConfig<Connection> pool = new GenericObjectPoolConfig<>();
        pool.setMaxTotal(POOL_SIZE);
        pool.setMaxIdle(POOL_SIZE);

        DefaultJedisClientConfig.Builder cfg = DefaultJedisClientConfig.builder()
                .password(endpoint.getPassword());
        if (withCSC) {
            // CSC requires RESP3 — invalidation messages are RESP3 push frames.
            cfg.protocol(RedisProtocol.RESP3);
        }

        StandaloneClientBuilder<RedisClient> b = RedisClient.builder()
                .hostAndPort(endpoint.getHost(), endpoint.getPort())
                .clientConfig(cfg.build())
                .poolConfig(pool);

        if (withCSC) {
            b.cacheConfig(CacheConfig.builder().maxSize(CACHE_SIZE).build());
        }

        return b.build();
    }
}
