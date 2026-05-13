package redis.clients.jedis.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.csc.Cache;
import redis.clients.jedis.csc.CacheConfig;
import redis.clients.jedis.csc.CacheFactory;
import redis.clients.jedis.csc.DefaultCacheable;
import redis.clients.jedis.util.RedisInputStream;
import redis.clients.jedis.util.RedisOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive JMH Benchmark for Jedis Protocol operations.
 *
 * This benchmark covers:
 * 1. Baseline protocol operations (parsing, encoding)
 * 2. Cache-aware protocol reads (push notification overhead)
 * 3. Push invalidation message processing at scale (1, 10, 100 pattern)
 * 4. Realistic mixed scenarios (push messages + regular responses)
 *
 * Run with: mvn -Pjmh clean test
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Threads(1)
public class ProtocolReadBenchmark {

    // ========== BASELINE PROTOCOL DATA ==========

    private static final byte[] KEY = "123456789".getBytes();
    private static final byte[] VAL = "FooBar".getBytes();

    // Baseline streams (no cache)
    private RedisInputStream baselineSimpleStringStream;      // +OK\r\n
    private RedisInputStream baselineBulkStringStream;        // $5\r\nHello\r\n
    private RedisInputStream baselineArrayStream;             // *3\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$3\r\nbaz\r\n
    private RedisInputStream baselineMultiBulkStream;         // *4\r\n$3\r\nfoo\r\n$13\r\nbarbarbarfooz\r\n$5\r\nHello\r\n$5\r\nWorld\r\n

    // ========== CACHE-AWARE PROTOCOL DATA ==========

    // Cache-aware streams (with cache checking)
    private RedisInputStream cacheSimpleStringStream;      // +OK\r\n
    private RedisInputStream cacheBulkStringStream;        // $5\r\nHello\r\n
    private RedisInputStream cacheArrayStream;             // *3\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$3\r\nbaz\r\n
    private RedisInputStream cacheMultiBulkStream;         // *4\r\n$3\r\nfoo\r\n$13\r\nbarbarbarfooz\r\n$5\r\nHello\r\n$5\r\nWorld\r\n

    // Push invalidation messages
    private RedisInputStream singlePushStream;        // >2\r\n$10\r\ninvalidate\r\n*1\r\n$3\r\nkey\r\n
    private RedisInputStream largePushStream;         // Push with many keys (100 keys)

    // Mixed streams (realistic scenarios)
    private RedisInputStream mixedSinglePush;         // 1 push + regular response
    private RedisInputStream mixedTenPush;            // 10 pushes + regular response
    private RedisInputStream mixedHundredPush;        // 100 pushes + regular response

    // Raw bytes for recreating streams
    private byte[] singlePushBytes;
    private byte[] largePushBytes;
    private byte[] mixedSinglePushBytes;
    private byte[] mixedTenPushBytes;
    private byte[] mixedHundredPushBytes;

    // Cache for testing
    private Cache cache;

    @Setup
    public void setup() {
        // === Store raw bytes for stream recreation ===

        // Single push with 1 key: >2\r\n$10\r\ninvalidate\r\n*1\r\n$3\r\nkey\r\n
        singlePushBytes = ">2\r\n$10\r\ninvalidate\r\n*1\r\n$3\r\nkey\r\n".getBytes();

        // Large push with 100 keys invalidated at once
        StringBuilder largePush = new StringBuilder(">2\r\n$10\r\ninvalidate\r\n*100\r\n");
        for (int i = 0; i < 100; i++) {
            String key = "key:" + i;
            largePush.append("$").append(key.length()).append("\r\n").append(key).append("\r\n");
        }
        largePushBytes = largePush.toString().getBytes();

        // === Mixed streams (push + regular response) ===

        // 1 push + response: >2\r\n$10\r\ninvalidate\r\n*1\r\n$3\r\nkey\r\n+OK\r\n
        mixedSinglePushBytes = ">2\r\n$10\r\ninvalidate\r\n*1\r\n$3\r\nkey\r\n+OK\r\n".getBytes();

        // 10 pushes + response
        StringBuilder mixed10 = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            mixed10.append(">2\r\n$10\r\ninvalidate\r\n*1\r\n$4\r\nkey").append(i).append("\r\n");
        }
        mixed10.append("+OK\r\n");
        mixedTenPushBytes = mixed10.toString().getBytes();

