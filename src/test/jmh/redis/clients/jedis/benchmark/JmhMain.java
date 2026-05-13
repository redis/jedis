package redis.clients.jedis.benchmark;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.annotations.Mode;

import java.util.concurrent.TimeUnit;

/**
 * Manual JMH Test Launcher.
 * 
 * Allows running JMH benchmarks directly from the IDE without Maven.
 * 
 * Available benchmark suites:
 * - All benchmarks
 * - Protocol benchmarks (RESP3, cache-aware, push messages)
 * - CRC16 benchmarks (hash slot calculation)
 * 
 * To run from IDE:
 * 1. Right-click this file
 * 2. Select "Run JmhMain.main()"
 * 
 * To run specific benchmark:
 * - Uncomment the desired run method in main()
 * - Or modify the include pattern
 * 
 * @author Jedis Contributors
 */
public class JmhMain {

    public static void main(String... args) throws RunnerException {
        // Uncomment the benchmark suite you want to run:

        // runAllBenchmarks();
        // runJedisGetSetBenchmarks();
        // runProtocolBenchmarks();
        // runCRC16Benchmarks();
        // runSafeEncoderBenchmarks();
        // runRedisClientGetSetBenchmarks();
        // runGetSetMixedR90W10Benchmarks();
        // runSpecificBenchmark("CRC16Benchmark.getSlotString");

        // results saved to benchmarks.json and benchmark.log
    }

    /**
     * Run all JMH benchmarks in the project.
     * Uses benchmark class defaults for mode and timeUnit.
     */
    private static void runAllBenchmarks() throws RunnerException {
        System.out.println("Running ALL benchmarks...");
        new Runner(prepareOptions()
                .build())
                .run();
    }

    /**
     * Run only Protocol-related benchmarks (RESP3, cache-aware, push messages).
     * Uses benchmark class defaults for mode and timeUnit.
     */
    private static void runProtocolBenchmarks() throws RunnerException {
        System.out.println("Running Protocol benchmarks...");
        new Runner(prepareOptions()
                .include(".*ProtocolReadBenchmark.*")
                .build())
                .run();
    }

    /**
     * Run only CRC16 hash slot calculation benchmarks.
     * Uses benchmark class defaults for mode and timeUnit.
     */
    private static void runCRC16Benchmarks() throws RunnerException {
        System.out.println("Running CRC16 benchmarks...");
        new Runner(prepareOptions()
                .include(".*CRC16Benchmark.*")
                .build())
                .run();
    }

    /**
     * Run only SafeEncoder UTF-8 encoding/decoding benchmarks.
     * Uses benchmark class defaults for mode and timeUnit.
     */
    private static void runSafeEncoderBenchmarks() throws RunnerException {
        System.out.println("Running SafeEncoder benchmarks...");
        new Runner(prepareOptions()
                .include(".*SafeEncoderBenchmark.*")
                .build())
                .run();
    }

    /**
     * Run only Jedis GET/SET benchmarks (requires live Redis server).
     * Uses benchmark class defaults for mode and timeUnit.
     */
    private static void runJedisGetSetBenchmarks() throws RunnerException {
        System.out.println("Running Jedis GET/SET benchmarks (requires Redis server)...");
        new Runner(prepareOptions()
                .include(".*JedisGetSetBenchmark.*")
                .build())
                .run();
    }

    /**
     * Run only RedisClient GET/SET benchmarks (requires live Redis server).
     * Uses benchmark class defaults for mode and timeUnit.
     */
    private static void runRedisClientGetSetBenchmarks() throws RunnerException {
        System.out.println("Running RedisClient GET/SET benchmarks (requires Redis server)...");
        new Runner(prepareOptions()
                .include(".*RedisClientGetSetBenchmark.*")
                .build())
                .run();
    }

    /**
     * Run only the 90% read / 10% write mixed workload benchmark (requires live Redis server).
     * Compares Jedis, RedisClient, and RedisClient+CSC under a fixed workload.
     * Uses benchmark class defaults for mode and timeUnit.
     */
    private static void runGetSetMixedR90W10Benchmarks() throws RunnerException {
        System.out.println("Running GetSetMixedR90W10 workload benchmarks (requires Redis server)...");
        new Runner(prepareOptions()
                .include(".*GetSetMixedR90W10Benchmark.*")
                .build())
                .run();
    }

    /**
     * Run a specific benchmark by name.
     * Uses benchmark class defaults for mode and timeUnit.
     *
     * @param benchmarkPattern Benchmark name pattern (e.g., "CRC16Benchmark.getSlotString")
     */
    private static void runSpecificBenchmark(String benchmarkPattern) throws RunnerException {
        System.out.println("Running benchmark: " + benchmarkPattern);
        new Runner(prepareOptions()
                .include(".*" + benchmarkPattern + ".*")
                .build())
                .run();
    }

    /**
     * Prepare common JMH options.
     *
     * Does NOT override benchmark class annotations.
     * Benchmark classes should explicitly define:
     * - @BenchmarkMode
     * - @OutputTimeUnit
     * - @Fork
     * - @Warmup
     * - @Measurement
     * - @Threads (if needed)
     *
     * Only sets:
     * - Result output format and files
     */
    private static ChainedOptionsBuilder prepareOptions() {
        return new OptionsBuilder()
                .resultFormat(ResultFormatType.JSON)
                .result("benchmarks.json")
                .output("benchmark.log");
    }
}
