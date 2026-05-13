package redis.clients.jedis.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import redis.clients.jedis.*;

import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmark for Jedis GET/SET operations on a live Redis server.
 *
 * Simple benchmark measuring Redis read/write performance with a single key-value pair.
 *
 * Requirements:
 * - Running Redis instance on localhost:6379 (or configured endpoint)
 * - Redis 6.0+ recommended
 *
 * Run with: mvn -Pjmh test -Djmh.includes="JedisGetSetBenchmark"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
public class JedisGetSetBenchmark {

    private static final EndpointConfig endpoint = Endpoints.getRedisEndpoint("standalone0");

    private Jedis jedis;

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

        // Clean database
        jedis.flushDB();

        // Pre-populate key for GET benchmark
        jedis.set("foo", "bar");
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
        String result = jedis.set("foo", "bar");
        blackhole.consume(result);
    }

    /**
     * Benchmark GET operation.
     */
    @Benchmark
    public void get(Blackhole blackhole) {
        String result = jedis.get("foo");
        blackhole.consume(result);
    }
}