        // 100 pushes + response (heavy invalidation scenario)
        StringBuilder mixed100 = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            mixed100.append(">2\r\n$10\r\ninvalidate\r\n*1\r\n$5\r\nkey").append(String.format("%02d", i)).append("\r\n");
        }
        mixed100.append("+OK\r\n");
        mixedHundredPushBytes = mixed100.toString().getBytes();

        // Create cache for testing
        cache = CacheFactory.getCache(
            CacheConfig.builder()
                .maxSize(10000)
                .cacheable(DefaultCacheable.INSTANCE)
                .build()
        );

        // Pre-populate cache with some keys that will be invalidated
        // This ensures the invalidation actually does work
        for (int i = 0; i < 100; i++) {
            // Cache entries would be here in real scenario
        }
    }

    /**
     * Recreate streams before each benchmark invocation.
     * This is necessary because streams get consumed during reading.
     */
    @Setup(Level.Invocation)
    public void recreateStreams() {
        // Recreate push streams
        singlePushStream = new RedisInputStream(new ByteArrayInputStream(singlePushBytes));
        largePushStream = new RedisInputStream(new ByteArrayInputStream(largePushBytes));

        // Recreate mixed streams
        mixedSinglePush = new RedisInputStream(new ByteArrayInputStream(mixedSinglePushBytes));
        mixedTenPush = new RedisInputStream(new ByteArrayInputStream(mixedTenPushBytes));
        mixedHundredPush = new RedisInputStream(new ByteArrayInputStream(mixedHundredPushBytes));

        // Recreate baseline streams
        baselineSimpleStringStream = new RedisInputStream(new ByteArrayInputStream("+OK\r\n".getBytes()));
        baselineBulkStringStream = new RedisInputStream(new ByteArrayInputStream("$5\r\nHello\r\n".getBytes()));
        baselineArrayStream = new RedisInputStream(new ByteArrayInputStream("*3\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$3\r\nbaz\r\n".getBytes()));
        baselineMultiBulkStream = new RedisInputStream(new ByteArrayInputStream("*4\r\n$3\r\nfoo\r\n$13\r\nbarbarbarfooz\r\n$5\r\nHello\r\n$5\r\nWorld\r\n".getBytes()));

        // Recreate cache-aware streams
        cacheSimpleStringStream = new RedisInputStream(new ByteArrayInputStream("+OK\r\n".getBytes()));
        cacheBulkStringStream = new RedisInputStream(new ByteArrayInputStream("$5\r\nHello\r\n".getBytes()));
        cacheArrayStream = new RedisInputStream(new ByteArrayInputStream("*3\r\n$3\r\nfoo\r\n$3\r\nbar\r\n$3\r\nbaz\r\n".getBytes()));
        cacheMultiBulkStream = new RedisInputStream(new ByteArrayInputStream("*4\r\n$3\r\nfoo\r\n$13\r\nbarbarbarfooz\r\n$5\r\nHello\r\n$5\r\nWorld\r\n".getBytes()));
    }
    
    // ========== BASELINE PROTOCOL OPERATIONS (NO CACHE) ==========

    /**
     * Baseline: Read simple string without cache.
     */
    @Benchmark
    public void readSimpleString(Blackhole blackhole) throws Exception {
        Object result = Protocol.read(baselineSimpleStringStream);
        blackhole.consume(result);
    }

    /**
     * Baseline: Read bulk string without cache.
     */
    @Benchmark
    public void readBulkString(Blackhole blackhole) throws Exception {
        Object result = Protocol.read(baselineBulkStringStream);
        blackhole.consume(result);
    }

    /**
     * Baseline: Read array without cache.
     */
    @Benchmark
    public void readArray(Blackhole blackhole) throws Exception {
        Object result = Protocol.read(baselineArrayStream);
        blackhole.consume(result);
    }

    /**
     * Baseline: Read multi-bulk response without cache.
     */
    @Benchmark
    public void readMultiBulkResponse(Blackhole blackhole) throws Exception {
        Object result = Protocol.read(baselineMultiBulkStream);
        blackhole.consume(result);
    }

    /**
     * Encode SET command (baseline protocol performance).
     */
    @Benchmark
    public void encodeSetCommand(Blackhole blackhole) throws Exception {
        RedisOutputStream out = new RedisOutputStream(new ByteArrayOutputStream(8192));
        Protocol.sendCommand(out, new CommandArguments(Protocol.Command.SET).key(KEY).add(VAL));
        blackhole.consume(out);
    }

    // ========== CACHE-AWARE READS: Overhead of checking for push messages ==========

    /**
     * Cache-aware: Read simple string with cache (checks for push messages but none present).
     * Compare to readSimpleString to measure overhead.
     */
    @Benchmark
    public void cacheAwareReadSimpleString(Blackhole blackhole) throws Exception {
        Object result = Protocol.read(cacheSimpleStringStream, cache);
        blackhole.consume(result);
    }

    /**
     * Cache-aware: Read bulk string with cache (checks for push messages but none present).
     * Compare to readBulkString to measure overhead.
     */
    @Benchmark
    public void cacheAwareReadBulkString(Blackhole blackhole) throws Exception {
        Object result = Protocol.read(cacheBulkStringStream, cache);
        blackhole.consume(result);
    }

    /**
     * Cache-aware: Read array with cache (checks for push messages but none present).
     * Compare to readArray to measure overhead.
     */
    @Benchmark
    public void cacheAwareReadArray(Blackhole blackhole) throws Exception {
        Object result = Protocol.read(cacheArrayStream, cache);
        blackhole.consume(result);
    }

    /**
     * Cache-aware: Read multi-bulk response with cache (checks for push messages but none present).
     * Compare to readMultiBulkResponse to measure overhead.
     */
    @Benchmark
    public void cacheAwareReadMultiBulkResponse(Blackhole blackhole) throws Exception {
        Object result = Protocol.read(cacheMultiBulkStream, cache);
        blackhole.consume(result);
    }

    // ========== PUSH MESSAGE PROCESSING: Direct invalidation overhead ==========

    /**
     * Process single push invalidation message (1 key).
     * Measures cost of processing one cache invalidation.
     */
    @Benchmark
    public void processSinglePushInvalidation(Blackhole blackhole) throws Exception {
        Object result = Protocol.readPushes(singlePushStream, cache, true);
        blackhole.consume(result);
    }

    /**
     * Process large push invalidation (100 keys in single message).
     * Simulates mass invalidation event (e.g., FLUSHDB on tracked keys).
     */
    @Benchmark
    public void processLargePushInvalidation(Blackhole blackhole) throws Exception {
        Object result = Protocol.readPushes(largePushStream, cache, true);
        blackhole.consume(result);
    }

    // ========== MIXED STREAMS: Realistic scenarios with push + response ==========

    /**
     * Read response preceded by 1 push invalidation.
     * Most common realistic scenario.
     */
    @Benchmark
    public void readWith1PushMessage(Blackhole blackhole) throws Exception {
        Object result = Protocol.read(mixedSinglePush, cache);
        blackhole.consume(result);
    }

    /**
     * Read response preceded by 10 push invalidations.
     * Moderate invalidation burst before response.
     */
    @Benchmark
    public void readWith10PushMessages(Blackhole blackhole) throws Exception {
        Object result = Protocol.read(mixedTenPush, cache);
        blackhole.consume(result);
    }

    /**
     * Read response preceded by 100 push invalidations.
     * Heavy invalidation scenario (worst-case for read latency).
     */
    @Benchmark
    public void readWith100PushMessages(Blackhole blackhole) throws Exception {
        Object result = Protocol.read(mixedHundredPush, cache);
        blackhole.consume(result);
    }
}
