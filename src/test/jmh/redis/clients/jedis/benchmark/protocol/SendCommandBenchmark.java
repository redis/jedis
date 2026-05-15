package redis.clients.jedis.benchmark.protocol;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.util.RedisOutputStream;

import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark for {@link Protocol#sendCommand} — measures RESP command encoding cost without any
 * I/O. Output goes to a discarding stream wrapped by {@link RedisOutputStream}; internal-buffer
 * flushes still happen periodically (when the 8 KB buffer fills) but cost nothing because the sink
 * is a no-op.
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
public class SendCommandBenchmark {

  private static final int BATCH = 100;

  private static final byte[] KEY = "123456789".getBytes();
  private static final byte[] VAL = "FooBar".getBytes();

  private RedisOutputStream out;

  @Setup(Level.Iteration)
  public void setupIteration() {
    out = new RedisOutputStream(NullOutputStream.INSTANCE);
  }

  /**
   * Full production path: build {@link CommandArguments} per call, then {@code sendCommand}. The
   * score includes both the args-builder cost and the encode cost.
   */
  @Benchmark
  @OperationsPerInvocation(BATCH)
  public void measureSendCommand(Blackhole blackhole) throws Exception {
    for (int i = 0; i < BATCH; i++) {
      Protocol.sendCommand(out, new CommandArguments(Protocol.Command.SET).key(KEY).add(VAL));
    }
    blackhole.consume(out);
  }

  /** Discards all output. Removes I/O cost from the measurement. */
  private static final class NullOutputStream extends OutputStream {
    static final NullOutputStream INSTANCE = new NullOutputStream();

    @Override
    public void write(int b) {
    }

    @Override
    public void write(byte[] b) {
    }

    @Override
    public void write(byte[] b, int off, int len) {
    }
  }
}
