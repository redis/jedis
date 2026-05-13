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
        // runProtocolBenchmarks();
        // runCRC16Benchmarks();
        runSafeEncoderBenchmarks();
        // runSpecificBenchmark("CRC16Benchmark.getSlotString");

        // results saved to benchmarks.json and benchmark.log
    }

    /**
     * Run all JMH benchmarks in the project.
     */
    private static void runAllBenchmarks() throws RunnerException {
        System.out.println("Running ALL benchmarks...");
        new Runner(prepareOptions()
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .build())
                .run();
    }

    /**
     * Run only Protocol-related benchmarks (RESP3, cache-aware, push messages).
     */
    private static void runProtocolBenchmarks() throws RunnerException {
        System.out.println("Running Protocol benchmarks...");
        new Runner(prepareOptions()
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .include(".*ProtocolReadBenchmark.*")
                .build())
                .run();
    }

    /**
     * Run only CRC16 hash slot calculation benchmarks.
     */
    private static void runCRC16Benchmarks() throws RunnerException {
        System.out.println("Running CRC16 benchmarks...");
        new Runner(prepareOptions()
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .include(".*CRC16Benchmark.*")
                .build())
                .run();
    }

    /**
     * Run only SafeEncoder UTF-8 encoding/decoding benchmarks.
     */
    private static void runSafeEncoderBenchmarks() throws RunnerException {
        System.out.println("Running SafeEncoder benchmarks...");
        new Runner(prepareOptions()
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .include(".*SafeEncoderBenchmark.*")
                .build())
                .run();
    }

    /**
     * Run a specific benchmark by name.
     * 
     * @param benchmarkPattern Benchmark name pattern (e.g., "CRC16Benchmark.getSlotString")
     */
    private static void runSpecificBenchmark(String benchmarkPattern) throws RunnerException {
        System.out.println("Running benchmark: " + benchmarkPattern);
        new Runner(prepareOptions()
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .include(".*" + benchmarkPattern + ".*")
                .build())
                .run();
    }

    /**
     * Prepare common JMH options.
     * 
     * Configuration:
     * - 1 fork (JVM instance)
     * - 3 warmup iterations (1 second each)
     * - 5 measurement iterations (1 second each)
     * - Single thread
     * - 10-second timeout per iteration
     * - Results saved to benchmarks.json and benchmark.log
     */
    private static ChainedOptionsBuilder prepareOptions() {
        return new OptionsBuilder()
                .forks(1)
                .warmupIterations(5)
                .warmupTime(TimeValue.seconds(2))
                .measurementIterations(5)
                .measurementTime(TimeValue.seconds(2))
                .threads(1)
                .timeout(TimeValue.seconds(10))
                .resultFormat(ResultFormatType.JSON)
                .result("benchmarks.json")
                .output("benchmark.log");
    }

    /**
     * Quick benchmark with minimal iterations (for development/testing).
     */
    private static void runQuickBenchmark(String benchmarkPattern) throws RunnerException {
        System.out.println("Running QUICK benchmark: " + benchmarkPattern);
        new Runner(new OptionsBuilder()
                .forks(1)
                .warmupIterations(1)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(1)
                .measurementTime(TimeValue.seconds(5))
                .threads(1)
                .timeout(TimeValue.seconds(5))
                .mode(Mode.AverageTime)
                .timeUnit(TimeUnit.NANOSECONDS)
                .include(".*" + benchmarkPattern + ".*")
                .build())
                .run();
    }
}
