package redis.clients.jedis.benchmark.csc;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Manual IDE entry point for the CSC workload suite.
 * <p>
 * Mirrors the {@code go-redis} CSC sweep — 7 workloads × {@code (none, csc)} × {@code (cold, warm)}
 * — and produces {@code benchmarks.json} alongside {@code benchmark.log} so results can be plotted
 * the same way.
 * <p>
 * Maven invocations from {@code CSC Jedis.md}:
 * 
 * <pre>
 * mvn -Pjmh test -Djmh.includes="csc\\..*Benchmark"          # full sweep
 * mvn -Pjmh test -Djmh.includes="csc\\.HotKeyZipfianBenchmark" # single workload
 * </pre>
 */
public class CscMain {

  public static void main(String... args) throws RunnerException {
    new Runner(new OptionsBuilder().include(".*benchmark\\.csc\\..*ReadHeavy99Benchmark.*")
        //.resultFormat(ResultFormatType.JSON).result("benchmarks-csc.json")
        /*.output("benchmark-csc.log")*/.build()).run();
  }
}
